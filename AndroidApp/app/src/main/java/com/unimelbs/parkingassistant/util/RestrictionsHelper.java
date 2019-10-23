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
                    "Restriction " + (i+1) +": \n"+
                    "\t"+this.restrictions.get(i).getDescription()+"\n";
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

    public void processRestrictionChecking(Long seconds, Date currentTime, Date toDate) {
        Long minutes = TimeUnit.MINUTES.convert(seconds,TimeUnit.SECONDS);
        this.invalidReason = "";

        Calendar cal=Calendar.getInstance();
        cal.setTime(currentTime);
        int dayOfWeekCurrentTime = cal.get(Calendar.DAY_OF_WEEK);
        int hourCurrentTime = cal.get(Calendar.HOUR_OF_DAY);
        int minsCurrentTime = cal.get(Calendar.MINUTE);

        Calendar calToDate=Calendar.getInstance();
        calToDate.setTime(toDate);
        int dayOfWeekToDate = calToDate.get(Calendar.DAY_OF_WEEK);
        int hourToDate = calToDate.get(Calendar.HOUR_OF_DAY);
        int minsToDate = calToDate.get(Calendar.MINUTE);

        for (int i = 0; i < this.restrictions.size(); i++) {

            Integer restrictionDuration = Integer.parseInt(this.restrictions.get(i).getDuration());
            Integer restrictionFromDay = Integer.parseInt(this.restrictions.get(i).getFromday());
            Integer restrictionToDay = Integer.parseInt(this.restrictions.get(i).getToday());
            final String[] restrictionStartTime =  this.restrictions.get(i).getStarttime().split(Pattern.quote(":"));
            Integer restrictionStartTimeHour = Integer.parseInt(restrictionStartTime[0]);
            Integer restrictionStartTimeMins = Integer.parseInt(restrictionStartTime[1]);

            final String[] restrictionEndTime =  this.restrictions.get(i).getEndtime().split(Pattern.quote(":"));
            Integer restrictionEndTimeHour = Integer.parseInt(restrictionEndTime[0]);
            Integer restrictionEndTimeMins = Integer.parseInt(restrictionEndTime[1]);
            Boolean violation = false;

            Log.d(TAG, "===========");

            if ((restrictionDuration != null)) {
                Log.d(TAG, "minutes: " + minutes);
                Log.d(TAG, "restrictionDuration: " + restrictionDuration);
                Log.d(TAG, "from: " + restrictionFromDay);
                Log.d(TAG, "today: " + restrictionToDay);
                Log.d(TAG, "restrictionStartTimeHour: " + restrictionStartTimeHour);
                Log.d(TAG, "restrictionStartTimeMins: " + restrictionStartTimeMins);
                Log.d(TAG, "restrictionEndTimeHour: " + restrictionEndTimeHour);
                Log.d(TAG, "restrictionEndTimeMins: " + restrictionEndTimeMins);

                violation = (minutes > restrictionDuration) ? true : false;
                if (violation) {
                    if ((dayOfWeekCurrentTime < restrictionFromDay) && (dayOfWeekToDate < restrictionToDay) || (dayOfWeekCurrentTime > restrictionFromDay) && (dayOfWeekToDate > restrictionToDay)) {
                        violation = false;
                    }

                    if ((hourCurrentTime < restrictionStartTimeHour) && (hourToDate < restrictionEndTimeHour) || (hourCurrentTime > restrictionStartTimeHour) && (hourToDate > restrictionEndTimeHour)) {
                        violation = false;
                    } else {
                        violation = true;
                    }
                }
                Log.d(TAG, "----------");
                Log.d(TAG, "Violation? = "+ violation);
                Log.d(TAG, "----------");
            }

            if (violation) {
                this.invalidReason = this.invalidReason + "\n Parked for period longer than indicated " + this.restrictions.get(i).getDescription();
            }
        }

        this.isValid = (this.invalidReason.isEmpty()) ? true : false;
    }
}
