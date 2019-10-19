package com.unimelbs.parkingassistant.util;


import android.content.SharedPreferences;

import com.google.gson.Gson;
import com.unimelbs.parkingassistant.model.Bay;

import java.util.Date;

public class PreferenceManager {
    public static final String PARKING_BAY = "com.unimelbs.parkingassistant.bayHistory";
    public static final String PARKING_END_DATE = "com.unimelbs.parkingassistant.bayEndDate";

    public static void saveBayToSharedPreferences(Bay selectedBay, SharedPreferences prefs) {
        SharedPreferences.Editor prefsEditor = prefs.edit();
        Gson gson = new Gson();
        String json = gson.toJson(selectedBay);
        prefsEditor.putString(PARKING_BAY, json);
        prefsEditor.commit();
    }

    public static void saveEndDateToSharedPreferences(Date endParkingDate, SharedPreferences prefs) {
        SharedPreferences.Editor prefsEditor = prefs.edit();
        Gson gson = new Gson();
        String json = gson.toJson(endParkingDate);
        prefsEditor.putString(PARKING_END_DATE, json);
        prefsEditor.commit();
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
