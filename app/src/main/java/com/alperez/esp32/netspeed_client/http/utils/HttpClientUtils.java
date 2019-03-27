package com.alperez.esp32.netspeed_client.http.utils;

import android.util.Log;

import java.io.IOException;

import okhttp3.Headers;
import okhttp3.MediaType;
import okhttp3.Protocol;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.ResponseBody;
import okio.Buffer;

/**
 * Created by stanislav.perchenko on 3/26/2019
 */
public class HttpClientUtils {
    /**
     * Extracts content of a request so it can be used in an interceptor for logging in an interceptor.
     * The original request instance can be used for processing safely.
     * @param tag      the TAG which is used for logging in case of unsuccessful try.
     * @param request  an original Request instance
     * @return
     */
    public static String getBodyContentFromRequest(String tag, Request request) {
        try {
            Request copy = request.newBuilder().build();
            Buffer buff = new Buffer();
            copy.body().writeTo(buff);
            return buff.readUtf8();
        } catch(IOException e) {
            Log.e(tag, "Request bodyString content cannot be extracted - " + e.getMessage());
            return null;
        } catch (NullPointerException e) {
            Log.e(tag, "No bodyString in a request");
            return null;
        }
    }


    /**
     * Extracts content of a response's bodyString so it can be used further in an interceptor for logging
     * @param tag      the TAG which is used for logging in case of unsuccessful try.
     * @param response original response.
     * @return The array of 2 objects: [0] - a copy of the original response which is not consumed so it can be
     * used safely be a client code. Response instance must be used as a return value in an interceptor.
     * [1] - String object which is a content of response bodyString.
     */
    public static Object[] getBodyContentFromResponse(String tag, Response response) {
        Object[] ret = new Object[2];


        try {
            MediaType contType = response.body().contentType();
            Headers respHeads = response.headers();
            String respMessage = response.message();
            Protocol respProtocol = response.protocol();
            int respCode = response.code();
            String payload = response.body().string();


            ResponseBody newBody = ResponseBody.create(contType, payload);
            Response newResp = response.newBuilder().headers(respHeads).message(respMessage).protocol(respProtocol).code(respCode).body(newBody).build();

            ret[0] = newResp;
            ret[1] = payload;
        } catch(IOException e) {
            Log.e(tag, "Response bodyString content cannot be extracted - "+e.getMessage());
            ret[0] = response;
        }
        return ret;
    }


    public static synchronized void printLongString(String tag, String text) {
        if (text == null) return;
        int start = 0;
        int segLen = 1000;
        do {
            int end = start + segLen;
            if (end > text.length()) {
                end = text.length();
            }
            Log.e(tag, text.substring(start, end));
            try {
                Thread.sleep(10);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            start = end;
        } while(start < text.length());
    }
}
