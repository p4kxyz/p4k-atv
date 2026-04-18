package com.files.codes.model.phim4k;

import com.google.gson.annotations.SerializedName;
import java.util.List;

public class KKPhim4kSearchData {
    @SerializedName("seoOnPage")
    private Object seoOnPage;
    
    @SerializedName("breadCrumb")
    private List<Object> breadCrumb;
    
    @SerializedName("titlePage")
    private String titlePage;
    
    @SerializedName("items")
    private List<Phim4kMovie> items;
    
    @SerializedName("params")
    private Object params;
    
    @SerializedName("type_list")
    private String typeList;
    
    @SerializedName("APP_DOMAIN_FRONTEND")
    private String appDomainFrontend;
    
    @SerializedName("APP_DOMAIN_CDN_IMAGE")
    private String appDomainCdnImage;

    // Getters and Setters
    public List<Phim4kMovie> getItems() { return items; }
    public void setItems(List<Phim4kMovie> items) { this.items = items; }
    
    public String getTitlePage() { return titlePage; }
    public void setTitlePage(String titlePage) { this.titlePage = titlePage; }
    
    public String getAppDomainCdnImage() { return appDomainCdnImage; }
    public void setAppDomainCdnImage(String appDomainCdnImage) { this.appDomainCdnImage = appDomainCdnImage; }
}
