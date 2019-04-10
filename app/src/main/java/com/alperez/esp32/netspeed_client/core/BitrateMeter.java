package com.alperez.esp32.netspeed_client.core;

import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.os.SystemClock;
import android.support.annotation.NonNull;
import android.util.Log;

import com.alperez.esp32.netspeed_client.BuildConfig;

/**
 * Created by stanislav.perchenko on 3/15/2018.
 */

public final class BitrateMeter {
    private static final boolean D = false;

    public interface OnBitrateListener {
        void onBitrateMeasured(int bitrate);
    }

    private final int measureWindowSeconds;
    private final int measureWindowNanos;
    private final OnBitrateListener callback; // This object is used as a lock for calculation


    private long tStamp;
    private long tNext;
    private int nBytes;

    private int currentBitrate;

    public BitrateMeter(int measureWindowSeconds, @NonNull OnBitrateListener callback) {
        assert(callback != null);
        measureWindowNanos = (this.measureWindowSeconds = measureWindowSeconds) * 1_000_000_000;
        this.callback = callback;
        reset();
    }

    public void reset() {
        synchronized (callback) {
            tStamp = -1;
            tNext = 0;
            nBytes = 0;
            currentBitrate = 0;
        }
        reportHandler.obtainMessage(MSG_BITRATE, 0, 0).sendToTarget();
    }

    public int getCurrentBitrate() {
        synchronized (callback) {
            return currentBitrate;
        }
    }

    public void updateWithSamples(int moreSamples, int encoding) {

        if (D && BuildConfig.DEBUG) {
            String msg = String.format("More samples: %d at time %d ns", moreSamples, SystemClock.elapsedRealtimeNanos());
            reportHandler.obtainMessage(MSG_LOG, msg).sendToTarget();
        }

        updateWithBytes(moreSamples * MediaUtils.getBytesPerSample(encoding));
    }

    public void updateWithBytes(int moreBytes) {
        boolean report = false;
        int br = 0;

        synchronized (callback) {
            long t_now = SystemClock.elapsedRealtimeNanos();
            if (tStamp < 0) {
                tNext = (tStamp = t_now) + measureWindowNanos;
                this.nBytes = moreBytes;
            } else if (t_now >= tNext) {
                tStamp = tNext;
                tNext += measureWindowNanos;
                currentBitrate = br = Math.round(nBytes * 8f / measureWindowSeconds);
                this.nBytes = moreBytes;
                report = true;
            } else {
                this.nBytes += moreBytes;
            }
        }// synchronized (callback)

        if (report) {
            reportHandler.obtainMessage(MSG_BITRATE, br, 0).sendToTarget();
        }
    }


    private static final int MSG_BITRATE = 1;
    private static final int MSG_LOG = 2;

    private final Handler reportHandler = new Handler(Looper.getMainLooper()) {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case MSG_BITRATE:
                    callback.onBitrateMeasured(msg.arg1);
                    break;
                case MSG_LOG:
                    Log.d(BitrateMeter.class.getSimpleName(), msg.obj.toString());
                    break;
            }


        }
    };
}

