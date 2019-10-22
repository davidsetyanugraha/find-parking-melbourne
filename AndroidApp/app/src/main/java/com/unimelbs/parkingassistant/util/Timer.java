package com.unimelbs.parkingassistant.util;

import java.text.Format;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * Utility class used to measure performance of different ways of showing data on the map.
 */
public class Timer {
    private static final int MILLIS_TO_SECONDS=1000;
    private long startTime;
    private long duration;

    public void start()
    {
        this.startTime = System.currentTimeMillis();
    }
    public void stop()
    {
        this.duration = (System.currentTimeMillis()-this.startTime);
    }

    public long getDurationInSeconds(){return this.duration/MILLIS_TO_SECONDS;}
    public static String convertToTimestamp(long time)
    {
        Date d = new Date(time);
        Format f = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.S");
        return f.format(d);
    }
}
