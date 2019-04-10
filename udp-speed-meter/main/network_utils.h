/*
 * network_utils.h
 *
 *  Created on: Mar 19, 2019
 *      Author: stanislav.perchenko
 */

#ifndef MAIN_NETWORK_UTILS_H_
#define MAIN_NETWORK_UTILS_H_

#ifdef __cplusplus
extern "C" {
#endif

#include <esp_system.h>
#include "tcpip_adapter.h"
#include "cJSON.h"
#include "state_defs.h"

#define IP_ADD_TEXT_LEN 16

#define RESPONSE_PARAM_SUCCESS "success"

typedef struct {
	char *txt_ip;
	char *txt_gw;
	char *txt_netmask;
} ip_info_text_t;

ip_info_text_t *build_ip_info_text();

char *ip2str(ip4_addr_t *ip, char *buf, uint8_t buf_len);

esp_err_t getIpAddressTextForm(tcpip_adapter_if_t tcpip_if, tcpip_adapter_ip_info_t *ip_info, ip_info_text_t *dst);

void clearIpAddress(tcpip_adapter_ip_info_t *ip_info, ip_info_text_t *dst);

cJSON *buildTopLevelOKResponseObject();

void addErrorToResponse(cJSON *jResp, esp_err_t esp_err_code, char *err_message);

void putStatisticsInJson(app_work_statistics *statistics, app_action_t action, cJSON *jResp, const char *name);

esp_err_t http_get_param_protocol(char *params_buf, cJSON *jResp, comm_protocol_t *paramProtocol);
esp_err_t http_get_param_port(char *params_buf, cJSON *jResp, uint16_t *paramPort);
esp_err_t http_get_param_pkg_size(char *params_buf, cJSON *jResp, uint16_t *paramPkgSize);

unsigned long getUsecTimestamp();


#ifdef __cplusplus
}
#endif

#endif /* MAIN_NETWORK_UTILS_H_ */
