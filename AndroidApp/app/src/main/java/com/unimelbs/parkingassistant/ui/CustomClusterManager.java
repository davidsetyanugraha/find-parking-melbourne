package com.unimelbs.parkingassistant.ui;

import android.app.Activity;
import android.content.Context;
import android.location.Location;
import android.util.Log;

import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.maps.android.MarkerManager;
import com.google.maps.android.clustering.ClusterItem;
import com.google.maps.android.clustering.ClusterManager;
import com.google.maps.android.clustering.algo.Algorithm;
import com.google.maps.android.clustering.view.ClusterRenderer;
import com.google.android.gms.location.FusedLocationProviderClient;

import java.util.Collection;

public class CustomClusterManager<T extends ClusterItem> extends ClusterManager {
    private static final String TAG = "TE-CustomClusterManager";
    private FusedLocationProviderClient fusedLocationProviderClient;
    private Activity mapActivity;
    private LatLng currentLocation;
    private MapControllable mapControllable;

    public CustomClusterManager(Context context,
                                GoogleMap map,
                                Activity activity,
                                MapControllable mapControllable) {
        super(context, map);
        this.mapActivity = activity;
        currentLocation = getCurrentLocation();
        this.mapControllable = mapControllable;
    }

    public CustomClusterManager(Context context,
                                GoogleMap map,
                                MarkerManager markerManager,
                                Activity activity,
                                MapControllable mapControllable) {
        super(context, map, markerManager);
        currentLocation = getCurrentLocation();
        this.mapControllable = mapControllable;
    }

    public LatLng getCurrentLocation()
    {
        fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(mapActivity);
        fusedLocationProviderClient.getLastLocation().addOnSuccessListener(mapActivity,
                new OnSuccessListener<Location>() {
                    @Override
                    public void onSuccess(Location location) {
                        if (location!=null)
                        {
                            currentLocation = new LatLng(location.getLatitude(),location.getLongitude());
                        }
                    }
                });
        return currentLocation;
    }

    @Override
    public MarkerManager.Collection getMarkerCollection() {
        return super.getMarkerCollection();
    }

    @Override
    public MarkerManager.Collection getClusterMarkerCollection() {
        return super.getClusterMarkerCollection();
    }

    @Override
    public MarkerManager getMarkerManager() {
        return super.getMarkerManager();
    }

    @Override
    public void setRenderer(ClusterRenderer view) {
        super.setRenderer(view);
    }

    @Override
    public void setAlgorithm(Algorithm algorithm) {
        super.setAlgorithm(algorithm);
    }

    @Override
    public void setAnimation(boolean animate) {
        super.setAnimation(animate);
    }

    @Override
    public ClusterRenderer getRenderer() {
        return super.getRenderer();
    }

    @Override
    public Algorithm getAlgorithm() {
        return super.getAlgorithm();
    }

    @Override
    public void clearItems() {
        super.clearItems();
    }

    @Override
    public void addItems(Collection items) {
        super.addItems(items);
    }

    @Override
    public void addItem(ClusterItem myItem) {
        super.addItem(myItem);
    }

    @Override
    public void removeItem(ClusterItem item) {
        super.removeItem(item);
    }

    @Override
    public void cluster() {
        super.cluster();
    }

    @Override
    public void onCameraIdle() {
        super.onCameraIdle();
        currentLocation = getCurrentLocation();
        if (mapControllable !=null)
        {
            Log.d(TAG, "onCameraIdle: current location is:"+currentLocation.toString()+
                    " current zoom:" + mapControllable.getCameraPosition().zoom );
        }
    }

    @Override
    public boolean onMarkerClick(Marker marker) {
        return super.onMarkerClick(marker);
    }

    @Override
    public void onInfoWindowClick(Marker marker) {
        super.onInfoWindowClick(marker);
    }

    @Override
    public void setOnClusterClickListener(OnClusterClickListener listener) {
        super.setOnClusterClickListener(listener);
    }

    @Override
    public void setOnClusterInfoWindowClickListener(OnClusterInfoWindowClickListener listener) {
        super.setOnClusterInfoWindowClickListener(listener);
    }

    @Override
    public void setOnClusterItemClickListener(OnClusterItemClickListener listener) {
        super.setOnClusterItemClickListener(listener);
    }

    @Override
    public void setOnClusterItemInfoWindowClickListener(OnClusterItemInfoWindowClickListener listener) {
        super.setOnClusterItemInfoWindowClickListener(listener);
    }
}
