package com.files.codes.model.movieDetails;

/**
 * Model for Pagination controls in TV Series episode list
 * Used to display page numbers and navigation buttons
 */
public class PaginationItem {
    private int currentPage;
    private int totalPages;
    private int totalEpisodes;

    public int getCurrentPage() {
        return currentPage;
    }

    public void setCurrentPage(int currentPage) {
        this.currentPage = currentPage;
    }

    public int getTotalPages() {
        return totalPages;
    }

    public void setTotalPages(int totalPages) {
        this.totalPages = totalPages;
    }

    public int getTotalEpisodes() {
        return totalEpisodes;
    }

    public void setTotalEpisodes(int totalEpisodes) {
        this.totalEpisodes = totalEpisodes;
    }
}
