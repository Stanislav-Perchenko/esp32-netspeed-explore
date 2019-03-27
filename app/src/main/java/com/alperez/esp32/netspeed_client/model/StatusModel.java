package com.alperez.esp32.netspeed_client.model;

import com.google.gson.annotations.SerializedName;

/**
 * Created by stanislav.perchenko on 3/26/2019
 */
public class StatusModel {
    @SerializedName("conn_mode")
    private String wifiConnectionMode;
    @SerializedName("action")
    private String deviceState;
    @SerializedName("protocol")
    private String transportProtocol;
    @SerializedName("port")
    private Integer socketPort;
    @SerializedName("pkg_size")
    private Integer transmitPackageSize;


    public String getWifiConnectionMode() {
        return wifiConnectionMode;
    }

    public String getDeviceState() {
        return deviceState;
    }

    public String getTransportProtocol() {
        return transportProtocol;
    }

    public int getSocketPort() {
        return (socketPort == null) ? -1 : socketPort;
    }

    public int getTransmitPackageSize() {
        return (transmitPackageSize == null) ? -1 : transmitPackageSize;
    }
}
