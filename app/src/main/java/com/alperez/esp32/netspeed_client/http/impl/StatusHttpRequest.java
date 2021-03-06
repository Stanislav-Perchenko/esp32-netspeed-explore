package com.alperez.esp32.netspeed_client.http.impl;

import com.alperez.esp32.netspeed_client.http.engine.BaseHttpRequest;
import com.alperez.esp32.netspeed_client.model.StatusApiModel;

import java.util.Map;

import okhttp3.OkHttpClient;

/**
 * Created by stanislav.perchenko on 3/26/2019
 */
public class StatusHttpRequest extends BaseHttpRequest<StatusApiModel> {

    private StatusHttpRequest() {
        super("/status");
    }


    public static BaseHttpRequest.Builder<StatusApiModel> withHttpClient(OkHttpClient httpClient) {
        return new BaseHttpRequest.Builder<StatusApiModel>() {
            private final StatusHttpRequest request = new StatusHttpRequest();
            @Override
            protected StatusHttpRequest getRequestInstance() {
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
