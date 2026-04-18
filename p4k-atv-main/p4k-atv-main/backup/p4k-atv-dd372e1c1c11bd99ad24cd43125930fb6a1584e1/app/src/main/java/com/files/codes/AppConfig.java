package com.files.codes;

public class AppConfig {
    static {
        System.loadLibrary("api_config");
    }

    public static native String getApiServerUrl();
    public static native String getApiKey();
    public static native String getPurchaseCode();

    public static final String API_SERVER_URL = getApiServerUrl();
    public static final String API_KEY = getApiKey();
    public static final String PURCHASE_CODE = getPurchaseCode();


    //copy your terms url from php admin dashboard & paste below
    public static final String TERMS_URL = "https://api.phim4k.lol/v120/api/v100/";

    // First, you have to configure firebase to enable phone and google login

    //Phone authentication
    public static final boolean ENABLE_PHONE_LOGIN = true;

    //Google authentication
    public static final boolean ENABLE_GOOGLE_LOGIN = true;
}
