package com.alperez.esp32.netspeed_client;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentTransaction;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.RadioGroup;
import android.widget.SeekBar;
import android.widget.TextView;

import com.alperez.esp32.netspeed_client.http.engine.BaseHttpRequest;
import com.alperez.esp32.netspeed_client.http.engine.HttpCallback;
import com.alperez.esp32.netspeed_client.http.engine.HttpExecutor;
import com.alperez.esp32.netspeed_client.http.error.HttpError;
import com.alperez.esp32.netspeed_client.http.impl.StartHttpRequest;
import com.alperez.esp32.netspeed_client.http.impl.StatusHttpRequest;
import com.alperez.esp32.netspeed_client.http.impl.StopHttpRequest;
import com.alperez.esp32.netspeed_client.http.utils.HttpClientProvider;
import com.alperez.esp32.netspeed_client.http.utils.ParseResponseHandler;
import com.alperez.esp32.netspeed_client.model.StatusApiModel;
import com.alperez.esp32.netspeed_client.model.StatusModel;
import com.google.gson.Gson;

import org.json.JSONObject;

import java.util.HashMap;
import java.util.Map;

import okhttp3.OkHttpClient;

public class MainActivity extends AppCompatActivity implements HttpErrorDisplayFragment.BadHttpResultProvider, HttpSuccessDisplayFragment.SuccessHttpResultProvider {


    private RadioGroup vRgProtocol;
    private TextView vTxtPort;
    private TextView vTxtSize;
    private SeekBar vSeek;

    //----  Device parameters  ----
    private int mPkgSize;
    private int mPort = 1111;
    private String mProtocol;


    private HttpExecutor httpExecutor;
    private Map<Integer, BaseHttpRequest<?>> mRequestsInProgress = new HashMap<>();

    private Fragment overlayFragment;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        httpExecutor = new HttpExecutor();


        vRgProtocol = (RadioGroup) findViewById(R.id.radio_select_protocol);
        vRgProtocol.setOnCheckedChangeListener((group, checkedId) -> checkProtocol(checkedId));
        checkProtocol(vRgProtocol.getCheckedRadioButtonId());

        (vTxtPort = (TextView) findViewById(R.id.txt_port)).setOnClickListener(v -> showPortDialog());
        vTxtPort.setText(""+mPort);

        vTxtSize = (TextView) findViewById(R.id.txt_pkg_size);
        (vSeek = (SeekBar) findViewById(R.id.seek_size)).setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                updatePackageSize(progress);
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {

            }
        });
        updatePackageSize(vSeek.getProgress());


        findViewById(R.id.action_start).setOnClickListener(this::onAction);
        findViewById(R.id.action_stop ).setOnClickListener(this::onAction);
        findViewById(R.id.action_stat ).setOnClickListener(this::onAction);
    }



    private void updatePackageSize(int progress) {
        progress ++;
        if (progress < 13) {
            mPkgSize = progress * 1024;
        } else if (progress < 23) {
            mPkgSize = (12 + 2*(progress-12)) * 1024;
        } else {
            mPkgSize = (32 + 4*(progress-22)) * 1024;
        }
        vTxtSize.setText(""+mPkgSize);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        httpExecutor.release();
    }

    private final ParseResponseHandler<StatusModel> statusParser = (String json, Gson parser) -> {
        StatusApiModel data = parser.fromJson(json, StatusApiModel.class);
        return data.statusModel;
    };

    private void showPortDialog() {
        NumberSelectionDialog dial = NumberSelectionDialog.builder()
                .setTitle("Select PORT")
                .setRange(1000, 9999)
                .setValue(mPort)
                .setWrapSelectorWheel(true)
                .setNegativeButton(android.R.string.cancel)
                .setPositiveButton(android.R.string.ok)
                .setCancellable(false)
                .build();
        dial.setOnNumbedSelectListener(value -> vTxtPort.setText(""+(mPort = value)));
        dial.show(getSupportFragmentManager(), "N participants picker");
    }

    private void checkProtocol(final int checkedId) {
        switch (checkedId) {
            case R.id.radio_udp:
                mProtocol = "UDP";
                break;
            case R.id.radio_tcp:
                mProtocol = "TCP";
                break;
            default:
                throw new RuntimeException("Wrong rad. butt. ID");
        }
    }

    private void showOverlayFragment(Fragment f) {
        final boolean needReplace = overlayFragment != null;
        overlayFragment = f;
        FragmentTransaction ft = getSupportFragmentManager().beginTransaction().setCustomAnimations(R.anim.fly_from_right, R.anim.fly_to_left);
        if (needReplace) {
            ft.replace(R.id.overlay_fragment_container, overlayFragment);
        } else {
            ft.add(R.id.overlay_fragment_container, overlayFragment);
        }
        ft.commit();
    }

    private void removeOverlayFragment() {
        //TODO !!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!
    }


    /**********************************************************************************************/
    /******************************  Make HTTP calls  *********************************************/
    /**********************************************************************************************/
    private void onAction(View v) {
        OkHttpClient httpClient = HttpClientProvider.getInstance().getDefaultRESTClient();
        BaseHttpRequest<?> request;
        switch (v.getId()) {
            case R.id.action_start:
                request = StartHttpRequest
                        .withHttpClient(httpClient)
                        .setProtocol(mProtocol)
                        .setPort(mPort)
                        .setPackageSize(mPkgSize)
                        .onExecutor(httpExecutor)
                        .execute(startCallback);
                break;
            case R.id.action_stop:
                request = StopHttpRequest.withHttpClient(httpClient).onExecutor(httpExecutor).execute(stopCallback);
                break;
            case R.id.action_stat:
                request = StatusHttpRequest
                        .withHttpClient(httpClient)
                        .useResponseParser((responseText, parser) -> parser.fromJson(responseText, StatusApiModel.class))
                        .onExecutor(httpExecutor)
                        .execute(statusCallback);
                break;
            default:
                request = null;
        }
        if (request != null) mRequestsInProgress.put(request.getSequenceNumber(), request);
    }






    private final HttpCallback<StatusApiModel> statusCallback = new HttpCallback<StatusApiModel>() {
        @Override
        public void onComplete(int seqNumber, JSONObject rawJson, @Nullable StatusApiModel parsedData) {
            showHttpSuccessResult(mRequestsInProgress.remove(seqNumber), rawJson);
        }

        @Override
        public void onError(int seqNumber, HttpError error) {
            showHttpRequestError(mRequestsInProgress.remove(seqNumber), error);
        }
    };

    private final HttpCallback<Void> startCallback = new HttpCallback<Void>() {
        @Override
        public void onComplete(int seqNumber, JSONObject rawJson, @Nullable Void parsedData) {
            showHttpSuccessResult(mRequestsInProgress.remove(seqNumber), rawJson);
        }

        @Override
        public void onError(int seqNumber, HttpError error) {
            showHttpRequestError(mRequestsInProgress.remove(seqNumber), error);
        }
    };

    private final HttpCallback<Void> stopCallback = new HttpCallback<Void>() {
        @Override
        public void onComplete(int seqNumber, JSONObject rawJson, @Nullable Void parsedData) {
            showHttpSuccessResult(mRequestsInProgress.remove(seqNumber), rawJson);
        }

        @Override
        public void onError(int seqNumber, HttpError error) {
            showHttpRequestError(mRequestsInProgress.remove(seqNumber), error);
        }
    };


    /**********************************************************************************************/
    /******************************  Display HTTP request success  ********************************/
    /**********************************************************************************************/
    private final Map<Integer, HttpSuccessDisplayFragment.SuccessHttpResultViewModel> successViewModels = new HashMap<>();

    private void showHttpSuccessResult(BaseHttpRequest<?> request, JSONObject rawJson) {
        if (request == null) return;
        HttpSuccessDisplayFragment.SuccessHttpResultViewModel viewModel = new HttpSuccessDisplayFragment.SuccessHttpResultViewModel(request.httpMethod(), request.getFinalHttpUrlWithParams().toString(), 200, "OK", rawJson);
        successViewModels.put(request.getSequenceNumber(), viewModel);

        showOverlayFragment(HttpSuccessDisplayFragment.newInstance(request.getSequenceNumber()));
    }

    @Override
    public HttpSuccessDisplayFragment.SuccessHttpResultViewModel getSuccessHttpResponseBySequenceNumber(int httpReqSequenceNum) {
        return successViewModels.get(httpReqSequenceNum);
    }

    @Override
    public void removeSuccessHttpResponseFromCache(int httpReqSequenceNum) {
        successViewModels.remove(httpReqSequenceNum);
    }

    /**********************************************************************************************/
    /******************************  Display HTTP request failure  ********************************/
    /**********************************************************************************************/

    private final Map<Integer, HttpErrorDisplayFragment.BadHttpResultViewModel> errorViewModels = new HashMap<>();

    private void showHttpRequestError(BaseHttpRequest<?> request, HttpError error) {
        if (request == null) return;
        HttpErrorDisplayFragment.BadHttpResultViewModel viewModel = new HttpErrorDisplayFragment.BadHttpResultViewModel(request.httpMethod(), request.getFinalHttpUrlWithParams().toString(), error);
        errorViewModels.put(request.getSequenceNumber(), viewModel);

        showOverlayFragment(HttpErrorDisplayFragment.newInstance(request.getSequenceNumber()));
    }

    @Override
    public HttpErrorDisplayFragment.BadHttpResultViewModel getFailedHttpResponseBySequenceNumber(int httpReqSequenceNum) {
        return errorViewModels.get(httpReqSequenceNum);
    }

    @Override
    public void removeFailedHttpResponseFromCache(int httpReqSequenceNum) {
        errorViewModels.remove(httpReqSequenceNum);
    }
}
