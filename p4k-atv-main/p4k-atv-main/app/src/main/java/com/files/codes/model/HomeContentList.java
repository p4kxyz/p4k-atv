package com.files.codes.model;

import androidx.annotation.NonNull;
import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.PrimaryKey;
import androidx.room.TypeConverters;

import com.google.gson.annotations.Expose;

import java.io.Serializable;
import java.util.List;


@Entity(tableName = "home_content_table")
public class HomeContentList implements Serializable {
    @PrimaryKey()
    @NonNull
    @ColumnInfo(name = "home_content_id")
    @Expose
    private int homeContentId;

    @ColumnInfo(name = "data")
    @TypeConverters(HomeContentConverter.class)
    private List<HomeContent> homeContentList;

    public HomeContentList() {
    }

    public int getHomeContentId() {
        return homeContentId;
    }

    public void setHomeContentId(int homeContentId) {
        this.homeContentId = homeContentId;
    }

    public List<HomeContent> getHomeContentList() {
        return homeContentList;
    }

    public void setHomeContentList(List<HomeContent> homeContentList) {
        this.homeContentList = homeContentList;
    }

}
