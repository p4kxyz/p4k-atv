package com.files.codes.model.free1;

import com.google.gson.annotations.SerializedName;
import java.io.Serializable;
import java.util.List;

/**
 * Model cho response chi tiết phim từ Free1 API
 */
public class Free1MovieDetail implements Serializable {
    
    @SerializedName("status")
    private boolean status;
    
    @SerializedName("message")
    private String message;
    
    @SerializedName("movie")
    private Free1Movie movie;
    
    @SerializedName("episodes")
    private List<Free1Episode> episodes;
    
    // Getters and Setters
    public boolean isStatus() {
        return status;
    }
    
    public void setStatus(boolean status) {
        this.status = status;
    }
    
    public String getMessage() {
        return message;
    }
    
    public void setMessage(String message) {
        this.message = message;
    }
    
    public Free1Movie getMovie() {
        return movie;
    }
    
    public void setMovie(Free1Movie movie) {
        this.movie = movie;
    }
    
    public List<Free1Episode> getEpisodes() {
        return episodes;
    }
    
    public void setEpisodes(List<Free1Episode> episodes) {
        this.episodes = episodes;
    }
}