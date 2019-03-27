/* Simple HTTP Server Example

   This example code is in the Public Domain (or CC0 licensed, at your option.)

   Unless required by applicable law or agreed to in writing, this
   software is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR
   CONDITIONS OF ANY KIND, either express or implied.
*/

#include <esp_wifi.h>
#include <esp_event_loop.h>
#include <esp_log.h>
#include <esp_system.h>

#include <nvs_flash.h>
#include <sys/param.h>

#include <esp_http_server.h>

#include "freertos/FreeRTOS.h"
#include "freertos/task.h"
#include "freertos/semphr.h"
#include "freertos/event_groups.h"


#include "sdkconfig.h"

#include "tcpip_adapter.h"
#include "network_utils.h"
#include "state_defs.h"
#include "http_control.h"


static const char *TAG="APP";


EventGroupHandle_t s_wifi_event_group;
static const int WIFI_STA_IP_READY_BIT = BIT0;
static const int WIFI_AP_STARTED_BIT = BIT1;
static const int WIFI_STA_DISCONNECT_BIT = BIT2;
static const int WIFI_WIFI_STOP_BIT = BIT3;
static const int WIFI_AP_STA_CONNECTED = BIT4;
static const int WIFI_AP_STA_DISCONNECTED = BIT5;

httpd_handle_t mControlHttpServer;

static app_state_t mAppState = {
		.statistics = {
			.speed = 0,
			.n_pkg_rcv_failed = 0,
			.n_pkg_rcv_total = 0,
			.n_pkg_transm = 0,
			.accessLock = NULL,
		},
		.netwTask = {
			.accessLock = NULL,
			.taskHandle = NULL,
			.isAlive = false,
			.groupTaskAlive = NULL,
			.semCmdFinish = NULL,
			.protocol = PROTOCOL_UNDEFINED,
			.action = ACTION_IDLE,
			.port = 0,
			.transmit_data = {0},
		},



};

//---------  Connection state (own IP addres in the 'Station' mode and Connected Station MAC address in the 'AP' mode)  --------
SemaphoreHandle_t connDataAccessLock;
tcpip_adapter_ip_info_t myIp = {0};
ip_info_text_t *mIpInfoText;
system_event_ap_staconnected_t *mStationConnected;


httpd_uri_t controlUriStatus = {
    .uri       = "/status",
    .method    = HTTP_GET,
    .handler   = http_control_handler_status,
    .user_ctx  = &mAppState
};

httpd_uri_t controlUriStartTransmit = {
    .uri       = "/start/transmit",
    .method    = HTTP_GET,
    .handler   = http_control_handler_start_transmit,
    .user_ctx  = &mAppState
};


httpd_uri_t controlUriStop = {
    .uri       = "/stop",
    .method    = HTTP_GET,
    .handler   = http_control_handler_stop,
    .user_ctx  = &mAppState
};




static esp_err_t event_handler(void *ctx, system_event_t *event) {
    switch(event->event_id) {
    case SYSTEM_EVENT_STA_START:
        ESP_LOGI(TAG, "SYSTEM_EVENT_STA_START");
        ESP_ERROR_CHECK(esp_wifi_connect());
        break;
    case SYSTEM_EVENT_STA_GOT_IP:
        ESP_LOGI(TAG, "SYSTEM_EVENT_STA_GOT_IP");

        xSemaphoreTake(connDataAccessLock, portMAX_DELAY);
        getIpAddressTextForm(TCPIP_ADAPTER_IF_STA, &myIp, mIpInfoText);
        ESP_LOGI(TAG, "Got IP: '%s'; Gateway: '%s'; Mask: '%s'", mIpInfoText->txt_ip, mIpInfoText->txt_gw, mIpInfoText->txt_netmask);
        xSemaphoreGive(connDataAccessLock);

        xEventGroupSetBits(s_wifi_event_group, WIFI_STA_IP_READY_BIT);
        break;
    case SYSTEM_EVENT_STA_DISCONNECTED:
        ESP_LOGI(TAG, "SYSTEM_EVENT_STA_DISCONNECTED");

        xSemaphoreTake(connDataAccessLock, portMAX_DELAY);
        clearIpAddress(&myIp, mIpInfoText);
        xSemaphoreGive(connDataAccessLock);

        ESP_ERROR_CHECK(esp_wifi_connect());
        xEventGroupSetBits(s_wifi_event_group, WIFI_STA_DISCONNECT_BIT);
        break;

    case SYSTEM_EVENT_AP_START:
    	ESP_LOGI(TAG, "SYSTEM_EVENT_AP_START");

    	xSemaphoreTake(connDataAccessLock, portMAX_DELAY);
    	getIpAddressTextForm(TCPIP_ADAPTER_IF_AP, &myIp, mIpInfoText);
    	ESP_LOGI(TAG, "Got own IP: '%s'; Gateway: '%s'; Mask: '%s'", mIpInfoText->txt_ip, mIpInfoText->txt_gw, mIpInfoText->txt_netmask);
    	xSemaphoreGive(connDataAccessLock);

    	xEventGroupSetBits(s_wifi_event_group, WIFI_AP_STARTED_BIT);
    	break;

    case SYSTEM_EVENT_AP_STOP:
    	xEventGroupSetBits(s_wifi_event_group, WIFI_WIFI_STOP_BIT);
    	break;

    case SYSTEM_EVENT_STA_STOP:
    	xEventGroupSetBits(s_wifi_event_group, WIFI_WIFI_STOP_BIT);
    	break;

    case SYSTEM_EVENT_AP_STACONNECTED:
    	xSemaphoreTake(connDataAccessLock, portMAX_DELAY);

    	mStationConnected = malloc(sizeof(*mStationConnected));
    	if (mStationConnected) {
    		memcpy(mStationConnected, &event->event_info.sta_connected, sizeof(*mStationConnected));
    	} else {
    		ESP_LOGE(TAG, "Error allocating memory for a connected WiFi station data structure");
    		ESP_ERROR_CHECK (ESP_ERR_NO_MEM);
    	}

    	xSemaphoreGive(connDataAccessLock);
    	ESP_LOGI(TAG, "station:"MACSTR" join, AID=%d",
    			MAC2STR(event->event_info.sta_connected.mac),
				event->event_info.sta_connected.aid);

    	xEventGroupSetBits(s_wifi_event_group, WIFI_AP_STA_CONNECTED);
    	break;

    case SYSTEM_EVENT_AP_STADISCONNECTED:
    	ESP_LOGI(TAG, "station:"MACSTR"leave, AID=%d",
    			MAC2STR(event->event_info.sta_disconnected.mac),
				event->event_info.sta_disconnected.aid);

    	xSemaphoreTake(connDataAccessLock, portMAX_DELAY);
    	if (mStationConnected && (event->event_info.sta_disconnected.aid == mStationConnected->aid)) {
    		free(mStationConnected);
    		mStationConnected = NULL;
    	}
    	xSemaphoreGive(connDataAccessLock);

    	xEventGroupSetBits(s_wifi_event_group, WIFI_AP_STA_DISCONNECTED);
    	break;
    default:
        break;
    }
    return ESP_OK;
}

static void initialise_wifi(esp_interface_t interf, wifi_config_t *wf_config) {
    tcpip_adapter_init();

    ESP_ERROR_CHECK( esp_event_loop_init(event_handler, NULL) );
    wifi_init_config_t cfg = WIFI_INIT_CONFIG_DEFAULT();
    ESP_ERROR_CHECK( esp_wifi_init(&cfg) );

    wifi_mode_t wf_mode = WIFI_MODE_APSTA;
    if (interf == ESP_IF_WIFI_STA) {
    	wf_mode = WIFI_MODE_STA;
    } else if (interf == ESP_IF_WIFI_AP) {
    	wf_mode = WIFI_MODE_AP;
    }

    ESP_ERROR_CHECK( esp_wifi_set_mode(wf_mode) );
    ESP_ERROR_CHECK( esp_wifi_set_config(interf, wf_config) );
    ESP_ERROR_CHECK( esp_wifi_start() );
    ESP_ERROR_CHECK( esp_wifi_set_ps(WIFI_PS_NONE) );
}

void initAppStateStructure(app_state_t *appState) {
	appState->statistics.accessLock = xSemaphoreCreateBinary();
	if (!appState->statistics.accessLock) {
		ESP_LOGE(TAG, "Failed to create semaphore appState->statistics.accessLock");
		ESP_ERROR_CHECK (ESP_ERR_NO_MEM);
	} else {
		xSemaphoreGive(appState->statistics.accessLock);
	}

	appState->netwTask.accessLock = xSemaphoreCreateBinary();
	if (!appState->netwTask.accessLock) {
		ESP_LOGE(TAG, "Failed to create semaphore appState->netwTask.accessLock");
		ESP_ERROR_CHECK (ESP_ERR_NO_MEM);
	} else {
		xSemaphoreGive(appState->netwTask.accessLock);
	}

	appState->netwTask.semCmdFinish = xSemaphoreCreateBinary();
	if (!appState->netwTask.semCmdFinish) {
		ESP_LOGE(TAG, "Failed to create semaphore to finish task appState->netwTask.semCmdFinish");
		ESP_ERROR_CHECK (ESP_ERR_NO_MEM);
	}

	appState->netwTask.groupTaskAlive = xEventGroupCreate();
	if (!appState->netwTask.groupTaskAlive) {
		ESP_LOGE(TAG, "Failed to create group for thread status - appState->netwTask.groupTaskAlive");
		ESP_ERROR_CHECK (ESP_ERR_NO_MEM);
	}
}

void app_main() {

	esp_err_t err = nvs_flash_init();
	if (err != ESP_OK) {
		ESP_ERROR_CHECK( nvs_flash_erase() );
		ESP_ERROR_CHECK( nvs_flash_init() );
	}

	/***********************  Initialize in-memory control structures  *********************/
	initAppStateStructure(&mAppState);


	/***************************  Init WiFi  *******************************************/
	s_wifi_event_group = xEventGroupCreate();
	if (s_wifi_event_group == NULL) {
		ESP_LOGE(TAG, "Failed to create WiFi state event group");
		ESP_ERROR_CHECK (ESP_ERR_NO_MEM);
	}
	mIpInfoText = build_ip_info_text();
	if (!mIpInfoText) {
		ESP_LOGE(TAG, "Failed to create holder for IpInfo text form");
		ESP_ERROR_CHECK (ESP_ERR_NO_MEM);
	}
	connDataAccessLock = xSemaphoreCreateBinary();
	if (connDataAccessLock == NULL) {
		ESP_LOGE(TAG, "Failed to create semaphore for the Conn. data access lock");
		ESP_ERROR_CHECK (ESP_ERR_NO_MEM);
	} else {
		xSemaphoreGive(connDataAccessLock);
	}


#ifdef CONFIG_WIFI_OPERATION_MODE_AP
    wifi_config_t wifi_config = {
    	.ap = {
    		.ssid = CONFIG_WIFI_SSID,
    		.ssid_len = strlen(CONFIG_WIFI_SSID),
    		.password = CONFIG_WIFI_PASSWORD,
    		.max_connection = 1,
    		.authmode = WIFI_AUTH_WPA2_PSK
    	},
    };
    if (strlen(CONFIG_WIFI_PASSWORD) == 0) {
    	wifi_config.ap.authmode = WIFI_AUTH_OPEN;
    }

    initialise_wifi(ESP_IF_WIFI_AP, &wifi_config);
    ESP_LOGI(TAG, "WiFi AP init finished. SSID:%s password:%s", wifi_config.sta.ssid, wifi_config.sta.password);

#else
    wifi_config_t wifi_config = {
    	.sta = {
    			.ssid = CONFIG_WIFI_SSID,
				.password = CONFIG_WIFI_PASSWORD,
    	},
    };
    initialise_wifi(ESP_IF_WIFI_STA, &wifi_config);
    ESP_LOGI(TAG, "Connecting to \"%s\", pass=%s", wifi_config.sta.ssid, wifi_config.sta.password);
#endif


    httpd_handle_t controlHttpServer = NULL;
    httpd_config_t controlHttpServerConf = HTTPD_DEFAULT_CONFIG();
    controlHttpServerConf.server_port = CONFIG_TCP_CONTROL_PORT;
    controlHttpServerConf.max_open_sockets = 3;

    do {
    	xEventGroupWaitBits(s_wifi_event_group, WIFI_AP_STA_CONNECTED|WIFI_STA_IP_READY_BIT, true, false, portMAX_DELAY);


    	uint8_t n_try = 1;
    	do {
    		ESP_LOGI(TAG, "Starting HTTP control server (try #%d)", n_try);
    		err = httpd_start(&controlHttpServer, &controlHttpServerConf);
    		if (err == ESP_OK) {
    			ESP_LOGI(TAG, "Control HTTP server has been started on port: '%d'", controlHttpServerConf.server_port);
    		} else {
    			ESP_LOGE(TAG, "Error start control HTTP server");
    			vTaskDelay(1000 / portTICK_PERIOD_MS);
    		}
    	} while((err != ESP_OK) && (++n_try <= 45));

    	if (err != ESP_OK) {
    		ESP_ERROR_CHECK (err);
    	}

    	err = httpd_register_uri_handler(controlHttpServer, &controlUriStatus);
    	if (err != ESP_OK) {
    		ESP_LOGE(TAG, "Error register control HTTP Uri - '/status'");
    		ESP_ERROR_CHECK (err);
    	} else {
    		ESP_LOGI(TAG, "Control HTTP Uri '/status' has been registered");
    	}

		err = httpd_register_uri_handler(controlHttpServer, &controlUriStartTransmit);
		if (err != ESP_OK) {
			ESP_LOGE(TAG, "Error register control HTTP Uri - '/start/transmit'");
			ESP_ERROR_CHECK (err);
		} else {
			ESP_LOGI(TAG, "Control HTTP Uri '/start/transmit' has been registered");
		}

		err = httpd_register_uri_handler(controlHttpServer, &controlUriStop);
		if (err != ESP_OK) {
			ESP_LOGE(TAG, "Error register control HTTP Uri - '/stop'");
			ESP_ERROR_CHECK (err);
		} else {
			ESP_LOGI(TAG, "Control HTTP Uri '/stop' has been registered");
		}




    	xEventGroupWaitBits(s_wifi_event_group, WIFI_STA_DISCONNECT_BIT | WIFI_AP_STA_DISCONNECTED | WIFI_WIFI_STOP_BIT, false, false, portMAX_DELAY);
    	xEventGroupClearBits(s_wifi_event_group, WIFI_STA_DISCONNECT_BIT | WIFI_AP_STA_DISCONNECTED);

    	if (controlHttpServer) {
    		err = httpd_stop(controlHttpServer);
    		controlHttpServer = NULL;
    		if (err != ESP_OK) {
    			ESP_LOGE(TAG, "Error stop control HTTP server");
    			ESP_ERROR_CHECK (err);
    		} else {
    			ESP_LOGI(TAG, "Control HTTP server has been stopped");
    		}
    	}

    } while((xEventGroupGetBits(s_wifi_event_group) & WIFI_WIFI_STOP_BIT) == 0); //Exit is WiFi adapter has been stopped

}
