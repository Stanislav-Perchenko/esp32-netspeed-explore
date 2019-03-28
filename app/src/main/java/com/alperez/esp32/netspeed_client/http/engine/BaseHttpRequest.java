package com.alperez.esp32.netspeed_client.http.engine;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.text.TextUtils;

import com.alperez.esp32.netspeed_client.GlobalProperties;
import com.alperez.esp32.netspeed_client.http.error.ApiError;
import com.alperez.esp32.netspeed_client.http.error.ApiHttpError;
import com.alperez.esp32.netspeed_client.http.error.HttpError;
import com.alperez.esp32.netspeed_client.http.error.HttpOnlyError;
import com.alperez.esp32.netspeed_client.http.error.IOHttpError;
import com.alperez.esp32.netspeed_client.http.error.LocalHttpError;
import com.alperez.esp32.netspeed_client.http.error.ParseHttpError;
import com.alperez.esp32.netspeed_client.http.utils.HttpClientProvider;
import com.alperez.esp32.netspeed_client.http.utils.MyUrlBuilder;
import com.alperez.esp32.netspeed_client.http.utils.ParseResponseHandler;
import com.google.gson.Gson;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import okhttp3.Headers;
import okhttp3.HttpUrl;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

/**
 * Created by stanislav.perchenko on 3/26/2019
 */
public abstract class BaseHttpRequest<T> {

    public enum Method {
        GET, POST, PUT, DELETE
    }

    private static final AtomicInteger SEQUENCE_COUNTER = new AtomicInteger(0);

    private final int sequenceNumber;
    private final String api;
    private OkHttpClient httpClient;
    @Nullable
    private Gson parserGson;

    private HttpUrl finalHttpUrlWithParams;

    protected BaseHttpRequest(String formattableApi, Object... apiArgs) {
        this.sequenceNumber = SEQUENCE_COUNTER.incrementAndGet();
        this.api = ((apiArgs != null) && (apiArgs.length > 0))
                ? String.format(GlobalProperties.FORMATTER_LOCALE, formattableApi, apiArgs)
                : formattableApi;
    }

    public int getSequenceNumber() {
        return sequenceNumber;
    }


    /**********************************************************************************************/
    /*****************************  Request creation and execution  *******************************/
    /**********************************************************************************************/

    public static <TB> BaseHttpRequest.Builder<TB> withHttpClient(OkHttpClient httpClient) {
        throw new RuntimeException("This method must be hidden in subclasses");
    }

    private void setHttpClient(OkHttpClient httpClient) {
        this.httpClient = httpClient;
    }

    private void setParserGson(@Nullable Gson parserGson) {
        this.parserGson = parserGson;
    }


    public abstract static class Builder<TT> {

        protected abstract BaseHttpRequest<TT> getRequestInstance();

        private OkHttpClient client;
        private HttpExecutor httpExecutor;
        private ParseResponseHandler<TT> responseParser;

        public final Builder<TT> setHttpClient(OkHttpClient client) {
            getRequestInstance().setHttpClient(this.client = client);
            return this;
        }

        public final Builder<TT> setParserGson(@Nullable Gson parserGson) {
            getRequestInstance().setParserGson(parserGson);
            return this;
        }

        public final Builder<TT> useResponseParser(@Nullable ParseResponseHandler<TT> responseParser) {
            this.responseParser = responseParser;
            return this;
        }

        public final Builder<TT> onExecutor(HttpExecutor executor) {
            httpExecutor = executor;
            return this;
        }

        /**
         * Override this method in subclasses to validate subclass parameters set
         * @return
         */
        public BaseHttpRequest<TT> build() {
            if (client == null) {
                throw new IllegalStateException("HTTP client is not set");
            } else {
                return getRequestInstance();
            }
        }

        public final BaseHttpRequest<TT> execute(@NonNull HttpCallback<TT> callback) {
            assert (callback != null);
            if (httpExecutor == null) {
                throw new IllegalStateException("HTTP executor is not set");
            } else {
                BaseHttpRequest<TT> req = build();
                httpExecutor.executeHttpRequest(req, responseParser, callback);
                return req;
            }
        }
    }




    /**********************************************************************************************/
    /***********************  Request logic to be implemented by subclasses  **********************/
    /**********************************************************************************************/

    protected abstract Method httpMethod();
    protected abstract void onSetParameters(Map<String, String> parameters);

    /**
     * Subclass may override this method to make modification to the URL Builder, before the final
     * URL is built;
     * @param urlBuilder
     * @return
     */
    protected MyUrlBuilder onInterceptUrlBuilder(MyUrlBuilder urlBuilder) {
        return urlBuilder;
    }

    /**
     * Subclass may override this method to provide a set of custom headers
     * @return
     */
    protected  Map<String, String> optMoreHeaders() {
        return null;
    }

    protected String bodyString() {
        return null;
    }


    /**********************************************************************************************/
    /********************************  Request execution logic  ***********************************/
    /**********************************************************************************************/



    public HttpUrl getFinalHttpUrlWithParams() {
        if (finalHttpUrlWithParams == null) {
            Map<String, String> customParams = new HashMap<>();
            onSetParameters(customParams);


            MyUrlBuilder urlBuilder = new MyUrlBuilder(GlobalProperties.BACKEND_API_URL, api)
                    .addGroupParameters(customParams);
            finalHttpUrlWithParams = onInterceptUrlBuilder(urlBuilder).build();
        }
        return finalHttpUrlWithParams;
    }

    public HttpResponse executeSynchronously(@Nullable ParseResponseHandler<T> optParser) {
        HttpResponse response = new HttpResponse(getSequenceNumber());
        try {

            //--- Create OkHTTP request ---
            Request.Builder reqBuilder = new Request.Builder().url(getFinalHttpUrlWithParams()).headers(HttpClientProvider.DEFAULT_HEADERS);
            Map<String, String> customHeaders = optMoreHeaders();

            //--- Set custom headers ---
            if (customHeaders != null) {
                for (String key : customHeaders.keySet()) {
                    String hValue = customHeaders.get(key);
                    if (!TextUtils.isEmpty(hValue)) reqBuilder.addHeader(key, hValue);
                }
            }

            //--- Set request method and body ---
            switch (httpMethod()) {
                case GET:
                    reqBuilder.get();
                    break;
                case POST:
                    reqBuilder.post(RequestBody.create(HttpClientProvider.MEDIA_TYPE_POST, bodyString()));
                    break;
                case PUT:
                    reqBuilder.put(RequestBody.create(HttpClientProvider.MEDIA_TYPE_POST, bodyString()));
                    break;
                case DELETE:
                    final RequestBody body = RequestBody.create(HttpClientProvider.MEDIA_TYPE_POST, bodyString());
                    if (body != null) {
                        reqBuilder.delete(body);
                    } else {
                        reqBuilder.delete();
                    }
                    break;
            }

            //----  Execute request  ----
            String httpRespPayload = "";
            try {
                Response okResponse = httpClient.newCall(reqBuilder.build()).execute();
                response.serverRespond = true;
                response.httpCode = okResponse.code();
                response.httpMessage = okResponse.message();
                response.headers = okResponse.headers();

                httpRespPayload = okResponse.body().string();
                final boolean hasPayload = !TextUtils.isEmpty(httpRespPayload);
                if (hasPayload) response.rawJson = new JSONObject(httpRespPayload);
                final boolean apiSuccess = hasPayload ? response.rawJson.getBoolean("success") : false;

                if ((response.httpCode >= 200) && (response.httpCode < 300) && apiSuccess) {
                    if (optParser != null) {
                        String dataJText = response.rawJson.getJSONObject("data").toString();
                        response.parsedData = optParser.parseResponse(dataJText, getFinalGson());
                    }
                } else if (hasPayload) {
                    ApiHttpError.Builder eBuild = ApiHttpError.builder().setHttpCode(response.httpCode).setHttpMessage(response.httpMessage);
                    JSONArray jErrors = response.rawJson.optJSONArray("errors");
                    if (jErrors != null) {
                        for (int i = 0; i < jErrors.length(); i++) {
                            JSONObject jE = jErrors.getJSONObject(i);
                            eBuild.addApiError(new ApiError(jE.getInt("code"), jE.getString("message")));
                        }
                    }
                    response.error = eBuild.build();
                } else {
                    response.error = HttpOnlyError.create(response.httpCode, response.httpMessage);
                }

            } catch (IOException e) {
                e.printStackTrace();
                response.error = new IOHttpError(e);
            } catch (JSONException e) {
                e.printStackTrace();
                response.error = ParseHttpError.builder().setHttpStatusCode(response.httpCode).setHttpStatusMessage(response.httpMessage).setHttpResponsePayload(httpRespPayload).setParseException(e).build();
            }

        } catch (Exception ex) {
            response.error = new LocalHttpError(ex);
        }

        return response;
    }

    private Gson getFinalGson() {
        if (parserGson == null) {
            parserGson = new Gson();
        }
        return parserGson;
    }


    /**********************************************************************************************/
    /*************************************  Response model  ***************************************/
    /**********************************************************************************************/

    public static class HttpResponse<DT> {
        private final int sequenceNumber;
        private boolean serverRespond;
        private int httpCode = -1;
        private String httpMessage = "";
        private Headers headers;
        private JSONObject rawJson;
        private DT parsedData;
        private HttpError error;


        private HttpResponse(int sequenceNumber) {
            this.sequenceNumber = sequenceNumber;
        }

        public boolean isSuccess() {
            return error == null;
        }

        public boolean wasServerRespond() {
            return serverRespond;
        }

        public int httpCode() {
            return httpCode;
        }

        public String httpMessage() {
            return httpMessage;
        }

        public int getSequenceNumber() {
            return sequenceNumber;
        }

        @Nullable
        public HttpError getError() {
            return error;
        }

        @Nullable
        public JSONObject getRawJson() {
            return rawJson;
        }

        @Nullable
        public DT getData() {
            return parsedData;
        }

        @Nullable
        public Headers getHeaders() {
            return headers;
        }
    }

}
