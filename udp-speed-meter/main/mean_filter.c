/*
 * mean_filter.c
 *
 *  Created on: Apr 8, 2019
 *      Author: stanislav.perchenko
 */

#include "mean_filter.h"

mean_filter16_t *flt16_buildFilter16(uint8_t capacity) {
	mean_filter16_t *ptr = (mean_filter16_t *)calloc(1, sizeof(mean_filter16_t));
	if (ptr) {
		ptr->buffer = (int16_t *)calloc(capacity, sizeof(int16_t));
		if (ptr->buffer) {
			ptr->phead = ptr->buffer;
			ptr->capacity = capacity;
			ptr->size = 0;
		} else {
			free(ptr);
			ptr = 0;
		}
	}
	return ptr;
}

void flt16_clearFilter16(mean_filter16_t *flt) {
	free(flt->buffer);
	free(flt);
}

uint8_t flt16_add(mean_filter16_t *flt, int16_t value) {
	*(flt->phead ++) = value;
	if (flt->phead >= (flt->buffer + flt->capacity)) {
		flt->phead = flt->buffer;
	}
	if (flt->size < flt->capacity) flt->size ++;
	return flt->size;
}

int32_t flt16_sum(mean_filter16_t *flt) {
	int32_t s = 0;
	for (uint8_t i=0; i<flt->size; i++) {
		s += *(flt->buffer + i);
	}
	return s;
}

int16_t flt16_meanValueI(mean_filter16_t *flt) {
	if (flt->size > 0) {
		int32_t s = flt16_sum(flt) << 1;
		s /= flt->size;
		int16_t result = (int16_t)(s >> 1) + (s & 0x0001);
		return result;
	} else {
		return 0;
	}

}

float flt16_meanValueF(mean_filter16_t *flt) {
	if (flt->size > 0) {
		float result = flt16_sum(flt);
		return result / flt->size;
	} else {
		return 0;
	}
}
