package com.files.codes.model.sync;

import com.google.gson.annotations.SerializedName;

/**
 * Response model cho API GET /get-link
 */
public class SyncLinkResponse {
    @SerializedName("success")
    private boolean success;
    
    @SerializedName("message")
    private String message;
    
    @SerializedName("user_id")
    private String userId;
    
    @SerializedName("link")
    private String link;

    // Constructor
    public SyncLinkResponse() {
    }

    // Getters and Setters
    public boolean isSuccess() {
        // If we have a link, consider it successful
        return success || (link != null && !link.trim().isEmpty());
    }

    public void setSuccess(boolean success) {
        this.success = success;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getUserId() {
        // If userId is not set but we have a link, extract it
        if (userId == null && link != null) {
            // Extract user ID from link like "/user-json/f4dec332efbfec767e1df88ab04c632a"
            String[] parts = link.split("/");
            if (parts.length >= 3) {
                return parts[2]; // Get the ID part
            }
        }
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getLink() {
        return link;
    }

    public void setLink(String link) {
        this.link = link;
    }
}