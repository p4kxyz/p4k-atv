package com.files.codes;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;

import androidx.leanback.widget.ImageCardView;
import androidx.leanback.widget.Presenter;

import com.files.codes.model.VideoContent;

import com.squareup.picasso.Picasso;
import com.squareup.picasso.Target;

/*
 * A CardPresenter is used to generate Views and bind Objects to them on demand.
 * It contains an Image CardView
 */
public class CardPresenter extends Presenter {

    private static int CARD_WIDTH = 260;  // Tăng từ 220 lên 260
    private static int CARD_HEIGHT = 390; // Tăng từ 330 lên 390

    @SuppressLint("StaticFieldLeak")
    private static Context mContext;

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent) {
        Log.d("onCreateViewHolder", "creating viewholder");
        mContext = parent.getContext();
        ImageCardView cardView = new ImageCardView(mContext);
        cardView.setFocusable(true);
        cardView.setFocusableInTouchMode(true);
        cardView.requestLayout();
        cardView.setInfoVisibility(View.VISIBLE); // Show title info
        return new ViewHolder(cardView);
    }

    @Override
    public void onBindViewHolder(Presenter.ViewHolder viewHolder, Object item) {
        VideoContent video = (VideoContent) item;
        ImageCardView cardView = ((ViewHolder) viewHolder).mCardView;
        
        // Set card dimensions
        cardView.setMainImageDimensions(CARD_WIDTH, CARD_HEIGHT);
        
        // Set title and content info
        cardView.setTitleText(video.getTitle());
        
        // Build content text: Year + IMDb rating
        StringBuilder contentText = new StringBuilder();
        if (video.getRelease() != null && !video.getRelease().isEmpty()) {
            contentText.append(video.getRelease());
        }
        
        // Log IMDb rating data
        Log.d("CardPresenter", "📊 Video: " + video.getTitle());
        Log.d("CardPresenter", "   Release: " + video.getRelease());
        Log.d("CardPresenter", "   IMDb: " + video.getImdbRating());
        
        if (video.getImdbRating() != null && !video.getImdbRating().isEmpty() && !video.getImdbRating().equals("0")) {
            if (contentText.length() > 0) {
                contentText.append(" • ");
            }
            contentText.append("⭐ ").append(video.getImdbRating());
            Log.d("CardPresenter", "   ✅ Added IMDb to display: " + contentText.toString());
        } else {
            Log.d("CardPresenter", "   ❌ No valid IMDb rating");
        }
        
        if (contentText.length() > 0) {
            cardView.setContentText(contentText.toString());
            Log.d("CardPresenter", "   Final text: " + contentText.toString());
        }
        
        // Load image
        ((ViewHolder) viewHolder).updateCardViewImage(video.getThumbnailUrl());
    }

    @Override
    public void onUnbindViewHolder(Presenter.ViewHolder viewHolder) {

    }


    class ViewHolder extends Presenter.ViewHolder {

        private ImageCardView mCardView;
        private Drawable mDefaultCardImage;
        private PicassoImageCardViewTarget mImageCardViewTarget;

        public ViewHolder(View view) {
            super(view);
            mCardView = (ImageCardView) view;
            mImageCardViewTarget = new PicassoImageCardViewTarget(mCardView);
            mDefaultCardImage = mContext
                    .getResources()
                    .getDrawable(R.drawable.logo);
        }

        public ImageCardView getCardView() {
            return mCardView;
        }

        protected void updateCardViewImage(String url) {
            // Check for null or empty URL
            if (url == null || url.trim().isEmpty()) {
                // Use default placeholder image for null or empty URLs
                mCardView.getMainImageView().setImageResource(R.drawable.logo);
                return;
            }
            Picasso.get().load(url).into(mCardView.getMainImageView());
        }
    }


    static class PicassoImageCardViewTarget implements Target {


        private ImageCardView mImageCardView;

        public PicassoImageCardViewTarget(ImageCardView mImageCardView) {
            this.mImageCardView = mImageCardView;
        }

        @Override
        public void onBitmapLoaded(Bitmap bitmap, Picasso.LoadedFrom from) {
            Drawable bitmapDrawable = new BitmapDrawable(mContext.getResources(), bitmap);
            mImageCardView.setMainImage(bitmapDrawable);
        }

        @Override
        public void onBitmapFailed(Exception e, Drawable errorDrawable) {
            mImageCardView.setMainImage(errorDrawable);
        }


        @Override
        public void onPrepareLoad(Drawable placeHolderDrawable) {

        }
    }

}