package com.alperez.esp32.netspeed_client.http.impl;

import com.alperez.esp32.netspeed_client.http.BaseHttpRequest;
import com.alperez.esp32.netspeed_client.model.StatusModel;

import java.util.Map;

import okhttp3.OkHttpClient;

/**
 * Created by stanislav.perchenko on 3/26/2019
 */
public class StatusHttpRequest extends BaseHttpRequest<StatusModel> {

    private StatusHttpRequest() {
        super("/status");
    }


    public static BaseHttpRequest.Builder<StatusModel> withHttpClient(OkHttpClient httpClient) {
        return new BaseHttpRequest.Builder<StatusModel>() {
            private final StatusHttpRequest request = new StatusHttpRequest();
            @Override
            protected StatusHttpRequest getRequestInstance() {
                return request;
            }
        }.setHttpClient(httpClient);
    }

    @Override
    protected Method httpMethod() {
        return Method.GET;
    }

    @Override
    protected void onSetParameters(Map<String, String> parameters) {
        //No params
    }
}
