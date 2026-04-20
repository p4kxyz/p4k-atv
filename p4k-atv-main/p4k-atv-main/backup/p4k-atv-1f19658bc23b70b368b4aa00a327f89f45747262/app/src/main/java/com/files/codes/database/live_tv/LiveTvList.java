package com.files.codes.database.live_tv;

import androidx.annotation.NonNull;
import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.PrimaryKey;
import androidx.room.TypeConverters;


import com.files.codes.model.LiveTv;

import java.io.Serializable;
import java.util.List;

@Entity(tableName = "live_tv_table")
public class LiveTvList implements Serializable {
    @PrimaryKey
    @NonNull
    private int id;

    @ColumnInfo(name = "live_tv_list")
    @TypeConverters(LiveTvConverter.class)
    private List<LiveTv> liveTvList;

    public LiveTvList() {
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public List<LiveTv> getLiveTvList() {
        return liveTvList;
    }

    public void setLiveTvList(List<LiveTv> liveTvList) {
        this.liveTvList = liveTvList;
    }
}
