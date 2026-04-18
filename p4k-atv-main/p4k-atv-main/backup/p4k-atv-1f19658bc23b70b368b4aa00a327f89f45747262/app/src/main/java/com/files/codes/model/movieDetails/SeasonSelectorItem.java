package com.files.codes.model.movieDetails;

import java.util.List;

/**
 * Model for Season Selector button in TV Series details
 * Used to display current season and provide season selection dialog
 */
public class SeasonSelectorItem {
    private String currentSeasonName;
    private int currentSeasonIndex;
    private List<Season> allSeasons;

    public String getCurrentSeasonName() {
        return currentSeasonName;
    }

    public void setCurrentSeasonName(String currentSeasonName) {
        this.currentSeasonName = currentSeasonName;
    }

    public int getCurrentSeasonIndex() {
        return currentSeasonIndex;
    }

    public void setCurrentSeasonIndex(int currentSeasonIndex) {
        this.currentSeasonIndex = currentSeasonIndex;
    }

    public List<Season> getAllSeasons() {
        return allSeasons;
    }

    public void setAllSeasons(List<Season> allSeasons) {
        this.allSeasons = allSeasons;
    }
}
