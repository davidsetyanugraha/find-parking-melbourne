package com.unimelbs.parkingassistant.model;

import android.content.Context;
import android.location.Location;
import android.util.Log;
import android.widget.Toast;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.maps.android.MarkerManager;
import com.google.maps.android.clustering.ClusterItem;
import com.google.maps.android.clustering.ClusterManager;
import com.google.maps.android.clustering.algo.PreCachingAlgorithmDecorator;
import com.google.maps.android.clustering.view.DefaultClusterRenderer;
import com.unimelbs.parkingassistant.ui.BayRenderer;
import com.unimelbs.parkingassistant.util.DistanceUtil;

/**
 *
 * @param <T>
 */
public class ExtendedClusterManager<T extends ClusterItem> extends ClusterManager
        implements
        GoogleMap.OnCameraIdleListener,
        GoogleMap.OnMarkerClickListener,
        GoogleMap.OnInfoWindowClickListener,
        ClusterManager.OnClusterItemClickListener<Bay>{
    private final static String TAG = "ExtendedClusterManager";
    private Context context;
    private GoogleMap mMap;
    private BayRenderer bayRenderer;
    private final float AVAILABLE_BAY_COLOR = BitmapDescriptorFactory.HUE_GREEN;
    private final float OCCUPIED_BAY_COLOR = BitmapDescriptorFactory.HUE_RED;

    private LatLng circleCentre;


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
        this.setAnimation(false);
    }

    public ExtendedClusterManager(Context context, GoogleMap map, DataFeed dataFeed) {
        this(context, map, new MarkerManager(map));
        this.bayRenderer = new BayRenderer(context,map,this,dataFeed);
        this.setRenderer(this.bayRenderer);
        this.setOnClusterItemClickListener(this);
        this.mMap = map;
    }

    @Override
    public boolean onClusterItemClick(Bay bay) {
        return false;
    }

    @Override
    public boolean onMarkerClick(Marker marker) {
        super.onMarkerClick(marker);
        return false;
    }


    public BayRenderer getBayRenderer()
    {
        return bayRenderer;
    }
}
