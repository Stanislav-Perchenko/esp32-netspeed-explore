/*
 * udp_transmit.c
 *
 *  Created on: Mar 20, 2019
 *      Author: stanislav.perchenko
 */

#include "lwip/err.h"
#include "lwip/sockets.h"
#include "lwip/sys.h"
#include <lwip/netdb.h>

#include "udp_transmit.h"
#include "network_utils.h"
#include "mean_filter.h"
#include "rc4.h"

static const char *TAG="UDP_TRANSMIT";

void buildDataPackage(transm_data_buffer_t *pkg, uint32_t index) {
	uint8_t *p = pkg->data.bytes;
	uint32_t tmp;
	index ++;

	/*tmp = (uint32_t) pkg->zise;
	ESP_LOGI(TAG, " tmp size = %d", tmp);
	p[3] = (uint8_t)(tmp & 0xFF);
	tmp = tmp >> 8;
	p[2] = (uint8_t)(tmp & 0xFF);
	tmp = tmp >> 8;
	p[1] = (uint8_t)(tmp & 0xFF);
	tmp = tmp >> 8;
	p[0] = (uint8_t)(tmp & 0xFF);

	tmp = index;
	p[7] = (uint8_t)(tmp & 0xFF);
	tmp = tmp >> 8;
	p[6] = (uint8_t)(tmp & 0xFF);
	tmp = tmp >> 8;
	p[5] = (uint8_t)(tmp & 0xFF);
	tmp = tmp >> 8;
	p[4] = (uint8_t)(tmp & 0xFF);

	tmp = (uint32_t) pkg->zise;
	p[11] = (uint8_t)(tmp & 0xFF);
	tmp = tmp >> 8;
	p[10] = (uint8_t)(tmp & 0xFF);
	tmp = tmp >> 8;
	p[9] = (uint8_t)(tmp & 0xFF);
	tmp = tmp >> 8;
	p[8] = (uint8_t)(tmp & 0xFF);*/

	pkg->data.numbers[0] = (uint32_t) pkg->zise;
	pkg->data.numbers[1] = index;

	uint8_t key[6];
	uint32_t inv_idx = index ^ 0xFFFF;
	key[0] = (uint8_t)index;
	index = index >> 8;
	key[1] = (uint8_t)index;
	index = index >> 8;
	key[2] = (uint8_t)index;
	key[3] = (uint8_t)inv_idx;
	inv_idx = inv_idx >> 8;
	key[4] = (uint8_t)inv_idx;
	inv_idx = inv_idx >> 8;
	key[5] = (uint8_t)inv_idx;
	rc4_init(key, 6);
	rc4_output(pkg->data.bytes, 8, pkg->zise - 8);

}

static void task_worker(void *arg) {
	int socket_id;
	app_state_t *pAppState = (app_state_t *) arg;
	uint64_t speed_acc;
	uint32_t pkg_index;

	vTaskDelay(600 / portTICK_PERIOD_MS);

	network_task_t *netwTask = &pAppState->netwTask;

	xSemaphoreTake(netwTask->accessLock, portMAX_DELAY);
	netwTask->isAlive = true;
	socket_id = netwTask->socketId;


	struct sockaddr_in destAddr = {0};
	destAddr.sin_addr.s_addr = netwTask->clientIpAddr;
	destAddr.sin_family = AF_INET;
	destAddr.sin_port = lwip_htons(netwTask->port);
	char destAddrText[IP_ADD_TEXT_LEN];
	ip2str((ip4_addr_t *)(&destAddr.sin_addr), destAddrText, IP_ADD_TEXT_LEN);
	xSemaphoreGive(netwTask->accessLock);

	ESP_LOGI(TAG, "---> Start sending data to %s:%d", destAddrText, destAddr.sin_port);

	unsigned long timestamp = getUsecTimestamp();
	unsigned long t_end;
	mean_filter16_t *flt16_speed = flt16_buildFilter16(128);
	pkg_index = 0;
	buildDataPackage(&netwTask->transmit_data, pkg_index);
	xEventGroupSetBits(netwTask->groupTaskAlive, NETWORK_TASK_FLAG_STARTED);
	while(xSemaphoreTake(netwTask->semCmdFinish, 0) != pdTRUE) {

		int snd_err = lwip_sendto(socket_id, netwTask->transmit_data.data.bytes, netwTask->transmit_data.zise, 0, (struct sockaddr *)&destAddr, sizeof(destAddr));
		if (snd_err !=netwTask->transmit_data.zise ) {
			vTaskDelay(3 / portTICK_PERIOD_MS);
			ESP_LOGI(TAG, "<~~~ Error send package[%d]: %d", pkg_index, snd_err);
			continue;
		}
		t_end = getUsecTimestamp();
		flt16_add(flt16_speed, (int16_t)(t_end - timestamp));
		timestamp = t_end;
		buildDataPackage(&netwTask->transmit_data, ++pkg_index);


		xSemaphoreTake(pAppState->statistics.accessLock, portMAX_DELAY);
		pAppState->statistics.n_pkg_transm = pkg_index;
		if (flt16_speed->size >= 48) {
			speed_acc = netwTask->transmit_data.zise * flt16_speed->size;
			speed_acc *= 8000000;
			speed_acc /= flt16_sum(flt16_speed);
			pAppState->statistics.speed = (uint32_t)speed_acc;
		}
		xSemaphoreGive(pAppState->statistics.accessLock);

		if (pkg_index % 50 == 0) {
			ESP_LOGI(TAG, "<--- 50 packages of %d bytes were sent. Nsent=%d", netwTask->transmit_data.zise, pkg_index);
		}

	}
	flt16_clearFilter16(flt16_speed);
	ESP_LOGE(TAG, "\tExit from task cycle");
	xSemaphoreTake(netwTask->accessLock, portMAX_DELAY);
	lwip_shutdown_r(netwTask->socketId, 0);
	lwip_close(netwTask->socketId);
	netwTask->isAlive = false;
	netwTask->taskHandle = NULL;
	xSemaphoreGive(netwTask->accessLock);
	ESP_LOGE(TAG, "\tExit from task");
	xEventGroupSetBits(netwTask->groupTaskAlive, NETWORK_TASK_FLAG_FINISHED);
	vTaskDelete(NULL);
}


esp_err_t executeStartUDPTransmitRequest(ip4_addr_t *remoteIpAddress, uint16_t port, uint16_t pkgSize, app_state_t *pAppState, cJSON *jResp) {
	if (isNetworkTaskAlive(&pAppState->netwTask)) {
		addErrorToResponse(jResp, ESP_ERR_INVALID_STATE, "Another instance of task is alive");
		return ESP_ERR_INVALID_STATE;
	} else {
		clearStatisticsStructure(&pAppState->statistics);

	}


	xSemaphoreTake(pAppState->netwTask.accessLock, portMAX_DELAY);


	pAppState->netwTask.socketId = lwip_socket(AF_INET, SOCK_DGRAM, IPPROTO_IP);
	if (pAppState->netwTask.socketId < 0) {
		addErrorToResponse(jResp, ESP_ERR_NO_MEM, "Cannot create socket object");
		return ESP_ERR_NO_MEM;
	}


	pAppState->netwTask.protocol = PROTOCOL_UDP;
	pAppState->netwTask.action = ACTION_TRANS;
	pAppState->netwTask.port = port;
	pAppState->netwTask.clientIpAddr = remoteIpAddress->addr;
	uint8_t rem = pkgSize % 4;
	size_t fin_size = pkgSize;
	if (rem > 0) fin_size += (4 - rem);
	pAppState->netwTask.transmit_data.zise = fin_size;
	pAppState->netwTask.transmit_data.data.bytes = malloc(fin_size);
	if (pAppState->netwTask.transmit_data.data.bytes) {
		pAppState->netwTask.transmit_data.data.numbers = (uint32_t *) pAppState->netwTask.transmit_data.data.bytes;
		uint32_t i, v, n = pAppState->netwTask.transmit_data.zise/4;
		for (i=0, v=1; i<n; i++, v++) pAppState->netwTask.transmit_data.data.numbers[i] = v;
	} else {
		addErrorToResponse(jResp, ESP_ERR_NO_MEM, "Cannot allocate transmission buffer");
		return ESP_ERR_NO_MEM;
	}

	xSemaphoreTake(pAppState->netwTask.semCmdFinish, 0); //Ensure semaphore state
	xEventGroupClearBits(pAppState->netwTask.groupTaskAlive, NETWORK_TASK_FLAG_STARTED | NETWORK_TASK_FLAG_FINISHED); //Ensure cleared state
	pAppState->netwTask.isAlive = false;

	BaseType_t result = xTaskCreatePinnedToCore(
			&task_worker,
			"udp-transmitter",
			4096,
			pAppState,
			tskIDLE_PRIORITY+2,
			&pAppState->netwTask.taskHandle,
			tskNO_AFFINITY);

	xSemaphoreGive(pAppState->netwTask.accessLock);

	if (result != pdPASS) {
		addErrorToResponse(jResp, ESP_ERR_NO_MEM, "Failed to Start transmitter task");
		return ESP_ERR_NO_MEM;
	}

	// Wait for the thread start completion
	xEventGroupWaitBits(pAppState->netwTask.groupTaskAlive, NETWORK_TASK_FLAG_STARTED, true, true, portMAX_DELAY);
	return ESP_OK;
}
