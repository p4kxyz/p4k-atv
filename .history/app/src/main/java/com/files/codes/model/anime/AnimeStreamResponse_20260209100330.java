package com.files.codes.model.anime;

import com.google.gson.annotations.SerializedName;
import java.util.Map;

public class AnimeStreamResponse {
    @SerializedName("server")
    private String server;
    @SerializedName("type")
    private String type;
    @SerializedName("corsProxyRequired")
    private boolean corsProxyRequired;
    @SerializedName("proxyHeaders")
    private Map<String, String> proxyHeaders;
    @SerializedName("url")
    private String url;

    public String getServer() { return server; }
    public String getType() { return type; }
    public boolean isCorsProxyRequired() { return corsProxyRequired; }
    public Map<String, String> getProxyHeaders() { return proxyHeaders; }
    public String getUrl() { return url; }

    /** Check if stream is playable in ExoPlayer (HLS or DIRECT) */
    public boolean isPlayable() {
        return "HLS".equals(type) || "DIRECT".equals(type);
    }
}
