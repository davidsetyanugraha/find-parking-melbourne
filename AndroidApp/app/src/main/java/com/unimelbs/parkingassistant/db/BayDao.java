package com.unimelbs.parkingassistant.db;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

import java.util.List;

@Dao
public interface BayDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insert(Bay bay);

    @Query("DELETE FROM Bay")
    void deleteAll();

    @Query("SELECT * FROM Bay order by bayId")
    LiveData<List<Bay>> getAllBays();



}
