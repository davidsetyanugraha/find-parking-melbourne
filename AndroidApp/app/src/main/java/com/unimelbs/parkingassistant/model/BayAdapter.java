package com.unimelbs.parkingassistant.model;

import android.util.Log;
import com.unimelbs.parkingassistant.parkingapi.Site;
import com.unimelbs.parkingassistant.util.Timer;
import java.util.List;

/**
 * Represents an adapter that converts sites objects returned
 * from the back-end API to their corresponding java objects.
 */
public class BayAdapter {
    private static final String TAG = "BayAdapter";
    DataFeed dataFeed;

    /**
     * Constructor.
     * @param dataFeed
     */
    public BayAdapter(DataFeed dataFeed)
    {this.dataFeed = dataFeed;}


    /**
     * Converts a list of sites returned from the API and adds them to the data repository.
     * @param sites
     */
    public void convertSites(List<Site> sites)
    {
        Log.d(TAG, "convertSites: started.");
        Timer timer = new Timer();
        timer.start();
        if (sites!=null&&sites.size()>0)
        {
            this.dataFeed.getBaysHashtable().clear();
            this.dataFeed.getItems().clear();
        }
        for (Site site: sites){
            this.dataFeed.addBay(convertSite(site));}
        timer.stop();
        Log.d(TAG, "convertSites: completed in "+timer.getDurationInSeconds()+" seconds.");
    }

    /**
     * Converts a single site to its corresponding java object.
     * @param site
     * @return
     */
    public Bay convertSite(Site site)
    {
        double lat = site.getLocation().getCoordinates().get(1);
        double lng = site.getLocation().getCoordinates().get(0);
        double[] position = {lat,lng};



        Bay bay = new Bay(Integer.parseInt(site.getId()),
                        position,
                        site.getRestrictions(),
                        site.getPolygon(),
                        site.getDescription(),
                        "Disability Extension: "+
                                ((site.getRestrictions()!=null&&
                                        site.getRestrictions().size()>1&&
                                        site.getRestrictions().get(0).getDisabilityext()!=null)?
                                site.getRestrictions().get(0).getDisabilityext()+" minutes.":"")
                        );
        return bay;
    }
}
