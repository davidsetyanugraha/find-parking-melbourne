package com.unimelbs.parkingassistant.util;

import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.BitmapDescriptor;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;

public class Constants
{
    public enum Status { AVAILABLE, OCCUPIED, UNAVAILABLE }

    public static final int MAP_ZOOM_DEFAULT = 15;
    public static final int MAP_ZOOM_CURRENT_LOCATION = 15;
    public static final int MAP_ZOOM_PLACE = 18;
    public static final int MAP_ZOOM_BAY = 20;
    public static final LatLng MAP_DEFAULT_LOCATION = new LatLng(-37.796201, 144.958266);
    public static final int MAP_DO_NOT_CLUSTER_ZOOM_LEVEL = 18;

    public static final BitmapDescriptor AVAILABLE_ICON= BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_GREEN);
    public static final BitmapDescriptor UNAVAILABLE_ICON=BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_RED);
    public static final BitmapDescriptor UNKNOWN_ICON=BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_YELLOW);
}
