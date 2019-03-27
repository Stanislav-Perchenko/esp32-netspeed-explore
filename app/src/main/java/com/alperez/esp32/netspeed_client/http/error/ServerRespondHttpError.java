package com.alperez.esp32.netspeed_client.http.error;

/**
 * Created by stanislav.perchenko on 3/26/2019
 */
public interface ServerRespondHttpError extends HttpError {
    int httpStatusCode();
    String httpStatusMessage();
}
