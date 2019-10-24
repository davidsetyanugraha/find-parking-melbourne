package com.unimelbs.parkingassistant.parkingapi;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

import java.io.Serializable;

/**
 * Defines a restriction object that comes from the API.
 */
public class Restriction implements Serializable {

    @SerializedName("description")
    @Expose
    private String description;
    @SerializedName("disabilityext")
    @Expose
    private String disabilityext;
    @SerializedName("duration")
    @Expose
    private String duration;
    @SerializedName("effectiveonph")
    @Expose
    private String effectiveonph;
    @SerializedName("endtime")
    @Expose
    private String endtime;
    @SerializedName("fromday")
    @Expose
    private String fromday;
    @SerializedName("starttime")
    @Expose
    private String starttime;
    @SerializedName("today")
    @Expose
    private String today;
    @SerializedName("typedesc")
    @Expose
    private String typedesc;

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getDisabilityext() {
        return disabilityext;
    }

    public void setDisabilityext(String disabilityext) {
        this.disabilityext = disabilityext;
    }

    public String getDuration() {
        return duration;
    }

    public void setDuration(String duration) {
        this.duration = duration;
    }

    public String getEffectiveonph() {
        return effectiveonph;
    }

    public void setEffectiveonph(String effectiveonph) {
        this.effectiveonph = effectiveonph;
    }

    public String getEndtime() {
        return endtime;
    }

    public void setEndtime(String endtime) {
        this.endtime = endtime;
    }

    public String getFromday() {
        return fromday;
    }

    public void setFromday(String fromday) {
        this.fromday = fromday;
    }

    public String getStarttime() {
        return starttime;
    }

    public void setStarttime(String starttime) {
        this.starttime = starttime;
    }

    public String getToday() {
        return today;
    }

    public void setToday(String today) {
        this.today = today;
    }

    public String getTypedesc() {
        return typedesc;
    }

    public void setTypedesc(String typedesc) {
        this.typedesc = typedesc;
    }

}