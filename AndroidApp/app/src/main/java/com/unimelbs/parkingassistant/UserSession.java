package com.unimelbs.parkingassistant;

import com.unimelbs.parkingassistant.model.Bay;

import java.util.Date;

/**
 * Holds data related to user's session, including details of where they
 * parked, parking time, reminders.
 */
public class UserSession {
    private Bay bay;
    private Date parkingStartTime;
    private boolean parkingActive;

    public boolean isParkingActive() {
        return parkingActive;
    }

    public void setParkingActive(boolean parkingActive) {
        this.parkingActive = parkingActive;
    }

    public Bay getBay() {
        return bay;
    }

    public void setBay(Bay bay) {
        this.bay = bay;
    }

    public Date getParkingStartTime() {
        return parkingStartTime;
    }

    public void setParkingStartTime(Date parkingStartTime) {
        this.parkingStartTime = parkingStartTime;
    }
}
