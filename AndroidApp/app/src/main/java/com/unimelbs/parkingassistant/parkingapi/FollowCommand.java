package com.unimelbs.parkingassistant.parkingapi;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

/**
 * Defines the paramaters to execute the Follow method on the API.
 */
public class FollowCommand {
    @SerializedName("connectionid")
    @Expose
    private String connectionId;
    @SerializedName("parkingbayid")
    @Expose
    private String parkingBayId;

    public FollowCommand(String connectionId, String parkingBayId) {
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
