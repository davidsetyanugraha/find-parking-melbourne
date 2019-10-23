package com.unimelbs.parkingassistant.util;

import com.unimelbs.parkingassistant.parkingapi.Restriction;

import java.util.Date;
import java.util.List;

public class RestrictionsHelper {
    private List<Restriction> restrictions;
    private Boolean isValid;
    private String invalidReason;
    public RestrictionsHelper(List<Restriction> restrictions) {
        this.restrictions = restrictions;
    }

    public String convertRestrictionsToString() {
        String restrictionMsg = "";
        for (int i = 0; i < this.restrictions.size(); i++) {
            restrictionMsg = restrictionMsg +
                    "Restriction " + (i+1) +": \n"+
                    "\t"+this.restrictions.get(i).getDescription()+"\n";
        }
        return restrictionMsg;
    }

    public boolean isValid() {
        return false;
    }

    public String getInvalidReason() {
        return "---";
    }

    public String setInvalidReason(String invalidReason) {
        this.invalidReason = invalidReason;
        return this.invalidReason;
    }

    public List<Restriction> getRestrictions() {
        return restrictions;
    }

    public void setRestrictions(List<Restriction> restrictions) {
        this.restrictions = restrictions;
    }

    public void processRestrictionChecking(Long seconds, Date currentTime) {
//        Long mins = seconds / 60;
//        currentTime.getDayOfWeek().;
//        Date endParkingDate = DateUtils.addSeconds(new Date(), seconds.intValue());
//
//        for (int i = 0; i < this.restrictions.size(); i++) {
//
//            this.restrictions.get(i).getDuration()
//
//        }
    }
}
