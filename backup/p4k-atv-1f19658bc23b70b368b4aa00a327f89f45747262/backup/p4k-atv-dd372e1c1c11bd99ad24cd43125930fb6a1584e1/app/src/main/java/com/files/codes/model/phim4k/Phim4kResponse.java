package com.files.codes.model.phim4k;

import com.google.gson.annotations.SerializedName;
import java.util.List;

public class Phim4kResponse {
    @SerializedName("status")
    private String status;
    
    @SerializedName("paginate")
    private Phim4kPagination paginate;
    
    @SerializedName("items")
    private List<Phim4kMovie> items;

    // Getters and Setters
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    
    public Phim4kPagination getPaginate() { return paginate; }
    public void setPaginate(Phim4kPagination paginate) { this.paginate = paginate; }
    
    public List<Phim4kMovie> getItems() { return items; }
    public void setItems(List<Phim4kMovie> items) { this.items = items; }
}