package com.alperez.esp32.netspeed_client.core;

/**
 * Created by stanislav.perchenko on 4/10/2019
 */
public class RC4 {
    private final byte S[] = new byte[256];
    private int i, j;


    public RC4() {
        init(new byte[4]);
    }

    public synchronized void init(byte[] key) {
        for (int idx=0; idx<256; idx++) S[idx] = (byte)idx;

        byte temp;
        for (int idxI=0, idxJ=0; idxI < 256; idxI++) {
            idxJ = (idxJ + key[idxI % key.length] + S[idxI]) % 256;
            temp = S[idxI];
            S[idxI] = S[idxJ];
            S[idxJ] = temp;
        }

        i = j = 0;
    }

    public synchronized void nextBytes(byte[] dst, int index, int length) {
        int temp;
        for (int counter = 0; counter < length; counter++) {

            i = (i + 1) % 256;
            j = (j + S[i]) % 256;

            temp = S[j];
            S[j] = S[i];
            S[i] = (byte)temp;

            dst[index++ ] = S[(temp + S[j]) % 256];
        }
    }
}
