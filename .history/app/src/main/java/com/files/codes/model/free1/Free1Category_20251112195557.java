package com.files.codes.model.free1;

import com.google.gson.annotations.SerializedName;
import java.io.Serializable;

/**
 * Model cho Category trong Free1 API
 */
public class Free1Category implements Serializable {
    
    @SerializedName("id")
    private String id;
    
    @SerializedName("name")
    private String name;
    
    @SerializedName("slug")
    private String slug;
    
    // Constructor
    public Free1Category() {}
    
    public Free1Category(String id, String name, String slug) {
        this.id = id;
        this.name = name;
        this.slug = slug;
    }
    
    // Getters and Setters
    public String getId() {
        return id;
    }
    
    public void setId(String id) {
        this.id = id;
    }
    
    public String getName() {
        return name;
    }
    
    public void setName(String name) {
        this.name = name;
    }
    
    public String getSlug() {
        return slug;
    }
    
    public void setSlug(String slug) {
        this.slug = slug;
    }
}