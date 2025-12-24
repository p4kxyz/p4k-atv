package com.files.codes.model.phim4k;

import com.google.gson.annotations.SerializedName;

public class KKPhim4kSearchResponse {
    @SerializedName("status")
    private String status;
    
    @SerializedName("msg")
    private String msg;
    
    @SerializedName("data")
    private KKPhim4kSearchData data;

    // Getters and Setters
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    
    public String getMsg() { return msg; }
    public void setMsg(String msg) { this.msg = msg; }
    
    public KKPhim4kSearchData getData() { return data; }
    public void setData(KKPhim4kSearchData data) { this.data = data; }
}
