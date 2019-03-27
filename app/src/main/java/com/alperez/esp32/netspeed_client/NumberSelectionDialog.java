package com.alperez.esp32.netspeed_client;

import android.app.Dialog;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.DialogFragment;
import android.support.v7.app.AlertDialog;
import android.text.TextUtils;
import android.view.Gravity;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.NumberPicker;

/**
 * Created by stanislav.perchenko on 2/13/2019
 */
public class NumberSelectionDialog extends DialogFragment {
    private static final String ARG_TITLE = "title";
    private static final String ARG_MESSAGE = "msg";
    private static final String ARG_RANGE_START = "min";
    private static final String ARG_RANGE_END = "max";
    private static final String ARG_VALUE = "value";
    private static final String ARG_WRAP = "wrap";
    private static final String ARG_BTN_YES = "btn_y";
    private static final String ARG_BTN_NO = "btn_n";
    private static final String ARG_CANCELLABLE = "cancellable";

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private final Bundle args;
        private Builder(){
            args = new Bundle();
        }

        public Builder setTitle(@Nullable String title) {
            args.putString(ARG_TITLE, title);
            return this;
        }
        public Builder setMessage(@Nullable String message) {
            args.putString(ARG_MESSAGE, message);
            return this;
        }
        public Builder setRange(int min, int max) {
            args.putInt(ARG_RANGE_START, min);
            args.putInt(ARG_RANGE_END, max);
            return this;
        }
        public Builder setValue(int v) {
            args.putInt(ARG_VALUE, v);
            return this;
        }
        public Builder setWrapSelectorWheel(boolean wrap) {
            args.putBoolean(ARG_WRAP, wrap);
            return this;
        }
        public Builder setPositiveButton(@Nullable Integer textResId) {
            args.putInt(ARG_BTN_YES, textResId);
            return this;
        }
        public Builder setNegativeButton(@Nullable Integer textResId) {
            args.putInt(ARG_BTN_NO, textResId);
            return this;
        }
        public Builder setCancellable(boolean cancellable) {
            args.putBoolean(ARG_CANCELLABLE, cancellable);
            return this;
        }

        public NumberSelectionDialog build() {
            NumberSelectionDialog f = new NumberSelectionDialog();
            f.setArguments(args);
            return f;
        }
    }

    public interface OnNumbedSelectListener {
        void onSelected(int v);
    }

    private OnNumbedSelectListener onNumbedSelectListener;

    public void setOnNumbedSelectListener(OnNumbedSelectListener l) {
        this.onNumbedSelectListener = l;
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        final NumberPicker nPicker = new NumberPicker(getActivity());

        FrameLayout vContent = new FrameLayout(getActivity());
        FrameLayout.LayoutParams lp = new FrameLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        lp.gravity = Gravity.CENTER_HORIZONTAL;
        vContent.addView(nPicker, lp);


        Bundle args = getArguments();
        int min = args.getInt(ARG_RANGE_START, -1);
        int max = args.getInt(ARG_RANGE_END, -1);
        if (min < 0 || max < 0) {
            throw new IllegalStateException("Both Range START and Range END must be provided");
        } else if (max <= min) {
            throw new IllegalStateException("Range END must be greater thr Range START");
        } else {
            nPicker.setMinValue(min);
            nPicker.setMaxValue(max);
        }
        int value = args.getInt(ARG_VALUE, -1);
        if (value>= min && value <= max) {
            nPicker.setValue(value);
        }
        nPicker.setWrapSelectorWheel(args.getBoolean(ARG_WRAP, false));

        AlertDialog.Builder bld = new AlertDialog.Builder(getActivity());
        String s = args.getString(ARG_TITLE);
        if (!TextUtils.isEmpty(s)) {
            bld.setTitle(s);
        }
        s = args.getString(ARG_MESSAGE);
        if (!TextUtils.isEmpty(s)) {
            bld.setMessage(s);
        }

        int resId = args.getInt(ARG_BTN_YES, -1);
        if (resId > 0) {
            bld.setPositiveButton(resId, (dialog, which) -> {
                if (onNumbedSelectListener != null) {
                    onNumbedSelectListener.onSelected(nPicker.getValue());
                }
            });
        }
        resId = args.getInt(ARG_BTN_NO, -1);
        if (resId > 0) {
            bld.setNegativeButton(resId, null);
        }

        bld.setView(vContent);

        boolean cancellable = args.getBoolean(ARG_CANCELLABLE, true);
        bld.setCancelable(cancellable);
        AlertDialog ad = bld.create();
        ad.setCanceledOnTouchOutside(cancellable);
        return ad;
    }
}

