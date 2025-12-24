package com.files.codes.model.phim4k;

import com.google.gson.annotations.SerializedName;

public class Phim4kTmdb {
    @SerializedName("type")
    private String type;
    
    @SerializedName("id")
    private String id;
    
    @SerializedName("season")
    private Integer season;
    
    @SerializedName("vote_average")
    private Double voteAverage;
    
    @SerializedName("vote_count")
    private Integer voteCount;

    // Getters and Setters
    public String getType() { return type; }
    public void setType(String type) { this.type = type; }
    
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    
    public Integer getSeason() { return season; }
    public void setSeason(Integer season) { this.season = season; }
    
    public Double getVoteAverage() { return voteAverage; }
    public void setVoteAverage(Double voteAverage) { this.voteAverage = voteAverage; }
    
    public Integer getVoteCount() { return voteCount; }
    public void setVoteCount(Integer voteCount) { this.voteCount = voteCount; }
}
