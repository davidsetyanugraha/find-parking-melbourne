package com.unimelbs.parkingassistant.model;

import com.google.android.gms.maps.model.LatLng;
import com.google.maps.android.clustering.ClusterItem;

import java.util.List;

public class Bay implements ClusterItem {

    private int bayId;
    private LatLng position;
    private List<LatLng> polygon;
    private List<String> restriction;
    private String title;
    private String snippet;


    /**
     * Constructors.
     * @param bayId
     * @param position
     */
    public Bay(int bayId, LatLng position) {
        this.bayId = bayId;
        this.position = position;
    }
    public Bay(int bayId, LatLng position, List<LatLng> polygon, List<String> restriction, String title, String snippet) {
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
    public List<LatLng> getPolygon() {
        return polygon;
    }

    public List<String> getRestriction() {
        return restriction;
    }

    @Override
    public LatLng getPosition() {
        return this.position;
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
