package com.files.codes.model.phim4k;

import com.google.gson.annotations.SerializedName;

public class Phim4kEpisode {
    @SerializedName("name")
    private String name;
    
    @SerializedName("slug")
    private String slug;
    
    @SerializedName("link")
    private String link;

    // Getters and Setters
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    
    public String getSlug() { return slug; }
    public void setSlug(String slug) { this.slug = slug; }
    
    public String getLink() { return link; }
    public void setLink(String link) { this.link = link; }
}