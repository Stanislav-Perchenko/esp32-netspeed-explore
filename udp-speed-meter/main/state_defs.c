/*
 * state_defs.c
 *
 *  Created on: Mar 20, 2019
 *      Author: stanislav.perchenko
 */

#include <esp_system.h>
#include "state_defs.h"

static char *STR_IDLE	= "IDLE";
static char *STR_RCV 	= "RCV";
static char *STR_TRANS 	= "TRANS";

static char *STR_UNDEF 	= "UNDEFINED";
static char *STR_TCP 	= "TCP";
static char *STR_UDP 	= "UDP";

char *appActionAsString(app_action_t action) {
	switch(action) {
		case ACTION_IDLE:
			return STR_IDLE;
		case ACTION_RCV:
			return STR_RCV;
		case ACTION_TRANS:
			return STR_TRANS;
		default:
			return NULL;
	}
}

char *commProtocolAsString(comm_protocol_t protocol) {
	switch(protocol) {
		case PROTOCOL_UNDEFINED:
			return STR_UNDEF;
		case PROTOCOL_TCP:
			return STR_TCP;
		case PROTOCOL_UDP:
			return STR_UDP;
		default:
			return NULL;
	}
}

bool isNetworkTaskAlive(network_task_t *nTask) {
	bool alive;
	xSemaphoreTake(nTask->accessLock, portMAX_DELAY);
	alive = (nTask->taskHandle != NULL);
	xSemaphoreGive(nTask->accessLock);
	return alive;
}

//TODO Implement this
//bool isNetworkTaskWorking(network_task_t *nTask) {}

void clearStatisticsStructure(app_work_statistics *stats) {
	xSemaphoreTake(stats->accessLock, portMAX_DELAY);
	stats->speed = 0;
	stats->n_pkg_transm = 0;
	stats->n_pkg_rcv_total = 0;
	stats->n_pkg_rcv_failed = 0;
	xSemaphoreGive(stats->accessLock);
}




