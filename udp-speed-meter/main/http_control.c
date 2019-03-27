/*
 * http_control.c
 *
 *  Created on: Mar 19, 2019
 *      Author: stanislav.perchenko
 */

#include <esp_log.h>
#include "http_control.h"
#include "state_defs.h"
#include "cJSON.h"
#include "freertos/FreeRTOS.h"
#include "network_utils.h"
#include "udp_transmit.h"

static const char *TAG="HTTP_CONTROL";

char *HTTP_CONTROL_SERVER_NAME = CONFIG_HTTP_SERVER_NAME;


esp_err_t send_json_response(httpd_req_t *req, const char *http_status, cJSON *jResp);

esp_err_t http_control_handler_status(httpd_req_t *req) {
	ESP_LOGI(TAG, "---> %s %s", http_method_str(req->method), req->uri);


				ip4_addr_t peer_addr = {0};
				http_get_peer_addr_from_request(req, &peer_addr);

	app_state_t *pAppState = (app_state_t *) req->user_ctx;

	cJSON *jStatus = cJSON_CreateObject();
	app_action_t nowAction;

	xSemaphoreTake(pAppState->netwTask.accessLock, portMAX_DELAY);
	nowAction = pAppState->netwTask.isAlive ? pAppState->netwTask.action : ACTION_IDLE;
	cJSON_AddStringToObject(jStatus, "action", appActionAsString(nowAction));
	if (nowAction != ACTION_IDLE) {
		cJSON_AddStringToObject(jStatus, "protocol", commProtocolAsString(pAppState->netwTask.protocol));
		cJSON_AddNumberToObject(jStatus, "port", pAppState->netwTask.port);
		if (nowAction == ACTION_TRANS) {
			cJSON_AddNumberToObject(jStatus, "pkg_size", pAppState->netwTask.transmit_data.zise);
		}
	}
	xSemaphoreGive(pAppState->netwTask.accessLock);
#ifdef CONFIG_WIFI_OPERATION_MODE_AP
	cJSON_AddStringToObject(jStatus, "conn_mode", "AP");
#else
	cJSON_AddStringToObject(jStatus, "conn_mode", "STA");
#endif

	cJSON *jData = cJSON_CreateObject();

	cJSON_AddItemToObject(jData, "status", jStatus);

	putStatisticsInJson(&pAppState->statistics, nowAction, jData, "statistics");


	cJSON *jResp = buildTopLevelOKResponseObject();
	cJSON_AddItemToObject(jResp, "data", jData);
	send_json_response(req, HTTPD_200, jResp);

	cJSON_Delete(jResp);
	return ESP_OK;
}



esp_err_t http_control_handler_stop(httpd_req_t *req) {
	char *http_status = HTTPD_200;

	ESP_LOGI(TAG, "---> %s %s", http_method_str(req->method), req->uri);
	app_state_t *pAppState = (app_state_t *) req->user_ctx;
	cJSON *jResp = buildTopLevelOKResponseObject();

	bool isAlive;
	xSemaphoreTake(pAppState->netwTask.accessLock, portMAX_DELAY);
	isAlive = pAppState->netwTask.isAlive;
	xSemaphoreGive(pAppState->netwTask.accessLock);
	if (isAlive) {
		xSemaphoreGive(pAppState->netwTask.semCmdFinish);
		EventBits_t bits = xEventGroupWaitBits(pAppState->netwTask.groupTaskAlive, NETWORK_TASK_FLAG_FINISHED, true, true, 5000/portTICK_PERIOD_MS);
		if (!(bits & NETWORK_TASK_FLAG_FINISHED)) {
			http_status = HTTPD_500;
			addErrorToResponse(jResp, 0, "Networking task was not stopped in 5s");
		}
	} else {
		http_status = HTTPD_423;
		addErrorToResponse(jResp, 0, "Networking task is not started");
	}

	send_json_response(req, http_status, jResp);
	cJSON_Delete(jResp);
	return ESP_OK;
}



esp_err_t executeStartRequest(comm_protocol_t protocol, uint16_t port, uint16_t pkgSize, app_state_t *pAppState, cJSON *jResp) {
	ESP_LOGI(TAG, "---> Executing 'START' command for protocol %s on port %u with Package Size = %u", commProtocolAsString(protocol), port, pkgSize);
	//TODO Implement this properly !!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!
	char message[75];
	sprintf(message, "'Start transmission' API for the protocol %s is not implemented yet.", commProtocolAsString(protocol));
	addErrorToResponse(jResp, 0, message);
	return ESP_ERR_HTTPD_RESP_SEND;
}

esp_err_t http_control_handler_start_transmit(httpd_req_t *req) {
	size_t params_buf_len;
	char *params_buf;
	char *http_status = HTTPD_200;
	esp_err_t err;

	ESP_LOGI(TAG, "---> %s %s", http_method_str(req->method), req->uri);
	app_state_t *pAppState = (app_state_t *) req->user_ctx;
	cJSON *jResp = buildTopLevelOKResponseObject();

	params_buf_len = httpd_req_get_url_query_len(req);
	if (params_buf_len > 0) {
		params_buf_len ++;
		params_buf = malloc(params_buf_len);
		memset(params_buf, 0, params_buf_len);
		err = httpd_req_get_url_query_str(req, params_buf, params_buf_len);
		if (err == ESP_OK) {
			ESP_LOGI(TAG, "Request params: %s", params_buf);

			comm_protocol_t paramProtocol = PROTOCOL_UNDEFINED;
			uint16_t paramPort = 0;
			uint16_t paramPkgSize = 0;

			err = ESP_OK;
			err |= http_get_param_protocol(params_buf, jResp, &paramProtocol);
			err |= http_get_param_port(params_buf, jResp, &paramPort);
			err |= http_get_param_pkg_size(params_buf, jResp, &paramPkgSize);

			if (err == ESP_OK) {
				ip4_addr_t peer_addr = {0};
				http_get_peer_addr_from_request(req, &peer_addr);

				if (paramProtocol == PROTOCOL_UDP) {
					err = executeStartUDPTransmitRequest(&peer_addr, paramPort, paramPkgSize, pAppState, jResp);
				} else {
					err = executeStartRequest(paramProtocol, paramPort, paramPkgSize, pAppState, jResp);
				}
				http_status = (err == ESP_OK) ? HTTPD_200 : HTTPD_400;
			} else {
				http_status = HTTPD_400;
			}

		} else {
			http_status = HTTPD_400;
			addErrorToResponse(jResp, err, "Error extract params from request");
		}
		free(params_buf);
	} else {
		http_status = HTTPD_400;
		addErrorToResponse(jResp, 0, "No parameters in request");
	}

	send_json_response(req, http_status, jResp);
	cJSON_Delete(jResp);
	return ESP_OK;
}

esp_err_t http_control_handler_start_receive(httpd_req_t *req) {
	return ESP_OK;
}








esp_err_t send_json_response(httpd_req_t *req, const char *http_status, cJSON *jResp) {
	const char *resp_str = cJSON_Print(jResp);
	httpd_resp_set_status(req, http_status);
	char contLenText[15];
	size_t contLen = strlen(resp_str);
	sprintf(contLenText, "%u", contLen);
	httpd_resp_set_hdr(req, "Content-Length", contLenText);
	httpd_resp_set_hdr(req, "Content-Type", "application/json");
	httpd_resp_set_hdr(req, "Server", HTTP_CONTROL_SERVER_NAME);
	esp_err_t result = httpd_resp_send(req, resp_str, contLen);
	if (result == ESP_OK) {
		ESP_LOGI(TAG, "<--- %s - %s\n%s", http_status, req->uri, resp_str);
	} else {
		ESP_LOGE(TAG, "Error send HTTP response for %s - %s", req->uri, resp_str);
	}
	return result;
}


esp_err_t http_get_peer_addr_from_request(httpd_req_t *req, ip4_addr_t *dst) {
	int soc_fd = httpd_req_to_sockfd(req);
	if (soc_fd < 0) {
		return ESP_ERR_HTTPD_BASE;
	}
	lwip_open_socket_addr_t sa = {0};
	uint32_t sa_len;
	lwip_getpeername_r(soc_fd, &sa, &sa_len);
	dst->addr = sa.sa_ip4;
	ESP_LOGI(TAG, "Peer IPv4 addr - %s", ip4addr_ntoa(dst));
	return ESP_OK;
}



