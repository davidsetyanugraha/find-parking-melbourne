package com.unimelbs.parkingassistant.util;

import com.unimelbs.parkingassistant.parkingapi.Restriction;

import java.time.LocalDateTime;
import java.util.List;

public class RestrictionsHelper {

    public static String convertRestrictionsToString(List<Restriction> restrictions) {
        String restrictionMsg = "";
        for (int i = 0; i < restrictions.size(); i++) {
            restrictionMsg = restrictionMsg +
                    "Restriction " + (i+1) +": \n"+
                    "\t"+restrictions.get(i).getDescription()+"\n";
        }
        return  restrictionMsg;
    }

    public static String convertRestrictionsToOneLineString(List<Restriction> restrictions) {
        String restrictionMsg = "Restriction: ";
        for (int i = 0; i < restrictions.size(); i++) {
            restrictionMsg = restrictionMsg +
                    "\t"+restrictions.get(i).getDescription()+", ";
        }
        return  restrictionMsg;
    }


    public static String checkhour(List<Restriction> restrictions) {
        String restrictionMsg = "";
        for (int i = 0; i < restrictions.size(); i++) {
            restrictionMsg = restrictionMsg +
                    "Restriction " + (i+1) +": \n"+
                    "\t"+restrictions.get(i).getDescription()+"\n";
        }
        return  restrictionMsg;
    }

    public static boolean isValid(List<Restriction> restrictions, long hour, LocalDateTime currentDate) {



        for (int i = 0; i < restrictions.size(); i++) {
//            int restrictionHour = (restrictions.get(i).getDuration()) / 60;
//            if () {
//
//            }
        }
        return true;
    }

    public static String getInvalidReason(List<Restriction> restrictions, long hour, LocalDateTime currentDate) {
        for (int i = 0; i < restrictions.size(); i++) {

        }
        return "Invalid Input";
    }

}
