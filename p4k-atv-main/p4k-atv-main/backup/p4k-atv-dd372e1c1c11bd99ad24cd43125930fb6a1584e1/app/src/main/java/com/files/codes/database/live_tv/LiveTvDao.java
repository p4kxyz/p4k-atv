package com.files.codes.database.live_tv;

import static androidx.room.OnConflictStrategy.REPLACE;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;

@Dao
public interface LiveTvDao {
    @Insert(onConflict = REPLACE)
    void insert(LiveTvList liveTvList);

    @Update
    void update(LiveTvList liveTvList);

    @Query("DELETE FROM live_tv_table")
    void deleteAll();

    @Query("SELECT * FROM live_tv_table")
    LiveData<LiveTvList> getAllLiveTv();

}
