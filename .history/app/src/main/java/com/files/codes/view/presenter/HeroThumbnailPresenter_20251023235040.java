package com.files.codes.view.presenter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.leanback.widget.Presenter;

import com.files.codes.R;
import com.files.codes.model.VideoContent;
import com.squareup.picasso.Picasso;

/**
 * Presenter for small thumbnail cards in hero banner
 */
public class HeroThumbnailPresenter extends Presenter {
    
    private OnThumbnailClickListener clickListener;
    private boolean isWatchHistoryCategory = false;
    
    public interface OnThumbnailClickListener {
        void onThumbnailClicked(VideoContent video, boolean isFromWatchHistory);
    }
    
    public void setOnThumbnailClickListener(OnThumbnailClickListener listener) {
        this.clickListener = listener;
    }
    
    public void setWatchHistoryCategory(boolean isWatchHistory) {
        this.isWatchHistoryCategory = isWatchHistory;
    }
    
    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent) {
        Context context = parent.getContext();
        View view = LayoutInflater.from(context).inflate(R.layout.item_hero_thumbnail, parent, false);
        return new ThumbnailViewHolder(view);
    }

    @Override
    public void onBindViewHolder(ViewHolder viewHolder, Object item) {
        if (!(item instanceof VideoContent)) {
            return;
        }
        
        ThumbnailViewHolder holder = (ThumbnailViewHolder) viewHolder;
        VideoContent video = (VideoContent) item;
        
        // Debug logging
        android.util.Log.d("HeroThumbnailPresenter", "📊 Binding video: " + video.getTitle());
        android.util.Log.d("HeroThumbnailPresenter", "   IMDb Rating: " + video.getImdbRating());
        android.util.Log.d("HeroThumbnailPresenter", "   Release: " + video.getRelease());
        
        // Set title with IMDb rating
        String title = video.getTitle();
        if (title != null) {
            // Extract Vietnamese title (before year)
            String displayTitle = title;
            if (title.contains("(")) {
                displayTitle = title.substring(0, title.indexOf("(")).trim();
            }
            
            // Add IMDb rating if available
            String imdbRating = video.getImdbRating();
            android.util.Log.d("HeroThumbnailPresenter", "   Processing IMDb: " + imdbRating);
            
            if (imdbRating != null && !imdbRating.isEmpty() && !imdbRating.equals("0")) {
                displayTitle = displayTitle + "\n⭐ " + imdbRating;
                android.util.Log.d("HeroThumbnailPresenter", "   ✅ Added IMDb to title: " + displayTitle);
            } else {
                android.util.Log.d("HeroThumbnailPresenter", "   ❌ No valid IMDb rating");
            }
            
            holder.titleText.setText(displayTitle);
            android.util.Log.d("HeroThumbnailPresenter", "   Final display: " + displayTitle);
        }
        
        // Load thumbnail with Cloudflare CDN optimization
        String thumbnailUrl = video.getThumbnailUrl();
        if (thumbnailUrl == null || thumbnailUrl.isEmpty()) {
            thumbnailUrl = video.getPosterUrl();
        }
        
        if (thumbnailUrl != null && !thumbnailUrl.isEmpty()) {
            // Apply Cloudflare CDN optimization for thumbnails (120x180dp)
            String optimizedUrl = getOptimizedThumbnailUrl(thumbnailUrl);
            
            Picasso.get()
                .load(optimizedUrl)
                .placeholder(R.drawable.logo)
                .error(R.drawable.logo)
                .into(holder.thumbnailImage);
        } else {
            holder.thumbnailImage.setImageResource(R.drawable.logo);
        }
        
        // Focus effect on entire card
        holder.view.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View v, boolean hasFocus) {
                float scale = hasFocus ? 1.1f : 1.0f;
                v.animate()
                    .scaleX(scale)
                    .scaleY(scale)
                    .setDuration(150)
                    .start();
            }
        });
        
        // Click listener
        holder.view.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (clickListener != null) {
                    clickListener.onThumbnailClicked(video, isWatchHistoryCategory);
                }
            }
        });
    }

    /**
     * Apply Cloudflare CDN optimization to thumbnail URL
     * Fixed size: 120x180dp (2:3 ratio)
     */
    private String getOptimizedThumbnailUrl(String imageUrl) {
        try {
            java.net.URL urlObj = new java.net.URL(imageUrl);
            String domain = urlObj.getProtocol() + "://" + urlObj.getHost();
            String path = urlObj.getPath();
            if (urlObj.getQuery() != null) {
                path += "?" + urlObj.getQuery();
            }
            
            // Thumbnail dimensions: 120x180dp (2:3 ratio)
            int thumbnailWidth = 120;
            int thumbnailHeight = 180;
            
            return domain + "/cdn-cgi/image/width=" + thumbnailWidth 
                         + ",height=" + thumbnailHeight
                         + ",fit=cover,quality=90,format=auto" + path;
        } catch (Exception e) {
            android.util.Log.e("HeroThumbnailPresenter", "Failed to optimize URL: " + e.getMessage());
            return imageUrl; // Return original URL on error
        }
    }
    
    @Override
    public void onUnbindViewHolder(ViewHolder viewHolder) {
        ThumbnailViewHolder holder = (ThumbnailViewHolder) viewHolder;
        // Don't cancel Picasso request - let it finish and cache for reuse
        holder.thumbnailImage.setImageDrawable(null);
        holder.view.setOnFocusChangeListener(null);
        holder.view.setOnClickListener(null);
    }
    
    static class ThumbnailViewHolder extends ViewHolder {
        ImageView thumbnailImage;
        TextView titleText;
        
        public ThumbnailViewHolder(View view) {
            super(view);
            thumbnailImage = view.findViewById(R.id.thumbnail_image);
            titleText = view.findViewById(R.id.thumbnail_title);
        }
    }
}
