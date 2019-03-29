package com.alperez.esp32.netspeed_client;

import android.content.Context;
import android.graphics.Typeface;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.style.StyleSpan;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.alperez.esp32.netspeed_client.http.engine.BaseHttpRequest;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * Created by stanislav.perchenko on 3/29/2019
 */
public class HttpSuccessDisplayFragment extends Fragment {

    public static class SuccessHttpResultViewModel {
        public final BaseHttpRequest.Method method;
        public final String url;
        public final int httpCode;
        public final String httpMessage;
        private final JSONObject rawResponseJson;

        public SuccessHttpResultViewModel(BaseHttpRequest.Method method, String url, int httpCode, String httpMessage, JSONObject rawResponseJson) {
            this.method = method;
            this.url = url;
            this.httpCode = httpCode;
            this.httpMessage = httpMessage;
            this.rawResponseJson = rawResponseJson;
        }
    }

    public interface SuccessHttpResultProvider {
        SuccessHttpResultViewModel getSuccessHttpResponseBySequenceNumber(int httpReqSequenceNum);
        void removeSuccessHttpResponseFromCache(int httpReqSequenceNum);
    }

    public static HttpSuccessDisplayFragment newInstance(int httpReqSequenceNum) {
        Bundle args = new Bundle();
        args.putInt("http_seq_num", httpReqSequenceNum);
        HttpSuccessDisplayFragment f = new HttpSuccessDisplayFragment();
        f.setArguments(args);
        return f;
    }

    private int httpReqSequenceNum;
    private SuccessHttpResultProvider httpResponseProvider;

    @Override
    public void onAttach(Context context) {
        if (context instanceof SuccessHttpResultProvider) {
            super.onAttach(context);
            httpResponseProvider = (SuccessHttpResultProvider) context;
        } else {
            throw new IllegalStateException("A host must implement the SuccessHttpResultProvider interface");
        }
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        httpReqSequenceNum = getArguments().getInt("http_seq_num");
    }

    private View vContent;
    private TextView vTxtRequest;
    private TextView vTxtHttpCode;
    private TextView vTxtRawPayload;


    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        if (vContent == null) {
            vContent = inflater.inflate(R.layout.fragment_http_success_display, container, false);
            vTxtRequest = (TextView) vContent.findViewById(R.id.txt_request);
            vTxtHttpCode = (TextView) vContent.findViewById(R.id.txt_resp_code_message);
            vTxtRawPayload = (TextView) vContent.findViewById(R.id.txt_raw_payload);
        } else {
            container.removeView(vContent);
        }
        return vContent;
    }

    private SuccessHttpResultViewModel okHttpResult;
    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        if (okHttpResult == null) {
            okHttpResult = httpResponseProvider.getSuccessHttpResponseBySequenceNumber(httpReqSequenceNum);
            if (okHttpResult != null) {
                httpResponseProvider.removeSuccessHttpResponseFromCache(httpReqSequenceNum);
            } else {
                return;
            }
        }

        vTxtRequest.setText(getDisplayRequest(okHttpResult.method, okHttpResult.url));
        displayHttpCode(okHttpResult.httpCode, okHttpResult.httpMessage, vTxtHttpCode);
        try {
            vTxtRawPayload.setText(okHttpResult.rawResponseJson.toString(2));
        } catch (JSONException e) {
            vTxtRawPayload.setText(okHttpResult.rawResponseJson.toString());
        }
    }

    private CharSequence getDisplayRequest(BaseHttpRequest.Method method, String url) {
        String txtMethod = method.toString();
        Spannable spanText = new SpannableString(String.format("%s %s", txtMethod, url));
        spanText.setSpan(new StyleSpan(Typeface.BOLD), 0, txtMethod.length(), 0);
        return spanText;
    }

    private void displayHttpCode(int code, String message, TextView tv) {
        Spannable spanText = new SpannableString(String.format("%d %s", code, message));
        spanText.setSpan(new StyleSpan(Typeface.BOLD), 0, 3, 0);
        tv.setText(spanText);
    }

}
