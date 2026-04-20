package com.files.codes;

import android.app.Application;
import android.content.Context;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;

import com.squareup.picasso.LruCache;
import com.squareup.picasso.OkHttp3Downloader;
import com.squareup.picasso.Picasso;
import com.files.codes.utils.AdsRemoteConfigService;

import java.io.File;
import java.util.Locale;

public class MyApplication extends Application {
    
    private static final String TAG = "MyApplication";
    // Disk cache limit for image thumbnails (20MB is enough for TV browsing)
    private static final long PICASSO_DISK_CACHE_MB = 20 * 1024 * 1024L;
    // Auto-clean app cache if total size exceeds this threshold
    private static final long CACHE_CLEANUP_THRESHOLD_MB = 50 * 1024 * 1024L;

    @Override
    public void onCreate() {
        super.onCreate();
        AppConfig.clearRuntimeApiConfig();
        AdsRemoteConfigService.refreshAndWait(5000);
        AdsRemoteConfigService.refreshInBackground();
        registerForegroundRefresh();
        setVietnameseLocale();
        configurePicasso();
        cleanupCacheIfNeeded();
    }

    private void registerForegroundRefresh() {
        registerActivityLifecycleCallbacks(new ActivityLifecycleCallbacks() {
            private int startedCount = 0;

            @Override
            public void onActivityStarted(android.app.Activity activity) {
                boolean enteringForeground = startedCount == 0;
                startedCount++;
                if (enteringForeground) {
                    AdsRemoteConfigService.refreshInBackground();
                }
            }

            @Override
            public void onActivityStopped(android.app.Activity activity) {
                if (startedCount > 0) {
                    startedCount--;
                }
            }

            @Override
            public void onActivityCreated(android.app.Activity activity, Bundle savedInstanceState) { }

            @Override
            public void onActivityResumed(android.app.Activity activity) { }

            @Override
            public void onActivityPaused(android.app.Activity activity) { }

            @Override
            public void onActivitySaveInstanceState(android.app.Activity activity, Bundle outState) { }

            @Override
            public void onActivityDestroyed(android.app.Activity activity) { }
        });
    }
    
    private void configurePicasso() {
        Picasso.Builder builder = new Picasso.Builder(this);
        // Memory cache: 15MB (enough for visible thumbnails on screen)
        builder.memoryCache(new LruCache(15 * 1024 * 1024));
        // Disk cache: 20MB (reduced from 100MB - thumbnails are small)
        builder.downloader(new OkHttp3Downloader(this, PICASSO_DISK_CACHE_MB));
        Picasso built = builder.build();
        try {
            Picasso.setSingletonInstance(built);
        } catch (IllegalStateException ignored) {
            // Already set
        }
    }

    /**
     * Cleans app cache dir if total size exceeds threshold.
     * Runs in background so it doesn't block startup.
     */
    private void cleanupCacheIfNeeded() {
        new Thread(() -> {
            try {
                File cacheDir = getCacheDir();
                long cacheSize = getFolderSize(cacheDir);
                Log.d(TAG, "App cache size: " + (cacheSize / 1024 / 1024) + " MB");
                if (cacheSize > CACHE_CLEANUP_THRESHOLD_MB) {
                    Log.d(TAG, "Cache exceeds " + (CACHE_CLEANUP_THRESHOLD_MB / 1024 / 1024) + "MB, trimming...");
                    trimFolder(cacheDir, CACHE_CLEANUP_THRESHOLD_MB / 2);
                    Log.d(TAG, "Cache trimmed to ~" + (getFolderSize(cacheDir) / 1024 / 1024) + " MB");
                }
                // Also clean up old OTA APK files from external storage
                cleanupOtaApks();
            } catch (Exception e) {
                Log.w(TAG, "Cache cleanup error: " + e.getMessage());
            }
        }, "CacheCleanup").start();
    }

    /** Delete all Phim4K_*.apk files in our external download dir */
    private void cleanupOtaApks() {
        try {
            File dlDir = getExternalFilesDir(android.os.Environment.DIRECTORY_DOWNLOADS);
            if (dlDir == null || !dlDir.exists()) return;
            File[] apks = dlDir.listFiles(f -> f.getName().startsWith("Phim4K_") && f.getName().endsWith(".apk"));
            if (apks != null) {
                for (File apk : apks) {
                    if (apk.delete()) Log.d(TAG, "Deleted old OTA APK: " + apk.getName());
                }
            }
        } catch (Exception e) {
            Log.w(TAG, "OTA APK cleanup error: " + e.getMessage());
        }
    }

    /** Returns total size of a directory in bytes */
    private long getFolderSize(File dir) {
        if (dir == null || !dir.exists()) return 0;
        long size = 0;
        File[] files = dir.listFiles();
        if (files != null) {
            for (File f : files) {
                size += f.isDirectory() ? getFolderSize(f) : f.length();
            }
        }
        return size;
    }

    /**
     * Deletes oldest files in a directory until total size <= targetBytes.
     * Leaves the newest files untouched.
     */
    private void trimFolder(File dir, long targetBytes) {
        if (dir == null || !dir.exists()) return;
        java.util.List<File> allFiles = new java.util.ArrayList<>();
        collectFiles(dir, allFiles);
        // Sort oldest first
        allFiles.sort((a, b) -> Long.compare(a.lastModified(), b.lastModified()));
        long current = getFolderSize(dir);
        for (File f : allFiles) {
            if (current <= targetBytes) break;
            long sz = f.length();
            if (f.delete()) current -= sz;
        }
    }

    private void collectFiles(File dir, java.util.List<File> out) {
        File[] files = dir.listFiles();
        if (files != null) {
            for (File f : files) {
                if (f.isDirectory()) collectFiles(f, out);
                else out.add(f);
            }
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