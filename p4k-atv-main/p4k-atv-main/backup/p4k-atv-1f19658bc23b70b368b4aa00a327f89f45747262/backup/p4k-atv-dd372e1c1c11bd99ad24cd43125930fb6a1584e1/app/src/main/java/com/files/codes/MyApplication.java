package com.files.codes;

import android.app.Application;
import android.content.Context;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.os.Build;
import android.util.DisplayMetrics;

import com.squareup.picasso.LruCache;
import com.squareup.picasso.OkHttp3Downloader;
import com.squareup.picasso.Picasso;

import java.util.Locale;

public class MyApplication extends Application {
    
    @Override
    public void onCreate() {
        super.onCreate();
        setVietnameseLocale();
        configurePicasso();
    }
    
    private void configurePicasso() {
        // Configure Picasso with larger cache for smooth scrolling
        Picasso.Builder builder = new Picasso.Builder(this);
        
        // Set memory cache to 50MB (default is ~15MB)
        builder.memoryCache(new LruCache(50 * 1024 * 1024));
        
        // Set disk cache to 100MB via OkHttp3Downloader
        builder.downloader(new OkHttp3Downloader(this, 100 * 1024 * 1024));
        
        // Enable indicators for debugging (optional - remove in production)
        // builder.indicatorsEnabled(true);
        
        Picasso built = builder.build();
        
        // Enable logging for debugging (optional - remove in production)
        // built.setLoggingEnabled(true);
        
        try {
            Picasso.setSingletonInstance(built);
        } catch (IllegalStateException ignored) {
            // Picasso singleton was already set
        }
    }
    
    private void setVietnameseLocale() {
        // Force tiếng Việt
        Locale vietnamese = new Locale("vi", "VN");
        Locale.setDefault(vietnamese);
        
        // Cập nhật configuration cho app
        Resources resources = getResources();
        Configuration config = resources.getConfiguration();
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            config.setLocale(vietnamese);
        } else {
            config.locale = vietnamese;
        }
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) {
            config.setLayoutDirection(vietnamese);
        }
        
        resources.updateConfiguration(config, resources.getDisplayMetrics());
    }
    
    @Override
    protected void attachBaseContext(Context base) {
        super.attachBaseContext(setLocale(base));
    }
    
    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        setVietnameseLocale();
    }
    
    private Context setLocale(Context context) {
        Locale vietnamese = new Locale("vi", "VN");
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            Configuration configuration = new Configuration(context.getResources().getConfiguration());
            configuration.setLocale(vietnamese);
            return context.createConfigurationContext(configuration);
        } else {
            Configuration configuration = context.getResources().getConfiguration();
            configuration.locale = vietnamese;
            context.getResources().updateConfiguration(configuration, 
                context.getResources().getDisplayMetrics());
            return context;
        }
    }
}