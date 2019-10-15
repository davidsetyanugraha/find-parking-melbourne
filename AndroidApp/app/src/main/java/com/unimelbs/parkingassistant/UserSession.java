package com.unimelbs.parkingassistant;

import android.content.Context;
import android.location.Location;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.tasks.OnSuccessListener;
import com.unimelbs.parkingassistant.model.Bay;

import java.util.Date;

/**
 * Holds data2 related to user's session, including details of where they
 * parked, parking time, reminders.
 */
public class UserSession {
    private Bay bay;
    private Date parkingStartTime;
    private boolean parkingActive;
    private Context context;
    private Location currentLocation;
    public UserSession(Context context)
    {
        this.context = context;

    }

    public boolean isParkingActive() {
        return parkingActive;
    }

    public void setParkingActive(boolean parkingActive) {
        this.parkingActive = parkingActive;
    }

    public Bay getBay() {
        return bay;
    }

    public void setBay(Bay bay) {
        this.bay = bay;
    }

    public Date getParkingStartTime() {
        return parkingStartTime;
    }

    public void setParkingStartTime(Date parkingStartTime) {
        this.parkingStartTime = parkingStartTime;
    }
    public Location getCurrentLocation()
    {
        currentLocation=null;
        FusedLocationProviderClient fusedLocationProviderClient =
                LocationServices.getFusedLocationProviderClient(context);
        fusedLocationProviderClient.getLastLocation().addOnSuccessListener(new OnSuccessListener<Location>() {
            @Override
            public void onSuccess(Location location) {
                if(location!=null)
                {
                    currentLocation = location;
                }
            }
        });
        return this.currentLocation;
    }
}
