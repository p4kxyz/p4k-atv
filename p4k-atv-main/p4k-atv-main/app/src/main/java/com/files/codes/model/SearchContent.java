package com.files.codes.model;

public class SearchContent {
    private String id;
    private String title;
    private String description;
    private String type;
    private String streamUrl;
    private String streamFrom;
    private String thumbnailUrl;
    private String releaseYear;
    
    private String runtime;
    private String videoQuality;
    private String imdbRating;

    public SearchContent() {
    }

    public SearchContent(String id, String title, String description, String type, String streamUrl, String streamFrom, String thumbnailUrl) {
        this.id = id;
        this.title = title;
        this.description = description;
        this.type = type;
        this.streamUrl = streamUrl;
        this.streamFrom = streamFrom;
        this.thumbnailUrl = thumbnailUrl;
    }

    public SearchContent(String id, String title, String description, String type, String streamUrl, String streamFrom, String thumbnailUrl, String releaseYear) {
        this.id = id;
        this.title = title;
        this.description = description;
        this.type = type;
        this.streamUrl = streamUrl;
        this.streamFrom = streamFrom;
        this.thumbnailUrl = thumbnailUrl;
        this.releaseYear = releaseYear;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getStreamUrl() {
        return streamUrl;
    }

    public void setStreamUrl(String streamUrl) {
        this.streamUrl = streamUrl;
    }

    public String getStreamFrom() {
        return streamFrom;
    }

    public void setStreamFrom(String streamFrom) {
        this.streamFrom = streamFrom;
    }

    public String getThumbnailUrl() {
        return thumbnailUrl;
    }

    public void setThumbnailUrl(String thumbnailUrl) {
        this.thumbnailUrl = thumbnailUrl;
    }

    public String getReleaseYear() {
        return releaseYear;
    }

    public void setReleaseYear(String releaseYear) {
        this.releaseYear = releaseYear;
    }

    public String getRuntime() {
        return runtime;
    }

    public void setRuntime(String runtime) {
        this.runtime = runtime;
    }

    public String getVideoQuality() {
        return videoQuality;
    }

    public void setVideoQuality(String videoQuality) {
        this.videoQuality = videoQuality;
    }

    public String getImdbRating() {
        return imdbRating;
    }

    public void setImdbRating(String imdbRating) {
        this.imdbRating = imdbRating;
    }
}
