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
import com.alperez.esp32.netspeed_client.http.error.ApiError;
import com.alperez.esp32.netspeed_client.http.error.ApiHttpError;
import com.alperez.esp32.netspeed_client.http.error.ExceptionProvidingError;
import com.alperez.esp32.netspeed_client.http.error.HttpError;
import com.alperez.esp32.netspeed_client.http.error.HttpOnlyError;
import com.alperez.esp32.netspeed_client.http.error.IOHttpError;
import com.alperez.esp32.netspeed_client.http.error.LocalHttpError;
import com.alperez.esp32.netspeed_client.http.error.ParseHttpError;
import com.alperez.esp32.netspeed_client.http.error.ServerRespondHttpError;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * Created by stanislav.perchenko on 3/28/2019
 */
public class HttpErrorDisplayFragment extends Fragment {

    public static class BadHttpResultViewModel {
        public final BaseHttpRequest.Method method;
        public final String url;
        public final HttpError error;

        public BadHttpResultViewModel(BaseHttpRequest.Method method, String url, HttpError error) {
            this.method = method;
            this.url = url;
            this.error = error;
        }
    }

    public interface BadHttpResultProvider {
        BadHttpResultViewModel getFailedHttpResponseBySequenceNumber(int httpReqSequenceNum);
        void removeFailedHttpResponseFromCache(int httpReqSequenceNum);
    }

    public static HttpErrorDisplayFragment newInstance(int httpReqSequenceNum) {
        Bundle args = new Bundle();
        args.putInt("http_seq_num", httpReqSequenceNum);
        HttpErrorDisplayFragment f = new HttpErrorDisplayFragment();
        f.setArguments(args);
        return f;
    }



    private int httpReqSequenceNum;
    private BadHttpResultProvider httpResponseProvider;


    @Override
    public void onAttach(Context context) {
        if (context instanceof BadHttpResultProvider) {
            super.onAttach(context);
            httpResponseProvider = (BadHttpResultProvider) context;
        } else {
            throw new IllegalStateException("A host must implement the BadHttpResultProvider interface");
        }
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        httpReqSequenceNum = getArguments().getInt("http_seq_num");
    }


    private View vContent;
    private TextView vTxtScreenTitle;
    private TextView vTxtRequest;
    private TextView vTxtHttpCode;
    private TextView vTxtExceptionDescription;
    private TextView vTxtRawPayload;
    private ViewGroup vApiErrors, vApiErrorsContainer;


    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        if (vContent == null) {
            vContent = inflater.inflate(R.layout.fragment_http_error_display, container, false);
            vTxtScreenTitle = (TextView) vContent.findViewById(R.id.err_scr_title);
            vTxtRequest = (TextView) vContent.findViewById(R.id.txt_request);
            vTxtHttpCode = (TextView) vContent.findViewById(R.id.txt_resp_code_message);
            vTxtExceptionDescription = (TextView) vContent.findViewById(R.id.txt_exc_descr);
            vTxtRawPayload = (TextView) vContent.findViewById(R.id.txt_raw_payload);
            vApiErrors = (ViewGroup) vContent.findViewById(R.id.api_errors);
            vApiErrorsContainer = (ViewGroup) vApiErrors.findViewById(R.id.api_errors_container);
        } else {
            container.removeView(vContent);
        }
        return vContent;
    }


    private BadHttpResultViewModel badHttpResult;
    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        if (badHttpResult == null) {
            badHttpResult = httpResponseProvider.getFailedHttpResponseBySequenceNumber(httpReqSequenceNum);
            if (badHttpResult != null) {
                httpResponseProvider.removeFailedHttpResponseFromCache(httpReqSequenceNum);
            } else {
                return;
            }
        }
        populateHttpResponse(badHttpResult.method, badHttpResult.url, badHttpResult.error);
    }

    private void populateHttpResponse(BaseHttpRequest.Method method, String url, HttpError error) {
        vTxtScreenTitle.setText(getScreenTitleByErrorType(error.getClass()));
        vTxtRequest.setText(getDisplayRequest(method, url));
        displayHttpCode(error, vTxtHttpCode);
        displayExceptionDescription(error, vTxtExceptionDescription);
        displayRawPayload(error, vTxtRawPayload);
        displayApiErrors(error);
    }

    private String getScreenTitleByErrorType(Class<? extends HttpError> errClass) {
        if (errClass == LocalHttpError.class) {
            return "App internal error";
        } else if (errClass == IOHttpError.class) {
            return "Communication error";
        } else if (errClass == HttpOnlyError.class) {
            return "Server error";
        } else if (errClass == ParseHttpError.class) {
            return "Payload format error";
        } else if (errClass == ApiHttpError.class) {
            return "Client API error";
        } else {
            return "Unknown error";
        }
    }

    private CharSequence getDisplayRequest(BaseHttpRequest.Method method, String url) {
        String txtMethod = method.toString();
        Spannable spanText = new SpannableString(String.format("%s %s", txtMethod, url));
        spanText.setSpan(new StyleSpan(Typeface.BOLD), 0, txtMethod.length(), 0);
        return spanText;
    }

    private void displayHttpCode(HttpError err, TextView tv) {
        if (err instanceof ServerRespondHttpError) {
            ServerRespondHttpError s_err = (ServerRespondHttpError) err;
            Spannable spanText = new SpannableString(String.format("%d %s", s_err.httpStatusCode(), s_err.httpStatusMessage()));
            spanText.setSpan(new StyleSpan(Typeface.BOLD), 0, 3, 0);
            tv.setText(spanText);
            tv.setVisibility(View.VISIBLE);
        } else {
            tv.setVisibility(View.GONE);
        }
    }

    private void displayExceptionDescription(HttpError err, TextView tv) {
        if (err instanceof ExceptionProvidingError) {
            ExceptionProvidingError e_err = (ExceptionProvidingError) err;
            String exName = e_err.getException().getClass().getSimpleName();
            String exMsg = e_err.getException().getMessage();
            Spannable spanText = new SpannableString(String.format("%s - %s", exName, exMsg));
            spanText.setSpan(new StyleSpan(Typeface.BOLD), 0, exName.length(), 0);
            tv.setText(spanText);
            tv.setVisibility(View.VISIBLE);
        } else {
            tv.setVisibility(View.GONE);
        }
    }

    private void displayRawPayload(HttpError err, TextView tv) {
        if (err instanceof ParseHttpError) {
            ParseHttpError p_err = (ParseHttpError) err;

            try {
                JSONObject jPL = new JSONObject(p_err.httpResponsePayload());
                tv.setText(jPL.toString(2));
            } catch (JSONException e) {
                tv.setText(p_err.httpResponsePayload());
            }
            tv.setVisibility(View.VISIBLE);
        } else {
            tv.setVisibility(View.GONE);
        }
    }

    private void displayApiErrors(HttpError error) {
        if (error instanceof ApiHttpError) {
            ApiHttpError api_e = (ApiHttpError) error;
            LayoutInflater inflater = getLayoutInflater();
            vApiErrorsContainer.removeAllViews();
            for (ApiError ae : api_e.apiErrors()) {
                View vItem = inflater.inflate(R.layout.layout_api_error_item, vApiErrorsContainer, false);
                ((TextView) vItem.findViewById(R.id.txt_code)).setText(""+ae.code());
                ((TextView) vItem.findViewById(R.id.txt_message)).setText(ae.message());
                vApiErrorsContainer.addView(vItem, ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            }
            vApiErrors.setVisibility(View.VISIBLE);
        } else {
            vApiErrors.setVisibility(View.GONE);
        }
    }
}
