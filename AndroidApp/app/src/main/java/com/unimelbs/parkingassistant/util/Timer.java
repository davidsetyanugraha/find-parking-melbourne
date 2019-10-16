package com.unimelbs.parkingassistant.util;

public class Timer {
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
        this.duration = (System.currentTimeMillis()-this.startTime)/1000;
    }

    public long getDuration(){return this.duration;}

}
