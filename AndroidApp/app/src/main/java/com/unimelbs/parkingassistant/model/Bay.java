package com.unimelbs.parkingassistant.model;

import com.google.android.gms.maps.model.LatLng;
import com.google.maps.android.clustering.ClusterItem;

import java.io.Serializable;
import java.util.List;

public class Bay implements ClusterItem, Serializable {

    private int bayId;

    private double[] position;
    private List<double[]> polygon;
    private List<String> restriction;
    private String title;
    private String snippet;


    /**
     * Constructors.
     * @param bayId
     * @param position
     */
    public Bay(int bayId, double[] position) {
        this.bayId = bayId;
        this.position = position;
    }


    public Bay(int bayId, double[] position, List<double[]> polygon, List<String> restriction, String title, String snippet) {
        this.bayId = bayId;
        this.position = position;
        this.polygon = polygon;
        this.restriction = restriction;
        this.title = title;
        this.snippet = snippet;
    }

    /**
     * Getters.
     * @return
     */
    public double[] getRawPosition() {return this.position;}

    public int getBayId() {return bayId;}

    public List<double[]> getPolygon() {
        return polygon;
    }

    public List<String> getRestriction() {
        return restriction;
    }

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
}
