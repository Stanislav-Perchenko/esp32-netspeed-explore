package com.alperez.esp32.netspeed_client.http.impl;

import com.alperez.esp32.netspeed_client.http.engine.BaseHttpRequest;

import java.util.Map;

import okhttp3.OkHttpClient;

/**
 * Created by stanislav.perchenko on 3/27/2019
 */
public class StopHttpRequest extends BaseHttpRequest<Void> {

    private StopHttpRequest() {
        super("/stop");
    }

    public static BaseHttpRequest.Builder<Void> withHttpClient(OkHttpClient httpClient) {
        return new BaseHttpRequest.Builder<Void>() {
            private final StopHttpRequest request = new StopHttpRequest();
            @Override
            protected StopHttpRequest getRequestInstance() {
                return request;
            }
        }.setHttpClient(httpClient);
    }

    @Override
    public Method httpMethod() {
        return Method.GET;
    }

    @Override
    protected void onSetParameters(Map<String, String> parameters) {
        //No params
    }
}
