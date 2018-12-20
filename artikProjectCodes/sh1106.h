/*
 * sh1106.h
 *
 *  Created on: 2018. 9. 29.
 *      Author: ������
 */

#ifndef SH1106_H_
#define SH1106_H_

//#include <stdio.h>
//#include <stdlib.h>
#include <tinyara/spi/spi.h>

#define SPI_MAX 3

#define SH1106_X_PIXELS		128
#define SH1106_Y_PIXELS		64
#define SH1106_ROWS			8

typedef enum
{
	SH1106_OK = 0,
	SH1106_POINT_ERROR = 1,
	SH1106_PORT_NUMBER_ERROR = 2,
	SH1106_NOT_INITIALIZED = 3,
}SH1106_RET;

typedef enum
{
	COMMAND = 0,
	WRITE   = 1,
}DC_STATUS;

typedef struct
{
	struct spi_dev_s *spi;
	bool is_init;
	uint8_t port : 4;

	uint8_t dc_pin;
	uint8_t reset_pin;
	uint8_t column;
	uint8_t line;
}Sh0116Manager;

extern const uint8_t ASCII[][5];

SH1106_RET sh1106_init(uint8_t port, uint8_t dc_pin, uint8_t reset_pin);

SH1106_RET sh1106_write_string(uint8_t x, uint8_t y, uint8_t *string);

SH1106_RET sh1106_write_btm(const uint8_t *btm, uint8_t x, uint8_t y, uint8_t width, uint8_t height);

SH1106_RET sh1106_clear(void);

SH1106_RET sh1106_inverse(void);

uint8_t get_width(void);

uint8_t get_height(void);



#endif /* SH1106_H_ */
