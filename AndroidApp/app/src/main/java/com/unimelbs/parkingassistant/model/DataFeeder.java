package com.unimelbs.parkingassistant.model;

import com.google.maps.android.clustering.ClusterItem;

import java.util.List;

public interface DataFeeder {
    List<ClusterItem> getItems();
}
