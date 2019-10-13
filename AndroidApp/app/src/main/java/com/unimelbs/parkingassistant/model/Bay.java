package com.unimelbs.parkingassistant.model;

import com.google.android.gms.maps.model.LatLng;
import com.google.maps.android.clustering.ClusterItem;
import com.unimelbs.parkingassistant.parkingapi.Restriction;
import com.unimelbs.parkingassistant.parkingapi.TheGeom;

import java.io.Serializable;
import java.util.List;

public class Bay implements ClusterItem, Serializable {

    private int bayId;

    //position[0] Lat, position[1] Lng
    private double[] position;
    //private List<double[]> polygon;
    //private List<String> restriction;
    private String title;
    private String snippet;
    private TheGeom theGeom;
    private boolean isAvailable;


    private List<Restriction> restrictions;


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
               //List<double[]> polygon,
               List<Restriction> restrictions,
               TheGeom theGeom,
               String title,
               String snippet) {
        this.bayId = bayId;
        this.position = position;
        //this.polygon = polygon;
        this.restrictions = restrictions;
        this.theGeom = theGeom;
        this.title = title;
        this.snippet = snippet;
    }

    /**
     * Getters.
     * @return
     */
    public double[] getRawPosition() {return this.position;}

    public int getBayId() {return bayId;}

    /*
    public List<double[]> getPolygon() {
        return polygon;
    }

     */

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
        return isAvailable;
    }

    public void setAvailable(boolean available) {
        isAvailable = available;
    }
}
