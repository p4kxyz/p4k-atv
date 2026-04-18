package com.files.codes.utils;

import com.files.codes.model.Movie;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

public class FavoriteListDeserializer implements JsonDeserializer<List<Movie>> {
    
    @Override
    public List<Movie> deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
        List<Movie> movies = new ArrayList<>();
        
        try {
            if (json.isJsonArray()) {
                // Case 1: Direct array response: [...]
                JsonArray array = json.getAsJsonArray();
                Type listType = new TypeToken<List<Movie>>(){}.getType();
                movies = new Gson().fromJson(array, listType);
            } else if (json.isJsonObject()) {
                // Case 2: Wrapper object response: {"data": [...]} or {"movies": [...]}
                JsonObject jsonObject = json.getAsJsonObject();
                JsonArray array = null;
                
                if (jsonObject.has("data") && jsonObject.get("data").isJsonArray()) {
                    array = jsonObject.getAsJsonArray("data");
                } else if (jsonObject.has("movies") && jsonObject.get("movies").isJsonArray()) {
                    array = jsonObject.getAsJsonArray("movies");
                }
                
                if (array != null) {
                    Type listType = new TypeToken<List<Movie>>(){}.getType();
                    movies = new Gson().fromJson(array, listType);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        
        return movies != null ? movies : new ArrayList<>();
    }
}
