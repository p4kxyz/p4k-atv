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
            // Apply Cloudflare CDN optimization for thumbnails
            String optimizedUrl = getOptimizedThumbnailUrl(thumbnailUrl);
            
            // Convert dp to px for proper sizing (90dp → 270px on xxhdpi)
            android.content.Context context = holder.view.getContext();
            float density = context.getResources().getDisplayMetrics().density;
            int widthPx = (int) (90 * density);
            int heightPx = (int) (135 * density);
            
            android.util.Log.d("HeroThumbnailPresenter", "Thumbnail size: " + widthPx + "x" + heightPx + "px (density: " + density + "x)");
            
            Picasso.get()
                .load(optimizedUrl)
                .noFade()
                .resize(widthPx, heightPx)
                .onlyScaleDown()
                .config(android.graphics.Bitmap.Config.ARGB_8888)
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
     * Size: 270x405px for xxhdpi (90dp * 3 = 270px, 135dp * 3 = 405px)
     */
    private String getOptimizedThumbnailUrl(String imageUrl) {
        try {
            java.net.URL urlObj = new java.net.URL(imageUrl);
            String domain = urlObj.getProtocol() + "://" + urlObj.getHost();
            String path = urlObj.getPath();
            if (urlObj.getQuery() != null) {
                path += "?" + urlObj.getQuery();
            }
            
            // Thumbnail dimensions for xxhdpi (3x): 90dp → 270px, 135dp → 405px
            int thumbnailWidth = 270;
            int thumbnailHeight = 405;
            
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
