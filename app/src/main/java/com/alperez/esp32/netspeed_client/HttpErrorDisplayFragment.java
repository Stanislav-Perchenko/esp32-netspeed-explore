package com.alperez.esp32.netspeed_client;

import android.content.Context;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.alperez.esp32.netspeed_client.http.error.HttpError;

/**
 * Created by stanislav.perchenko on 3/28/2019
 */
public class HttpErrorDisplayFragment extends Fragment {

    public static class BadHttpResultViewModel {
        public final String url;
        public final HttpError error;

        public BadHttpResultViewModel(String url, HttpError error) {
            this.url = url;
            this.error = error;
        }
    }

    public interface BadHttpResultProvider {
        HttpErrorDisplayFragment.BadHttpResultViewModel getHttpResponseBySequenceNumber(int httpReqSequenceNum);
        void removeHttpResponseFromCache(int httpReqSequenceNum);
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

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        if (vContent == null) {
            vContent = inflater.inflate(R.layout.fragment_http_error_display, container, false);
            //TODO Find all UI elements !!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!
        } else {
            container.removeView(vContent);
        }
        return vContent;
    }


    private HttpErrorDisplayFragment.BadHttpResultViewModel badHttpResult;
    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        if (badHttpResult == null) {
            badHttpResult = httpResponseProvider.getHttpResponseBySequenceNumber(httpReqSequenceNum);
            if (badHttpResult != null) {
                httpResponseProvider.removeHttpResponseFromCache(httpReqSequenceNum);
            } else {
                return;
            }
        }
        populateHttpResponse(badHttpResult.url, badHttpResult.error);
    }

    private void populateHttpResponse(String url, HttpError error) {
        //TODO Implement this !!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!
    }
}
