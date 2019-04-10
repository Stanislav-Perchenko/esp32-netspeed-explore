/*
 * state_defs.h
 *
 *  Created on: Mar 19, 2019
 *      Author: stanislav.perchenko
 */

#ifndef MAIN_STATE_DEFS_H_
#define MAIN_STATE_DEFS_H_

#ifdef __cplusplus
extern "C" {
#endif

#include "freertos/FreeRTOS.h"
#include "freertos/semphr.h"
#include "freertos/event_groups.h"

typedef enum {
	PROTOCOL_UNDEFINED = 0,
	PROTOCOL_UDP = 1,
	PROTOCOL_TCP = 2,
} comm_protocol_t;

typedef enum {
	ACTION_IDLE = 0,
	ACTION_TRANS = 1,
	ACTION_RCV = 2,
} app_action_t;




typedef struct {
	uint32_t speed;
	uint32_t n_pkg_transm;
	uint32_t n_pkg_rcv_total;
	uint32_t n_pkg_rcv_failed;
	SemaphoreHandle_t accessLock;////////////////////////////////////////////
} app_work_statistics;

typedef union {
	uint32_t *numbers;
	uint8_t *bytes;
} int32_char_union_t;

typedef struct {
	size_t zise;
	int32_char_union_t data;
} transm_data_buffer_t;

typedef struct {
	uint32_t clientIpAddr;
	int socketId;
	//-------  Controls and status  ---------------
	SemaphoreHandle_t 	accessLock;//////////////////////////////////////////
	TaskHandle_t 		taskHandle;
	bool 				isAlive;
	EventGroupHandle_t	groupTaskAlive;//////////////////////////////////////
	SemaphoreHandle_t	semCmdFinish;////////////////////////////////////////
	//-------  Business-logic-related  ------------
	comm_protocol_t			protocol;
	app_action_t 			action;
	uint16_t 				port;
	transm_data_buffer_t 	transmit_data;
} network_task_t;


typedef struct {
	app_work_statistics statistics;
	network_task_t netwTask;
} app_state_t;

#define NETWORK_TASK_FLAG_STARTED BIT0
#define NETWORK_TASK_FLAG_FINISHED BIT1

char *appActionAsString(app_action_t action);
char *commProtocolAsString(comm_protocol_t protocol);

bool isNetworkTaskAlive(network_task_t *nTask);

//TODO Implement this
//bool isNetworkTaskWorking(network_task_t *nTask);

void clearStatisticsStructure(app_work_statistics *stats);


#ifdef __cplusplus
}
#endif


#endif /* MAIN_STATE_DEFS_H_ */
