package com.unimelbs.parkingassistant.util;

import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.BitmapDescriptor;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.maps.android.ui.IconGenerator;
import com.unimelbs.parkingassistant.R;

public class Constants
{
    //Defines the parking states
    public enum Status { AVAILABLE, OCCUPIED, UNAVAILABLE }

    //The zoom constants needed for the map
    public static final int MAP_ZOOM_DEFAULT = 15;
    public static final int MAP_ZOOM_CURRENT_LOCATION = 15;
    public static final int MAP_ZOOM_PLACE = 18;
    public static final int MAP_ZOOM_BAY = 20;
    public static final LatLng MAP_DEFAULT_LOCATION = new LatLng(-37.814, 144.96);
    public static final int MAP_DO_NOT_CLUSTER_ZOOM_LEVEL = 18;
    public static final String BAY_COLLECTION_ID="unimelbs";


    //The server URLs to call the API
    public static final String API_URL = "https://parkingappapi.azurewebsites.net/api/";
    public static final String HUB_CONNECTION_URL = "https://parkingappapi.azurewebsites.net/api/sites/state/connection/";

    //The markers configuration
    //public static final BitmapDescriptor test = BitmapDescriptorFactory.fromResource(R.drawable.occupied_bay);
    public static final BitmapDescriptor AVAILABLE_ICON=BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_GREEN);
    public static final BitmapDescriptor UNAVAILABLE_ICON=BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_RED);
    public static final BitmapDescriptor UNKNOWN_ICON=BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_YELLOW);
}
