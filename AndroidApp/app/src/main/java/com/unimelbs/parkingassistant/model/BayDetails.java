package com.unimelbs.parkingassistant.model;

import com.unimelbs.parkingassistant.parkingapi.Restriction;
import com.unimelbs.parkingassistant.parkingapi.TheGeom;

import java.io.Serializable;
import java.util.List;

public class BayDetails implements Serializable {
    private int bayId;
    private List<Restriction> restrictions;
    private TheGeom theGeom;

    public BayDetails(int bayId, List<Restriction> restrictions, TheGeom theGeom) throws Exception {
        if (bayId==0) throw new Exception("Bay ID can not be 0");
        this.bayId = bayId;
        this.restrictions = restrictions;
        this.theGeom = theGeom;
    }

    public int getBayId() {
        return bayId;
    }

    public List<Restriction> getRestrictions() {
        return restrictions;
    }

    public void setRestrictions(List<Restriction> restrictions) {
        this.restrictions = restrictions;
    }

    public TheGeom getTheGeom() {
        return theGeom;
    }

    public void setTheGeom(TheGeom theGeom) {
        this.theGeom = theGeom;
    }
}
