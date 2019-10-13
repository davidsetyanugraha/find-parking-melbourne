package com.unimelbs.parkingassistant.model;

import com.unimelbs.parkingassistant.parkingapi.Site;

import java.io.Serializable;
import java.util.List;

public class SiteSerialisable implements Serializable {
    Site site;
    public SiteSerialisable(Site site)
    {
        this.site = site;
    }
}
