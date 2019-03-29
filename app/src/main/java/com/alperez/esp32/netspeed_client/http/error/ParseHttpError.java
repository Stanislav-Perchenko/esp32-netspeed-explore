package com.alperez.esp32.netspeed_client.http.error;

import com.google.auto.value.AutoValue;

import org.json.JSONException;

/**
 * Created by stanislav.perchenko on 3/26/2019
 */
@AutoValue
public abstract class ParseHttpError implements ServerRespondHttpError, ExceptionProvidingError {

    public abstract int httpStatusCode();
    public abstract String httpStatusMessage();
    public abstract String httpResponsePayload();
    public abstract JSONException parseException();

    public static Builder builder() {
        return new AutoValue_ParseHttpError.Builder();
    }

    @AutoValue.Builder
    public abstract static class Builder {
        public abstract Builder setHttpStatusCode(int httpStatusCode);
        public abstract Builder setHttpStatusMessage(String httpStatusMessage);
        public abstract Builder setHttpResponsePayload(String httpResponsePayload);
        public abstract Builder setParseException(JSONException parseException);

        public abstract ParseHttpError build();
    }

    @Override
    public Exception getException() {
        return parseException();
    }
}
