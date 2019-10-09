package com.unimelbs.parkingassistant.model;

import com.google.android.gms.maps.model.LatLng;
import com.google.maps.android.clustering.ClusterItem;

import java.util.List;

public class Bay implements ClusterItem {
    private int bayId;
    private LatLng position;
    private List<LatLng> polygon;
    private String title;
    private String snippet;

    public Bay(int bayId, LatLng position, List<LatLng> polygon, String title, String snippet) {
        this.bayId = bayId;
        this.position = position;
        this.polygon = polygon;
        this.title = title;
        this.snippet = snippet;
    }

    public Bay(int bayId, LatLng position) {
        this.bayId = bayId;
        this.position = position;
    }

    @Override
    public LatLng getPosition() {
        return position;
    }

    @Override
    public String getTitle() {
        return title;
    }

    @Override
    public String getSnippet() {
        return snippet;
    }
}
