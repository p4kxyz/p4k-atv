package com.files.codes.utils;

import android.app.Activity;
import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;
import android.os.Handler;
import android.util.DisplayMetrics;

import androidx.core.content.ContextCompat;
import androidx.leanback.app.BackgroundManager;

import com.files.codes.R;
import com.squareup.picasso.Picasso;
import com.squareup.picasso.Target;

import java.util.Timer;
import java.util.TimerTask;

import jp.wasabeef.picasso.transformations.BlurTransformation;

public class BackgroundHelper {

    private static long BACKGROUND_UPDATE_DELAY = 0;

    private final Handler mHandler = new Handler();

    private Activity mActivity;
    private DisplayMetrics mMetrics;
    private Timer mBackgroundTimer;
    private String mBackgroundURL;

    private BackgroundManager mBackgroundManager;
    private Drawable mDefaultBackground;
    private Target mBackgroundTarget;

    public BackgroundHelper(Activity mActivity) {
        this.mActivity = mActivity;
    }

    public void setBackgroundUrl(String backgroundUrl) {
        this.mBackgroundURL = backgroundUrl;
    }


    static class PicassoBackgroundManagerTarget implements Target {
        BackgroundManager mBackgroundManager;

        public PicassoBackgroundManagerTarget(BackgroundManager backgroundManager) {
            this.mBackgroundManager = backgroundManager;
        }

        @Override
        public void onBitmapLoaded(Bitmap bitmap, Picasso.LoadedFrom loadedFrom) {
            this.mBackgroundManager.setBitmap(bitmap);
        }

        @Override
        public void onBitmapFailed(Exception e, Drawable errorDrawable) {
            this.mBackgroundManager.setDrawable(errorDrawable);
        }


        @Override
        public void onPrepareLoad(Drawable drawable) {
            // Do nothing, default_background manager has its own transitions
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (o == null || getClass() != o.getClass()) {
                return false;
            }

            PicassoBackgroundManagerTarget that = (PicassoBackgroundManagerTarget) o;

            return mBackgroundManager.equals(that.mBackgroundManager);
        }

        @Override
        public int hashCode() {
            return mBackgroundManager.hashCode();
        }
    }


    public void prepareBackgroundManager() {
        mBackgroundManager = BackgroundManager.getInstance(mActivity);

        if (!mBackgroundManager.isAttached()) {
            mBackgroundManager.attach(mActivity.getWindow());
        }

        mBackgroundTarget = new PicassoBackgroundManagerTarget(mBackgroundManager);

        mDefaultBackground = ContextCompat.getDrawable(mActivity, R.drawable.gradient_2);

        mMetrics = new DisplayMetrics();
        mActivity.getWindowManager().getDefaultDisplay().getMetrics(mMetrics);
    }


    public void updateBackground(String url) {
        // Check for null or empty URL
        if (url == null || url.trim().isEmpty()) {
            // Use default background for null or empty URLs
            if (mBackgroundManager != null) {
                mBackgroundManager.setDrawable(mDefaultBackground);
            }
            if (null != mBackgroundTimer) {
                mBackgroundTimer.cancel();
            }
            return;
        }
        
        Picasso.get()
                .load(url)
                .resize(mMetrics.widthPixels, mMetrics.heightPixels)
                .centerCrop()
                .transform(new BlurTransformation(mActivity, 25, 2))
                .error(mDefaultBackground)
                .into(mBackgroundTarget);

        if (null != mBackgroundTimer) {
            mBackgroundTimer.cancel();
        }
    }

    private static class UpdateBackgroundTask extends TimerTask {
        @Override
        public void run() {

        }
    }

    public void startBackgroundTimer(String url) {
        this.mBackgroundURL = url;
        mHandler.post(new Runnable() {
            @Override
            public void run() {
                if (mBackgroundURL != null) {
                    updateBackground(mBackgroundURL);
                }
            }
        });
    }


}
