package com.files.codes.model.subscription;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

import java.io.Serializable;

public class ActivationModel implements Serializable {
    @SerializedName("status")
    @Expose
    private String status;
    @SerializedName("user_info")
    @Expose
    private User userInfo;

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public User getUserInfo() {
        return userInfo;
    }

    public void setUserInfo(User userInfo) {
        this.userInfo = userInfo;
    }


}
