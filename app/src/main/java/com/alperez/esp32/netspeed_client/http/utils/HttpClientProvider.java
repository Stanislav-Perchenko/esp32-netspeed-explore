package com.alperez.esp32.netspeed_client.http.utils;

import android.util.Log;

import com.alperez.esp32.netspeed_client.BuildConfig;
import com.alperez.esp32.netspeed_client.GlobalProperties;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import okhttp3.ConnectionPool;
import okhttp3.Headers;
import okhttp3.Interceptor;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Protocol;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.logging.HttpLoggingInterceptor;

/**
 * Created by stanislav.perchenko on 10/24/2016.
 */

public class HttpClientProvider {
    private static final String TAG = "HttpClientIntercept";

    public static final MediaType MEDIA_TYPE_POST = MediaType.parse("application/json; charset=utf-8");

    public static final Headers DEFAULT_HEADERS;

    static {
        DEFAULT_HEADERS = new Headers.Builder()
                .add("Connection", "Keep-Alive")
                .add("Accept", "application/json")
                .add("Content-Type", "application/x-www-form-urlencoded; charset=utf-8").build();
    }

    private static HttpClientProvider instance;


    public static HttpClientProvider getInstance() {
        if (instance == null) {
            synchronized (HttpClientProvider.class) {
                if (instance == null) {
                    instance = new HttpClientProvider();
                }
            }
        }
        return instance;
    }





    private final OkHttpClient mImageUploadClient;
    private final OkHttpClient mDefaultRESTClient;

    private final OkHttpClient mDefaultRESTNogzipClient;


    private HttpClientProvider() {
        List<Protocol> protocols = new ArrayList<Protocol>(1);
        protocols.add(Protocol.HTTP_1_1);

        OkHttpClient client = new OkHttpClient.Builder()
                .connectionPool(new ConnectionPool(3, 150, TimeUnit.SECONDS))
                .protocols(protocols)
                .addNetworkInterceptor(new SetUserAgentInterceptor())
                .connectTimeout(10, TimeUnit.SECONDS)
                .readTimeout(20, TimeUnit.SECONDS)
                .writeTimeout(10, TimeUnit.SECONDS).build();


        mImageUploadClient = client.newBuilder().addInterceptor(new HttpLoggingInterceptor().setLevel(BuildConfig.DEBUG ? HttpLoggingInterceptor.Level.HEADERS : HttpLoggingInterceptor.Level.NONE)).build();
        mDefaultRESTClient = client.newBuilder().addInterceptor(new HttpLoggingInterceptor().setLevel(BuildConfig.DEBUG ? HttpLoggingInterceptor.Level.BODY : HttpLoggingInterceptor.Level.NONE)).build();
        mDefaultRESTNogzipClient = client.newBuilder().addInterceptor(new HttpLoggingInterceptor().setLevel(BuildConfig.DEBUG ? HttpLoggingInterceptor.Level.BODY : HttpLoggingInterceptor.Level.NONE)).addNetworkInterceptor(new NoGzipInterceptor()).build();


    }



    public OkHttpClient getDefaultRESTClient() {
        return GlobalProperties.HTTP_UPSTREAM_USE_GZIP ? mDefaultRESTClient : mDefaultRESTNogzipClient;
    }


    public OkHttpClient getImageUploadClient() {
        return mImageUploadClient;
    }


    private class SetUserAgentInterceptor implements Interceptor {
        @Override
        public Response intercept(Chain chain) throws IOException {
            Request newRequest = chain.request().newBuilder().removeHeader("User-Agent").header("User-Agent", GlobalProperties.HTTP_USER_AGENT_NAME).build();
            return chain.proceed(newRequest);
        }
    }


    private class NoGzipInterceptor implements Interceptor {
        @Override
        public Response intercept(Chain chain) throws IOException {

            Request newRequest = chain.request().newBuilder().removeHeader("Accept-Encoding").build();



            Log.i(TAG, String.format(GlobalProperties.FORMATTER_LOCALE, "Sending request %s on %s\n%s", newRequest.url(), chain.connection(), newRequest.headers()));
            HttpClientUtils.printLongString(TAG, "Request data - " + HttpClientUtils.getBodyContentFromRequest(TAG, newRequest));

            long tStart = System.nanoTime();
            Response origResp = chain.proceed(newRequest);
            long tEnd = System.nanoTime();


            Object[] objs = HttpClientUtils.getBodyContentFromResponse(TAG, origResp);


            Log.d(TAG, String.format(GlobalProperties.FORMATTER_LOCALE, "Received response for %s in %.1fms\n%s", origResp.request().url(), (tEnd - tStart) / 1e6d, origResp.headers()));
            HttpClientUtils.printLongString(TAG, "Response data - " + objs[1]);

            return (Response)objs[0];
        }
    }
}
