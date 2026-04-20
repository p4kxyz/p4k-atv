package com.files.codes.model;

import androidx.room.TypeConverter;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.io.Serializable;
import java.lang.reflect.Type;
import java.util.List;

public class HomeContentConverter implements Serializable {
    @TypeConverter
    public static String fromList(List<HomeContent> list){
        Gson gson = new Gson();
        return gson.toJson(list);
    }

    @TypeConverter
    public static List<HomeContent> jsonList(String value){
        Type listType = new TypeToken<List<HomeContent>>(){}.getType();
        Gson gson = new Gson();
        return gson.fromJson(value, listType);
    }
}
