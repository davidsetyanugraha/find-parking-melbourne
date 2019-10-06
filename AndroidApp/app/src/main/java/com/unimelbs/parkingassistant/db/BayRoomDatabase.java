package com.unimelbs.parkingassistant.db;

import android.content.Context;

import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;

@Database(entities={Bay.class},version=1, exportSchema = false)
public abstract class BayRoomDatabase extends RoomDatabase {
    public abstract BayDao bayDao();

    private static volatile BayRoomDatabase INSTANCE;

    static BayRoomDatabase getDatabase(final Context context)
    {
        if(INSTANCE==null)
        {
            synchronized (BayRoomDatabase.class)
            {
                if (INSTANCE==null)
                {
                    //create database here --how?
                    INSTANCE = Room.databaseBuilder(context.getApplicationContext(),
                            BayRoomDatabase.class,
                            "bay_database")
                            .build();
                }
            }
        }
        return INSTANCE;
    }
}
