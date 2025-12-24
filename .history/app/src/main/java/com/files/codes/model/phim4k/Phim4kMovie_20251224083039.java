package com.files.codes.model.phim4k;

import com.google.gson.annotations.SerializedName;
import java.util.List;
import java.util.Map;

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
    private Object director;
    
    @SerializedName("casts")
    private Object casts;
    
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
    
    public Object getCreated() { return created; }
    public void setCreated(Object created) { this.created = created; }
    
    public Object getModified() { return modified; }
    public void setModified(Object modified) { this.modified = modified; }

    public String getCreatedTime() {
        if (created instanceof String) {
            return (String) created;
        } else if (created instanceof Map) {
            Map<?, ?> map = (Map<?, ?>) created;
            if (map.containsKey("time")) {
                Object time = map.get("time");
                return time != null ? time.toString() : null;
            }
        }
        return null;
    }

    public String getModifiedTime() {
        if (modified instanceof String) {
            return (String) modified;
        } else if (modified instanceof Map) {
            Map<?, ?> map = (Map<?, ?>) modified;
            if (map.containsKey("time")) {
                Object time = map.get("time");
                return time != null ? time.toString() : null;
            }
        }
        return null;
    }
    
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
    
    public Object getDirector() { return director; }
    public void setDirector(Object director) { this.director = director; }
    
    public Object getCasts() { return casts; }
    public void setCasts(Object casts) { this.casts = casts; }

    public String getDirectorString() {
        if (director instanceof String) {
            return (String) director;
        } else if (director instanceof List) {
            List<?> list = (List<?>) director;
            if (list.isEmpty()) return "";
            StringBuilder sb = new StringBuilder();
            for (Object item : list) {
                if (sb.length() > 0) sb.append(", ");
                sb.append(item.toString());
            }
            return sb.toString();
        }
        return "";
    }

    public String getCastsString() {
        if (casts instanceof String) {
            return (String) casts;
        } else if (casts instanceof List) {
            List<?> list = (List<?>) casts;
            if (list.isEmpty()) return "";
            StringBuilder sb = new StringBuilder();
            for (Object item : list) {
                if (sb.length() > 0) sb.append(", ");
                sb.append(item.toString());
            }
            return sb.toString();
        }
        return "";
    }
    
    public Object getCategory() { return category; }
    public void setCategory(Object category) { this.category = category; }
    
    public List<Phim4kEpisodeServer> getEpisodes() { return episodes; }
    public void setEpisodes(List<Phim4kEpisodeServer> episodes) { this.episodes = episodes; }
}