package com.files.codes.model.phim4k;

import com.google.gson.annotations.SerializedName;
import java.util.List;

public class Phim4kMovie {
    @SerializedName(value = "id", alternate = {"_id"})
    private String id;
    
    @SerializedName("name")
    private String name;
    
    @SerializedName("slug")
    private String slug;
    
    @SerializedName(value = "original_name", alternate = {"origin_name"})
    private String originalName;
    
    @SerializedName("thumb_url")
    private String thumbUrl;
    
    @SerializedName("poster_url")
    private String posterUrl;
    
    @SerializedName("created")
    private Object created;
    
    @SerializedName("modified")
    private Object modified; // Can be String or Object in KK
    
    @SerializedName("description")
    private String description;
    
    @SerializedName("total_episodes")
    private int totalEpisodes;
    
    @SerializedName("current_episode")
    private String currentEpisode;
    
    @SerializedName("time")
    private String time;
    
    @SerializedName("quality")
    private String quality;
    
    @SerializedName("language")
    private String language;
    
    @SerializedName("director")
    private String director;
    
    @SerializedName("casts")
    private String casts;
    
    @SerializedName("year")
    private int year;
    
    @SerializedName("tmdb")
    private Phim4kTmdb tmdb;
    
    @SerializedName("category")
    private Object category; // Complex nested object
    
    @SerializedName("episodes")
    private List<Phim4kEpisodeServer> episodes;

    // Getters and Setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    
    public String getSlug() { return slug; }
    public void setSlug(String slug) { this.slug = slug; }
    
    public String getOriginalName() { return originalName; }
    public void setOriginalName(String originalName) { this.originalName = originalName; }
    
    public String getThumbUrl() { return thumbUrl; }
    public void setThumbUrl(String thumbUrl) { this.thumbUrl = thumbUrl; }
    
    public String getPosterUrl() { return posterUrl; }
    public void setPosterUrl(String posterUrl) { this.posterUrl = posterUrl; }
    
    public String getCreated() { return created; }
    public void setCreated(String created) { this.created = created; }
    
    public Object getModified() { return modified; }
    public void setModified(Object modified) { this.modified = modified; }
    
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    
    public int getTotalEpisodes() { return totalEpisodes; }
    public void setTotalEpisodes(int totalEpisodes) { this.totalEpisodes = totalEpisodes; }
    
    public String getCurrentEpisode() { return currentEpisode; }
    public void setCurrentEpisode(String currentEpisode) { this.currentEpisode = currentEpisode; }
    
    public String getTime() { return time; }
    public void setTime(String time) { this.time = time; }
    
    public String getQuality() { return quality; }
    public void setQuality(String quality) { this.quality = quality; }
    
    public int getYear() { return year; }
    public void setYear(int year) { this.year = year; }
    
    public Phim4kTmdb getTmdb() { return tmdb; }
    public void setTmdb(Phim4kTmdb tmdb) { this.tmdb = tmdb; }
    
    public String getLanguage() { return language; }
    public void setLanguage(String language) { this.language = language; }
    
    public String getDirector() { return director; }
    public void setDirector(String director) { this.director = director; }
    
    public String getCasts() { return casts; }
    public void setCasts(String casts) { this.casts = casts; }
    
    public Object getCategory() { return category; }
    public void setCategory(Object category) { this.category = category; }
    
    public List<Phim4kEpisodeServer> getEpisodes() { return episodes; }
    public void setEpisodes(List<Phim4kEpisodeServer> episodes) { this.episodes = episodes; }
}