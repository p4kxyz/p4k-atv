package com.files.codes.view.presenter;

import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;

import androidx.leanback.widget.ImageCardView;
import androidx.leanback.widget.Presenter;

import com.files.codes.R;
import com.files.codes.database.DatabaseHelper;
import com.files.codes.model.PlaybackModel;
import com.files.codes.model.Video;
import com.files.codes.model.movieDetails.Episode;
import com.files.codes.utils.LoginAlertDialog;
import com.files.codes.utils.PaidDialog;
import com.files.codes.utils.PreferenceUtils;
import com.files.codes.utils.ToastMsg;
import com.files.codes.view.PlayerActivity;
import com.files.codes.view.VideoPlaybackActivity;
import com.squareup.picasso.Picasso;
import com.squareup.picasso.Target;

public class EpisodPresenter extends Presenter {
    private static int CARD_WIDTH = 330;
    private static int CARD_HEIGHT = 180;
    private static Context mContext;

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent) {
        Log.d("onCreateViewHolder", "creating viewholder");
        mContext = parent.getContext();
        ImageCardView cardView = new ImageCardView(mContext);
        cardView.setFocusable(true);
        cardView.setFocusableInTouchMode(true);

        cardView.requestLayout();
        return new ViewHolder(cardView);
    }

    @Override
    public void onBindViewHolder(Presenter.ViewHolder viewHolder, Object item) {
        final Episode video = (Episode) item;
        ((ViewHolder) viewHolder).mCardView.setTitleText(video.getEpisodesName());
        ((ViewHolder) viewHolder).mCardView.findViewById(R.id.content_text).setVisibility(View.GONE);
        ((ViewHolder) viewHolder).mCardView.setMainImageDimensions(CARD_WIDTH, CARD_HEIGHT);
        ((ViewHolder) viewHolder).updateCardViewImage(video.getImageUrl());
        final DatabaseHelper db = new DatabaseHelper(mContext);

        ((ViewHolder) viewHolder).mCardView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (video.getIsPaid().equals("1")) {
                    if (PreferenceUtils.isLoggedIn(mContext)) {
                        String status= db.getActiveStatusData() != null ? db.getActiveStatusData().getStatus() : "inactive";
                        if (  status.equalsIgnoreCase("active")) {

                            if (video.getFileType() != null && video.getFileType().equalsIgnoreCase("embed")) {
                                new ToastMsg(mContext).toastIconError(mContext.getResources().getString(R.string.embed_not_supported));
                                return;
                            }
                            PlaybackModel model = new PlaybackModel();
                            
                            // Use the TV series main ID for watch history tracking, not episode ID
                            String seriesId = video.getVideosId(); // This should be the main series ID
                            if (seriesId == null || seriesId.isEmpty()) {
                                // Fallback to episode ID if series ID is not available
                                seriesId = video.getEpisodesId();
                            }
                            
                            Log.e("EpisodPresenter", "Setting up TV Series episode: seriesId=" + seriesId + ", episodeId=" + video.getEpisodesId());
                            
                            // Set movieId for watch history (use series ID, not episode ID)
                            model.setMovieId(seriesId);
                            
                            // Handle phim4k episode IDs differently since they're not numeric
                            String episodeId = video.getEpisodesId();
                            if (episodeId != null && !episodeId.matches("\\d+")) {
                                // For non-numeric IDs (like phim4k "tap-1"), use hash code
                                model.setId((long) episodeId.hashCode());
                            } else {
                                // For regular numeric episode IDs
                                try {
                                    model.setId(Long.parseLong(episodeId));
                                } catch (NumberFormatException e) {
                                    Log.e("EpisodPresenter", "Failed to parse episode ID as Long: " + episodeId + ", using hashCode");
                                    model.setId((long) episodeId.hashCode());
                                }
                            }
                            model.setTitle(video.getTvSeriesTitle());
                            model.setDescription("Seasson: " + video.getSeasonName() + "; Episode: " + video.getEpisodesName());
                            model.setVideoType("tvseries"); // Set correct video type for TV series episodes
                            model.setCategory("tvseries");
                            model.setVideoUrl(video.getFileUrl());
                            Video videoModel = new Video();
                            videoModel.setSubtitle(video.getSubtitle());
                            model.setVideo(videoModel);
                            model.setCardImageUrl(video.getCardBackgroundUrl());
                            model.setBgImageUrl(video.getImageUrl());
                            model.setIsPaid(video.getIsPaid());
                            model.setIsTvSeries("1"); // Set isTvSeries for proper watch history saving

                            Intent intent = new Intent(mContext, PlayerActivity.class);
                            intent.putExtra(VideoPlaybackActivity.EXTRA_VIDEO, model);
                            mContext.startActivity(intent);

                        } else {
                            //saved data is not valid, because it was saved more than 2 hours ago
                            PreferenceUtils.updateSubscriptionStatus(mContext);
                            //subscription is not active
                            //new PaidDialog(getActivity()).showPaidContentAlertDialog();
                            PaidDialog dialog = new PaidDialog(mContext);
                            dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
                            dialog.getWindow().setLayout(WindowManager.LayoutParams.MATCH_PARENT, WindowManager.LayoutParams.MATCH_PARENT);
                            dialog.show();
                        }
                    } else {
                        // user is not logged in
                        // show an alert dialog
                        LoginAlertDialog dialog = new LoginAlertDialog(mContext);
                        dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
                        dialog.getWindow().setLayout(WindowManager.LayoutParams.MATCH_PARENT, WindowManager.LayoutParams.MATCH_PARENT);
                        dialog.show();
                    }
                } else {
                    if (video.getFileType() != null && video.getFileType().equalsIgnoreCase("embed")) {
                        new ToastMsg(mContext).toastIconError(mContext.getResources().getString(R.string.embed_not_supported));
                        return;
                    }
                    PlaybackModel model = new PlaybackModel();
                    
                    // Use the TV series main ID for watch history tracking, not episode ID
                    String seriesId = video.getVideosId(); // This should be the main series ID
                    if (seriesId == null || seriesId.isEmpty()) {
                        // Fallback to episode ID if series ID is not available
                        seriesId = video.getEpisodesId();
                    }
                    
                    Log.e("EpisodPresenter", "Setting up TV Series episode: seriesId=" + seriesId + ", episodeId=" + video.getEpisodesId());
                    
                    // Set movieId for watch history (use series ID, not episode ID)
                    model.setMovieId(seriesId);
                    
                    // Handle phim4k episode IDs differently since they're not numeric
                    String episodeId = video.getEpisodesId();
                    if (episodeId != null && !episodeId.matches("\\d+")) {
                        // For non-numeric IDs (like phim4k "tap-1"), use hash code
                        model.setId((long) episodeId.hashCode());
                    } else {
                        // For regular numeric episode IDs
                        try {
                            model.setId(Long.parseLong(episodeId));
                        } catch (NumberFormatException e) {
                            Log.e("EpisodPresenter", "Failed to parse episode ID as Long: " + episodeId + ", using hashCode");
                            model.setId((long) episodeId.hashCode());
                        }
                    }
                    
                    model.setTitle(video.getTvSeriesTitle());
                    model.setDescription("Seasson: " + video.getSeasonName() + "; Episode: " + video.getEpisodesName());
                    model.setVideoType("tvseries"); // Set correct video type for TV series episodes
                    model.setCategory("tvseries");
                    model.setVideoUrl(video.getFileUrl());
                    Video videoModel = new Video();
                    videoModel.setSubtitle(video.getSubtitle());
                    model.setVideo(videoModel);
                    model.setCardImageUrl(video.getCardBackgroundUrl());
                    model.setBgImageUrl(video.getImageUrl());
                    model.setIsPaid(video.getIsPaid());
                    model.setIsTvSeries("1"); // Set isTvSeries for proper watch history saving

                    Intent intent = new Intent(mContext, PlayerActivity.class);
                    intent.putExtra(VideoPlaybackActivity.EXTRA_VIDEO, model);
                    mContext.startActivity(intent);
                }
            }
        });

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
                    .resize(CARD_WIDTH * 2, CARD_HEIGHT * 2)
                    .centerCrop()
                    .error(mDefaultCardImage)
                    .into(mImageCardViewTarget);
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


