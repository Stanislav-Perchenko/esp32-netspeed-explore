package com.alperez.esp32.netspeed_client.model;

import android.support.annotation.Nullable;

import com.google.gson.annotations.SerializedName;

/**
 * Created by stanislav.perchenko on 3/27/2019
 */
public class StatusApiModel {

    @SerializedName("status")
    @Nullable
    public StatusModel statusModel;

    @SerializedName("statistic")
    @Nullable
    public StatisticsModel statisticsModel;
}
