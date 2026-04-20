package com.files.codes.model.phim4k;

import com.google.gson.annotations.SerializedName;

public class KKPhim4kDetailResponse {
    @SerializedName("status")
    private boolean status;
    
    @SerializedName("movie")
    private Phim4kMovie movie;
    
    @SerializedName("episodes")
    private java.util.List<Phim4kEpisodeServer> episodes;

    // Getters and Setters
    public boolean getStatus() { return status; }
    public void setStatus(boolean status) { this.status = status; }
    
    public Phim4kMovie getMovie() { return movie; }
    public void setMovie(Phim4kMovie movie) { this.movie = movie; }
    
    public java.util.List<Phim4kEpisodeServer> getEpisodes() { return episodes; }
    public void setEpisodes(java.util.List<Phim4kEpisodeServer> episodes) { this.episodes = episodes; }
}
