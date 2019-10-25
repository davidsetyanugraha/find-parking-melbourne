package com.unimelbs.parkingassistant.util;

import android.util.Log;

import com.google.android.gms.maps.model.LatLng;

public class StateValues
{
    private static final String TAG = "StateValues";
    private static LatLng lastPositionS;
    private static float lastZoomS;
    private static boolean positionChanged = false;

    public static LatLng getLastPosition()
    {
        Log.d(TAG, "getLastPosition: "+lastPositionS);
        return (lastPositionS==null)?Constants.MAP_DEFAULT_LOCATION:lastPositionS;
    }

    public static void setLastPosition(LatLng lastPosition)
    {
        Log.d(TAG, "setLastPosition: "+lastPosition.toString());
        lastPositionS = lastPosition;
        positionChanged = true;
    }

    public static float getLastZoom()
    {
        Log.d(TAG, "getLastZoom: "+lastZoomS);
        return (lastZoomS==0)?Constants.MAP_ZOOM_DEFAULT:lastZoomS;
    }

    public static void setLastZoom(float lastZoom)
    {
        Log.d(TAG, "setLastZoom: "+lastZoom);
        lastZoomS = lastZoom;
        positionChanged = true;
    }

    public static boolean isPositionChanged() {
        return positionChanged;
    }
}
