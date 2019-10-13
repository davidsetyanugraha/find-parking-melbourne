package com.unimelbs.parkingassistant.model;

import android.util.Log;
import com.unimelbs.parkingassistant.parkingapi.Site;
import java.util.Hashtable;
import java.util.List;

public class BayAdapter {
    private static final String TAG = "BayAdapter";

    public void convertSites(List<Site> sites, List<Bay> bays, Hashtable<Integer,BayDetails> bayDetailsHashtable)
    {
        for (Site site: sites)
        {
            bays.add(convertSite(site));
            bayDetailsHashtable.put(Integer.parseInt(site.getId()),convertSiteDetails(site));
        }
    }

    public Bay convertSite(Site site)
    {
        double lat = site.getLocation().getCoordinates().get(1);
        double lng = site.getLocation().getCoordinates().get(0);

        double[] position = {lat,lng};
        Log.d(TAG, "convertSite: coordinates("+lat+","+lng+"). position size:"+position.length+
                "position: "+position[0]+","+position[1]);
        return new Bay(Integer.parseInt(site.getId()), position);
    }

    public BayDetails convertSiteDetails(Site site)
    {
        BayDetails bayDetails=null;
        try
        {
            bayDetails = new BayDetails(Integer.parseInt(site.getId()),
                    site.getRestrictions(),
                    site.getTheGeom()
            );
        }
        catch (Exception e)
        {
            Log.d(TAG, "convertSiteDetails: ");
        }

        return bayDetails;
    }
}
