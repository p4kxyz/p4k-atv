package com.files.codes.model.subscription;

import com.google.gson.annotations.SerializedName;

public class TvCodeCheckResponse {
    @SerializedName("status")
    private String status;

    @SerializedName("message")
    private String message;

    @SerializedName("user_info")
    private User userInfo;

    public String getStatus() { return status; }
    public String getMessage() { return message; }
    public User getUserInfo() { return userInfo; }
}
