#include "wifi.h"
#include <tinyara/gpio.h>
#include <apps/netutils/mqtt_api.h>
#include <apps/netutils/dhcpc.h>
#include <tinyara/analog/adc.h>
#include <tinyara/analog/ioctl.h>
#include <apps/shell/tash.h>
#include <apps/netutils/ntpclient.h>
#include <apps/netutils/cJSON.h>

#define DEFAULT_CLIENT_ID "123456789"
#define SERVER_ADDR "api.artik.cloud"
#define SERVER_PORT 8883
#define RED_LED 45 // on-board LED
#define BLUE_LED 49 // on-board LED
#define RED_ON_BOARD_LED 45
#define NET_DEVNAME "wl1"

char channel_name[10];
char channel_count[10];

static const char mqtt_ca_cert_str[] = "-----BEGIN CERTIFICATE-----\r\n"
		"MIIGrTCCBZWgAwIBAgIQASAP9e8Tbenonqd/EQFJaDANBgkqhkiG9w0BAQsFADBN\r\n"
		"MQswCQYDVQQGEwJVUzEVMBMGA1UEChMMRGlnaUNlcnQgSW5jMScwJQYDVQQDEx5E\r\n"
		"aWdpQ2VydCBTSEEyIFNlY3VyZSBTZXJ2ZXIgQ0EwHhcNMTgwMzA4MDAwMDAwWhcN\r\n"
		"MjAwNDA1MTIwMDAwWjBzMQswCQYDVQQGEwJVUzETMBEGA1UECBMKQ2FsaWZvcm5p\r\n"
		"YTERMA8GA1UEBxMIU2FuIEpvc2UxJDAiBgNVBAoTG1NhbXN1bmcgU2VtaWNvbmR1\r\n"
		"Y3RvciwgSW5jLjEWMBQGA1UEAwwNKi5hcnRpay5jbG91ZDCCASIwDQYJKoZIhvcN\r\n"
		"AQEBBQADggEPADCCAQoCggEBANghNaTXWDfYV/JWgBnX4hmhcClPSO0onx5B2url\r\n"
		"YzpvTc3MBaQ+08YBpAKvTqZvPqrJUIM45Q91M301I5e2kz0DMq2zQZOGB0B83V/O\r\n"
		"O4vwETq4PCjAPhMinF4dN6HeJCuqo1CLh8evhfkFiJvpEfQWTxdjzPJ0Zdj/2U8E\r\n"
		"8Ht7zV5pWiDtuejtIDHB5H6fCx4xeQy/E+5l4V6R3BnRKpZsJtlhTh0RFqWhw5DJ\r\n"
		"/WWpGP//1VTZSHyW9SABsPd+jP1YgDraRD4b4lZBU6c8nC5qT3dhdiYoG6xUgTb3\r\n"
		"kfgUhhlOFpe3sBtR32OS8RuFrFeQDGaa3r6pfSy06Kph/eECAwEAAaOCA2EwggNd\r\n"
		"MB8GA1UdIwQYMBaAFA+AYRyCMWHVLyjnjUY4tCzhxtniMB0GA1UdDgQWBBSNBf6r\r\n"
		"7S/j0oV3A0XmEflXErutQDAlBgNVHREEHjAcgg0qLmFydGlrLmNsb3VkggthcnRp\r\n"
		"ay5jbG91ZDAOBgNVHQ8BAf8EBAMCBaAwHQYDVR0lBBYwFAYIKwYBBQUHAwEGCCsG\r\n"
		"AQUFBwMCMGsGA1UdHwRkMGIwL6AtoCuGKWh0dHA6Ly9jcmwzLmRpZ2ljZXJ0LmNv\r\n"
		"bS9zc2NhLXNoYTItZzYuY3JsMC+gLaArhilodHRwOi8vY3JsNC5kaWdpY2VydC5j\r\n"
		"b20vc3NjYS1zaGEyLWc2LmNybDBMBgNVHSAERTBDMDcGCWCGSAGG/WwBATAqMCgG\r\n"
		"CCsGAQUFBwIBFhxodHRwczovL3d3dy5kaWdpY2VydC5jb20vQ1BTMAgGBmeBDAEC\r\n"
		"AjB8BggrBgEFBQcBAQRwMG4wJAYIKwYBBQUHMAGGGGh0dHA6Ly9vY3NwLmRpZ2lj\r\n"
		"ZXJ0LmNvbTBGBggrBgEFBQcwAoY6aHR0cDovL2NhY2VydHMuZGlnaWNlcnQuY29t\r\n"
		"L0RpZ2lDZXJ0U0hBMlNlY3VyZVNlcnZlckNBLmNydDAJBgNVHRMEAjAAMIIBfwYK\r\n"
		"KwYBBAHWeQIEAgSCAW8EggFrAWkAdgCkuQmQtBhYFIe7E6LMZ3AKPDWYBPkb37jj\r\n"
		"d80OyA3cEAAAAWIHFb1dAAAEAwBHMEUCIQCQ0UjVVJSQDRB3oxzI5aD1Hs5GhbXj\r\n"
		"I6Cqt3/tkXT1WQIgNVWRgbJ72Ik9gp5QoNxhCZ+h//or0uL7PHnv3cP5L9UAdgBv\r\n"
		"U3asMfAxGdiZAKRRFf93FRwR2QLBACkGjbIImjfZEwAAAWIHFb73AAAEAwBHMEUC\r\n"
		"IQDxCxJCsZjuqbQvuwipgdUf1l6qXdiekM5zn33i1+KYxgIgKDMJEuKHzhkweT2S\r\n"
		"Y4dWBuzSdOAzZfoDrIGdsFvkxi0AdwC72d+8H4pxtZOUI5eqkntHOFeVCqtS6BqQ\r\n"
		"lmQ2jh7RhQAAAWIHFb1YAAAEAwBIMEYCIQCNDYdxWmqUGGwNzXlJ1/NXxzwqPYIB\r\n"
		"eSJDuR1xfWtSsQIhAJsygf2rqPS+O7qQAzggCQ2V/3JDRUhuxNDPqwooo47uMA0G\r\n"
		"CSqGSIb3DQEBCwUAA4IBAQBvRGWibvHFrRUWsArJ9lmS5MMZFbXXQPXbflgv3nSG\r\n"
		"ShmhBC3o+k97J0Wgp/wH7uDf01RrRMAVNm458g1Mr4AMAXq3zzxNNTwjGYw/USuG\r\n"
		"UprrKqc9onugtAUX8DGvlZr8SWO3FhPlyamWQ69jutx/X4nfHyZr41bX9WQ/ay0F\r\n"
		"GQJ1tRTrX1eUPO+ucXeG8vTbt09bRNnoY+i97dzrwHakXySfHohNsIbwmrsS4SQv\r\n"
		"7eG9g5+5vsc2B9ugGcELIYKrzDWNPshir37KSpcwLUCmDJkTQp8+KhJUKgbTALTa\r\n"
		"nxuDyNwZIwW66vv1t0Zi4vKU8hfUsAN2N3wcsb6pY/RA\r\n"
		"-----END CERTIFICATE-----\r\n"
		"-----BEGIN CERTIFICATE-----\r\n"
		"MIIElDCCA3ygAwIBAgIQAf2j627KdciIQ4tyS8+8kTANBgkqhkiG9w0BAQsFADBh\r\n"
		"MQswCQYDVQQGEwJVUzEVMBMGA1UEChMMRGlnaUNlcnQgSW5jMRkwFwYDVQQLExB3\r\n"
		"d3cuZGlnaWNlcnQuY29tMSAwHgYDVQQDExdEaWdpQ2VydCBHbG9iYWwgUm9vdCBD\r\n"
		"QTAeFw0xMzAzMDgxMjAwMDBaFw0yMzAzMDgxMjAwMDBaME0xCzAJBgNVBAYTAlVT\r\n"
		"MRUwEwYDVQQKEwxEaWdpQ2VydCBJbmMxJzAlBgNVBAMTHkRpZ2lDZXJ0IFNIQTIg\r\n"
		"U2VjdXJlIFNlcnZlciBDQTCCASIwDQYJKoZIhvcNAQEBBQADggEPADCCAQoCggEB\r\n"
		"ANyuWJBNwcQwFZA1W248ghX1LFy949v/cUP6ZCWA1O4Yok3wZtAKc24RmDYXZK83\r\n"
		"nf36QYSvx6+M/hpzTc8zl5CilodTgyu5pnVILR1WN3vaMTIa16yrBvSqXUu3R0bd\r\n"
		"KpPDkC55gIDvEwRqFDu1m5K+wgdlTvza/P96rtxcflUxDOg5B6TXvi/TC2rSsd9f\r\n"
		"/ld0Uzs1gN2ujkSYs58O09rg1/RrKatEp0tYhG2SS4HD2nOLEpdIkARFdRrdNzGX\r\n"
		"kujNVA075ME/OV4uuPNcfhCOhkEAjUVmR7ChZc6gqikJTvOX6+guqw9ypzAO+sf0\r\n"
		"/RR3w6RbKFfCs/mC/bdFWJsCAwEAAaOCAVowggFWMBIGA1UdEwEB/wQIMAYBAf8C\r\n"
		"AQAwDgYDVR0PAQH/BAQDAgGGMDQGCCsGAQUFBwEBBCgwJjAkBggrBgEFBQcwAYYY\r\n"
		"aHR0cDovL29jc3AuZGlnaWNlcnQuY29tMHsGA1UdHwR0MHIwN6A1oDOGMWh0dHA6\r\n"
		"Ly9jcmwzLmRpZ2ljZXJ0LmNvbS9EaWdpQ2VydEdsb2JhbFJvb3RDQS5jcmwwN6A1\r\n"
		"oDOGMWh0dHA6Ly9jcmw0LmRpZ2ljZXJ0LmNvbS9EaWdpQ2VydEdsb2JhbFJvb3RD\r\n"
		"QS5jcmwwPQYDVR0gBDYwNDAyBgRVHSAAMCowKAYIKwYBBQUHAgEWHGh0dHBzOi8v\r\n"
		"d3d3LmRpZ2ljZXJ0LmNvbS9DUFMwHQYDVR0OBBYEFA+AYRyCMWHVLyjnjUY4tCzh\r\n"
		"xtniMB8GA1UdIwQYMBaAFAPeUDVW0Uy7ZvCj4hsbw5eyPdFVMA0GCSqGSIb3DQEB\r\n"
		"CwUAA4IBAQAjPt9L0jFCpbZ+QlwaRMxp0Wi0XUvgBCFsS+JtzLHgl4+mUwnNqipl\r\n"
		"5TlPHoOlblyYoiQm5vuh7ZPHLgLGTUq/sELfeNqzqPlt/yGFUzZgTHbO7Djc1lGA\r\n"
		"8MXW5dRNJ2Srm8c+cftIl7gzbckTB+6WohsYFfZcTEDts8Ls/3HB40f/1LkAtDdC\r\n"
		"2iDJ6m6K7hQGrn2iWZiIqBtvLfTyyRRfJs8sjX7tN8Cp1Tm5gr8ZDOo0rwAhaPit\r\n"
		"c+LJMto4JQtV05od8GiG7S5BNO98pVAdvzr508EIDObtHopYJeS4d60tbvVS3bR0\r\n"
		"j6tJLp07kzQoH3jOlOrHvdPJbRzeXDLz\r\n"
		"-----END CERTIFICATE-----\r\n";

const unsigned char *get_ca_cert(void) {
	return (const unsigned char*) mqtt_ca_cert_str;
}

void onConnect(void* client, int result) {
	printf("mqtt client connected to the server\n");
}

void onDisconnect(void* client, int result) {
	printf("mqtt client disconnected from the server\n");
}

void onPublish(void* client, int result) {
	printf("mqtt client Published message\n");
}

void gpio_write(int port, int value) {
	char str[4];
	static char devpath[16];
	snprintf(devpath, 16, "/dev/gpio%d", port);
	int fd = open(devpath, O_RDWR);

	ioctl(fd, GPIOIOC_SET_DIRECTION, GPIO_DIRECTION_OUT);
	write(fd, str, snprintf(str, 4, "%d", value != 0) + 1);

	close(fd);
}

static void ntp_link_error(void) {
	printf("ntp_link_error() callback is called.\n");
}

//================================ Publish ===================================

char device_id_pub[] = "0e1ba782bd1645c29ddf897e06ca5041";	// IoT TV Following
char device_token_pub[] = "87d080f5d4954d3ca84cfca40bb53937";

char *strTopicMsg_pub;
char *strTopicAct_pub;

mqtt_client_t* pClientHandle_pub = NULL;
mqtt_client_config_t clientConfig_pub;
mqtt_tls_param_t clientTls_pub;
struct ntpc_server_conn_s g_server_conn_pub[2];

void initializeConfigUtil_pub(void) {
	uint8_t macId[IFHWADDRLEN];
	int result = netlib_getmacaddr("wl1", macId);
	if (result < 0) {
		printf(
				"Get MAC Address failed. Assigning \
                Client ID as 123456789");
		clientConfig_pub.client_id =
		DEFAULT_CLIENT_ID; // MAC id Artik 053
	} else {
		printf("MAC: %02x:%02x:%02x:%02x:%02x:%02x\n", ((uint8_t *) macId)[0],
				((uint8_t *) macId)[1], ((uint8_t *) macId)[2],
				((uint8_t *) macId)[3], ((uint8_t *) macId)[4],
				((uint8_t *) macId)[5]);
		char buf[12];
		sprintf(buf, "%02x%02x%02x%02x%02x%02x", ((uint8_t *) macId)[0],
				((uint8_t *) macId)[1], ((uint8_t *) macId)[2],
				((uint8_t *) macId)[3], ((uint8_t *) macId)[4],
				((uint8_t *) macId)[5]);
		clientConfig_pub.client_id = buf; // MAC id Artik 053
		printf("Registering mqtt client with id = %s\n", buf);
	}

	clientConfig_pub.user_name = device_id_pub;
	clientConfig_pub.password = device_token_pub;
	clientConfig_pub.debug = true;
	clientConfig_pub.on_connect = (void*) onConnect;
	clientConfig_pub.on_disconnect = (void*) onDisconnect;
	clientConfig_pub.on_publish = (void*) onPublish;

	clientConfig_pub.protocol_version = MQTT_PROTOCOL_VERSION_311;
	clientConfig_pub.clean_session = true;

	clientTls_pub.ca_cert = get_ca_cert();
	clientTls_pub.ca_cert_len = sizeof(mqtt_ca_cert_str);
	clientTls_pub.cert = NULL;
	clientTls_pub.cert_len = 0;
	clientTls_pub.key = NULL;
	clientTls_pub.key_len = 0;

	clientConfig_pub.tls = &clientTls_pub;
}

//================================ Subscribe ===================================

char device_id_sub[] = "0e1ba782bd1645c29ddf897e06ca5041";	// IoT TV Following
char device_token_sub[] = "87d080f5d4954d3ca84cfca40bb53937";

char *strTopicMsg_sub;
char *strTopicAct_sub;

mqtt_client_t* pClientHandle_sub = NULL;
mqtt_client_config_t clientConfig_sub;
mqtt_tls_param_t clientTls_sub;
struct ntpc_server_conn_s g_server_conn_sub[2];

void onMessage(void *client, mqtt_msg_t *msg) {
	printf("-------------------- Start onMessage --------------------\n");
	int i;
	cJSON *jsonMsg = NULL;
	char *strActName = NULL;
	char *payload = strdup(msg->payload);

	printf("Received message\n");
	printf("Topic: %s\n", msg->topic);
	printf("Message: %s\n", payload);

	jsonMsg = cJSON_Parse((const char*) payload);
	cJSON *data = cJSON_GetObjectItem(jsonMsg, "actions");

	if (data == NULL) {
		printf("data is null\n");
		return;
	}

	cJSON *action = cJSON_GetArrayItem(data, 0);
	cJSON *actName = cJSON_GetObjectItem(action, "name");
	cJSON *actParams = cJSON_GetObjectItem(action, "parameters");
	strActName = cJSON_Print(actName);
	char *strParamValue = NULL;

	if (strncmp(strActName, "\"tv_channel_name\"", 17) == 0) {
		// Channel Name
		cJSON *param1 = cJSON_GetObjectItem(actParams, "tv_channel_name");// "detection_number":5
		strParamValue = cJSON_Print(param1);
		printf("action name: %s, param value: %s\n", strActName, strParamValue);

		//strcpy(channel_name, strParamValue);
		channel_name[0] = strParamValue[1];
		channel_name[1] = strParamValue[2];
		channel_name[2] = strParamValue[3];
		channel_name[3] = '\0';

		// sub 받은걸 다시 pub으로 보냄
		char buf[40];
		sprintf(buf, "{\"tv_channel_name\" : \"%s\"}", channel_name);
		printf("pub_data_name Value: %s\n", channel_name);

		mqtt_msg_t message;
		message.payload = (char*) buf;
		message.payload_len = 12;
		message.topic = strTopicMsg_pub;
		message.qos = 0;
		message.retain = 0;

		int ret = mqtt_publish(pClientHandle_pub, message.topic,
				(char*) message.payload, message.payload_len, message.qos,
				message.retain);

		lcd_channel_name_display(channel_name);

	} else if (strncmp(strActName, "\"tv_channel_count\"", 18) == 0) {
		// Channel Count
		cJSON *param1 = cJSON_GetObjectItem(actParams, "tv_channel_count");	// "detection_number":5
		strParamValue = cJSON_Print(param1);
		printf("action name: %s, param value: %s\n", strActName, strParamValue);

		strcpy(channel_count, strParamValue);
		lcd_channel_count_display(strParamValue);

	} else if (strncmp(strActName, "\"detection_number\"", 18) == 0) {
		// 5 : palor -> toilet
		// 6 : toilet -> palor
		cJSON *param1 = cJSON_GetObjectItem(actParams, "detection_number");	// "detection_number":5
		strParamValue = cJSON_Print(param1);
		printf("action name: %s, param value: %s\n", strActName, strParamValue);

		if (strncmp(strParamValue, "5", 1) == 0) {// "detection_number":5 (거실->화장실)
			printf("palor -> toilet\n");
			// TV 화면을 거실에서 화장실로 옮김
			lcd_toilet_init(channel_name, channel_count);
		} else if (strncmp(strParamValue, "6", 1) == 0) {// "detection_number":6 (화장실->거실)
			printf("toilet -> palor\n");
			// TV 화면을 화장실에서 거실로 옮김
			lcd_palor_init(channel_name, channel_count);
		}
	}

	cJSON_Delete(jsonMsg);
	free(strActName);
	free(strParamValue);
	free(payload);
	printf("-------------------- End onMessage --------------------\n");
}

// Utility function to configure mqtt client
void initializeConfigUtil_sub(void) {
	uint8_t macId[IFHWADDRLEN];
	int result = netlib_getmacaddr("wl1", macId);
	if (result < 0) {
		printf(
				"Get MAC Address failed. Assigning \
                Client ID as 123456789");
		clientConfig_sub.client_id =
		DEFAULT_CLIENT_ID; // MAC id Artik 053
	} else {
		printf("MAC: %02x:%02x:%02x:%02x:%02x:%02x\n", ((uint8_t *) macId)[0],
				((uint8_t *) macId)[1], ((uint8_t *) macId)[2],
				((uint8_t *) macId)[3], ((uint8_t *) macId)[4],
				((uint8_t *) macId)[5]);
		char buf[12];
		sprintf(buf, "%02x%02x%02x%02x%02x%02x", ((uint8_t *) macId)[0],
				((uint8_t *) macId)[1], ((uint8_t *) macId)[2],
				((uint8_t *) macId)[3], ((uint8_t *) macId)[4],
				((uint8_t *) macId)[5]);
		clientConfig_sub.client_id = buf; // MAC id Artik 053
		printf("Registering mqtt client with id = %s\n", buf);
	}

	clientConfig_sub.user_name = device_id_sub;
	clientConfig_sub.password = device_token_sub;
	clientConfig_sub.debug = true;
	clientConfig_sub.on_connect = (void*) onConnect;
	clientConfig_sub.on_disconnect = (void*) onDisconnect;
	clientConfig_sub.on_message = (void*) onMessage;
	clientConfig_sub.on_publish = (void*) onPublish;

	clientConfig_sub.protocol_version = MQTT_PROTOCOL_VERSION_311;
	clientConfig_sub.clean_session = true;

	clientTls_sub.ca_cert = get_ca_cert();
	clientTls_sub.ca_cert_len = sizeof(mqtt_ca_cert_str);
	clientTls_sub.cert = NULL;
	clientTls_sub.cert_len = 0;
	clientTls_sub.key = NULL;
	clientTls_sub.key_len = 0;

	clientConfig_sub.tls = &clientTls_sub;
}

//================================ Main ===================================

#ifdef CONFIG_BUILD_KERNEL
int main(int argc, FAR char *argv[])
#else
int sensorbd_main(int argc, FAR char *argv[])
#endif
{
	//-------------------------- Connection -----------------------------
	printf("-------------------- Start Connection --------------------\n");
	bool wifiConnected = false;
	gpio_write(RED_ON_BOARD_LED, 1); // Turn on on board Red LED to indicate no WiFi connection is established
	int ret;

	while (!wifiConnected) {
		ret = mkfifo(CONFIG_WPA_CTRL_FIFO_DEV_REQ,
		CONFIG_WPA_CTRL_FIFO_MK_MODE);
		if (ret != 0 && ret != -EEXIST) {
			printf("mkfifo error for %s: %s", CONFIG_WPA_CTRL_FIFO_DEV_REQ,
					strerror(errno));
		}
		ret = mkfifo(CONFIG_WPA_CTRL_FIFO_DEV_CFM,
		CONFIG_WPA_CTRL_FIFO_MK_MODE);
		if (ret != 0 && ret != -EEXIST) {
			printf("mkfifo error for %s: %s", CONFIG_WPA_CTRL_FIFO_DEV_CFM,
					strerror(errno));
		}

		ret = mkfifo(CONFIG_WPA_MONITOR_FIFO_DEV, CONFIG_WPA_CTRL_FIFO_MK_MODE);
		if (ret != 0 && ret != -EEXIST) {
			printf("mkfifo error for %s: %s", CONFIG_WPA_MONITOR_FIFO_DEV,
					strerror(errno));
		}

		if (start_wifi_interface() == SLSI_STATUS_ERROR) {
			printf("Connect Wi-Fi failed. Try Again.\n");
		} else {
			wifiConnected = true;
			gpio_write(RED_ON_BOARD_LED, 0); // Turn off Red LED to indicate WiFi connection is established
		}
	}

	printf("Connect to Wi-Fi success\n");

	bool mqttConnected = false;
	bool ipObtained = false;
	printf("Get IP address\n");

	struct dhcpc_state state;
	void *dhcp_handle;

	while (!ipObtained) {
		dhcp_handle = dhcpc_open(NET_DEVNAME);
		ret = dhcpc_request(dhcp_handle, &state);
		dhcpc_close(dhcp_handle);

		if (ret != OK) {
			printf("Failed to get IP address\n");
			printf("Try again\n");
			sleep(1);
		} else {
			ipObtained = true;
		}
	}
	netlib_set_ipv4addr(NET_DEVNAME, &state.ipaddr);
	netlib_set_ipv4netmask(NET_DEVNAME, &state.netmask);
	netlib_set_dripv4addr(NET_DEVNAME, &state.default_router);

	printf("IP address  %s\n", inet_ntoa(state.ipaddr));
	printf("-------------------- End Connection --------------------\n");
	up_mdelay(1000);

	//-------------------------- Publish -----------------------------
	printf("-------------------- Start Publish Conn. --------------------\n");
	strTopicMsg_pub = (char*) malloc(sizeof(char) * 256);
	strTopicAct_pub = (char*) malloc(sizeof(char) * 256);
	sprintf(strTopicMsg_pub, "/v1.1/messages/%s", device_id_pub);
	sprintf(strTopicAct_pub, "/v1.1/actions/%s", device_token_pub);

	memset(&clientConfig_pub, 0, sizeof(clientConfig_pub));
	memset(&clientTls_pub, 0, sizeof(clientTls_pub));

	// for NTP Client
	memset(&g_server_conn_pub, 0, sizeof(g_server_conn_pub));
	g_server_conn_pub[0].hostname = "0.asia.pool.ntp.org";
	g_server_conn_pub[0].port = 123;
	g_server_conn_pub[1].hostname = "1.asia.pool.ntp.org";
	g_server_conn_pub[1].port = 123;

	int ret_ntp_pub = ntpc_start(g_server_conn_pub, 2, 1000, ntp_link_error);
	printf("pub ret: %d\n", ret_ntp_pub);

	// Connect to the WiFi network for Internet connectivity
	printf("mqtt client tutorial\n");

	// Initialize mqtt client
	initializeConfigUtil_pub();

	pClientHandle_pub = mqtt_init_client(&clientConfig_pub);
	if (pClientHandle_pub == NULL) {
		printf("mqtt client handle initialization fail\n");
		return 0;
	}

	while (mqttConnected == false ) {
		sleep(2);
		// Connect mqtt client to server
		int result = mqtt_connect(pClientHandle_pub, SERVER_ADDR, SERVER_PORT,
				60);

		if (result == 0) {
			mqttConnected = true;
			printf("mqtt client connected to server\n");
			break;
		} else {
			continue;
		}
	}
	printf("-------------------- End Publish Conn. --------------------\n");

	//-------------------------- Subscribe -----------------------------
	printf("-------------------- Start Subscribe Conn. --------------------\n");
	mqttConnected = false;

	strTopicMsg_sub = (char*) malloc(sizeof(char) * 256);
	strTopicAct_sub = (char*) malloc(sizeof(char) * 256);
	sprintf(strTopicMsg_sub, "/v1.1/messages/%s", device_id_sub);
	sprintf(strTopicAct_sub, "/v1.1/actions/%s", device_id_sub);

	memset(&clientConfig_sub, 0, sizeof(clientConfig_sub));
	memset(&clientTls_sub, 0, sizeof(clientTls_sub));

	// for NTP Client
	memset(&g_server_conn_sub, 0, sizeof(g_server_conn_sub));
	g_server_conn_sub[0].hostname = "0.asia.pool.ntp.org";
	g_server_conn_sub[0].port = 123;
	g_server_conn_sub[1].hostname = "1.asia.pool.ntp.org";
	g_server_conn_sub[1].port = 123;

	int ret_ntp_sub = ntpc_start(g_server_conn_sub, 2, 1000, ntp_link_error);
	printf("sub ret: %d\n", ret_ntp_sub);

	// Connect to the WiFi network for Internet connectivity
	printf("mqtt client tutorial\n");

	// Initialize mqtt client
	initializeConfigUtil_sub();

	pClientHandle_sub = mqtt_init_client(&clientConfig_sub);
	if (pClientHandle_sub == NULL) {
		printf("mqtt client handle initialization fail\n");
		return 0;
	}

	while (mqttConnected == false ) {
		sleep(2);
		// Connect mqtt client to server
		int result = mqtt_connect(pClientHandle_sub, SERVER_ADDR, SERVER_PORT,
				60);

		if (result == 0) {
			mqttConnected = true;
			printf("mqtt client connected to server\n");
			break;
		} else {
			continue;
		}
	}

	// Subscribe to topic of interest
	while (1) {
		sleep(2);
		int result = mqtt_subscribe(pClientHandle_sub, strTopicAct_sub, 0); //topic - color, QOS - 0
		if (result < 0) {
			printf("mqtt client subscribe to topic failed\n");
			continue;
		} else {
			printf("mqtt client Subscribed to the topic successfully\n");
			break;
		}
	}

	printf("-------------------- End Subscribe Conn. --------------------\n");

	/*
	 printf("-------------------- Start Publish Data --------------------\n");
	 while (1) {

	 // channel name
	 char buf[40];
	 char pub_data_name[] = "qqq";
	 sprintf(buf, "{\"tv_channel_name\" : \"%s\"}", pub_data_name);
	 printf("pub_data_name Value: %s\n", pub_data_name);

	 mqtt_msg_t message;
	 message.payload = (char*) buf;
	 message.payload_len = strlen(buf);
	 message.topic = strTopicMsg_pub;
	 message.qos = 0;
	 message.retain = 0;

	 ret = mqtt_publish(pClientHandle_pub, message.topic,
	 (char*) message.payload, message.payload_len, message.qos,
	 message.retain);

	 if (ret < 0) {
	 printf("Error publishing name\n");
	 } else {
	 printf("Success publishing name\n");
	 break;
	 }

	 up_mdelay(100);
	 }

	 while (1) {
	 // channel count
	 char buf[40];
	 int pub_data = 548;
	 sprintf(buf, "{\"tv_channel_count\" : %d}", pub_data);
	 printf("Published Value: %d\n", pub_data);

	 mqtt_msg_t message;
	 message.payload = (char*) buf;
	 message.payload_len = strlen(buf);
	 message.topic = strTopicMsg_pub;
	 message.qos = 0;
	 message.retain = 0;

	 ret = mqtt_publish(pClientHandle_pub, message.topic,
	 (char*) message.payload, message.payload_len, message.qos,
	 message.retain);

	 if (ret < 0) {
	 printf("Error publishing count\n");
	 } else {
	 printf("Success publishing count\n");
	 break;
	 }

	 up_mdelay(100);
	 }
	 printf("-------------------- End Publish Data --------------------\n");
	 */
}
