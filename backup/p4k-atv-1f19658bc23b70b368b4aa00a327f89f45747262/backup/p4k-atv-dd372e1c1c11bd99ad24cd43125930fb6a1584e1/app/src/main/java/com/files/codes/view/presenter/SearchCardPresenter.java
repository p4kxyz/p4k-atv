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

import com.files.codes.model.SearchContent;
import com.files.codes.model.Movie;
import com.files.codes.model.TvModel;
import com.files.codes.R;

import com.squareup.picasso.Picasso;
import com.squareup.picasso.Target;

public class SearchCardPresenter extends Presenter {

    private static int CARD_WIDTH = 340;
    private static int CARD_HEIGHT = 510;

    private static Context mContext;

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent) {
        Log.d("onCreateViewHolder", "creating viewholder");
        mContext = parent.getContext();
        ImageCardView cardView = new ImageCardView(mContext);
        cardView.setFocusable(true);
        cardView.setFocusableInTouchMode(true);
        cardView.requestLayout();
        cardView.setInfoVisibility(View.VISIBLE);
        return new ViewHolder(cardView);
    }

    @Override
    public void onBindViewHolder(Presenter.ViewHolder viewHolder, Object item) {
        ((ViewHolder) viewHolder).mCardView.setMainImageDimensions(CARD_WIDTH, CARD_HEIGHT);
        
        // Handle both Movie and SearchContent objects
        if (item instanceof Movie) {
            Movie movie = (Movie) item;
            ((ViewHolder) viewHolder).mCardView.setTitleText(movie.getTitle());
            
            // Display type and year
            String typeLabel = movie.getIsTvseries().equals("1") ? "TV Series" : "Movie";
            String releaseYear = movie.getRelease();
            String contentText = typeLabel;
            if (releaseYear != null && !releaseYear.isEmpty()) {
                contentText = typeLabel + " • " + releaseYear;
            }
            ((ViewHolder) viewHolder).mCardView.setContentText(contentText);
            
            ((ViewHolder) viewHolder).updateCardViewImage(movie.getThumbnailUrl());
        } else if (item instanceof TvModel) {
            TvModel tv = (TvModel) item;
            ((ViewHolder) viewHolder).mCardView.setTitleText(tv.getTvName());
            ((ViewHolder) viewHolder).mCardView.setContentText("Live TV");
            ((ViewHolder) viewHolder).updateCardViewImage(tv.getThumbnailUrl());
        } else if (item instanceof SearchContent) {
            SearchContent content = (SearchContent) item;
            ((ViewHolder) viewHolder).mCardView.setTitleText(content.getTitle());
            
            // Display type and year
            String type = content.getType();
            String typeLabel = "";
            if (type.equalsIgnoreCase("tvseries")) {
                typeLabel = "TV Series";
            } else if (type.equalsIgnoreCase("movie")) {
                typeLabel = "Movie";
            }
            
            String releaseYear = content.getReleaseYear();
            String contentText = typeLabel;
            if (releaseYear != null && !releaseYear.isEmpty()) {
                contentText = typeLabel + " • " + releaseYear;
            }
            ((ViewHolder) viewHolder).mCardView.setContentText(contentText);
            
            ((ViewHolder) viewHolder).updateCardViewImage(content.getThumbnailUrl());
        }
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

            Picasso.get()
                    .load(url)
                    .placeholder(R.drawable.poster_placeholder)
                    .error(mDefaultCardImage)
                    .into(mCardView.getMainImageView());

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
