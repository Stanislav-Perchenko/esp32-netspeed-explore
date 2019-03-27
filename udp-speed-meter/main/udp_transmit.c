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

static const char *TAG="UDP_TRANSMIT";

static void task_worker(void *arg) {
	int socket_id;
	int32_char_union_t *data;
	size_t data_size;
	app_state_t *pAppState = (app_state_t *) arg;

	vTaskDelay(500 / portTICK_PERIOD_MS);

	network_task_t *netwTask = &pAppState->netwTask;

	xSemaphoreTake(netwTask->accessLock, portMAX_DELAY);
	netwTask->isAlive = true;
	socket_id = netwTask->socketId;
	data = &netwTask->transmit_data.data;
	data_size = netwTask->transmit_data.zise;

	struct sockaddr_in destAddr = {0};

	destAddr.sin_addr.s_addr = netwTask->clientIpAddr;
	destAddr.sin_family = AF_INET;
	destAddr.sin_port = netwTask->port;

	xSemaphoreGive(netwTask->accessLock);

	struct timeval tv_start, tv_end;

	xEventGroupSetBits(netwTask->groupTaskAlive, NETWORK_TASK_FLAG_STARTED);
	while(xSemaphoreTake(netwTask->semCmdFinish, 0) != pdTRUE) {

		ESP_LOGI(TAG, "---> Start sending data");
		uint32_t i, n = data_size / 4, origin = pAppState->statistics.n_pkg_transm * data_size;
		for (i=0; i<n; i++) {
			data->numbers[i] = origin + i;
		}

		gettimeofday(&tv_start, NULL);
		int snd_err = lwip_sendto(socket_id, data->bytes, data_size, 0, (struct sockaddr *)&destAddr, sizeof(destAddr));
		gettimeofday(&tv_end, NULL);
		uint32_t dt = (uint32_t)(tv_end.tv_sec - tv_start.tv_sec)*1000000 + (uint32_t)(tv_end.tv_usec - tv_start.tv_usec);
		ESP_LOGI(TAG, "<--- %u bytes were sent in %u us. Send result = %d", data_size, dt, snd_err);
		vTaskDelay(1500 / portTICK_PERIOD_MS);
		xSemaphoreTake(pAppState->statistics.accessLock, portMAX_DELAY);
		pAppState->statistics.n_pkg_transm += 10;
		//pAppState->statistics.speed ++;
		xSemaphoreGive(pAppState->statistics.accessLock);

	}
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
