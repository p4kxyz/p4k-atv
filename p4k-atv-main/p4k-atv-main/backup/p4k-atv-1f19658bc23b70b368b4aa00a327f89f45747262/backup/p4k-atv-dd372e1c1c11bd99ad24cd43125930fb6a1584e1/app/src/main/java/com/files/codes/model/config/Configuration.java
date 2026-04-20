package com.files.codes.model.config;

import androidx.annotation.NonNull;
import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.PrimaryKey;
import androidx.room.TypeConverters;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;
import com.files.codes.viewmodel.config.AppConfigConverter;

import java.io.Serializable;

@Entity(tableName = "configuration_table")
public class Configuration implements Serializable {
    @PrimaryKey
    @NonNull
    private int id;

    @ColumnInfo(name = "app_config")
    @TypeConverters(AppConfigConverter.class)
    @SerializedName("app_config")
    @Expose
    private AppConfig appConfig;

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public AppConfig getAppConfig() {
        return appConfig;
    }

    public void setAppConfig(AppConfig appConfig) {
        this.appConfig = appConfig;
    }

}
