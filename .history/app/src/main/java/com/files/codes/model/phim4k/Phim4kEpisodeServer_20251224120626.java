package com.files.codes.model.phim4k;

import com.google.gson.annotations.SerializedName;
import java.util.List;

public class Phim4kEpisodeServer {
    @SerializedName("server_name")
    private String serverName;
    
    @SerializedName(value = "items", alternate = {"server_data"})
    private List<Phim4kEpisode> items;

    // Getters and Setters
    public String getServerName() { return serverName; }
    public void setServerName(String serverName) { this.serverName = serverName; }
    
    public List<Phim4kEpisode> getItems() { return items; }
    public void setItems(List<Phim4kEpisode> items) { this.items = items; }
}