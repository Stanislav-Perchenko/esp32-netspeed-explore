/*
 * rc4.h
 *
 *  Created on: Apr 9, 2019
 *      Author: stanislav.perchenko
 */

#ifndef MAIN_RC4_H_
#define MAIN_RC4_H_

#ifdef __cplusplus
extern "C" {
#endif

#include <stdlib.h>
#include <stdint.h>

void rc4_init(uint8_t *key, uint8_t key_length);
void rc4_output(uint8_t *dst, size_t index, size_t length);

#ifdef __cplusplus
}
#endif

#endif /* MAIN_RC4_H_ */
