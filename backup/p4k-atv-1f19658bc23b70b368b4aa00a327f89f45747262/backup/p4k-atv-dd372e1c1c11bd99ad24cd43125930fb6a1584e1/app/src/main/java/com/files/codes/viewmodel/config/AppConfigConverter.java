package com.files.codes.viewmodel.config;

import androidx.room.TypeConverter;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.files.codes.model.config.AppConfig;

import java.io.Serializable;
import java.lang.reflect.Type;

public class AppConfigConverter implements Serializable {
    @TypeConverter
    public static String fromList(AppConfig appConfig){
        Gson gson = new Gson();
        return gson.toJson(appConfig);
    }
    @TypeConverter
    public static AppConfig jsonList(String value){
        Type type = new TypeToken<AppConfig>(){}.getType();
        Gson gson = new Gson();
        return gson.fromJson(value, type);
    }
}
