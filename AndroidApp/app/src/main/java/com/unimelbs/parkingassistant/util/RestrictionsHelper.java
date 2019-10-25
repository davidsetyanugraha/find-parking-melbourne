package com.unimelbs.parkingassistant.util;

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


    public String convertRestrictionsToString(Restriction restriction) {
        String restrictionMsg = "";

        if (restriction.getFromday() != null) {
            restrictionMsg = restrictionMsg + "" + convertToDay(Integer.parseInt(restriction.getFromday()));
        }

        if ((restriction.getToday() != null) && (Integer.parseInt(restriction.getToday()) != 0)) {
            if (Integer.parseInt(restriction.getToday()) != Integer.parseInt(restriction.getFromday())) {
                restrictionMsg = restrictionMsg + " - " + convertToDay(Integer.parseInt(restriction.getToday()));
            }
        }

        if (restriction.getStarttime() != null) {
            String time = restriction.getStarttime();
            String timeWithoutSeconds = time.substring(0, time.length() - 3);
            restrictionMsg = restrictionMsg + " " + timeWithoutSeconds;
        }

        if (restriction.getEndtime() != null) {
            String time = restriction.getEndtime();
            String timeWithoutSeconds = time.substring(0, time.length() - 3);
            restrictionMsg = restrictionMsg + " - " + timeWithoutSeconds;
        }

        return restrictionMsg;
    }

    private String convertToDay(int day) {
        String str_day;

        switch (day) {
            case 1:
                str_day = "Monday";
                break;
            case 2:
                str_day = "Tuesday";
                break;
            case 3:
                str_day = "Wednesday";
                break;
            case 4:
                str_day = "Thursday";
                break;
            case 5:
                str_day = "Friday";
                break;
            case 6:
                str_day = "Saturday";
                break;
            case 0:
                str_day = "Sunday";
                break;
            default:
                str_day = "";
        }

        return str_day;
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
            Boolean violation = false;

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
            }

            if (violation) {
                this.invalidReason = "You are probably violating the parking restriction.";
            }
        }

        if (!this.invalidReason.isEmpty()) {
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
