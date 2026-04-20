package com.files.codes.viewmodel.config;

import static androidx.room.OnConflictStrategy.REPLACE;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;

import com.files.codes.model.config.Configuration;

@Dao
public interface ConfigDao {
    @Insert(onConflict = REPLACE)
    void inset(Configuration configuration);

    @Update
    void update(Configuration configuration);

    @Query("DELETE FROM configuration_table")
    void deleteAll();

    @Query("SELECT * FROM configuration_table")
    Configuration getConfigData();

}
