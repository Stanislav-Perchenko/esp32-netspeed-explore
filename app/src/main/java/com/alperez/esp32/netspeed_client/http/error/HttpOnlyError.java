package com.alperez.esp32.netspeed_client.http.error;

import com.google.auto.value.AutoValue;

/**
 * Created by stanislav.perchenko on 3/27/2019
 */
@AutoValue
public abstract class HttpOnlyError implements ServerRespondHttpError {
    public abstract int httpStatusCode();
    public abstract String httpStatusMessage();

    public static HttpOnlyError create(int code, String message) {
        return new AutoValue_HttpOnlyError(code, message);
    }
}
