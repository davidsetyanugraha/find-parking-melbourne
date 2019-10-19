package com.unimelbs.parkingassistant.model;

import android.util.Log;
import com.unimelbs.parkingassistant.parkingapi.Site;
import com.unimelbs.parkingassistant.util.Timer;
import java.util.List;

public class BayAdapter {
    private static final String TAG = "BayAdapter";
    DataFeed dataFeed;

    public BayAdapter(DataFeed dataFeed)
    {this.dataFeed = dataFeed;}


    public void convertSites(List<Site> sites)
    {
        Log.d(TAG, "convertSites: started.");
        Timer timer = new Timer();
        timer.start();
        for (Site site: sites){
            this.dataFeed.addBay(convertSite(site));}
        timer.stop();
        Log.d(TAG, "convertSites: completed in "+timer.getDurationInSeconds()+" seconds.");
    }

    public Bay convertSite(Site site)
    {
        double lat = site.getLocation().getCoordinates().get(1);
        double lng = site.getLocation().getCoordinates().get(0);
        double[] position = {lat,lng};
        Bay bay = new Bay(Integer.parseInt(site.getId()),
                        position,
                        site.getRestrictions(),
                        site.getTheGeom(),
                        site.getDescription(),
                        site.getDescription());
        return bay;
    }

}
