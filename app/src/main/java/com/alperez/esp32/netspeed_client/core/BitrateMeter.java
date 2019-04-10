package com.alperez.esp32.netspeed_client.core;

import android.os.SystemClock;
import android.support.annotation.NonNull;
import android.util.Log;

import com.alperez.esp32.netspeed_client.BuildConfig;

import java.util.Deque;
import java.util.LinkedList;

/**
 * Created by stanislav.perchenko on 3/15/2018.
 */

public final class BitrateMeter {
    private static final boolean D = false;


    private final long measureWindowNanos;


    private final Object lock = new Object();

    private long tLastUpdate;
    private int currentBitrate;


    private final Deque<MemItem> delayLine = new LinkedList<>();
    private int nBytesAccum;

    public BitrateMeter(int measureWindowMs) {
        measureWindowNanos = measureWindowMs * 1_000_000L;
        reset();
    }

    public void reset() {
        synchronized (lock) {
            currentBitrate = 0;
            tLastUpdate = SystemClock.elapsedRealtimeNanos();
            nBytesAccum = 0;
        }
    }

    public int getCurrentBitrate() {
        synchronized (lock) {
            return currentBitrate;
        }
    }

    public int moreBytes(int nBytes) {
        final long t_now = SystemClock.elapsedRealtimeNanos();
        final long t_past_cutoff = t_now - measureWindowNanos;

        //--- Add segment  ----
        MemItem mi = getFreeMemItem();
        mi.set(tLastUpdate, nBytes);
        delayLine.addLast(mi);
        nBytesAccum += nBytes;


        //--- Remove oldest segments ---
        int n_removed = 0;
        while (true) {
            mi = delayLine.peekFirst();
            if (mi.tSegmentStart >= t_past_cutoff) {
                break;
            } else {
                delayLine.removeFirst();
                nBytesAccum -= mi.segmentLength;
                recycleMemItem(mi);
                n_removed ++;
            }
        }

        //--- Re-calculate bitrate ---
        long dt = t_now - delayLine.peekFirst().tSegmentStart;
        if (dt == 0) dt = 1;
        int br = (int)(8_000_000_000L*nBytesAccum / dt);

        if (D && BuildConfig.DEBUG) {
            String text = String.format("t_now=%d, t_past_cutoff=%d, n_removed=%d, delay_len=%d, n_bytes=%d, dt=%d", t_now, t_past_cutoff, n_removed, delayLine.size(), nBytesAccum, dt);
            Log.d(Thread.currentThread().getName(), text);
        }

        //--- Update global state values ---
        synchronized (lock) {
            tLastUpdate = t_now;
            return (currentBitrate = br);
        }
    }










    /**********************************************************************************************/
    private class MemItem {
        long tSegmentStart;
        int segmentLength;

        void set(long tSegmentStart, int segmentLength) {
            this.tSegmentStart = tSegmentStart;
            this.segmentLength = segmentLength;
        }

        void clear() {
            tSegmentStart = 0;
            segmentLength = 0;
        }
    }


    private final Deque<MemItem> heap = new LinkedList<>();


    private MemItem getFreeMemItem() {
        return heap.isEmpty() ? new MemItem() : heap.pop();
    }

    private void recycleMemItem(@NonNull MemItem mi) {
        assert (mi != null);
        mi.clear();
        heap.push(mi);
    }


}

