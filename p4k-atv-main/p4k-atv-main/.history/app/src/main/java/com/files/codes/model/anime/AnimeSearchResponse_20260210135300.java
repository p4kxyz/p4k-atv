package com.files.codes.model.anime;

import com.google.gson.annotations.SerializedName;
import java.util.List;

public class AnimeSearchResponse {
    @SerializedName("success")
    private boolean success;
    @SerializedName("results")
    private List<AnimeMedia> results;
    @SerializedName("total")
    private int total;
    @SerializedName("limit")
    private int limit;
    @SerializedName("offset")
    private int offset;
    @SerializedName("hasNextPage")
    private boolean hasNextPage;

    public boolean isSuccess() { return success; }
    public List<AnimeMedia> getResults() { return results; }
    public int getTotal() { return total; }
    public int getLimit() { return limit; }
    public int getOffset() { return offset; }
    public boolean isHasNextPage() { return hasNextPage; }
}
