/*
 * rc4.c
 *
 *  Created on: Apr 9, 2019
 *      Author: stanislav.perchenko
 */

#include "rc4.h"

static uint8_t S[256+1];
static size_t i, j;

void rc4_init(uint8_t *key, uint8_t key_length) {
	uint8_t temp;
	for(i = 0; i < 256; i++) S[i] = i;
	for(i = j = 0; i < 256; i++) {
		j = (j + key[i % key_length] + S[i]) % 256;
		temp = S[i];
		S[i] = S[j];
		S[j] = temp;
	}
	i = j = 0;
}


void rc4_output(uint8_t *dst, size_t index, size_t length) {
	size_t count, acc;
	uint8_t temp;
	for (count=0; count<length; count ++, index++) {
		i = ( i + 1 ) % 256;
		j = ( j + S[ i ] ) % 256;
		temp = S[ j ];
		S[ j ] = S[ i ];
		S[ i ] = temp;
		acc = S[i] + S[j];
		dst[index] = S[acc % 256];
	}
}
