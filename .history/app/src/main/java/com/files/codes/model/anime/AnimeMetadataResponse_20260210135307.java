package com.files.codes.model.anime;

import com.google.gson.annotations.SerializedName;

public class AnimeMetadataResponse {
    @SerializedName("success")
    private boolean success;
    @SerializedName("result")
    private AnimeMedia result;
    @SerializedName("message")
    private String message;

    public boolean isSuccess() { return success; }
    public AnimeMedia getResult() { return result; }
    public String getMessage() { return message; }
}
