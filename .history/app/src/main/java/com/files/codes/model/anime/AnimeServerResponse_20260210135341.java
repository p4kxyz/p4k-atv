package com.files.codes.model.anime;

import com.google.gson.annotations.SerializedName;
import java.util.List;

public class AnimeServerResponse {
    @SerializedName("provider")
    private String provider;
    @SerializedName("servers")
    private List<String> servers;

    public String getProvider() { return provider; }
    public List<String> getServers() { return servers; }
}
