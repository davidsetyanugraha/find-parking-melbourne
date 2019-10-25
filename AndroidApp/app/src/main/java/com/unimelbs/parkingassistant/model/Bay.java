package com.unimelbs.parkingassistant.model;

import com.google.android.gms.maps.model.LatLng;
import com.google.maps.android.clustering.ClusterItem;
import com.unimelbs.parkingassistant.parkingapi.Restriction;

import java.io.Serializable;
import java.util.List;
import com.unimelbs.parkingassistant.util.Constants;

/**
 * Represents a parking bay.
 */

public class Bay
        implements ClusterItem, Serializable
{

    private int bayId;
    private double[] position;
    //private List<double[]> polygon;
    private String title;
    private String snippet;
    private List<Double[]> polygon;
    private boolean isAvailable;
    private List<Restriction> restrictions;
    private Constants.Status status;


    /**
     * Constructors.
     * @param bayId
     * @param position
     */
    public Bay(int bayId, double[] position) {
        this.bayId = bayId;
        this.position = position;
    }


    public Bay(int bayId,
               double[] position,
               List<Restriction> restrictions,
               List<Double[]> polygon,
               String title,
               String snippet) {
        this.bayId = bayId;
        this.position = position;
        this.restrictions = restrictions;
        this.polygon = polygon;
        this.title = title;
        this.snippet = snippet;
        this.status = Constants.Status.UNAVAILABLE;
    }



    /**
     * Getters.
     * @return
     */
    public Constants.Status getStatus()
    {
        return status;
    }

    public void setStatus(Constants.Status status)
    {
        this.status = status;

    }

    public double[] getRawPosition() {return this.position;}

    public int getBayId() {return bayId;}

    public List<Double[]> getPolygon() {return this.polygon;}

    @Override
    public LatLng getPosition() {
        return new LatLng(this.position[0],this.position[1]);
    }

    @Override
    public String getTitle() {
        return this.title;
    }

    @Override
    public String getSnippet() {
        return this.snippet;
    }

    public List<Restriction> getRestrictions() {
        return restrictions;
    }
    public boolean isAvailable() {
        boolean result=false;
        switch(this.status)
        {
            case AVAILABLE:{result=true;break;}
            case OCCUPIED:{result=false;break;}
            case UNAVAILABLE:{result=false;break;}
        }
        return result;
    }

    public void setAvailable(boolean available) {
        isAvailable = available;
    }
}
