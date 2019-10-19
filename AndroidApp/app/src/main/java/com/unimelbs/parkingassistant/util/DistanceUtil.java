package com.unimelbs.parkingassistant.util;

import android.location.Location;

import com.google.android.gms.maps.model.LatLng;


//https://stackoverflow.com/questions/6981916/how-to-calculate-distance-between-two-locations-using-their-longitude-and-latitu
public class DistanceUtil {
    public static double getDistance(LatLng topLeft, LatLng bottomRight)
    {
        //private static final int KMS_IN_DEGREE=
        double distance=0;
        if(topLeft!=null && bottomRight!=null)
        {
            double theta = topLeft.longitude - bottomRight.longitude;
            distance = Math.sin(deg2rad(topLeft.latitude))
                    * Math.sin(deg2rad(bottomRight.latitude))
                    + Math.cos(deg2rad(topLeft.latitude))
                    * Math.cos(deg2rad(bottomRight.latitude))
                    * Math.cos(deg2rad(theta));
            distance = Math.acos(distance);
            distance = rad2deg(distance);
            distance = distance * 60 * 1.1515;

        }
        return distance/0.62137;
    }

    private static double deg2rad(double deg) {
        return (deg * Math.PI / 180.0);
    }

    private static double rad2deg(double rad) {
        return (rad * 180.0 / Math.PI);
    }




}
