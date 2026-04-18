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
        
        // Load thumbnail with Cloudflare CDN optimization
        String thumbnailUrl = video.getThumbnailUrl();
        if (thumbnailUrl == null || thumbnailUrl.isEmpty()) {
            thumbnailUrl = video.getPosterUrl();
        }
        
        if (thumbnailUrl != null && !thumbnailUrl.isEmpty()) {
            // Apply Cloudflare CDN optimization for thumbnails (120x180dp)
            String optimizedUrl = getOptimizedThumbnailUrl(thumbnailUrl);
            
            // Log URL để debug
            android.util.Log.d("HeroThumbnailPresenter", "Loading thumbnail: " + optimizedUrl);
            
            Picasso.get()
                .load(optimizedUrl)
                .noFade()                    // Tắt fade animation
                .resize(120, 180)            // Resize chính xác 120x180px
                .onlyScaleDown()             // Chỉ scale down nếu ảnh lớn hơn
                .config(android.graphics.Bitmap.Config.ARGB_8888)  // Chất lượng cao nhất
                .placeholder(R.drawable.logo)
                .error(R.drawable.logo)
                .into(holder.thumbnailImage);
        } else {
            holder.thumbnailImage.setImageResource(R.drawable.logo);
        }
        
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
     * Fixed size: 120x180dp (exact 2:3 ratio)
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
        
        public ThumbnailViewHolder(View view) {
            super(view);
            thumbnailImage = view.findViewById(R.id.thumbnail_image);
        }
    }
}
