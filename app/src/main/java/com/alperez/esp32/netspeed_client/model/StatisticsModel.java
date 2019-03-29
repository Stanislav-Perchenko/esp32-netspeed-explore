package com.alperez.esp32.netspeed_client.model;

import com.google.gson.annotations.SerializedName;

/**
 * Created by stanislav.perchenko on 3/27/2019
 */
public class StatisticsModel {

    @SerializedName("speed")
    private Integer speedBytesPerSecond;
    @SerializedName("n_pkg_transm")
    private Integer nPackagesSent;
    @SerializedName("n_pkg_rcv_total")
    private Integer nPackagesReceiveTotal;
    @SerializedName("n_pkg_rcv_failed")
    private Integer nPackagesReceiveFailed;

    public int getSpeedBytesPerSecond() {
        return (speedBytesPerSecond == null) ? -1 : speedBytesPerSecond;
    }

    public int getnPackagesSent() {
        return (nPackagesSent == null) ? -1 : nPackagesSent;
    }

    public int getnPackagesReceiveTotal() {
        return (nPackagesReceiveTotal == null) ? -1 : nPackagesReceiveTotal;
    }

    public int getnPackagesReceiveFailed() {
        return (nPackagesReceiveFailed == null) ? -1 : nPackagesReceiveFailed;
    }
}
