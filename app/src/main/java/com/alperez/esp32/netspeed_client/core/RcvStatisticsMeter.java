package com.alperez.esp32.netspeed_client.core;

import android.util.Log;

import java.util.Arrays;

/**
 * Created by stanislav.perchenko on 4/9/2019
 */
public class RcvStatisticsMeter {


    public void reset() {
        synchronized (lock) {
            nTotalPkgReceived = 0;
        }
    }

    private final Object lock = new Object();
    private int nTotalPkgReceived = 0;


    public int addNewPackage(byte[] data, int offset, int length) {

        final long pkgSize = getUint32LSB(data, 0);
        final long pkgIndex= getUint32LSB(data, 4);
        //final byte[] pkgKey= getPackageRC4Key(data[4], data[5], data[6]);





        synchronized (lock) {
            return ++nTotalPkgReceived;
        }
    }

    private long getUint32LSB(byte[] src, int index) {
        int value = (int)src[index];
        value |= (int)src[index + 1] << 8;
        value |= (int)src[index + 2] << 16;
        value |= (int)src[index + 3] << 24;
        return value;
    }

    


    /**
     *
     * @param dst [0] - total received; [1] - N failed; [2] - N packages failed; [3] - total bytes received; [4] - speed bit/s
     */
    public void getResult(long[] dst) {
        synchronized (lock) {
            dst[0] = nTotalPkgReceived;
        }
    }
}
