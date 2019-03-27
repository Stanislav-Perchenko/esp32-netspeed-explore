package com.alperez.esp32.netspeed_client;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;

import com.alperez.esp32.netspeed_client.http.utils.ParseResponseHandler;
import com.alperez.esp32.netspeed_client.http.engine.HttpCallback;
import com.alperez.esp32.netspeed_client.http.engine.HttpExecutor;
import com.alperez.esp32.netspeed_client.http.error.HttpError;
import com.alperez.esp32.netspeed_client.http.impl.StatusHttpRequest;
import com.alperez.esp32.netspeed_client.http.impl.StopHttpRequest;
import com.alperez.esp32.netspeed_client.http.utils.HttpClientProvider;
import com.alperez.esp32.netspeed_client.model.CommonApiModel;
import com.alperez.esp32.netspeed_client.model.StatusModel;
import com.google.gson.Gson;

import org.json.JSONObject;

import okhttp3.OkHttpClient;

public class MainActivity extends AppCompatActivity {


    private HttpExecutor httpExecutor;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        httpExecutor = new HttpExecutor();
        OkHttpClient httpClient = HttpClientProvider.getInstance().getDefaultRESTClient();
        int sn = StatusHttpRequest.withHttpClient(httpClient).setParserGson(new Gson()).useResponseParser(statusParser).onExecutor(null).execute(new HttpCallback<StatusModel>() {
            @Override
            public void onComplete(int seqNumber, JSONObject jData, StatusModel parsedData) {

            }

            @Override
            public void onError(int seqNumber, HttpError error) {

            }
        });

        sn = StopHttpRequest.withHttpClient(httpClient).setParserGson(new Gson()).useResponseParser((json, parser) -> null).onExecutor(httpExecutor).execute(new HttpCallback<Void>() {
            @Override
            public void onComplete(int seqNumber, JSONObject jData, Void parsedData) {

            }

            @Override
            public void onError(int seqNumber, HttpError error) {

            }
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        httpExecutor.release();
    }

    private final ParseResponseHandler<StatusModel> statusParser = (String json, Gson parser) -> {
        CommonApiModel data = parser.fromJson(json, CommonApiModel.class);
        return data.statusModel;
    };
}
