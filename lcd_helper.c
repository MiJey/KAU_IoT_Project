#include <string.h>
#include <fcntl.h>
#include <tinyara/gpio.h>
#include "sh1106.h"

int lcd_livingroom_init(char *channel_name, char *channel_count) {
	// lcd toilet init
	int code = sh1106_clear();
	if (code == 0 || code == 3) {	// OK or NOT_INITIALIZED
	// lcd livingroom init --> use lcd livingroom
		if (sh1106_init(0, 51, 52)) { // port, A0, reset
			printf("Error: sh1106 initialization \n");
			return 0;
		} else {
			lcd_display("Livingroom TV", channel_name, channel_count);
		}
	}
}

int lcd_toilet_init(char *channel_name, char *channel_count) {
	// lcd livingroom init
	int code = sh1106_clear();
	if (code == 0 || code == 3) {	// OK or NOT_INITIALIZED
	// lcd toilet init --> use lcd toilet
		if (sh1106_init(0, 53, 54)) { // port, A0, reset
			printf("Error: sh1106 initialization \n");
			return 0;
		} else {
			lcd_display("Toilet TV", channel_name, channel_count);
		}
	}
}

int lcd_display(char *s0, char *s1, char *s2) {
	sh1106_write_string(10, 0, s0);
	sh1106_write_string(10, 1, s1);
	sh1106_write_string(10, 2, s2);
}

int lcd_channel_name_display(char *channel_name) {
	sh1106_write_string(10, 1, channel_name);
}

int lcd_channel_count_display(char *channel_count) {
	sh1106_write_string(10, 2, channel_count);
}
