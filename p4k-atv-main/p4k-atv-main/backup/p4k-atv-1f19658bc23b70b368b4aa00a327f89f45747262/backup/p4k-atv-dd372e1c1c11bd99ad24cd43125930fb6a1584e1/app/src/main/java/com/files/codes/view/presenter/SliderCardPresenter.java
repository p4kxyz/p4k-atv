package com.files.codes.view.presenter;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;

import androidx.leanback.widget.ImageCardView;
import androidx.leanback.widget.Presenter;

import com.files.codes.R;
import com.files.codes.model.VideoContent;
import com.squareup.picasso.Picasso;
import com.squareup.picasso.Target;

public class SliderCardPresenter extends Presenter {
    private static int CARD_WIDTH = 1000;
    private static int CARD_HEIGHT = 431;

    private static Context mContext;

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent) {
        Log.d("onCreateViewHolder", "creating viewholder");
        mContext = parent.getContext();
        ImageCardView cardView = new ImageCardView(mContext);
        cardView.setFocusable(true);
        cardView.setFocusableInTouchMode(true);
        cardView.requestLayout();
        //((TextView)cardView.findViewById(R.id.content_text)).setTextColor(Color.LTGRAY);
        return new ViewHolder(cardView);
    }

    @Override
    public void onBindViewHolder(Presenter.ViewHolder viewHolder, Object item) {
        VideoContent video;
        video = (VideoContent) item;
        
        // Show title and description for slider
        ((ViewHolder) viewHolder).mCardView.setTitleText(video.getTitle());
        if (video.getDescription() != null && !video.getDescription().isEmpty()) {
            // Truncate description if too long
            String description = video.getDescription();
            if (description.length() > 100) {
                description = description.substring(0, 100) + "...";
            }
            ((ViewHolder) viewHolder).mCardView.setContentText(description);
        }
        
        ((ViewHolder) viewHolder).mCardView.setMainImageDimensions(CARD_WIDTH, CARD_HEIGHT);
        ((ViewHolder) viewHolder).mCardView.setInfoVisibility(View.VISIBLE); // Show info
        ((ViewHolder) viewHolder).updateCardViewImage(video.getPosterUrl());
    }

    @Override
    public void onUnbindViewHolder(Presenter.ViewHolder viewHolder) {

    }


    static class ViewHolder extends Presenter.ViewHolder {

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
