package com.files.codes.model;

import com.google.gson.annotations.SerializedName;
import java.io.Serializable;

public class Genre implements Serializable {
    private static final long serialVersionUID = 1L;
    @SerializedName("genre_id")
    private String genreId;
    
    @SerializedName("name")
    private String name;
    
    @SerializedName("slug")
    private String slug;
    
    @SerializedName("description")
    private String description;
    
    @SerializedName("status")
    private String status;
    
    @SerializedName("image_url")
    private String imageUrl;

    public String getGenreId() {
        return genreId;
    }

    public void setGenreId(String genreId) {
        this.genreId = genreId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getSlug() {
        return slug;
    }

    public void setSlug(String slug) {
        this.slug = slug;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getImageUrl() {
        return imageUrl;
    }

    public void setImageUrl(String imageUrl) {
        this.imageUrl = imageUrl;
    }
}
