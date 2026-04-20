package com.files.codes.database.live_tv;

import androidx.room.TypeConverter;

import com.google.common.reflect.TypeToken;
import com.google.gson.Gson;
import com.files.codes.model.LiveTv;

import java.io.Serializable;
import java.lang.reflect.Type;
import java.util.List;

public class LiveTvConverter implements Serializable {
    @TypeConverter
    public static String fromList(List<LiveTv> list){
        Gson gson = new Gson();
        return gson.toJson(list);
    }

    @TypeConverter
    public static List<LiveTv> jsonList(String value){
        Type listType = new TypeToken<List<LiveTv>>(){}.getType();
        Gson gson = new Gson();
        return gson.fromJson(value, listType);
    }

}