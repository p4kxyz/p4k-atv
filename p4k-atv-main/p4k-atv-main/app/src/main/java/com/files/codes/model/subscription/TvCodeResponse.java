package com.files.codes.model.subscription;

import com.google.gson.annotations.SerializedName;

public class TvCodeResponse {
    @SerializedName("status")
    private String status;

    @SerializedName("code")
    private String code;

    @SerializedName("expires_in")
    private int expiresIn;

    @SerializedName("message")
    private String message;

    public String getStatus() { return status; }
    public String getCode() { return code; }
    public int getExpiresIn() { return expiresIn; }
    public String getMessage() { return message; }
}
