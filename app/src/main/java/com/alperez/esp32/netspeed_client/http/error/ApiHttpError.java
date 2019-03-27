package com.alperez.esp32.netspeed_client.http.error;

import android.support.annotation.NonNull;
import android.text.TextUtils;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by stanislav.perchenko on 3/26/2019
 */
public class ApiHttpError implements ServerRespondHttpError {
    private int httpCode;
    private String httpMessage;
    private List<ApiError> apiErrors;

    private ApiHttpError() { }

    @Override
    public int httpStatusCode() {
        return httpCode;
    }

    @Override
    public String httpStatusMessage() {
        return httpMessage;
    }

    public List<ApiError> apiErrors() {
        return apiErrors;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private final List<ApiError> aErrs;
        Integer code;
        String msg;
        private Builder() {
            aErrs = new ArrayList<>(1);
        }

        public Builder setHttpCode(int httpCode) {
            this.code = httpCode;
            return this;
        }

        public Builder setHttpMessage(String httpMessage) {
            this.msg = httpMessage;
            return this;
        }

        public Builder addApiError(@NonNull ApiError err) {
            assert (err != null);
            aErrs.add(err);
            return this;
        }

        public ApiHttpError build() {
            if (code == null) throw new IllegalStateException("HTTP response code is not set");
            else if (TextUtils.isEmpty(msg)) throw new IllegalStateException("HTTP message is not set");

            ApiHttpError model = new ApiHttpError();
            model.httpCode = this.code;
            model.httpMessage = this.msg;
            model.apiErrors = this.aErrs;
            return model;
        }
    }
}
