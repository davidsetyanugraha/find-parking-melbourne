package com.unimelbs.parkingassistant.util;

public class Timer {
    private static final int MILLIS_TO_SECONDS=1000;
    private long startTime;
    private long duration;
    private String TAG;

    public Timer()
    {
        this.TAG = TAG;

    }
    public void start()
    {
        this.startTime = System.currentTimeMillis();
    }
    public void stop()
    {
        this.duration = (System.currentTimeMillis()-this.startTime);
    }

    public long getDurationInSeconds(){return this.duration/MILLIS_TO_SECONDS;}
    public long getDuration() { return duration;}

}
