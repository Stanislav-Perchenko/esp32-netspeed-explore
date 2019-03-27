/*
 * udp_transmit.h
 *
 *  Created on: Mar 20, 2019
 *      Author: stanislav.perchenko
 */

#ifndef MAIN_UDP_TRANSMIT_H_
#define MAIN_UDP_TRANSMIT_H_

#ifdef __cplusplus
extern "C" {
#endif

#include <esp_log.h>
#include <esp_system.h>
#include <esp_http_server.h>

#include "lwip/sockets.h"

#include "freertos/FreeRTOS.h"
#include "freertos/task.h"
#include "freertos/semphr.h"
#include "freertos/event_groups.h"

#include "state_defs.h"
#include "cJSON.h"

esp_err_t executeStartUDPTransmitRequest(ip4_addr_t *remoteIpAddress, uint16_t port, uint16_t pkgSize, app_state_t *pAppState, cJSON *jResp);

#ifdef __cplusplus
}
#endif

#endif /* MAIN_UDP_TRANSMIT_H_ */
