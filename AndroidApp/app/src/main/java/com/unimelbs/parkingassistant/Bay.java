package com.unimelbs.parkingassistant;

import androidx.annotation.NonNull;
import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName="Bay")
public class Bay {
    @PrimaryKey
    @NonNull
    @ColumnInfo(name="bayId")
    private int bayId;

    @NonNull
    private double lat;
    @NonNull
    private double lon;

    public Bay(@NonNull int bayId,
               double lat,
               double lon)
    {
        this.bayId = bayId;
        this.lat = lat;
        this.lon = lon;
    }

    public double getLat() {
        return lat;
    }

    public int getBayId() {
        return bayId;
    }

    public double getLon() {
        return lon;
    }
}
