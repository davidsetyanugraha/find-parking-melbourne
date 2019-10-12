package com.unimelbs.parkingassistant.model;

import android.content.Context;

import com.google.android.gms.maps.GoogleMap;
import com.google.maps.android.MarkerManager;
import com.google.maps.android.clustering.ClusterItem;
import com.google.maps.android.clustering.ClusterManager;

public class ExtendedClusterManager<T extends ClusterItem> extends ClusterManager
        implements GoogleMap.OnCameraIdleListener, GoogleMap.OnMarkerClickListener, GoogleMap.OnInfoWindowClickListener
{
    private final static String TAG = "TE-ExtendedClusterMgr";
    /**
     * Constructors.
     * @param context
     * @param map
     * @param markerManager
     */
    public ExtendedClusterManager(Context context, GoogleMap map, MarkerManager markerManager)
    {
        super(context,map,markerManager);
    }
    public ExtendedClusterManager(Context context, GoogleMap map, DataFeed dataFeed) {
        this(context, map, new MarkerManager(map));

    }



    @Override
    public void onCameraIdle() {
        super.onCameraIdle();

    }
}