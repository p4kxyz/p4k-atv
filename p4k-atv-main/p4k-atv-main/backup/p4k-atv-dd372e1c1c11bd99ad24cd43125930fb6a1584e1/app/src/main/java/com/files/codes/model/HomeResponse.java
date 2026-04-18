package com.files.codes.model;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

import java.util.List;

public class HomeResponse {
    @SerializedName("slider")
    @Expose
    private SliderData slider;

    @SerializedName("popular_stars")
    @Expose
    private List<Object> popularStars;

    @SerializedName("all_country")
    @Expose
    private List<Object> allCountry;

    @SerializedName("all_genre")
    @Expose
    private List<Object> allGenre;

    @SerializedName("featured_tv_channel")
    @Expose
    private List<Object> featuredTvChannel;

    @SerializedName("latest_movies")
    @Expose
    private List<VideoContent> latestMovies;

    @SerializedName("latest_tvseries")
    @Expose
    private List<VideoContent> latestTvseries;

    @SerializedName("features_genre_and_movie")
    @Expose
    private List<GenreWithMovies> featuresGenreAndMovie;

    public SliderData getSlider() {
        return slider;
    }

    public void setSlider(SliderData slider) {
        this.slider = slider;
    }

    public List<Object> getPopularStars() {
        return popularStars;
    }

    public void setPopularStars(List<Object> popularStars) {
        this.popularStars = popularStars;
    }

    public List<Object> getAllCountry() {
        return allCountry;
    }

    public void setAllCountry(List<Object> allCountry) {
        this.allCountry = allCountry;
    }

    public List<Object> getAllGenre() {
        return allGenre;
    }

    public void setAllGenre(List<Object> allGenre) {
        this.allGenre = allGenre;
    }

    public List<Object> getFeaturedTvChannel() {
        return featuredTvChannel;
    }

    public void setFeaturedTvChannel(List<Object> featuredTvChannel) {
        this.featuredTvChannel = featuredTvChannel;
    }

    public List<VideoContent> getLatestMovies() {
        return latestMovies;
    }

    public void setLatestMovies(List<VideoContent> latestMovies) {
        this.latestMovies = latestMovies;
    }

    public List<VideoContent> getLatestTvseries() {
        return latestTvseries;
    }

    public void setLatestTvseries(List<VideoContent> latestTvseries) {
        this.latestTvseries = latestTvseries;
    }

    public List<GenreWithMovies> getFeaturesGenreAndMovie() {
        return featuresGenreAndMovie;
    }

    public void setFeaturesGenreAndMovie(List<GenreWithMovies> featuresGenreAndMovie) {
        this.featuresGenreAndMovie = featuresGenreAndMovie;
    }

    // Inner class for slider data
    public static class SliderData {
        @SerializedName("slider_type")
        @Expose
        private String sliderType;

        @SerializedName("slide")
        @Expose
        private List<SliderContent> slide;

        public String getSliderType() {
            return sliderType;
        }

        public void setSliderType(String sliderType) {
            this.sliderType = sliderType;
        }

        public List<SliderContent> getSlide() {
            return slide;
        }

        public void setSlide(List<SliderContent> slide) {
            this.slide = slide;
        }
    }

    // Inner class for slider content
    public static class SliderContent {
        @SerializedName("id")
        @Expose
        private String id;

        @SerializedName("title")
        @Expose
        private String title;

        @SerializedName("description")
        @Expose
        private String description;

        @SerializedName("image_link")
        @Expose
        private String imageLink;

        @SerializedName("imdb_rating")
        @Expose
        private String imdbRating;

        @SerializedName("slug")
        @Expose
        private String slug;

        @SerializedName("action_type")
        @Expose
        private String actionType;

        @SerializedName("action_btn_text")
        @Expose
        private String actionBtnText;

        @SerializedName("action_id")
        @Expose
        private String actionId;

        @SerializedName("action_url")
        @Expose
        private String actionUrl;

        // Getters and setters
        public String getId() { return id; }
        public void setId(String id) { this.id = id; }
        public String getTitle() { return title; }
        public void setTitle(String title) { this.title = title; }
        public String getDescription() { return description; }
        public void setDescription(String description) { this.description = description; }
        public String getImageLink() { return imageLink; }
        public void setImageLink(String imageLink) { this.imageLink = imageLink; }
        public String getImdbRating() { return imdbRating; }
        public void setImdbRating(String imdbRating) { this.imdbRating = imdbRating; }
        public String getSlug() { return slug; }
        public void setSlug(String slug) { this.slug = slug; }
        public String getActionType() { return actionType; }
        public void setActionType(String actionType) { this.actionType = actionType; }
        public String getActionBtnText() { return actionBtnText; }
        public void setActionBtnText(String actionBtnText) { this.actionBtnText = actionBtnText; }
        public String getActionId() { return actionId; }
        public void setActionId(String actionId) { this.actionId = actionId; }
        public String getActionUrl() { return actionUrl; }
        public void setActionUrl(String actionUrl) { this.actionUrl = actionUrl; }
    }
}