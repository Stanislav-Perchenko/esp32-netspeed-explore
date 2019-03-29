package com.alperez.esp32.netspeed_client.http.error;

/**
 * Created by stanislav.perchenko on 3/29/2019
 */
public interface ExceptionProvidingError {
    Exception getException();
}
