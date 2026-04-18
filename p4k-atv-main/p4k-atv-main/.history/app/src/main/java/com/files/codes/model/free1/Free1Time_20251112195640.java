package com.files.codes.model.free1;

import com.google.gson.annotations.SerializedName;
import java.io.Serializable;

/**
 * Model cho Time trong Free1 API
 */
public class Free1Time implements Serializable {
    
    @SerializedName("time")
    private String time;
    
    // Constructor
    public Free1Time() {}
    
    public Free1Time(String time) {
        this.time = time;
    }
    
    // Getters and Setters
    public String getTime() {
        return time;
    }
    
    public void setTime(String time) {
        this.time = time;
    }
}