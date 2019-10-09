package com.unimelbs.parkingassistant.ui;

import com.google.android.gms.maps.model.LatLng;
import com.unimelbs.parkingassistant.model.Bay;

import java.util.ArrayList;
import java.util.List;

public class DataFeed {
    private static final String TAG = "TE-DataFeed";
    private List<Bay> bayList;
    public void addBays()
    {
        String lat = "-37.79";
        String lng = "144.96";
        bayList = new ArrayList<Bay>();
        bayList.add(new Bay(3787, new LatLng(-37.81075000660076,144.98359695184072)));
        bayList.add(new Bay(3793, new LatLng(-37.818099049118516,144.98935803814706)));
        bayList.add(new Bay(4317, new LatLng(-37.818498706487105,144.98926900116132)));
        bayList.add(new Bay(5626, new LatLng(-37.81824811551666,144.9893113154258)));
        bayList.add(new Bay(5996, new LatLng(-37.81819774695974,144.98931981979553)));
    }

    public List<Bay> getBayList() {
        return bayList;
    }
}
