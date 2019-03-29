package com.alperez.esp32.netspeed_client.http.error;

import java.io.IOException;

/**
 * Created by stanislav.perchenko on 3/26/2019
 */
public class IOHttpError implements HttpError, ExceptionProvidingError {

    private final IOException ioException;

    public IOHttpError(IOException ioException) {
        this.ioException = ioException;
    }

    @Override
    public Exception getException() {
        return ioException;
    }
}
