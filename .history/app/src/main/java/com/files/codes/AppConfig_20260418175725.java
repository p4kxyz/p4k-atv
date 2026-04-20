package com.files.codes;

public class AppConfig {
    static {
        System.loadLibrary("api_config");
    }

    public static native String getApiServerUrl();
    public static native String getApiKey();
    public static native String getPurchaseCode();

    private static volatile String runtimeApiServerUrl = "";
    private static volatile String runtimeApiKey = "";

    public static volatile String API_SERVER_URL = runtimeApiServerUrl;
    public static volatile String API_KEY = runtimeApiKey;
    public static final String PURCHASE_CODE = getPurchaseCode();

    public static String getCurrentApiServerUrl() {
        return runtimeApiServerUrl;
    }

    public static String getCurrentApiKey() {
        return runtimeApiKey;
    }

    public static synchronized void updateRuntimeApiConfig(String apiUrl, String apiKey) {
        if (apiUrl != null) {
            String normalizedUrl = apiUrl.trim();
            if (!normalizedUrl.isEmpty()) {
                runtimeApiServerUrl = normalizedUrl;
                API_SERVER_URL = normalizedUrl;
            }
        }
        if (apiKey != null) {
            String normalizedKey = apiKey.trim();
            if (!normalizedKey.isEmpty()) {
                runtimeApiKey = normalizedKey;
                API_KEY = normalizedKey;
            }
        }
    }


    //copy your terms url from php admin dashboard & paste below
    public static final String TERMS_URL = "https://api.phim4k.lol/v120/api/v100/";

    // First, you have to configure firebase to enable phone and google login

    //Phone authentication
    public static final boolean ENABLE_PHONE_LOGIN = true;

    //Google authentication
    public static final boolean ENABLE_GOOGLE_LOGIN = true;
}
