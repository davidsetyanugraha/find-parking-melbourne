package com.unimelbs.parkingassistant.ui;

import android.content.Context;
import android.util.Log;

import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.maps.android.clustering.Cluster;
import com.google.maps.android.clustering.ClusterManager;
import com.google.maps.android.clustering.view.DefaultClusterRenderer;
import com.unimelbs.parkingassistant.model.Bay;

public class BayRenderer extends DefaultClusterRenderer<Bay> {
    private Context context;
    private GoogleMap mMap;
    private ClusterManager<Bay> clusterManager;
    private static final String TAG="BayRenderer";

    public BayRenderer(Context context, GoogleMap mMap, ClusterManager<Bay> clusterManager)
    {
        super(context,mMap,clusterManager);
        this.context = context;
        this.mMap = mMap;
        this.clusterManager = clusterManager;
    }

    @Override
    protected void onBeforeClusterItemRendered(Bay item, MarkerOptions markerOptions) {
        super.onBeforeClusterItemRendered(item, markerOptions);

    }

    @Override
    protected void onClusterItemRendered(Bay clusterItem, Marker marker) {
        super.onClusterItemRendered(clusterItem, marker);
        LatLng topRight = mMap.getProjection().getVisibleRegion().latLngBounds.northeast;
        LatLng bottomLeft = mMap.getProjection().getVisibleRegion().latLngBounds.southwest;
        try
        {
            if (clusterItem.isDisplayed(topRight,bottomLeft)) {
                String msg = "Bay ID "+clusterItem.getBayId()+" is displayed"+
                        "zoom level: "+mMap.getCameraPosition().zoom;
                Log.d(TAG, "onClusterItemRendered:" +msg );

            }

        } catch (Exception e){
            Log.d(TAG, "onClusterItemRendered: "+e.getMessage());}
    }


}