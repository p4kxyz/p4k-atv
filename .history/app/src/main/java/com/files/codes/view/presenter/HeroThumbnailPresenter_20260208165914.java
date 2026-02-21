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
        
        // Special handling for clear history button
        if ("clear_button".equals(video.getType()) || 
            (video.getDescription() != null && "CLEAR_HISTORY_BUTTON".equals(video.getDescription()))) {
            
            // Set custom background
            holder.thumbnailImage.setImageResource(R.drawable.clear_history_thumbnail);
            holder.thumbnailImage.setScaleType(ImageView.ScaleType.FIT_XY);
            
            // Add overlay with text and icon
            Context context = holder.view.getContext();
            if (holder.view instanceof ViewGroup) {
                ViewGroup container = (ViewGroup) holder.view;
                
                // Remove any existing overlay
                for (int i = container.getChildCount() - 1; i >= 1; i--) {
                    View child = container.getChildAt(i);
                    if (child.getTag() != null && "clear_overlay".equals(child.getTag())) {
                        container.removeViewAt(i);
                    }
                }
                
                // Create overlay layout
                android.widget.LinearLayout overlay = new android.widget.LinearLayout(context);
                overlay.setOrientation(android.widget.LinearLayout.VERTICAL);
                overlay.setGravity(android.view.Gravity.CENTER);
                overlay.setTag("clear_overlay");
                
                // Create trash icon text
                TextView trashIcon = new TextView(context);
                trashIcon.setText("🗑️");
                trashIcon.setTextSize(android.util.TypedValue.COMPLEX_UNIT_SP, 20);
                trashIcon.setTextColor(0xFFFFFFFF);
                trashIcon.setShadowLayer(2, 1, 1, 0xFF000000);
                trashIcon.setGravity(android.view.Gravity.CENTER);
                
                // Create text
                TextView text = new TextView(context);
                text.setText("Xóa tất cả");
                text.setTextSize(android.util.TypedValue.COMPLEX_UNIT_SP, 10);
                text.setTextColor(0xFFFFFFFF);
                text.setShadowLayer(1, 1, 1, 0xFF000000);
                text.setGravity(android.view.Gravity.CENTER);
                text.setTypeface(null, android.graphics.Typeface.BOLD);
                
                overlay.addView(trashIcon);
                overlay.addView(text);
                
                // Set layout params to fill parent
                android.widget.FrameLayout.LayoutParams overlayParams = 
                    new android.widget.FrameLayout.LayoutParams(
                        android.widget.FrameLayout.LayoutParams.MATCH_PARENT,
                        android.widget.FrameLayout.LayoutParams.MATCH_PARENT);
                overlayParams.gravity = android.view.Gravity.CENTER;
                
                container.addView(overlay, overlayParams);
            }
            
            // Click listener for clear button
            holder.view.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (clickListener != null) {
                        clickListener.onThumbnailClicked(video, isWatchHistoryCategory);
                    }
                }
            });
            return; // Skip normal thumbnail loading
        }
        
        // Load thumbnail with Cloudflare CDN optimization
        String videoId = video.getId();
        String thumbnailUrl;

        // Ưu tiên dùng ảnh ngang 16:9 từ server
        if (videoId != null && !videoId.trim().isEmpty()) {
             thumbnailUrl = "https://api.phim4k.lol/uploads/bg/" + videoId.trim() + ".jpg";
        } else {
             thumbnailUrl = video.getThumbnailUrl();
             if (thumbnailUrl == null || thumbnailUrl.isEmpty()) {
                 thumbnailUrl = video.getPosterUrl();
             }
        }
        
        if (thumbnailUrl != null && !thumbnailUrl.isEmpty()) {
            // Apply Cloudflare CDN optimization for thumbnails
            String optimizedUrl = getOptimizedThumbnailUrl(thumbnailUrl);
            
            // Convert dp to px for proper sizing (161dp x 91dp)
            android.content.Context context = holder.view.getContext();
            float density = context.getResources().getDisplayMetrics().density;
            int widthPx = (int) (161 * density);
            int heightPx = (int) (91 * density);
            
            android.util.Log.d("HeroThumbnailPresenter", "Thumbnail size: " + widthPx + "x" + heightPx + "px (density: " + density + "x)");
            
            Picasso.get()
                .load(optimizedUrl)
                .noFade()
                .resize(widthPx, heightPx)
                .onlyScaleDown()
                .config(android.graphics.Bitmap.Config.ARGB_8888)
                // No placeholder - keep old image while loading new one
                .error(R.drawable.logo)
                .into(holder.thumbnailImage);
        } else {
            holder.thumbnailImage.setImageResource(R.drawable.logo);
        }
        
        // Set title below thumbnail (extract Vietnamese title like Hero Banner)
        if (holder.thumbnailTitle != null) {
            String rawTitle = video.getTitle();
            if (rawTitle != null && !rawTitle.isEmpty()) {
                // Parse format: "Vietnamese Title (Year) English Title"
                java.util.regex.Pattern pattern = java.util.regex.Pattern.compile("^(.+?)\\s*\\(\\d{4}\\)");
                java.util.regex.Matcher matcher = pattern.matcher(rawTitle);
                String displayTitle;
                if (matcher.find()) {
                    displayTitle = matcher.group(1).trim();
                } else {
                    displayTitle = rawTitle;
                }
                holder.thumbnailTitle.setText(smartThumbnailLineBreak(displayTitle));
                holder.thumbnailTitle.setVisibility(View.VISIBLE);
            } else {
                holder.thumbnailTitle.setVisibility(View.GONE);
            }
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
     * Size: 402x228px for xxhdpi (134dp * 3 = 402px, 76dp * 3 = 228px)
     */
    private String getOptimizedThumbnailUrl(String imageUrl) {
        try {
            java.net.URL urlObj = new java.net.URL(imageUrl);
            String domain = urlObj.getProtocol() + "://" + urlObj.getHost();
            String path = urlObj.getPath();
            if (urlObj.getQuery() != null) {
                path += "?" + urlObj.getQuery();
            }
            
            // Thumbnail dimensions for xxhdpi (3x): 161dp → 483px, 91dp → 273px
            int thumbnailWidth = 483;
            int thumbnailHeight = 273;
            
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
        if (holder.thumbnailTitle != null) {
            holder.thumbnailTitle.setText(null);
        }
        holder.view.setOnFocusChangeListener(null);
        holder.view.setOnClickListener(null);
    }
    
    static class ThumbnailViewHolder extends ViewHolder {
        ImageView thumbnailImage;
        TextView thumbnailTitle;
        
        public ThumbnailViewHolder(View view) {
            super(view);
            thumbnailImage = view.findViewById(R.id.thumbnail_image);
            thumbnailTitle = view.findViewById(R.id.thumbnail_title);
        }
    }
}
