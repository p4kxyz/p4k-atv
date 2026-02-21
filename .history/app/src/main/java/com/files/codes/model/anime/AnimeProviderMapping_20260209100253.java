package com.files.codes.model.anime;

import com.google.gson.annotations.SerializedName;

public class AnimeProviderMapping {
    @SerializedName("providerMediaId")
    private String providerMediaId;
    @SerializedName("similarity")
    private double similarity;
    @SerializedName("mappingType")
    private String mappingType;

    public String getProviderMediaId() { return providerMediaId; }
    public double getSimilarity() { return similarity; }
    public String getMappingType() { return mappingType; }

    public void setProviderMediaId(String providerMediaId) { this.providerMediaId = providerMediaId; }
    public void setSimilarity(double similarity) { this.similarity = similarity; }
    public void setMappingType(String mappingType) { this.mappingType = mappingType; }
}
