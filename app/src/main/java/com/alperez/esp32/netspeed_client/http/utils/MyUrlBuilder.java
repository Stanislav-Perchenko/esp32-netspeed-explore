package com.alperez.esp32.netspeed_client.http.utils;

import android.support.annotation.Nullable;

import java.util.Map;

import okhttp3.HttpUrl;

/**
 * Helper class which wraps the HttpUrl.Builder and allows to add
 * banch of parameters as Map<String, String> - See addGroupParameters(Map<String, String> params)
 *
 * Created by stanislav.perchenko on 10/24/2016.
 */

public class MyUrlBuilder {
    private HttpUrl.Builder hUrlBuilder;

    public MyUrlBuilder(String url, String fullApi) {
        StringBuilder finalUrl = new StringBuilder(url);
        if (!url.endsWith("/") && !fullApi.startsWith("/")) {
            finalUrl.append('/');
        }
        finalUrl.append(fullApi);

        hUrlBuilder = HttpUrl.parse(finalUrl.toString()).newBuilder();
    }

    public MyUrlBuilder addQueryParameter(String name, String value) {
        hUrlBuilder.addQueryParameter(name, value);
        return this;
    }

    public MyUrlBuilder addGroupParameters(@Nullable Map<String, String> params) {
        if (params != null) {
            for (String key : params.keySet()) {
                hUrlBuilder.addQueryParameter(key, params.get(key));
            }
        }
        return this;
    }

    public HttpUrl build() {
        return hUrlBuilder.build();
    }
}

