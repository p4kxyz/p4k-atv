package com.files.codes.model.anime;

import com.google.gson.annotations.SerializedName;
import java.util.Map;

public class AnimeMedia {
    @SerializedName("id")
    private int id;
    @SerializedName("mediaType")
    private String mediaType;
    @SerializedName("format")
    private String format;
    @SerializedName("status")
    private String status;
    @SerializedName("source")
    private String source;
    @SerializedName("countryOfOrigin")
    private String countryOfOrigin;
    @SerializedName("startDate")
    private String startDate;
    @SerializedName("endDate")
    private String endDate;
    @SerializedName("season")
    private String season;
    @SerializedName("seasonYear")
    private int seasonYear;
    @SerializedName("totalUnits")
    private int totalUnits;
    @SerializedName("unitDurationMin")
    private int unitDurationMin;
    @SerializedName("titles")
    private AnimeTitles titles;
    @SerializedName("descriptions")
    private AnimeTitles descriptions;
    @SerializedName("images")
    private AnimeImages images;
    @SerializedName("streamingProviders")
    private Map<String, AnimeProviderMapping> streamingProviders;

    public int getId() { return id; }
    public String getMediaType() { return mediaType; }
    public String getFormat() { return format; }
    public String getStatus() { return status; }
    public String getSource() { return source; }
    public String getCountryOfOrigin() { return countryOfOrigin; }
    public String getStartDate() { return startDate; }
    public String getEndDate() { return endDate; }
    public String getSeason() { return season; }
    public int getSeasonYear() { return seasonYear; }
    public int getTotalUnits() { return totalUnits; }
    public int getUnitDurationMin() { return unitDurationMin; }
    public AnimeTitles getTitles() { return titles; }
    public AnimeTitles getDescriptions() { return descriptions; }
    public AnimeImages getImages() { return images; }
    public Map<String, AnimeProviderMapping> getStreamingProviders() { return streamingProviders; }

    public void setId(int id) { this.id = id; }
    public void setMediaType(String mediaType) { this.mediaType = mediaType; }
    public void setFormat(String format) { this.format = format; }
    public void setStatus(String status) { this.status = status; }
    public void setTitles(AnimeTitles titles) { this.titles = titles; }
    public void setDescriptions(AnimeTitles descriptions) { this.descriptions = descriptions; }
    public void setImages(AnimeImages images) { this.images = images; }
    public void setStreamingProviders(Map<String, AnimeProviderMapping> streamingProviders) { this.streamingProviders = streamingProviders; }
    public void setSeasonYear(int seasonYear) { this.seasonYear = seasonYear; }
    public void setTotalUnits(int totalUnits) { this.totalUnits = totalUnits; }
    public void setUnitDurationMin(int unitDurationMin) { this.unitDurationMin = unitDurationMin; }

    /** Check if this is a TV series (multi-episode) */
    public boolean isTvSeries() {
        if ("TV".equals(format) || "TV_SHORT".equals(format) || "ONA".equals(format)) return true;
        return totalUnits > 1;
    }

    /** Get year as string */
    public String getYearString() {
        if (seasonYear > 0) return String.valueOf(seasonYear);
        if (startDate != null && startDate.length() >= 4) return startDate.substring(0, 4);
        return "";
    }

    /** Get duration as string */
    public String getDurationString() {
        if (unitDurationMin > 0) return unitDurationMin + " min";
        return "";
    }
}
