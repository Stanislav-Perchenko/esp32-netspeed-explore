package com.alperez.esp32.netspeed_client.http.impl;

import com.alperez.esp32.netspeed_client.http.engine.BaseHttpRequest;

import java.util.Map;

import okhttp3.OkHttpClient;

/**
 * Created by stanislav.perchenko on 3/27/2019
 */
public class StartHttpRequest extends BaseHttpRequest<Void> {

    Integer port;
    String protocol;
    Integer pkgSize;

    private StartHttpRequest() {
        super("/start/transmit");
    }

    @Override
    protected Method httpMethod() {
        return Method.GET;
    }

    @Override
    protected void onSetParameters(Map<String, String> parameters) {
        parameters.put("protocol", protocol);
        parameters.put("port", port.toString());
        parameters.put("pkg_size", pkgSize.toString());
    }


    public static Builder withHttpClient(OkHttpClient httpClient) {
        Builder b = new Builder();
        b.setHttpClient(httpClient);
        return b;
    }



    public static class Builder extends BaseHttpRequest.Builder<Void> {
        private final StartHttpRequest request = new StartHttpRequest();

        @Override
        protected StartHttpRequest getRequestInstance() {
            return request;
        }

        public Builder setPort(int port) {
            request.port = port;
            return this;
        }

        public Builder setPackageSize(int pkgSize) {
            request.pkgSize = pkgSize;
            return this;
        }

        public Builder setProtocol(String protocol) {
            request.protocol = protocol;
            return this;
        }

        @Override
        public BaseHttpRequest<Void> build() {
            //TODO Validate request parameters !!!!!!!!!!!!!!!!!!!!!!!
            return super.build();
        }
    }

}
