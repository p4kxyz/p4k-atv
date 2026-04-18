package com.files.codes.model.movieDetails;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

import java.io.Serializable;
import java.util.List;

public class Episode implements Serializable {
    @SerializedName("episodes_id")
    @Expose
    private String episodesId;
    @SerializedName("episodes_name")
    @Expose
    private String episodesName;
    @SerializedName("stream_key")
    @Expose
    private String streamKey;
    @SerializedName("file_type")
    @Expose
    private String fileType;
    @SerializedName("image_url")
    @Expose
    private String imageUrl;
    @SerializedName("file_url")
    @Expose
    private String fileUrl;
    @SerializedName("subtitle")
    @Expose
    private List<Subtitle> subtitle = null;
    private String isPaid;
    private String seasonName;
    private String tvSeriesTitle;
    private String cardBackgroundUrl;
    private String videosId; // ID of the main TV series for watch history tracking
    private int episodeIndexInSeason = -1; // Index of this episode within its season (for navigation)

    public String getEpisodesId() {
        return episodesId;
    }

    public void setEpisodesId(String episodesId) {
        this.episodesId = episodesId;
    }

    public String getEpisodesName() {
        return episodesName;
    }

    public void setEpisodesName(String episodesName) {
        this.episodesName = episodesName;
    }

    public String getStreamKey() {
        return streamKey;
    }

    public void setStreamKey(String streamKey) {
        this.streamKey = streamKey;
    }

    public String getFileType() {
        return fileType;
    }

    public void setFileType(String fileType) {
        this.fileType = fileType;
    }

    public String getImageUrl() {
        return imageUrl;
    }

    public void setImageUrl(String imageUrl) {
        this.imageUrl = imageUrl;
    }

    public String getFileUrl() {
        return fileUrl;
    }

    public void setFileUrl(String fileUrl) {
        this.fileUrl = fileUrl;
    }

    public List<Subtitle> getSubtitle() {
        return subtitle;
    }

    public void setSubtitle(List<Subtitle> subtitle) {
        this.subtitle = subtitle;
    }

    public String getIsPaid() {
        return isPaid;
    }

    public void setIsPaid(String isPaid) {
        this.isPaid = isPaid;
    }

    public String getSeasonName() {
        return seasonName;
    }

    public void setSeasonName(String seasonName) {
        this.seasonName = seasonName;
    }

    public String getTvSeriesTitle() {
        return tvSeriesTitle;
    }

    public void setTvSeriesTitle(String tvSeriesTitle) {
        this.tvSeriesTitle = tvSeriesTitle;
    }

    public String getCardBackgroundUrl() {
        return cardBackgroundUrl;
    }

    public void setCardBackgroundUrl(String cardBackgroundUrl) {
        this.cardBackgroundUrl = cardBackgroundUrl;
    }
    
    public String getVideosId() {
        return videosId;
    }
    
    public void setVideosId(String videosId) {
        this.videosId = videosId;
    }
    
    public int getEpisodeIndexInSeason() {
        return episodeIndexInSeason;
    }
    
    public void setEpisodeIndexInSeason(int episodeIndexInSeason) {
        this.episodeIndexInSeason = episodeIndexInSeason;
    }
}
