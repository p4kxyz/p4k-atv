package com.files.codes.model.anime;

import com.google.gson.annotations.SerializedName;

public class AnimeImages {
    @SerializedName("coverXl")
    private String coverXl;
    @SerializedName("coverLg")
    private String coverLg;
    @SerializedName("coverMd")
    private String coverMd;
    @SerializedName("coverColor")
    private String coverColor;
    @SerializedName("bannerUrl")
    private String bannerUrl;

    public String getCoverXl() { return coverXl; }
    public String getCoverLg() { return coverLg; }
    public String getCoverMd() { return coverMd; }
    public String getCoverColor() { return coverColor; }
    public String getBannerUrl() { return bannerUrl; }

    public void setCoverXl(String coverXl) { this.coverXl = coverXl; }
    public void setCoverLg(String coverLg) { this.coverLg = coverLg; }
    public void setCoverMd(String coverMd) { this.coverMd = coverMd; }
    public void setCoverColor(String coverColor) { this.coverColor = coverColor; }
    public void setBannerUrl(String bannerUrl) { this.bannerUrl = bannerUrl; }

    /** Returns best poster image: coverXl > coverLg > coverMd */
    public String getBestPoster() {
        if (coverXl != null && !coverXl.isEmpty()) return coverXl;
        if (coverLg != null && !coverLg.isEmpty()) return coverLg;
        if (coverMd != null && !coverMd.isEmpty()) return coverMd;
        return "";
    }

    /** Returns best thumbnail: coverLg > coverMd > coverXl */
    public String getBestThumbnail() {
        if (coverLg != null && !coverLg.isEmpty()) return coverLg;
        if (coverMd != null && !coverMd.isEmpty()) return coverMd;
        if (coverXl != null && !coverXl.isEmpty()) return coverXl;
        return "";
    }
}
