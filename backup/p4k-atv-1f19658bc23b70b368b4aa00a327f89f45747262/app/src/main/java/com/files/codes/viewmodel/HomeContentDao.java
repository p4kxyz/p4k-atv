package com.files.codes.viewmodel;

import static androidx.room.OnConflictStrategy.REPLACE;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;

import com.files.codes.model.HomeContentList;

@Dao
public interface HomeContentDao {
    @Insert(onConflict = REPLACE)
    void insert(HomeContentList homeContentList);

    @Update()
    void update(HomeContentList homeContentList);

    @Query("DELETE FROM home_content_table")
    void deleteAllData();

    @Query("SELECT * FROM home_content_table")
    LiveData<HomeContentList> getHomeContentLiveData();
}
