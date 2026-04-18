package com.files.codes.model.anime;

import com.google.gson.annotations.SerializedName;

public class AnimeEpisodeItem {
    @SerializedName("episodeNumber")
    private String episodeNumber;
    @SerializedName("episodeId")
    private String episodeId;
    @SerializedName("server")
    private String server;

    public String getEpisodeNumber() { return episodeNumber; }
    public String getEpisodeId() { return episodeId; }
    public String getServer() { return server; }

    public void setEpisodeNumber(String episodeNumber) { this.episodeNumber = episodeNumber; }
    public void setEpisodeId(String episodeId) { this.episodeId = episodeId; }
    public void setServer(String server) { this.server = server; }
}
