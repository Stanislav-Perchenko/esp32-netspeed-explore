package com.alperez.esp32.netspeed_client.http.engine;

import android.support.annotation.Nullable;

import com.alperez.esp32.netspeed_client.http.error.HttpError;

import org.json.JSONObject;

/**
 * Created by stanislav.perchenko on 3/26/2019
 */
public interface HttpCallback<T> {
    void onComplete(int seqNumber, JSONObject rawJson, @Nullable T parsedData);
    void onError(int seqNumber, HttpError error);
}
