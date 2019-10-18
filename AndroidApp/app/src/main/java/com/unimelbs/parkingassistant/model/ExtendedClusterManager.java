package com.unimelbs.parkingassistant.model;

import android.content.Context;
import android.location.Location;
import android.util.Log;
import android.widget.Toast;

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

public class ExtendedClusterManager<T extends ClusterItem> extends ClusterManager
        implements
        GoogleMap.OnCameraIdleListener,
        GoogleMap.OnMarkerClickListener,
        GoogleMap.OnInfoWindowClickListener,
        ClusterManager.OnClusterItemClickListener<Bay>{
    private final static String TAG = "ExtendedClusterManager";
    private Context context;
    private GoogleMap mMap;
    private final float AVAILABLE_BAY_COLOR = BitmapDescriptorFactory.HUE_GREEN;
    private final float OCCUPIED_BAY_COLOR = BitmapDescriptorFactory.HUE_RED;


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
        this.setRenderer(new BayRenderer(context,map,this));
        this.setOnClusterItemClickListener(this);
        this.mMap = map;
    }

    @Override
    public void onCameraIdle() {
        super.onCameraIdle();
        LatLng topRight = mMap.getProjection().getVisibleRegion().latLngBounds.northeast;
        LatLng bottomLeft = mMap.getProjection().getVisibleRegion().latLngBounds.southwest;
        LatLng topLeft = new LatLng(topRight.latitude,bottomLeft.longitude);
        Location loc1 = new Location("p1");
        Location loc2 = new Location("p2");
        loc1.setLatitude(topLeft.latitude);
        loc1.setLongitude(topLeft.longitude);
        loc2.setLatitude(topRight.latitude);
        loc2.setLongitude(topRight.longitude);

        long rad = Math.round((double)loc1.distanceTo(loc2)/2);
        long radius =
                //Location.distanceBetween(topLeft.latitude,topLeft.longitude,topRight.latitude,topRight.longitude,);

                Math.round((DistanceUtil.getDistance(topLeft,topRight)/2)*1000);
        Log.d(TAG, "onClusterItemRendered: radius:"+
                radius+" rad:"+rad);
    }

    @Override
    public boolean onClusterItemClick(Bay bay) {
        Log.d(TAG, "onClusterItemClick: ClickedBay"+bay.getBayId()+" "+((bay.isAvailable())?"Available":"Occupied"));
        Toast.makeText(context,bay.getBayId()+" "+((bay.isAvailable())?"Available":"Occupied"),Toast.LENGTH_LONG).show();
        return false;
    }

    @Override
    public boolean onMarkerClick(Marker marker) {
        super.onMarkerClick(marker);
        //Log.d(TAG, "onMarkerClick: "+marker.getId());
        return false;
    }






}
