package com.files.codes.model.phim4k;

import com.google.gson.annotations.SerializedName;

public class Phim4kPagination {
    @SerializedName("current_page")
    private int currentPage;
    
    @SerializedName("total_page")
    private int totalPage;
    
    @SerializedName("total_items")
    private int totalItems;
    
    @SerializedName("items_per_page")
    private int itemsPerPage;

    // Getters and Setters
    public int getCurrentPage() { return currentPage; }
    public void setCurrentPage(int currentPage) { this.currentPage = currentPage; }
    
    public int getTotalPage() { return totalPage; }
    public void setTotalPage(int totalPage) { this.totalPage = totalPage; }
    
    public int getTotalItems() { return totalItems; }
    public void setTotalItems(int totalItems) { this.totalItems = totalItems; }
    
    public int getItemsPerPage() { return itemsPerPage; }
    public void setItemsPerPage(int itemsPerPage) { this.itemsPerPage = itemsPerPage; }
}