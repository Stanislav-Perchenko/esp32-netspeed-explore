package com.alperez.esp32.netspeed_client.core;

import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.util.Log;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.SocketTimeoutException;

/**
 * Created by stanislav.perchenko on 4/1/2019
 */
public class UDPReceiver {

    public static class Statistics {
        public final int nTotalPkgReceived;
        public final int nPkgFailed;
        public final int nPkgLost;
        public final long nBytesReceived;
        public final int speed;

        private Statistics(int nTotalPkgReceived, int nPkgFailed, int nPkgLost, long nBytesReceived, int speed) {
            this.nTotalPkgReceived = nTotalPkgReceived;
            this.nPkgFailed = nPkgFailed;
            this.nPkgLost = nPkgLost;
            this.nBytesReceived = nBytesReceived;
            this.speed = speed;
        }
    }

    public interface OnStatisticsUpdateListener {
        void onStatisticsUpdate(Statistics statistics);
    }

    private final int port;
    private final String threadName;
    private final Thread worker, notifier;
    private volatile boolean released;
    private volatile boolean alive;

    private final byte dataBuffer[] = new byte[32*1024];
    private final RcvStatisticsMeter rcvMeter;
    private final OnStatisticsUpdateListener statisticsListener;

    public UDPReceiver(int port, String threadName, OnStatisticsUpdateListener statisticsListener) {
        this.port = port;
        this.statisticsListener = statisticsListener;
        rcvMeter = new RcvStatisticsMeter();
        worker = new Thread(this::job, this.threadName = threadName);
        worker.start();
        notifier = new Thread(this::notifyWorker, "rcv_notifier");
        notifier.start();
        alive = true;
    }

    public int getPort() {
        return port;
    }

    public void release() {
        released = true;
        worker.interrupt();
        notifier.interrupt();
    }

    public boolean isAlive() {
        return alive && !released;
    }

    private void job() {
        try (DatagramSocket socket = new DatagramSocket(port)) {
            socket.setBroadcast(true);
            socket.setSoTimeout(100);
            Log.d(threadName, "SO_RCVBUF="+socket.getReceiveBufferSize());
            DatagramPacket rcv = new DatagramPacket(dataBuffer, 0, dataBuffer.length);
            int n_rcv;
            Log.d(threadName, "---> Start receiving data on port "+port);
            while(!released) {
                try {
                    socket.receive(rcv);
                    n_rcv = rcvMeter.addNewPackage(dataBuffer, 0, rcv.getLength());
                    if ((n_rcv % 10) == 0) {
                        Log.d(threadName, String.format("<--- 10 packages were received from the %s", rcv.getAddress().toString()));
                    }

                } catch (SocketTimeoutException e) {
                    Log.e(threadName, "<~~~        Timeout");
                } catch (IOException e) {
                    Log.e(threadName, String.format("<~~~ Error receiving data on port %d - %s", port, e.getMessage()));
                    e.printStackTrace();
                }
            }
            Log.e(threadName, "Receiving thread has been finished.");
        } catch (IOException ex) {
            Log.e(threadName, String.format("Error create receiving socket on port %d - %s", port, ex.getMessage()));
        }
        alive = false;
    }





    /**********************************************************************************************/
    /****************************  Measure and notify statistics  *********************************/
    /**********************************************************************************************/
    private void notifyWorker() {
        final long[] rslt = new long[6];
        while(!released) {
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                continue;
            }

            rcvMeter.getResult(rslt);
            Statistics s = new Statistics((int)rslt[0], (int)rslt[1], (int)rslt[2], rslt[3], (int)rslt[4]);
            mUiHandler.obtainMessage(1, s).sendToTarget();
        }
    }

    private final Handler mUiHandler = new Handler(Looper.getMainLooper()) {
        @Override
        public void handleMessage(Message msg) {
            if ((msg.what == 1) && (statisticsListener != null)) statisticsListener.onStatisticsUpdate((Statistics) msg.obj);
        }
    };




}
