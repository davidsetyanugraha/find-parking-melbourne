package com.unimelbs.parkingassistant.util;


import android.content.SharedPreferences;

import com.google.android.gms.maps.model.LatLng;
import com.google.gson.Gson;
import com.unimelbs.parkingassistant.model.Bay;

import java.util.Date;

public class PreferenceManager {
    public static final String PREFERENCE_NAME = "com.unimelbs.parkingassistant";
    public static final String PARKING_BAY = "com.unimelbs.parkingassistant.bayHistory";
    public static final String PARKING_END_DATE = "com.unimelbs.parkingassistant.bayEndDate";
    public static final String LAST_POSITION = "com.unimelbs.parkingassistant.lastPosition";
    public static final String LAST_ZOOM = "com.unimelbs.parkingassistant.lastZoom";

    public static void saveLastPositionToSharedPrefs(LatLng position, SharedPreferences prefs)
    {
        SharedPreferences.Editor prefsEditor = prefs.edit();
        Gson gson = new Gson();
        String json = gson.toJson(position);
        prefsEditor.putString(LAST_POSITION, json);
        prefsEditor.apply();
    }

    public static LatLng getLastPositionFromSharedPrefs(SharedPreferences prefs) {
        Gson gson = new Gson();
        LatLng lastPosition=null;
        String json = prefs.getString(LAST_POSITION, "");
        if(json!=null){ lastPosition= gson.fromJson(json, LatLng.class);}
        return lastPosition;
    }

    public static void saveLastZoomToSharedPrefs(float lastZoom,SharedPreferences prefs)
    {
        SharedPreferences.Editor prefsEditor = prefs.edit();
        prefsEditor.putFloat(LAST_ZOOM,lastZoom);
        prefsEditor.apply();
    }


    public static float getLastZoomFromSharedPrefs(SharedPreferences prefs)
    {
        float lastZoom = prefs.getFloat(LAST_ZOOM,Constants.MAP_ZOOM_DEFAULT);
        return lastZoom;
    }



    public static Boolean isAvailable(SharedPreferences prefs) {
        String jsonParking = prefs.getString(PARKING_BAY, "");
        String jsonEndDate = prefs.getString(PARKING_END_DATE, "");
        return ((jsonParking != "") && (jsonEndDate != ""));
    }

    public static void saveBayToSharedPreferences(Bay selectedBay, SharedPreferences prefs) {
        SharedPreferences.Editor prefsEditor = prefs.edit();
        Gson gson = new Gson();
        String json = gson.toJson(selectedBay);
        prefsEditor.putString(PARKING_BAY, json);
        prefsEditor.apply();
    }

    public static void saveEndDateToSharedPreferences(Date endParkingDate, SharedPreferences prefs) {
        SharedPreferences.Editor prefsEditor = prefs.edit();
        Gson gson = new Gson();
        String json = gson.toJson(endParkingDate);
        prefsEditor.putString(PARKING_END_DATE, json);
        prefsEditor.apply();
    }

    public static Bay getBayFromSharedPreference(SharedPreferences prefs) {
        Gson gson = new Gson();
        String json = prefs.getString(PARKING_BAY, "");
        Bay bay = gson.fromJson(json, Bay.class);
        return bay;
    }

    public static Date getEndDateFromSharedPreference(SharedPreferences prefs) {
        Gson gson = new Gson();
        String json = prefs.getString(PARKING_END_DATE, "");
        Date endParkingDate = gson.fromJson(json, Date.class);
        return endParkingDate;
    }

    public static void clearPreference(SharedPreferences prefs) {
        SharedPreferences.Editor prefsEditor = prefs.edit();
        prefsEditor.clear().commit();
    }
}
