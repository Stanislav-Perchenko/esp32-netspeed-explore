package com.alperez.esp32.netspeed_client.core;

import android.media.AudioFormat;
import android.util.Log;

import java.util.Arrays;

/**
 * Created by stanislav.perchenko on 4/9/2019
 */
public class RcvStatisticsMeter {


    private final Object lock = new Object();
    private long nTotalPkgReceived = 0;
    private int nPkgFailed = 0;
    private int nPkgLost = 0;
    private long nTotalBytes = 0;
    private int mBitrate;


    private long lastPkgIndex = -1;
    private final RC4 rc4;
    private byte[] reference_array;
    private final BitrateMeter bitrateMeter;

    public RcvStatisticsMeter() {
        rc4 = new RC4();
        bitrateMeter = new BitrateMeter(3, br -> {});
        reset();
    }

    public void reset() {
        bitrateMeter.reset();
        synchronized (lock) {
            nTotalPkgReceived = 0;
            nPkgFailed = 0;
            nPkgLost = 0;
            nTotalBytes = 0;
            mBitrate = 0;
        }
    }


    public int addNewPackage(byte[] data, int offset, int length) {
        boolean isPkgOk = true;
        int n_lost = 0;
        try {
            bitrateMeter.updateWithSamples(length, AudioFormat.ENCODING_PCM_8BIT);
            final int pkgSize = (int)getUint32LSB(data, 0);
            if (pkgSize != length) {
                isPkgOk = false;
                return 0;
            }
            final int nBytesPayload = pkgSize - 8;
            final long pkgIndex= getUint32LSB(data, 4);
            if ((lastPkgIndex >= 0) && (pkgIndex - lastPkgIndex) > 1) {
                n_lost = (int)(pkgIndex - lastPkgIndex - 1);
            }
            lastPkgIndex = pkgIndex;

            final byte[] pkgKey= getPackageRC4Key(data[4], data[5], data[6]);

            rc4.init(pkgKey);
            if (reference_array == null || reference_array.length < nBytesPayload) reference_array = new byte[nBytesPayload];
            rc4.nextBytes(reference_array, 0, nBytesPayload);
            for (int i=0; i<nBytesPayload; i++) {
                if (data[offset + 8 + i] != reference_array[i]) {
                    isPkgOk = false;
                    return 0;
                }
            }

        } finally {
            synchronized (lock) {
                if (!isPkgOk) nPkgFailed ++;
                nPkgLost += n_lost;
                nTotalBytes += length;
                mBitrate = bitrateMeter.getCurrentBitrate();
                return (int)(++nTotalPkgReceived);
            }
        }
    }

    private long getUint32LSB(byte[] src, int index) {
        int value = (int)src[index];
        value |= (int)src[index + 1] << 8;
        value |= (int)src[index + 2] << 16;
        value |= (int)src[index + 3] << 24;
        return value;
    }

    byte[] getPackageRC4Key(byte b0, byte b1, byte b2) {
        byte[] key = new byte[6];
        key[0] = b0;
        key[1] = b1;
        key[2] = b2;
        key[3] = (byte)(b0 ^ 0xFFFF_FFFF);
        key[4] = (byte)(b1 ^ 0xFFFF_FFFF);
        key[5] = (byte)(b2 ^ 0xFFFF_FFFF);
        return key;
    }


    /**
     *
     * @param dst [0] - total received; [1] - N packages failed; [2] - N packages lost; [3] - total bytes received; [4] - speed bit/s
     */
    public void getResult(long[] dst) {
        synchronized (lock) {
            dst[0] = nTotalPkgReceived;
            dst[1] = nPkgFailed;
            dst[2] = nPkgLost;
            dst[3] = nTotalBytes;
            dst[4] = mBitrate;
        }
    }
}
