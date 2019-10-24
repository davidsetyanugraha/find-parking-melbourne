package com.unimelbs.parkingassistant.util;

import com.google.android.gms.maps.model.BitmapDescriptor;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;

public class Constants
{
    public enum Status
    {AVAILABLE,OCCUPIED,UNAVAILABLE};
    public static final BitmapDescriptor AVAILABLE_ICON= BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_GREEN);
    public static final BitmapDescriptor UNAVAILABLE_ICON=BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_RED);
    public static final BitmapDescriptor UNKNOWN_ICON=BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_YELLOW);
}
