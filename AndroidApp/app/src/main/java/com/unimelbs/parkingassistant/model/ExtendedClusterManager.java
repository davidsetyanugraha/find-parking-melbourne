package com.unimelbs.parkingassistant.model;

import android.content.Context;
import android.util.Log;

import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.model.Marker;
import com.google.maps.android.MarkerManager;
import com.google.maps.android.clustering.ClusterItem;
import com.google.maps.android.clustering.ClusterManager;
import com.google.maps.android.clustering.view.DefaultClusterRenderer;
import com.unimelbs.parkingassistant.ui.BayRenderer;

public class ExtendedClusterManager<T extends ClusterItem> extends ClusterManager
        implements
        GoogleMap.OnCameraIdleListener,
        GoogleMap.OnMarkerClickListener,
        GoogleMap.OnInfoWindowClickListener,
        ClusterManager.OnClusterItemClickListener<Bay>{
    private final static String TAG = "ExtendedClusterManager";
    private Context context;
    private BayRenderer bayRenderer;
    /**
     * Constructors.
     * @param context
     * @param map
     * @param markerManager
     */
    public ExtendedClusterManager(Context context, GoogleMap map, MarkerManager markerManager)
    {
        super(context,map,markerManager);
        this.context = context;
    }
    public ExtendedClusterManager(Context context, GoogleMap map, DataFeed dataFeed) {
        this(context, map, new MarkerManager(map));
        this.setRenderer(new BayRenderer(context,map,this));
    }

    @Override
    public void onCameraIdle() {
        super.onCameraIdle();

    }


    @Override
    public boolean onClusterItemClick(Bay bay) {

        Log.d(TAG, "onClusterItemClick: ClickedBay"+bay.getBayId()+" "+((bay.isAvailable())?"Available":"Occupied"));
        return true;
    }

}
