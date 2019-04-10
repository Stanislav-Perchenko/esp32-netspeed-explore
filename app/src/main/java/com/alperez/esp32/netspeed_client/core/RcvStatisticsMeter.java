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

    private final byte[] bbb = new byte[12];

    public int addNewPackage(byte[] data, int offset, int length) {
        System.arraycopy(data, offset, bbb, 0, 12);
        Log.d(Thread.currentThread().getName(), Arrays.toString(bbb));
        synchronized (lock) {
            return ++nTotalPkgReceived;
        }
    }


    /**
     *
     * @param dst [0] - total received; [1] - N failed; [2] - total bytes received; [3] - speed bit/s
     */
    public void getResult(long[] dst) {
        synchronized (lock) {
            dst[0] = nTotalPkgReceived;
        }
    }
}
