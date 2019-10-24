package com.unimelbs.parkingassistant.parkingapi;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

import java.io.Serializable;
import java.util.List;

/**
 * Represents the coordinates of the object that comes from an API call.
 */
public class Location implements Serializable {

    @SerializedName("type")
    @Expose
    private String type;
    @SerializedName("coordinates")
    @Expose
    private List<Double> coordinates = null;

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public List<Double> getCoordinates() {
        return coordinates;
    }

    public void setCoordinates(List<Double> coordinates) {
        this.coordinates = coordinates;
    }
}
