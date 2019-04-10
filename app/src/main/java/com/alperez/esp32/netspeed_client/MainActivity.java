package com.alperez.esp32.netspeed_client;

import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentTransaction;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.RadioGroup;
import android.widget.SeekBar;
import android.widget.TextView;

import com.alperez.esp32.netspeed_client.core.UDPReceiver;
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
    private View vFullScreenProgress;
    private ViewGroup vStatisticsPanel;
    private TextView vTxtRemoteDataSize;
    private TextView vTxtRemoteSpeed;

    private TextView vTxtLocalPkgTotal;
    private TextView vTxtLocalPkgFailed;
    private TextView vTxtLocalPercentFailed;
    private TextView vTxtLocalBytesReceived;
    private TextView vTxtLocalRcvSpeed;

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

        (vFullScreenProgress = findViewById(R.id.full_screen_progress)).setVisibility(View.GONE);


        (vStatisticsPanel = (ViewGroup) findViewById(R.id.statistics_panel)).setVisibility(View.GONE);

        vTxtRemoteDataSize = (TextView) vStatisticsPanel.findViewById(R.id.remote_data_size);
        vTxtRemoteSpeed = (TextView) vStatisticsPanel.findViewById(R.id.remote_speed);
        vTxtLocalPkgTotal = (TextView) vStatisticsPanel.findViewById(R.id.local_n_packs_total);
        vTxtLocalPkgFailed = (TextView) vStatisticsPanel.findViewById(R.id.local_n_packs_failed);
        vTxtLocalPercentFailed = (TextView) vStatisticsPanel.findViewById(R.id.local_percent_failed);
        vTxtLocalBytesReceived = (TextView) vStatisticsPanel.findViewById(R.id.local_n_bytes_total);
        vTxtLocalRcvSpeed = (TextView) vStatisticsPanel.findViewById(R.id.local_speed);

        findViewById(R.id.action_start).setOnClickListener(this::onAction);
        findViewById(R.id.action_stop ).setOnClickListener(this::onAction);
        findViewById(R.id.action_stat ).setOnClickListener(this::onAction);
    }

    @Override
    public void onBackPressed() {
        if (!removeOverlayFragment()) {
            super.onBackPressed();
        }
    }

    private void updatePackageSize(int progress) {
        progress ++;
        if (progress < 13) {
            mPkgSize = progress * 256;
        } else if (progress < 23) {
            mPkgSize = (12 + 2*(progress-12)) * 256;
        } else {
            mPkgSize = (32 + 4*(progress-22)) * 256;
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

    private boolean removeOverlayFragment() {
        if (overlayFragment == null) return false;

        getSupportFragmentManager().beginTransaction()
                .setCustomAnimations(R.anim.fly_from_left, R.anim.fly_to_right)
                .remove(overlayFragment)
                .commit();
        overlayFragment = null;
        return true;
    }


    private int mFullScreenProgressDepth;
    private void showFullScreenProgressIncremental() {
        if (++ mFullScreenProgressDepth == 1) vFullScreenProgress.setVisibility(View.VISIBLE);
    }

    private void hideFullScreenProgressIncremental() {
        if (mFullScreenProgressDepth > 0) {
            mFullScreenProgressDepth --;
            if (mFullScreenProgressDepth == 0) vFullScreenProgress.setVisibility(View.GONE);
        }
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
        if (request != null) {
            mRequestsInProgress.put(request.getSequenceNumber(), request);
            showFullScreenProgressIncremental();
        }
    }


    private final HttpCallback<StatusApiModel> statusCallback = new HttpCallback<StatusApiModel>() {
        @Override
        public void onComplete(int seqNumber, JSONObject rawJson, @Nullable StatusApiModel parsedData) {
            if ((parsedData != null) && (parsedData.statusModel != null) && (parsedData.statisticsModel !=null)) {
                setRemoteDeviceStatus(parsedData);
            }
            hideFullScreenProgressIncremental();
            showHttpSuccessResult(mRequestsInProgress.remove(seqNumber), rawJson);
        }

        @Override
        public void onError(int seqNumber, HttpError error) {
            hideFullScreenProgressIncremental();
            showHttpRequestError(mRequestsInProgress.remove(seqNumber), error);
        }
    };

    private final HttpCallback<Void> startCallback = new HttpCallback<Void>() {
        @Override
        public void onComplete(int seqNumber, JSONObject rawJson, @Nullable Void parsedData) {
            hideFullScreenProgressIncremental();
            showHttpSuccessResult(mRequestsInProgress.remove(seqNumber), rawJson);
            startReceiver(mPort);
            vStatisticsPanel.setVisibility(View.VISIBLE);
        }

        @Override
        public void onError(int seqNumber, HttpError error) {
            hideFullScreenProgressIncremental();
            showHttpRequestError(mRequestsInProgress.remove(seqNumber), error);
        }
    };

    private final HttpCallback<Void> stopCallback = new HttpCallback<Void>() {
        @Override
        public void onComplete(int seqNumber, JSONObject rawJson, @Nullable Void parsedData) {
            hideFullScreenProgressIncremental();
            showHttpSuccessResult(mRequestsInProgress.remove(seqNumber), rawJson);
            stopReceiver();
            vStatisticsPanel.setVisibility(View.GONE);
        }

        @Override
        public void onError(int seqNumber, HttpError error) {
            hideFullScreenProgressIncremental();
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


    /**********************************************************************************************/
    /******************************  Display Remote device statistics  ****************************/
    /**********************************************************************************************/
    private StatusApiModel mRemoteDeviceStatus;

    private void setRemoteDeviceStatus(StatusApiModel nStatus) {
        final String prevDeviceState = (mRemoteDeviceStatus == null) ? "IDLE" : mRemoteDeviceStatus.statusModel.getDeviceState();
        mRemoteDeviceStatus = nStatus;

        final int n_packs = nStatus.statisticsModel.getnPackagesSent();
        vTxtRemoteDataSize.setText(String.format("%d/%d", n_packs, n_packs * nStatus.statusModel.getTransmitPackageSize()));
        final float spd = nStatus.statisticsModel.getSpeedBytesPerSecond() / 1000f;
        vTxtRemoteSpeed.setText(String.format("%.1f", spd));


        if ("IDLE".equals(prevDeviceState) && "TRANS".equals(mRemoteDeviceStatus.statusModel.getDeviceState())) {
            vStatisticsPanel.setVisibility(View.VISIBLE);
        } else if ("IDLE".equals(mRemoteDeviceStatus.statusModel.getDeviceState())) {
            vStatisticsPanel.setVisibility(View.GONE);
        }
    }


    private UDPReceiver udpReceiver;

    private void startReceiver(int port) {
        if (udpReceiver != null) {
            if((udpReceiver.getPort() == port) && udpReceiver.isAlive()) {
                return;
            } else if (udpReceiver.isAlive()) {
                udpReceiver.release();
            }
        }
        udpReceiver = new UDPReceiver(port, "esp-udp-receiver", stats -> {
            vTxtLocalPkgTotal.setText(""+stats.nTotalPkgReceived);
            vTxtLocalPkgFailed.setText(""+stats.nPkgFailed);
            vTxtLocalPercentFailed.setText(String.format("%.2f%%", stats.nPkgFailed*100f/stats.nTotalPkgReceived));
            vTxtLocalBytesReceived.setText(""+stats.nBytesReceived);
            vTxtLocalRcvSpeed.setText(String.format("%.1f kbit/s", stats.speed/1000f));
        });
    }

    private void stopReceiver() {
        if ((udpReceiver != null) && udpReceiver.isAlive()) {
            udpReceiver.release();
            udpReceiver = null;
        }
    }


}
