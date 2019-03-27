/*
 * network_utils.c
 *
 *  Created on: Mar 19, 2019
 *      Author: stanislav.perchenko
 */

#include <stdlib.h>
#include <string.h>
#include "network_utils.h"
#include "esp_http_server.h"


ip_info_text_t *build_ip_info_text() {
	ip_info_text_t *ret = (ip_info_text_t *) calloc(sizeof(ip_info_text_t), 1);
	if (!ret) return NULL;
	ret->txt_ip = malloc(IP_ADD_TEXT_LEN);
	if (!ret->txt_ip) return NULL;
	memset(ret->txt_ip, 0, IP_ADD_TEXT_LEN);

	ret->txt_gw = malloc(IP_ADD_TEXT_LEN);
	if (!ret->txt_gw) return NULL;
	memset(ret->txt_gw, 0, IP_ADD_TEXT_LEN);

	ret->txt_netmask = malloc(IP_ADD_TEXT_LEN);
	if (!ret->txt_netmask) return NULL;
	memset(ret->txt_netmask, 0, IP_ADD_TEXT_LEN);
	return ret;
}


char *ip2str(ip4_addr_t *ip, char *buf, uint8_t buf_len) {
	uint32_t addr = ip->addr;
	snprintf(buf, buf_len, "%d.%d.%d.%d", addr & 0xFF, (addr >> 8) & 0xFF, (addr >> 16) & 0xFF, (addr >> 24) & 0xFF );
	return buf;
}


esp_err_t getIpAddressTextForm(tcpip_adapter_if_t tcpip_if, tcpip_adapter_ip_info_t *ip_info, ip_info_text_t *dst) {

	esp_err_t result = tcpip_adapter_get_ip_info(tcpip_if, ip_info);
	if (result == ESP_OK) {
		ip2str(&ip_info->ip, dst->txt_ip, IP_ADD_TEXT_LEN);
		ip2str(&ip_info->gw, dst->txt_gw, IP_ADD_TEXT_LEN);
		ip2str(&ip_info->netmask, dst->txt_netmask, IP_ADD_TEXT_LEN);
	}
	return result;
}


void clearIpAddress(tcpip_adapter_ip_info_t *ip_info, ip_info_text_t *ip_text) {
	if (ip_info) {
		ip_info->ip.addr = IPADDR_NONE;
		ip_info->gw.addr = IPADDR_NONE;
		ip_info->netmask.addr = IPADDR_NONE;
	}
	if (ip_text->txt_ip) {
		memset(ip_text->txt_ip, 0, IP_ADD_TEXT_LEN);
	}
	if (ip_text->txt_gw) {
		memset(ip_text->txt_gw, 0, IP_ADD_TEXT_LEN);
	}
	if (ip_text->txt_netmask) {
		memset(ip_text->txt_netmask, 0, IP_ADD_TEXT_LEN);
	}
}

cJSON *buildTopLevelOKResponseObject() {
	cJSON *jResp = cJSON_CreateObject();
	cJSON_AddTrueToObject(jResp, RESPONSE_PARAM_SUCCESS);
	return jResp;
}


void addErrorToResponse(cJSON *jResp, esp_err_t esp_err_code, char *err_message) {
	cJSON_ReplaceItemInObject(jResp, RESPONSE_PARAM_SUCCESS, cJSON_CreateBool(false));
	cJSON *jErrors = cJSON_GetObjectItem(jResp, "errors");
	if (!jErrors) {
		jErrors = cJSON_AddArrayToObject(jResp, "errors");
	}
	cJSON *jE = cJSON_CreateObject();
	cJSON_AddNumberToObject(jE, "code", esp_err_code);
	cJSON_AddStringToObject(jE, "message", err_message);
	cJSON_AddItemToArray(jErrors, jE);
}

void putStatisticsInJson(app_work_statistics *statistics, app_action_t action, cJSON *jObj, const char *name) {
	cJSON *jStats = cJSON_CreateObject();
	xSemaphoreTake(statistics->accessLock, portMAX_DELAY);
	cJSON_AddNumberToObject(jStats, "speed", statistics->speed);
	if (action == ACTION_TRANS) {
		cJSON_AddNumberToObject(jStats, "n_pkg_transm", statistics->n_pkg_transm);
	} else if (action == ACTION_RCV) {
		cJSON_AddNumberToObject(jStats, "n_pkg_rcv_total", statistics->n_pkg_rcv_total);
		cJSON_AddNumberToObject(jStats, "n_pkg_rcv_failed", statistics->n_pkg_rcv_failed);
	}
	xSemaphoreGive(statistics->accessLock);
	cJSON_AddItemToObject(jObj, name, jStats);
}

char *extractParamErrToString(esp_err_t error) {
	switch(error) {
		case ESP_ERR_NOT_FOUND:
			return "ESP_ERR_NOT_FOUND";
		case ESP_ERR_INVALID_ARG:
			return "ESP_ERR_INVALID_ARG";
		case ESP_ERR_HTTPD_RESULT_TRUNC:
			return "ESP_ERR_HTTPD_RESULT_TRUNC";
		default:
			return "???";
	}
}

esp_err_t http_get_param_protocol(char *params_buf, cJSON *jResp, comm_protocol_t *paramProtocol) {
	char param[5];
	esp_err_t err = httpd_query_key_value(params_buf, "protocol", param, sizeof(param));
	if (err == ESP_OK) {
		if (strcmp(param, "TCP") == 0 || strcmp(param, "tcp") == 0) {
			(*paramProtocol) = PROTOCOL_TCP;
		} else if (strcmp(param, "UDP") == 0 || strcmp(param, "udp") == 0) {
			(*paramProtocol) = PROTOCOL_UDP;
		} else {
			(*paramProtocol) = PROTOCOL_UNDEFINED;
			addErrorToResponse(jResp, 0, "Bad value for the 'protocol' parameter.");
			err = ESP_ERR_HTTPD_BASE;
		}
	} else {
		char err_text[75];
		snprintf(err_text, sizeof(err_text), "Can't extract 'protocol' parameter - %s", extractParamErrToString(err));
		addErrorToResponse(jResp, err, err_text);
	}
	return err;
}


esp_err_t http_get_param_port(char *params_buf, cJSON *jResp, uint16_t *paramPort) {
	char param[7];
	esp_err_t err = httpd_query_key_value(params_buf, "port", param, sizeof(param));
	if (err == ESP_OK) {
		char *end_ptr;
		unsigned long v = strtoul(param, &end_ptr, 10);
		if (v > 0) {
			(*paramPort) = (v < 0xFFFF) ? (uint16_t)v : 0xFFFF;
		} else {
			addErrorToResponse(jResp, 0, "Bad value for the 'port' parameter.");
			err = ESP_ERR_HTTPD_BASE;
		}
	} else {
		char err_text[75];
		snprintf(err_text, sizeof(err_text), "Can't extract 'port' parameter - %s", extractParamErrToString(err));
		addErrorToResponse(jResp, err, err_text);
	}
	return err;
}

esp_err_t http_get_param_pkg_size(char *params_buf, cJSON *jResp, uint16_t *paramPkgSize) {
	char param[7];
	esp_err_t err = httpd_query_key_value(params_buf, "pkg_size", param, sizeof(param));
	if (err == ESP_OK) {
		char *endptr;
		unsigned long v = strtoul(param, &endptr, 10);
		if (v > 0) {
			(*paramPkgSize) = (v < 0xFFFF) ? (uint16_t)v : 0xFFFF;
		} else {
			addErrorToResponse(jResp, 0, "Bad value for the 'pkg_size' parameter.");
			err = ESP_ERR_HTTPD_BASE;
		}
	} else {
		char err_text[75];
		snprintf(err_text, sizeof(err_text), "Can't extract 'pkg_size' parameter - %s", extractParamErrToString(err));
		addErrorToResponse(jResp, err, err_text);
	}
	return err;
}

