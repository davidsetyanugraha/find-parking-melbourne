package com.unimelbs.parkingassistant.util;

import android.location.Location;
import android.util.Log;

import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.model.LatLng;


//https://stackoverflow.com/questions/6981916/how-to-calculate-distance-between-two-locations-using-their-longitude-and-latitu
public class DistanceUtil {
    static final String TAG = "DistanceUtil";
    public static double getDistance(LatLng topLeft, LatLng bottomRight)
    {
        //private static final int KMS_IN_DEGREE=

        //Timer timer = new Timer();
        //timer.start();
        final double MILE_TO_METERS=1609.34;
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
            distance = distance * 60 * 1.1515 * MILE_TO_METERS;
            //timer.stop();
            //Log.d(TAG, "getDistance: completed in: "+timer.getDuration());
        }
        return distance;
    }

    private static double deg2rad(double deg) {
        return (deg * Math.PI / 180.0);
    }

    private static double rad2deg(double rad) {
        return (rad * 180.0 / Math.PI);
    }

    /**
     * Calculates distance between two points on a map using Location class.
     * @param p1
     * @param p2
     * @return
     */
    public static double getDistanceS(LatLng p1, LatLng p2)
    {
        //Timer timer = new Timer();
        //timer.start();
        Location loc1 = new Location("p1");
        Location loc2 = new Location("p2");
        loc1.setLatitude(p1.latitude);
        loc1.setLongitude(p1.longitude);
        loc2.setLatitude(p2.latitude);
        loc2.setLongitude(p2.longitude);
        double res = loc1.distanceTo(loc2);
        //timer.stop();
        //Log.d(TAG, "getDistanceS: completed in: "+timer.getDuration());
        return res;
    }


    public static double getRadius(GoogleMap mMap)
    {
        double radius = 0;
        if (mMap!=null)
        {
            LatLng topRight = mMap.getProjection().getVisibleRegion().latLngBounds.northeast;
            LatLng bottomLeft = mMap.getProjection().getVisibleRegion().latLngBounds.southwest;
            radius = Math.round(DistanceUtil.getDistanceS(topRight,bottomLeft)/2);
        }
        return radius;
    }




}
