package com.files.codes.model.free1;

import com.google.gson.annotations.SerializedName;
import java.io.Serializable;
import java.util.List;

/**
 * Model cho thông tin phim trong Free1 API
 */
public class Free1Movie implements Serializable {
    
    @SerializedName("_id")
    private String id;
    
    @SerializedName("name")
    private String name;
    
    @SerializedName("slug")
    private String slug;
    
    @SerializedName("origin_name")
    private String originName;
    
    @SerializedName("content")
    private String content;
    
    @SerializedName("type")
    private String type;
    
    @SerializedName("status")
    private String status;
    
    @SerializedName("poster_url")
    private String posterUrl;
    
    @SerializedName("thumb_url")
    private String thumbUrl;
    
    @SerializedName("is_copyright")
    private boolean isCopyright;
    
    @SerializedName("sub_docquyen")
    private boolean subDocquyen;
    
    @SerializedName("chieurap")
    private boolean chieurap;
    
    @SerializedName("trailer_url")
    private String trailerUrl;
    
    @SerializedName("time")
    private String time;
    
    @SerializedName("episode_current")
    private String episodeCurrent;
    
    @SerializedName("episode_total")
    private String episodeTotal;
    
    @SerializedName("quality")
    private String quality;
    
    @SerializedName("lang")
    private String lang;
    
    @SerializedName("notify")
    private String notify;
    
    @SerializedName("showtimes")
    private String showtimes;
    
    @SerializedName("year")
    private int year;
    
    @SerializedName("view")
    private long view;
    
    @SerializedName("actor")
    private List<String> actor;
    
    @SerializedName("director")
    private List<String> director;
    
    @SerializedName("category")
    private List<Free1Category> category;
    
    @SerializedName("country")
    private List<Free1Country> country;
    
    @SerializedName("created")
    private Free1Time created;
    
    @SerializedName("modified")
    private Free1Time modified;
    
    // Getters and Setters
    public String getId() {
        return id;
    }
    
    public void setId(String id) {
        this.id = id;
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
    
    public String getOriginName() {
        return originName;
    }
    
    public void setOriginName(String originName) {
        this.originName = originName;
    }
    
    public String getContent() {
        return content;
    }
    
    public void setContent(String content) {
        this.content = content;
    }
    
    public String getType() {
        return type;
    }
    
    public void setType(String type) {
        this.type = type;
    }
    
    public String getStatus() {
        return status;
    }
    
    public void setStatus(String status) {
        this.status = status;
    }
    
    public String getPosterUrl() {
        return posterUrl;
    }
    
    public void setPosterUrl(String posterUrl) {
        this.posterUrl = posterUrl;
    }
    
    public String getThumbUrl() {
        return thumbUrl;
    }
    
    public void setThumbUrl(String thumbUrl) {
        this.thumbUrl = thumbUrl;
    }
    
    public boolean isCopyright() {
        return isCopyright;
    }
    
    public void setCopyright(boolean copyright) {
        isCopyright = copyright;
    }
    
    public boolean isSubDocquyen() {
        return subDocquyen;
    }
    
    public void setSubDocquyen(boolean subDocquyen) {
        this.subDocquyen = subDocquyen;
    }
    
    public boolean isChieurap() {
        return chieurap;
    }
    
    public void setChieurap(boolean chieurap) {
        this.chieurap = chieurap;
    }
    
    public String getTrailerUrl() {
        return trailerUrl;
    }
    
    public void setTrailerUrl(String trailerUrl) {
        this.trailerUrl = trailerUrl;
    }
    
    public String getTime() {
        return time;
    }
    
    public void setTime(String time) {
        this.time = time;
    }
    
    public String getEpisodeCurrent() {
        return episodeCurrent;
    }
    
    public void setEpisodeCurrent(String episodeCurrent) {
        this.episodeCurrent = episodeCurrent;
    }
    
    public String getEpisodeTotal() {
        return episodeTotal;
    }
    
    public void setEpisodeTotal(String episodeTotal) {
        this.episodeTotal = episodeTotal;
    }
    
    public String getQuality() {
        return quality;
    }
    
    public void setQuality(String quality) {
        this.quality = quality;
    }
    
    public String getLang() {
        return lang;
    }
    
    public void setLang(String lang) {
        this.lang = lang;
    }
    
    public String getNotify() {
        return notify;
    }
    
    public void setNotify(String notify) {
        this.notify = notify;
    }
    
    public String getShowtimes() {
        return showtimes;
    }
    
    public void setShowtimes(String showtimes) {
        this.showtimes = showtimes;
    }
    
    public int getYear() {
        return year;
    }
    
    public void setYear(int year) {
        this.year = year;
    }
    
    public long getView() {
        return view;
    }
    
    public void setView(long view) {
        this.view = view;
    }
    
    public List<String> getActor() {
        return actor;
    }
    
    public void setActor(List<String> actor) {
        this.actor = actor;
    }
    
    public List<String> getDirector() {
        return director;
    }
    
    public void setDirector(List<String> director) {
        this.director = director;
    }
    
    public List<Free1Category> getCategory() {
        return category;
    }
    
    public void setCategory(List<Free1Category> category) {
        this.category = category;
    }
    
    public List<Free1Country> getCountry() {
        return country;
    }
    
    public void setCountry(List<Free1Country> country) {
        this.country = country;
    }
    
    public Free1Time getCreated() {
        return created;
    }
    
    public void setCreated(Free1Time created) {
        this.created = created;
    }
    
    public Free1Time getModified() {
        return modified;
    }
    
    public void setModified(Free1Time modified) {
        this.modified = modified;
    }
}