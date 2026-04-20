package com.files.codes.model.anime;

import com.google.gson.annotations.SerializedName;
import java.util.List;

public class AnimeEpisodeResponse {
    @SerializedName("provider")
    private String provider;
    @SerializedName("total")
    private int total;
    @SerializedName("hasNextPage")
    private boolean hasNextPage;
    @SerializedName("episodes")
    private List<AnimeEpisodeItem> episodes;

    public String getProvider() { return provider; }
    public int getTotal() { return total; }
    public boolean isHasNextPage() { return hasNextPage; }
    public List<AnimeEpisodeItem> getEpisodes() { return episodes; }
}
