package com.unimelbs.parkingassistant.model;

import android.util.Log;
import com.unimelbs.parkingassistant.parkingapi.Site;

import java.util.ArrayList;
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

    public ArrayList<Object> convertSites(List<Site> sites)
    {
        Log.d(TAG, "convertSites: ");
        ArrayList<Bay> bays = new ArrayList<>();
        Hashtable<Integer,BayDetails> bayDetailsHashtable = new Hashtable<>();
        for (Site site: sites)
        {
            bays.add(convertSite(site));
            bayDetailsHashtable.put(Integer.parseInt(site.getId()),convertSiteDetails(site));
        }
        ArrayList<Object> result = new ArrayList<>();
        result.add(bays);
        result.add(bayDetailsHashtable);
        return result;
    }

    public Bay convertSite(Site site)
    {
        double lat = site.getLocation().getCoordinates().get(1);
        double lng = site.getLocation().getCoordinates().get(0);
        double[] position = {lat,lng};
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
