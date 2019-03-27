/*
 * http_control.h
 *
 *  Created on: Mar 19, 2019
 *      Author: stanislav.perchenko
 */

#ifndef MAIN_HTTP_CONTROL_H_
#define MAIN_HTTP_CONTROL_H_

#ifdef __cplusplus
extern "C" {
#endif

#include <esp_system.h>
#include "esp_http_server.h"
#include "lwip/sockets.h"

extern char *HTTP_CONTROL_SERVER_NAME;

typedef struct {
	uint8_t sa_len;
	sa_family_t sa_family;
	in_port_t sa_port;
	uint8_t some_data[12];
	uint32_t some_value1;
	uint32_t sa_ip4;
	uint32_t some_value2;
} lwip_open_socket_addr_t;


esp_err_t http_get_peer_addr_from_request(httpd_req_t *req, ip4_addr_t *addr);

esp_err_t http_control_handler_status(httpd_req_t *req);
esp_err_t http_control_handler_stop(httpd_req_t *req);
esp_err_t http_control_handler_start_transmit(httpd_req_t *req);
esp_err_t http_control_handler_start_receive(httpd_req_t *req);


#ifdef __cplusplus
}
#endif

#endif /* MAIN_HTTP_CONTROL_H_ */
