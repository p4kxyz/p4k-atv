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
import com.files.codes.model.CountryModel;
import com.files.codes.model.Genre;
import com.files.codes.model.Movie;
import com.files.codes.model.VideoContent;
import com.files.codes.view.fragments.CountryFragment;
import com.files.codes.view.fragments.FavouriteFragment;
import com.files.codes.view.fragments.GenreFragment;
import com.files.codes.view.fragments.MoviesFragment;
import com.files.codes.view.fragments.TvSeriesFragment;
import com.files.codes.view.fragments.WatchHistoryPageFragment;
import com.squareup.picasso.Picasso;
import com.squareup.picasso.Target;

public class VerticalCardPresenter extends Presenter {

    private static int CARD_WIDTH = 340;  // Tăng lên để phù hợp 5 cột
    private static int CARD_HEIGHT = 510; // Tăng lên để giữ tỷ lệ
    private String type;
    private static Context mContext;

    public VerticalCardPresenter(String type) {
        this.type = type;
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent) {
        Log.d("onCreateViewHolder", "creating viewholder");
        mContext = parent.getContext();
        ImageCardView cardView = new ImageCardView(mContext);
        cardView.setFocusable(true);
        cardView.setFocusableInTouchMode(true);
        cardView.requestLayout();
        cardView.setInfoVisibility(View.VISIBLE); // Show info
        
        return new ViewHolder(cardView);
    }

    @Override
    public void onBindViewHolder(Presenter.ViewHolder viewHolder, Object item) {
        ImageCardView cardView = ((ViewHolder) viewHolder).mCardView;
        
        if (type.equals(TvSeriesFragment.TV_SERIES)) {
            Movie movie = (Movie) item;
            cardView.setMainImageDimensions(CARD_WIDTH, CARD_HEIGHT);
            cardView.setTitleText(movie.getTitle());
            if (movie.getRelease() != null && !movie.getRelease().isEmpty()) {
                cardView.setContentText(movie.getRelease());
            }
            ((ViewHolder) viewHolder).updateCardViewImage(movie.getThumbnailUrl());
        } else if (type.equals(MoviesFragment.MOVIE)) {
            Movie movie = (Movie) item;
            cardView.setMainImageDimensions(CARD_WIDTH, CARD_HEIGHT);
            cardView.setTitleText(movie.getTitle());
            if (movie.getRelease() != null && !movie.getRelease().isEmpty()) {
                cardView.setContentText(movie.getRelease());
            }
            ((ViewHolder) viewHolder).updateCardViewImage(movie.getThumbnailUrl());
        } else if (type.equals(FavouriteFragment.FAVORITE)) {
            Movie movie = (Movie) item;
            cardView.setMainImageDimensions(CARD_WIDTH, CARD_HEIGHT);
            cardView.setTitleText(movie.getTitle());
            if (movie.getRelease() != null && !movie.getRelease().isEmpty()) {
                cardView.setContentText(movie.getRelease());
            }
            ((ViewHolder) viewHolder).updateCardViewImage(movie.getThumbnailUrl());
        } else if (type.equals(WatchHistoryPageFragment.WATCH_HISTORY)) {
            VideoContent vc = (VideoContent) item;
            cardView.setMainImageDimensions(CARD_WIDTH, CARD_HEIGHT);
            cardView.setTitleText(vc.getTitle());
            String imageUrl = vc.getPosterUrl();
            if (imageUrl == null || imageUrl.isEmpty()) {
                imageUrl = vc.getThumbnailUrl();
            }
            ((ViewHolder) viewHolder).updateCardViewImage(imageUrl);
        } else if (type.equals(GenreFragment.GENRE)) {
            Genre genre = (Genre) item;
            cardView.setMainImageDimensions(200, 200);
            cardView.setTitleText(genre.getName());
            ((ViewHolder) viewHolder).updateCardViewImage(genre.getImageUrl());
        } else if (type.equals(CountryFragment.COUNTRY)) {
            CountryModel countryModel = (CountryModel) item;
            cardView.setMainImageDimensions(CARD_WIDTH, CARD_HEIGHT);
            cardView.setTitleText(countryModel.getName());
            ((ViewHolder) viewHolder).updateCardViewImage(countryModel.getImageUrl());
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
            
            // Add focus listener for white border effect
            view.setOnFocusChangeListener(new View.OnFocusChangeListener() {
                @Override
                public void onFocusChange(View v, boolean hasFocus) {
                    // Find the actual card view - might be wrapped in ShadowOverlayContainer
                    ImageCardView cardView = findImageCardView(v);
                    if (cardView != null) {
                        if (hasFocus) {
                            // White border on focus
                            cardView.setBackgroundResource(R.drawable.card_focus_border);
                            cardView.setElevation(12f);
                            v.animate().scaleX(1.08f).scaleY(1.08f).setDuration(150).start();
                        } else {
                            // Remove border when not focused
                            cardView.setBackground(null);
                            cardView.setElevation(0f);
                            v.animate().scaleX(1.0f).scaleY(1.0f).setDuration(150).start();
                        }
                    }
                }
            });
        }
        
        private ImageCardView findImageCardView(View view) {
            if (view instanceof ImageCardView) {
                return (ImageCardView) view;
            }
            if (view instanceof ViewGroup) {
                ViewGroup group = (ViewGroup) view;
                for (int i = 0; i < group.getChildCount(); i++) {
                    View child = group.getChildAt(i);
                    if (child instanceof ImageCardView) {
                        return (ImageCardView) child;
                    }
                    ImageCardView result = findImageCardView(child);
                    if (result != null) return result;
                }
            }
            return null;
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

