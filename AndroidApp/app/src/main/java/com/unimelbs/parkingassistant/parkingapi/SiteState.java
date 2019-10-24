package com.unimelbs.parkingassistant.parkingapi;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

/**
 * Defines a SiteState object that is returned from the API.
 */
public class SiteState {
    @SerializedName("id")
    @Expose
    private String id;
    @SerializedName("status")
    @Expose
    private String status;
    @SerializedName("location")
    @Expose
    private Location location;
    @SerializedName("recordState")
    @Expose
    private String recordState;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public Location getLocation() {
        return location;
    }

    public void setLocation(Location location) {
        this.location = location;
    }

    public String getRecordState() {
        return recordState;
    }

    public void setRecordState(String recordState) {
        this.recordState = recordState;
    }
}
