package com.alperez.esp32.netspeed_client.http.error;

/**
 * Created by stanislav.perchenko on 3/26/2019
 */
public class ApiError {

    private final int code;
    private final String message;

    public ApiError(int code, String message) {
        this.code = code;
        this.message = message;
    }

    public int code() {
        return code;
    }

    public String message() {
        return message;
    }
}
