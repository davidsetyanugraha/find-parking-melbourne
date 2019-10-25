package com.unimelbs.parkingassistant.util;

import android.util.Log;

import com.unimelbs.parkingassistant.parkingapi.Restriction;

import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;

public class RestrictionsHelper {
    private static final String TAG = "RestrictionHelper";
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
                    "Restriction " + (i + 1) + ": \n" +
                    "\t" + this.restrictions.get(i).getDescription() + "\n";
        }
        return restrictionMsg;
    }

    public boolean isValid() {
        return this.isValid;
    }

    public String getInvalidReason() {
        return this.invalidReason;
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

    public int getDefaultDuration(Date currentDate) {
        int finalDurationRestriction = 0;
        for (int i = 0; i < this.restrictions.size(); i++) {
            int durationRestriction = Integer.parseInt(this.restrictions.get(i).getDuration());

            if (i == 0) {
                finalDurationRestriction = durationRestriction;
            } else if (durationRestriction < finalDurationRestriction) {
                finalDurationRestriction =  durationRestriction;
            }
        }

        Log.d(TAG, "Default Duration: " +finalDurationRestriction);
        return finalDurationRestriction-1;
    }

    public void processRestrictionChecking(Long seconds, Date currentTime, Date toDate) {
        Long durationMinutes = TimeUnit.MINUTES.convert(seconds, TimeUnit.SECONDS);
        this.invalidReason = "";

        Calendar cal = Calendar.getInstance();
        cal.setTime(currentTime);
        int currentDay = cal.get(Calendar.DAY_OF_WEEK);
        int currentHour = cal.get(Calendar.HOUR_OF_DAY);
        int currentMins = cal.get(Calendar.MINUTE);

        Calendar calToDate = Calendar.getInstance();
        calToDate.setTime(toDate);
        int targetDay = calToDate.get(Calendar.DAY_OF_WEEK);
        int targetHour = calToDate.get(Calendar.HOUR_OF_DAY);
        int targetMins = calToDate.get(Calendar.MINUTE);

        for (int i = 0; i < this.restrictions.size(); i++) {

            Integer durationRestriction = Integer.parseInt(this.restrictions.get(i).getDuration());
            Integer restFrDay = Integer.parseInt(this.restrictions.get(i).getFromday());
            Integer restToDay = Integer.parseInt(this.restrictions.get(i).getToday());
            final String[] restrictionStartTime = this.restrictions.get(i).getStarttime().split(Pattern.quote(":"));
            Integer restStHour = Integer.parseInt(restrictionStartTime[0]);
            Integer restStMins = Integer.parseInt(restrictionStartTime[1]);

            final String[] restrictionEndTime = this.restrictions.get(i).getEndtime().split(Pattern.quote(":"));
            Integer restEndHour = Integer.parseInt(restrictionEndTime[0]);
            Integer restEndMins = Integer.parseInt(restrictionEndTime[1]);

            final String restTypeDesc = this.restrictions.get(i).getTypedesc();
            final String restDisExt = this.restrictions.get(i).getDisabilityext();
            final String restEffOnPh = this.restrictions.get(i).getEffectiveonph();
            final String restDesc = this.restrictions.get(i).getDescription();
            Boolean violation = false;

            Log.d(TAG, "=========== "+restDesc +"=============");
            Log.d(TAG, "minutes: " + durationMinutes);
            Log.d(TAG, "restrictionDuration: " + durationRestriction);
            Log.d(TAG, "from: " + restFrDay);
            Log.d(TAG, "today: " + restToDay);
            Log.d(TAG, "restrictionStartTimeHour: " + restStHour);
            Log.d(TAG, "restrictionStartTimeMins: " + restStMins);
            Log.d(TAG, "restrictionEndTimeHour: " + restEndHour);
            Log.d(TAG, "restrictionEndTimeMins: " + restEndMins);
            Log.d(TAG, "restrictionTypeDesc: " + restTypeDesc);
            Log.d(TAG, "restrictionDisExt: " + restDisExt);
            Log.d(TAG, "restEffOnPh: " + restEffOnPh);
            Log.d(TAG, "=======================================");

            if ((durationRestriction != null)) {
                violation = (durationMinutes > durationRestriction) ? true : false;
                if (violation) {
                    if (!restrictedDay(currentDay, targetDay, restFrDay, restToDay)
                    || !restrictedTime(currentHour, targetHour, currentMins, targetMins, restStHour,
                            restEndHour, restStMins,restEndMins))
                    {
                        violation = false;
                    }
                }
                Log.d(TAG, "----------");
                Log.d(TAG, "Violation? = " + violation);
                Log.d(TAG, "----------");
            }

            if (violation) {
                this.invalidReason = this.invalidReason + "\n" + restDesc;
            }
        }

        if (!this.invalidReason.isEmpty()) {
            this.invalidReason = "You are probably violating the parking restriction " + this.invalidReason;
            this.isValid = false;
        } else {
            this.isValid = true;
        }
    }

    private boolean restrictedTime(int currentHour, int targetHour, int currentMins, int targetMins,
                                   int restrictionStartTimeHour, int restrictionEndTimeHour,
                                   int restrictionStartTimeMins, int restrictionEndTimeMins) {
        return (currentHour > restrictionStartTimeHour) && (currentHour < restrictionEndTimeHour) ||
                (targetHour > restrictionStartTimeHour) && (targetHour < restrictionEndTimeHour) ||
                (currentHour == restrictionStartTimeHour) && (currentMins >= restrictionStartTimeMins) ||
                (targetHour == restrictionEndTimeHour) && (targetMins <= restrictionEndTimeMins);
    }

    private boolean restrictedDay(int currentDay, int targetDay, int restrictionFromDay, int restrictionToDay) {
        return (currentDay > restrictionFromDay) && (currentDay < restrictionToDay)
                || (targetDay > restrictionFromDay) && (targetDay < restrictionToDay)
                || (currentDay == restrictionFromDay)
                || (currentDay == restrictionToDay)
                || (targetDay == restrictionFromDay)
                || (targetDay == restrictionToDay);
    }
}
