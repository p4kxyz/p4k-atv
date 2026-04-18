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

public class VerticalCardPresenter extends Presenter {

    private String type;
    private static Context mContext;

    public VerticalCardPresenter(String type) {
        this.type = type;
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent) {
        mContext = parent.getContext();
        View view = LayoutInflater.from(mContext).inflate(R.layout.item_vertical_card, parent, false);
        return new CustomViewHolder(view);
    }

    @Override
    public void onBindViewHolder(Presenter.ViewHolder viewHolder, Object item) {
        CustomViewHolder holder = (CustomViewHolder) viewHolder;

        if (type.equals(TvSeriesFragment.TV_SERIES) || type.equals(MoviesFragment.MOVIE) || type.equals(FavouriteFragment.FAVORITE)) {
            Movie movie = (Movie) item;
            holder.bindMovie(movie);
        } else if (type.equals(WatchHistoryPageFragment.WATCH_HISTORY)) {
            VideoContent vc = (VideoContent) item;
            holder.bindVideoContent(vc);
        } else if (type.equals(GenreFragment.GENRE)) {
            Genre genre = (Genre) item;
            holder.bindGenreOrCountry(genre.getName(), genre.getImageUrl());
        } else if (type.equals(CountryFragment.COUNTRY)) {
            CountryModel countryModel = (CountryModel) item;
            holder.bindGenreOrCountry(countryModel.getName(), countryModel.getImageUrl());
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

            // Tắt hoàn toàn hiệu ứng dimming của Leanback
            view.addOnAttachStateChangeListener(new View.OnAttachStateChangeListener() {
                @Override
                public void onViewAttachedToWindow(View v) {
                    // Disable shadow overlay completely
                    View parent = (View) v.getParent();
                    if (parent != null) {
                        parent.setBackgroundColor(android.graphics.Color.TRANSPARENT);
                        parent.setAlpha(1.0f);
                        
                        // Kill ShadowOverlayContainer effects
                        if (parent.getClass().getName().contains("ShadowOverlayContainer")) {
                            try {
                                // Force transparent background
                                parent.setBackground(null);
                                android.view.ViewGroup.LayoutParams lp = parent.getLayoutParams();
                                if (lp != null) parent.setLayoutParams(lp);
                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                        }
                    }
                    // Ensure card itself is always fully opaque
                    v.setAlpha(1.0f);
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
                    // Always keep unfocused cards fully visible (alpha = 1.0)
                    v.setAlpha(1.0f);
                }
            });
        }

        public void bindMovie(Movie movie) {
            setupTitles(movie.getTitle());
            setupTags(movie.getRuntime(), movie.getVideoQuality(), movie.getRelease(), movie.getImdbRating());
            loadImage(movie.getThumbnailUrl(), movie.getPosterUrl());
        }

        public void bindVideoContent(VideoContent vc) {
            setupTitles(vc.getTitle());
            setupTags(vc.getRuntime(), vc.getVideoQuality(), vc.getRelease(), vc.getImdbRating());
            loadImage(vc.getThumbnailUrl(), vc.getPosterUrl());
        }
        
        public void bindGenreOrCountry(String name, String imageUrl) {
            tvTitle.setText(name);
            tvSubtitle.setVisibility(View.GONE);
            hideAllTags();
            loadImage(imageUrl, null);
        }

        private void setupTitles(String fullTitle) {
            if (fullTitle == null) fullTitle = "";
            fullTitle = fullTitle.trim();

            // Try to match API format: "Tên Việt (năm) tên gốc"
            // Example: "Cuồng Nộ Tập Kích (2026) Furious Attack"
            java.util.regex.Pattern pattern = java.util.regex.Pattern.compile("^(.*?)\\s*\\(\\d{4}\\)\\s*(.*)$");
            java.util.regex.Matcher matcher = pattern.matcher(fullTitle);

            if (matcher.find()) {
                String vietName = matcher.group(1).trim();
                String originalName = matcher.group(2).trim();

                tvTitle.setText(vietName);
                tvTitle.setMaxLines(1);

                if (!originalName.isEmpty()) {
                    tvSubtitle.setText(originalName);
                    tvSubtitle.setVisibility(View.VISIBLE);
                } else {
                    tvSubtitle.setText(" "); // Dùng khoảng trắng để giữ nguyên độ cao text
                    tvSubtitle.setVisibility(View.INVISIBLE); // Ẩn nhưng vẫn chiếm không gian
                }
            } else if (fullTitle.contains(" - ")) {
                // Fallback for "Tên Việt - Tên gốc" format
                String[] parts = fullTitle.split(" - ", 2);
                tvTitle.setText(parts[0].trim());
                tvTitle.setMaxLines(1);
                if (parts.length > 1 && !parts[1].trim().isEmpty()) {
                    tvSubtitle.setText(parts[1].trim());
                    tvSubtitle.setVisibility(View.VISIBLE);
                } else {
                    tvSubtitle.setText(" ");
                    tvSubtitle.setVisibility(View.INVISIBLE);
                }
            } else {
                tvTitle.setText(fullTitle);
                tvTitle.setMaxLines(1);
                tvSubtitle.setText(" ");
                tvSubtitle.setVisibility(View.INVISIBLE);
            }
        }

        private void setupTags(String runtime, String quality, String release, String imdb) {
            if (runtime != null && !runtime.isEmpty() && !runtime.equals("0")) {
                llDuration.setVisibility(View.VISIBLE);
                tvDuration.setText(runtime);
            } else {
                llDuration.setVisibility(View.GONE);
            }

            if (quality != null && !quality.isEmpty()) {
                tvQuality.setVisibility(View.VISIBLE);
                tvQuality.setText(quality);
                tvQuality.setTextColor(Color.WHITE); // White text for better contrast
                
                String qLower = quality.toLowerCase();
                if (qLower.contains("4k") || qLower.contains("uhd") || qLower.contains("2160")) {
                    // 4K / UHD - Yellow
                    tvQuality.setBackgroundResource(R.drawable.bg_tag_yellow);
                } else if (qLower.contains("full hd") || qLower.contains("fullhd") || qLower.contains("1080")) {
                    // Full HD / 1080p - Blue
                    tvQuality.setBackgroundResource(R.drawable.bg_tag_blue);
                } else if (qLower.contains("hd") || qLower.contains("720")) {
                    // HD / 720p - Green
                    tvQuality.setBackgroundResource(R.drawable.bg_tag_green);
                } else if (qLower.contains("sd") || qLower.contains("480") || qLower.contains("360")) {
                    // SD / Low quality - Gray
                    tvQuality.setBackgroundResource(R.drawable.bg_tag_gray);
                } else {
                    // Unknown quality - Gray
                    tvQuality.setBackgroundResource(R.drawable.bg_tag_gray);
                }
            } else {
                tvQuality.setVisibility(View.GONE);
            }

            if (release != null && !release.isEmpty()) {
                tvYear.setVisibility(View.VISIBLE);
                tvYear.setText(release);
            } else {
                tvYear.setVisibility(View.GONE);
            }

            if (imdb != null && !imdb.isEmpty() && !imdb.equals("0")) {
                tvImdb.setVisibility(View.VISIBLE);
                tvImdb.setText("IMDb " + imdb);
            } else {
                tvImdb.setVisibility(View.GONE);
            }
        }
        
        private void hideAllTags() {
            llDuration.setVisibility(View.GONE);
            tvQuality.setVisibility(View.GONE);
            tvYear.setVisibility(View.GONE);
            tvImdb.setVisibility(View.GONE);
        }

        private void loadImage(String primaryUrl, String fallbackUrl) {
            String url = (primaryUrl != null && !primaryUrl.trim().isEmpty()) ? primaryUrl : fallbackUrl;
            if (url == null || url.trim().isEmpty()) {
                mainImage.setImageResource(R.drawable.logo);
            } else {
                Picasso.get().load(url).placeholder(R.drawable.logo).error(R.drawable.logo).fit().centerCrop().into(mainImage);
            }
        }
    }
}
