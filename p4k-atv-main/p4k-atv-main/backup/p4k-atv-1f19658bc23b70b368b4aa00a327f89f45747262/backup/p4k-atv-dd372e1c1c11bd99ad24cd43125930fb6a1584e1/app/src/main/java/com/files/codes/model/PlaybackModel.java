package com.files.codes.model;

import androidx.annotation.NonNull;
import java.io.Serializable;
import java.util.List;

public class PlaybackModel implements Serializable {
    private long id;
    private String movieId;
    private String title;
    private String description;
    private String bgImageUrl;
    private String cardImageUrl;
    private String videoUrl;
    private String videoType;
    private String category;
    private List<Video> videoList;
    private String isPaid;
    private  Video video;
    private long programId;
    private long watchNextId;
    
    // Additional metadata fields for complete watch history
    private String releaseDate;
    private String imdbRating;
    private String runtime;
    private String videoQuality;
    private String isTvSeries;
    private String genre;
    private List<Genre> genreList;

    // Episode navigation data (TV series only)
    private int currentSeasonIndex = -1;
    private int currentEpisodeIndex = -1;
    private int totalEpisodesInSeason = 0;
    private java.util.List<com.files.codes.model.movieDetails.Season> allSeasons;

    public PlaybackModel() {
    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public String getMovieId() {
        return movieId;
    }

    public void setMovieId(String movieId) {
        this.movieId = movieId;
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

    public String getBgImageUrl() {
        return bgImageUrl;
    }

    public void setBgImageUrl(String bgImageUrl) {
        this.bgImageUrl = bgImageUrl;
    }

    public String getCardImageUrl() {
        return cardImageUrl;
    }

    public void setCardImageUrl(String cardImageUrl) {
        this.cardImageUrl = cardImageUrl;
    }

    public String getVideoUrl() {
        return videoUrl;
    }

    public String getVideoType() {
        return videoType;
    }

    public void setVideoType(String videoType) {
        this.videoType = videoType;
    }

    public void setVideoUrl(String videoUrl) {
        this.videoUrl = videoUrl;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public List<Video> getVideoList() {
        return videoList;
    }

    public void setVideoList(List<Video> videoList) {
        this.videoList = videoList;
    }

    public Video getVideo() {
        return video;
    }

    public void setVideo(Video video) {
        this.video = video;
    }

    public long getProgramId() {
        return programId;
    }

    public void setProgramId(long programId) {
        this.programId = programId;
    }

    public String getIsPaid() {
        return isPaid;
    }

    public void setIsPaid(String isPaid) {
        this.isPaid = isPaid;
    }

    public long getWatchNextId() {
        return watchNextId;
    }

    public void setWatchNextId(long watchNextId) {
        this.watchNextId = watchNextId;
    }
    
    // Getter/Setter methods for additional metadata
    public String getReleaseDate() {
        return releaseDate;
    }

    public void setReleaseDate(String releaseDate) {
        this.releaseDate = releaseDate;
    }

    public String getImdbRating() {
        return imdbRating;
    }

    public void setImdbRating(String imdbRating) {
        this.imdbRating = imdbRating;
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

    public String getIsTvSeries() {
        return isTvSeries;
    }

    public void setIsTvSeries(String isTvSeries) {
        this.isTvSeries = isTvSeries;
    }

    public List<Genre> getGenreList() {
        return genreList;
    }

    public void setGenreList(List<Genre> genreList) {
        this.genreList = genreList;
    }

    public String getGenre() {
        return genre;
    }

    public void setGenre(String genre) {
        this.genre = genre;
    }

    // Episode navigation getters and setters
    public int getCurrentSeasonIndex() {
        return currentSeasonIndex;
    }

    public void setCurrentSeasonIndex(int currentSeasonIndex) {
        this.currentSeasonIndex = currentSeasonIndex;
    }

    public int getCurrentEpisodeIndex() {
        return currentEpisodeIndex;
    }

    public void setCurrentEpisodeIndex(int currentEpisodeIndex) {
        this.currentEpisodeIndex = currentEpisodeIndex;
    }

    public int getTotalEpisodesInSeason() {
        return totalEpisodesInSeason;
    }

    public void setTotalEpisodesInSeason(int totalEpisodesInSeason) {
        this.totalEpisodesInSeason = totalEpisodesInSeason;
    }

    public java.util.List<com.files.codes.model.movieDetails.Season> getAllSeasons() {
        return allSeasons;
    }

    public void setAllSeasons(java.util.List<com.files.codes.model.movieDetails.Season> allSeasons) {
        this.allSeasons = allSeasons;
    }

    @NonNull
    @Override
    public String toString() {
        return "PlaybackModel{"
                + "id="
                + id
                +", title='"
                + title
                + '\''
                + ", description='"
                + description
                + '\''
                + ", category='"
                + category
                + '\''
                + ", bgImageUrl='"
                + bgImageUrl
                + '\''
                + ", videoUrl='"
                + videoUrl
                + '\''
                + ", videoType='"
                + videoType
                + '\''
                + ", cardImageUrl='"
                + cardImageUrl
                + '\''
                + ", video='"
                + video
                + '\''
                + ", videoList='"
                + videoList
                + '\''
                + ", programId='"
                + programId
                + '\''
                + ", watchNextId='"
                + watchNextId
                + '\''
                + ", isPaid='"
                + isPaid
                + '\''
                + '}';
    }
}
