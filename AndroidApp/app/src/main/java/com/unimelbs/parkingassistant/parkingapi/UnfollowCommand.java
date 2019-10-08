package com.unimelbs.parkingassistant.parkingapi;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

public class UnfollowCommand {
    @SerializedName("connectionid")
    @Expose
    private String connectionId;
    @SerializedName("parkingbayid")
    @Expose
    private String parkingBayId;

    public UnfollowCommand(String connectionId, String parkingBayId) {
        this.connectionId = connectionId;
        this.parkingBayId = parkingBayId;
    }

    public String getConnectionId() {
        return connectionId;
    }

    public String getParkingBayId() {
        return parkingBayId;
    }
}
