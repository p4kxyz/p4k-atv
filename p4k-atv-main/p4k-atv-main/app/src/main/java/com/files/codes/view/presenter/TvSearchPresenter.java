package com.files.codes.view.presenter;

import android.content.Context;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.cardview.widget.CardView;

import androidx.leanback.widget.Presenter;

import com.files.codes.model.SearchContent;
import com.files.codes.model.TvModel;
import com.files.codes.R;

import com.squareup.picasso.Picasso;

public class TvSearchPresenter extends Presenter {

    private static Context mContext;

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent) {
        mContext = parent.getContext();
        View view = LayoutInflater.from(mContext).inflate(R.layout.item_vertical_card, parent, false);
        return new CustomViewHolder(view);
    }

    @Override
    public void onBindViewHolder(Presenter.ViewHolder viewHolder, Object item) {
        CustomViewHolder holder = (CustomViewHolder) viewHolder;

        if (item instanceof TvModel) {
            holder.bindTv((TvModel) item);
        } else if (item instanceof SearchContent) {
            holder.bindSearchContent((SearchContent) item);
        }
    }

    @Override
    public void onUnbindViewHolder(Presenter.ViewHolder viewHolder) {
        CustomViewHolder holder = (CustomViewHolder) viewHolder;
        holder.mainImage.setImageDrawable(null);
    }

    class CustomViewHolder extends Presenter.ViewHolder {
        public CardView posterCard;
        public ImageView mainImage;
        public TextView tvTitle;
        public TextView tvSubtitle;

        public LinearLayout llDuration;
        public TextView tvDuration;
        public TextView tvQuality;
        public TextView tvYear;
        public TextView tvImdb;
        public View focusOverlay;

        public CustomViewHolder(View view) {
            super(view);
            posterCard = view.findViewById(R.id.poster_card);
            mainImage = view.findViewById(R.id.main_image);
            tvTitle = view.findViewById(R.id.tv_title);
            tvSubtitle = view.findViewById(R.id.tv_subtitle);

            llDuration = view.findViewById(R.id.ll_duration);
            tvDuration = view.findViewById(R.id.tv_duration);
            tvQuality = view.findViewById(R.id.tv_quality);
            tvYear = view.findViewById(R.id.tv_year);
            tvImdb = view.findViewById(R.id.tv_imdb);
            focusOverlay = view.findViewById(R.id.focus_overlay);

            view.addOnAttachStateChangeListener(new View.OnAttachStateChangeListener() {
                @Override
                public void onViewAttachedToWindow(View v) {
                    if (v.getParent() instanceof View) {
                        ((View) v.getParent()).setBackgroundResource(0);
                        if (v.getParent().getClass().getName().contains("ShadowOverlayContainer")) {
                            v.getParent().requestLayout();
                        }
                    }
                }
                @Override
                public void onViewDetachedFromWindow(View v) {}
            });

            view.setOnFocusChangeListener(new View.OnFocusChangeListener() {
                @Override
                public void onFocusChange(View v, boolean hasFocus) {
                    if (hasFocus) {
                        focusOverlay.setVisibility(View.VISIBLE);
                        v.animate().scaleX(1.08f).scaleY(1.08f).setDuration(150).start();
                    } else {
                        focusOverlay.setVisibility(View.INVISIBLE);
                        v.animate().scaleX(1.0f).scaleY(1.0f).setDuration(150).start();
                    }
                }
            });
        }

        public void bindSearchContent(SearchContent vc) {
            setupTitles(vc.getTitle());
            hideAllTags();
            loadImage(vc.getThumbnailUrl());
        }

        public void bindTv(TvModel tv) {
            setupTitles(tv.getTvName());
            hideAllTags();
            loadImage(tv.getThumbnailUrl());
        }

        private void setupTitles(String fullTitle) {
            if (fullTitle == null) fullTitle = "";
            fullTitle = fullTitle.trim();

            tvTitle.setText(fullTitle);
            tvSubtitle.setVisibility(View.GONE);
        }

        private void hideAllTags() {
            llDuration.setVisibility(View.GONE);
        }

        private void loadImage(String imageUrl) {
            if (imageUrl != null && !imageUrl.isEmpty()) {
                Picasso.get()
                        .load(imageUrl)
                        .placeholder(R.drawable.poster_placeholder)
                        .error(R.drawable.logo)
                        .into(mainImage);
            } else {
                mainImage.setImageResource(R.drawable.logo);
            }
        }
    }
}
