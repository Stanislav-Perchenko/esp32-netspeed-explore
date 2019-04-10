/*
 * mean_filter.h
 *
 *  Created on: Apr 8, 2019
 *      Author: stanislav.perchenko
 */

#ifndef MAIN_MEAN_FILTER_H_
#define MAIN_MEAN_FILTER_H_

#ifdef __cplusplus
extern "C" {
#endif

#include <stdlib.h>
#include <stdint.h>

typedef struct {
	int16_t *buffer;
	uint8_t capacity;
	uint8_t size;
	int16_t *phead;
} mean_filter16_t;

mean_filter16_t *flt16_buildFilter16(uint8_t capacity);
void flt16_clearFilter16(mean_filter16_t *flt);
uint8_t flt16_add(mean_filter16_t *flt, int16_t value);
int32_t flt16_sum(mean_filter16_t *flt);
int16_t flt16_meanValueI(mean_filter16_t *flt);
float flt16_meanValueF(mean_filter16_t *flt);

#ifdef __cplusplus
}
#endif


#endif /* MAIN_MEAN_FILTER_H_ */
