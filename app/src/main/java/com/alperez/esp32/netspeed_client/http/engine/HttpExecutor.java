package com.alperez.esp32.netspeed_client.http.engine;

import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.support.annotation.Nullable;

import com.alperez.esp32.netspeed_client.http.utils.ParseResponseHandler;

import java.util.Deque;
import java.util.LinkedList;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Created by stanislav.perchenko on 3/26/2019
 */
public class HttpExecutor {

    private static final AtomicInteger EXECUTOR_SEQUENCE_COUNTER = new AtomicInteger(0);

    private final Deque<Job> jobQueue = new LinkedList<>();
    private volatile boolean released;
    private final Worker worker;
    private final ResultHandler resultHandler;

    public HttpExecutor() {
        worker = new Worker(EXECUTOR_SEQUENCE_COUNTER.incrementAndGet());
        worker.start();
        resultHandler = new ResultHandler();
    }

    public <T> void executeHttpRequest(BaseHttpRequest<T> request, @Nullable ParseResponseHandler<T> responseParser, HttpCallback<T> callback) {
        synchronized (jobQueue) {
            if (!released) {
                jobQueue.addLast(new Job(request, responseParser, callback));
                jobQueue.notify();
            }
        }
    }

    public void release() {
        synchronized (jobQueue) {
            if (!released) {
                worker.release();
                released = true;
                jobQueue.notify();
            }
        }
    }


    private class Job<T> {
        public final BaseHttpRequest<T> request;
        public final ParseResponseHandler<T> responseParser;
        public final HttpCallback<T> callback;

        public Job(BaseHttpRequest<T> request, ParseResponseHandler<T> responseParser, HttpCallback<T> callback) {
            this.request = request;
            this.responseParser = responseParser;
            this.callback = callback;
        }
    }


    private class Worker extends Thread {

        private volatile boolean workerReleased;

        public Worker(int number) {
            super("http-executor-worker-"+number);
        }

        public void release() {
            workerReleased = true;
        }

        @Override
        public void run() {
            while (!workerReleased) {
                Job currJob;
                //----  Wait for a job  ----
                synchronized (jobQueue) {
                    while (!workerReleased && jobQueue.isEmpty()) {
                        try {
                            jobQueue.wait();
                        } catch (InterruptedException e) {
                            // Ignore this. In case of this worker was released we will exit on the
                            // outside while-loop next iteration.

                            // Re-interrupt this worker Thread to notify possible job
                            interrupt();
                        }
                    }
                    if (workerReleased) return;
                    currJob = jobQueue.removeFirst();
                }

                BaseHttpRequest.HttpResponse<?> response = currJob.request.executeSynchronously(currJob.responseParser);

                resultHandler.notifyResult(response, currJob.callback);

            } // while()
        }
    }




    private class JobResult<T> {
        public final BaseHttpRequest.HttpResponse<T> response;
        public final HttpCallback<T> callback;

        public JobResult(BaseHttpRequest.HttpResponse<T> response, HttpCallback<T> callback) {
            this.response = response;
            this.callback = callback;
        }
    }

    private class ResultHandler extends Handler {

        public ResultHandler() {
            super(Looper.getMainLooper());
        }

        public <T> void notifyResult(BaseHttpRequest.HttpResponse<T> response, HttpCallback<T> cb) {
            JobResult<T> result = new JobResult<>(response, cb);
            this.obtainMessage(8351, result).sendToTarget();
        }

        @Override
        public void handleMessage(Message msg) {
            if (msg.what == 8351) {
                JobResult result = (JobResult) msg.obj;
                if (result.response.isSuccess()) {
                    result.callback.onComplete(result.response.getSequenceNumber(), result.response.getRawJson(), result.response.getData());
                } else {
                    result.callback.onError(result.response.getSequenceNumber(), result.response.getError());
                }
            }
        }
    }

}
