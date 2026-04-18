package com.files.codes.model.phim4k;

import com.google.gson.annotations.SerializedName;
import java.util.List;

public class KKPhim4kResponse {
    @SerializedName("status")
    private boolean status;
    
    @SerializedName("pagination")
    private Phim4kPagination pagination;
    
    @SerializedName("items")
    private List<Phim4kMovie> items;

    // Getters and Setters
    public boolean getStatus() { return status; }
    public void setStatus(boolean status) { this.status = status; }
    
    public Phim4kPagination getPagination() { return pagination; }
    public void setPagination(Phim4kPagination pagination) { this.pagination = pagination; }
    
    public List<Phim4kMovie> getItems() { return items; }
    public void setItems(List<Phim4kMovie> items) { this.items = items; }
}
