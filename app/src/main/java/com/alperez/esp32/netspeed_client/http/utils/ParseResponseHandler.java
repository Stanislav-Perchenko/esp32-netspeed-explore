package com.alperez.esp32.netspeed_client.http.utils;

import com.google.gson.Gson;

/**
 * Created by stanislav.perchenko on 3/27/2019
 */
public interface ParseResponseHandler<T> {
    T parseResponse(String responseText, Gson parser);
}
