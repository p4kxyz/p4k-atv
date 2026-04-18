package com.files.codes.utils;

import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonParseException;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import java.lang.reflect.Type;

public class GenreDeserializer implements JsonDeserializer<String> {
    @Override
    public String deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
        try {
            if (json.isJsonPrimitive()) {
                // If it's a string, return it directly
                return json.getAsString();
            } else if (json.isJsonArray()) {
                // If it's an array, join the elements with comma
                JsonArray array = json.getAsJsonArray();
                StringBuilder result = new StringBuilder();
                for (int i = 0; i < array.size(); i++) {
                    if (i > 0) {
                        result.append(", ");
                    }
                    JsonElement element = array.get(i);
                    if (element.isJsonObject() && element.getAsJsonObject().has("name")) {
                        result.append(element.getAsJsonObject().get("name").getAsString());
                    } else if (element.isJsonPrimitive()) {
                        result.append(element.getAsString());
                    }
                }
                return result.toString();
            } else if (json.isJsonObject()) {
                // If it's an object with "name" field, extract the name
                JsonObject obj = json.getAsJsonObject();
                if (obj.has("name")) {
                    return obj.get("name").getAsString();
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        // Default case
        return "";
    }
}
