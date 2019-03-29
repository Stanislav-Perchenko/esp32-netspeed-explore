package com.alperez.esp32.netspeed_client.http.error;

/**
 * Created by stanislav.perchenko on 3/26/2019
 */
public class LocalHttpError implements HttpError, ExceptionProvidingError {

    private final Exception ex;

    public LocalHttpError(Exception ex) {
        this.ex = ex;
    }

    @Override
    public Exception getException() {
        return ex;
    }
}
