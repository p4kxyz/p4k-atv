package com.files.codes.view.presenter;

import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.leanback.widget.ArrayObjectAdapter;
import androidx.leanback.widget.HorizontalGridView;
import androidx.leanback.widget.ItemBridgeAdapter;
import androidx.leanback.widget.OnItemViewClickedListener;
import androidx.leanback.widget.Presenter;
import androidx.leanback.widget.Row;
import androidx.leanback.widget.RowPresenter;

import com.files.codes.AppConfig;
import com.files.codes.R;
import com.files.codes.model.FavoriteModel;
import com.files.codes.model.VideoContent;
import com.files.codes.model.api.ApiService;
import com.files.codes.utils.PreferenceUtils;
import com.files.codes.utils.RetrofitClient;
import com.files.codes.view.VideoDetailsActivity;
import com.files.codes.view.HeroStyleVideoDetailsActivity;
import com.squareup.picasso.Picasso;
import com.squareup.picasso.Target;

import java.util.ArrayList;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;

/**
 * Netflix-style Hero Banner Presenter
 * Features:
 * - Full-width banner with gradient overlay
 * - Large title + detailed description
 * - Action buttons (Play, Info)
 * - Auto-cycling content (optional)
 * - Smooth fade transitions
 */
public class HeroBannerPresenter extends Presenter {
    private static final String TAG = "HeroBannerPresenter";
    private static final int AUTO_CYCLE_DELAY = 7000; // 7 seconds per slide
    
    private Context mContext;
    private Handler autoPlayHandler;
    private Runnable autoPlayRunnable;
    private List<VideoContent> allContent;
    private List<VideoContent> featuredContent; // Featured movies for thumbnails
    private int currentIndex = 0;
    private boolean isAutoCycleEnabled = false; // Disabled auto-cycling

    /**
     * Set featured content for thumbnails row
     */
    public void setFeaturedContent(List<VideoContent> content) {
        this.featuredContent = content;
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent) {
        mContext = parent.getContext();
        
        // Inflate custom hero banner layout
        View view = LayoutInflater.from(mContext).inflate(R.layout.item_hero_banner, parent, false);
        
        return new HeroBannerViewHolder(view);
    }

    @Override
    public void onBindViewHolder(Presenter.ViewHolder viewHolder, Object item) {
        if (!(item instanceof VideoContent)) {
            return;
        }
        
        VideoContent video = (VideoContent) item;
        HeroBannerViewHolder holder = (HeroBannerViewHolder) viewHolder;
        
        // Bind data to views
        holder.bind(video);
        
        Log.d(TAG, "Hero Banner bound: " + video.getTitle());
    }

    @Override
    public void onUnbindViewHolder(Presenter.ViewHolder viewHolder) {
        HeroBannerViewHolder holder = (HeroBannerViewHolder) viewHolder;
        holder.cleanup();
    }

    /**
     * Custom ViewHolder for Hero Banner
     */
    class HeroBannerViewHolder extends Presenter.ViewHolder {
        
        private ImageView heroBackground;
        private TextView heroTitle;
        private TextView heroTitleLine2;
        private TextView heroDescription;
        private TextView heroImdbRating;
        private TextView heroYear;
        private TextView heroQuality;
        private LinearLayout heroImdbContainer;
        private LinearLayout heroQualityContainer;
        private Button playButton;
        private Button favoriteButton;
        private LinearLayout indicatorContainer;
        private View gradientOverlay;
        private HorizontalGridView thumbnailsGrid;
        
        private VideoContent currentVideo;
        private PicassoImageTarget imageTarget;
        private boolean isFavorite = false;

        public HeroBannerViewHolder(View view) {
            super(view);
            
            // Find views
            heroBackground = view.findViewById(R.id.hero_background);
            heroTitle = view.findViewById(R.id.hero_title);
            heroTitleLine2 = view.findViewById(R.id.hero_title_line2);
            heroDescription = view.findViewById(R.id.hero_description);
            heroImdbRating = view.findViewById(R.id.hero_imdb_rating);
            heroImdbContainer = view.findViewById(R.id.hero_imdb_container);
            heroYear = view.findViewById(R.id.hero_year);
            heroQuality = view.findViewById(R.id.hero_quality);
            heroQualityContainer = view.findViewById(R.id.hero_quality_container);
            playButton = view.findViewById(R.id.hero_play_button);
            favoriteButton = view.findViewById(R.id.hero_favorite_button);
            indicatorContainer = view.findViewById(R.id.hero_indicator_container);
            gradientOverlay = view.findViewById(R.id.gradient_overlay);
            thumbnailsGrid = view.findViewById(R.id.hero_thumbnails_grid);
            
            imageTarget = new PicassoImageTarget();
            
            // Setup button click listeners
            setupClickListeners();
        }

        private void setupClickListeners() {
            // Play Button - Go to Video Details
            playButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (currentVideo != null) {
                        openVideoDetails(currentVideo);
                    }
                }
            });
            
            // ❤ Favorite Button - Toggle Add/Remove Favorite
            favoriteButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (currentVideo != null) {
                        toggleFavorite();
                    }
                }
            });
            
            // Focus change listeners for smooth animations
            playButton.setOnFocusChangeListener(new View.OnFocusChangeListener() {
                @Override
                public void onFocusChange(View v, boolean hasFocus) {
                    animateButtonFocus(playButton, hasFocus);
                }
            });
            
            favoriteButton.setOnFocusChangeListener(new View.OnFocusChangeListener() {
                @Override
                public void onFocusChange(View v, boolean hasFocus) {
                    animateButtonFocus(favoriteButton, hasFocus);
                }
            });
            
            // ❌ REMOVED initial focus request - will be handled by Leanback framework
            // Prevents focus jumping when scrolling between hero banner items
        }

        public void bind(VideoContent video) {
            this.currentVideo = video;
            
            // Fade in animation
            fadeInContent();
            
            // ❌ REMOVED auto-focus - prevents scroll jumping when multiple items exist
            // Focus will be set naturally by Leanback framework
            
            // Load background image from API
            String imageUrl = video.getPosterUrl(); // Use posterUrl for wide banners
            if (imageUrl == null || imageUrl.trim().isEmpty()) {
                imageUrl = video.getThumbnailUrl(); // Fallback to thumbnail
            }
            
            loadBackgroundImage(imageUrl);
            
            // Set title with support for two lines - Sci-Fi/Tech styling
            if (video.getTitle() != null) {
                String rawTitle = video.getTitle(); // Use original title, not formatted
                
                // Parse format: "Vietnamese Title (Year) English Title"
                // Use regex to find pattern: text (year) text
                String vietnameseTitle = "";
                String englishTitle = "";
                
                // Look for pattern like "Đầm Lầy Cá Mập (2011) Swamp Shark"
                java.util.regex.Pattern pattern = java.util.regex.Pattern.compile("^(.+?)\\s*\\((\\d{4})\\)\\s*(.+)$");
                java.util.regex.Matcher matcher = pattern.matcher(rawTitle);
                
                if (matcher.find()) {
                    vietnameseTitle = matcher.group(1).trim();
                    englishTitle = matcher.group(3).trim(); // group 3 because group 2 is the year
                    
                    // Vietnamese title: UPPERCASE for sci-fi/tech style
                    heroTitle.setText(vietnameseTitle.toUpperCase());
                    
                    if (!englishTitle.isEmpty()) {
                        // English title: Add spaces between letters for elegance
                        String spacedTitle = addLetterSpacing(englishTitle.toUpperCase());
                        heroTitleLine2.setText(spacedTitle);
                        heroTitleLine2.setVisibility(View.VISIBLE);
                    } else {
                        heroTitleLine2.setVisibility(View.GONE);
                    }
                } else {
                    // Fallback: if no pattern match, use formatted title on line 1
                    String formattedTitle = formatMovieTitle(rawTitle);
                    if (formattedTitle.contains("\n")) {
                        String[] parts = formattedTitle.split("\n", 2);
                        heroTitle.setText(parts[0].toUpperCase());
                        heroTitleLine2.setText(addLetterSpacing(parts[1].toUpperCase()));
                        heroTitleLine2.setVisibility(View.VISIBLE);
                    } else {
                        heroTitle.setText(formattedTitle.toUpperCase());
                        heroTitleLine2.setVisibility(View.GONE);
                    }
                }
                heroTitle.setVisibility(View.VISIBLE);
            } else {
                heroTitle.setVisibility(View.GONE);
                heroTitleLine2.setVisibility(View.GONE);
            }
            
            // Set description (max 120 characters for 2-3 lines, UX best practice)
            if (video.getDescription() != null && !video.getDescription().isEmpty()) {
                String description = video.getDescription().trim();
                
                // Smart truncate: cut at word boundary for better readability
                if (description.length() > 120) {
                    description = description.substring(0, 120);
                    int lastSpace = description.lastIndexOf(' ');
                    if (lastSpace > 80) { // Ensure we don't cut too early
                        description = description.substring(0, lastSpace);
                    }
                    description = description + "...";
                }
                
                heroDescription.setText(description);
                heroDescription.setVisibility(View.VISIBLE);
            } else {
                heroDescription.setVisibility(View.GONE);
            }
            
            // Set metadata from API
            // IMDB Rating (from imdbRating field)
            if (video.getImdbRating() != null && !video.getImdbRating().isEmpty()) {
                try {
                    // Parse and format IMDB rating (e.g., "8.5" or "7.083" -> "8.5" or "7.1")
                    double rating = Double.parseDouble(video.getImdbRating());
                    String formattedRating = String.format("%.1f", rating);
                    heroImdbRating.setText(formattedRating);
                    heroImdbContainer.setVisibility(View.VISIBLE);
                } catch (NumberFormatException e) {
                    // If parsing fails, hide IMDB rating
                    heroImdbContainer.setVisibility(View.GONE);
                }
            } else {
                heroImdbContainer.setVisibility(View.GONE);
            }
            
            // Year (from release field)
            if (video.getRelease() != null && !video.getRelease().isEmpty()) {
                heroYear.setText(video.getRelease());
                heroYear.setVisibility(View.VISIBLE);
            } else {
                heroYear.setVisibility(View.GONE);
            }
            
            // Quality (from videoQuality field) with dynamic color
            if (video.getVideoQuality() != null && !video.getVideoQuality().isEmpty()) {
                String quality = video.getVideoQuality().trim();
                heroQuality.setText(quality);
                
                // Set background color based on quality type
                int backgroundRes;
                if (quality.equals("4K")) {
                    backgroundRes = R.drawable.metadata_quality_4k; // Purple for 4K
                } else if (quality.equalsIgnoreCase("Full HD") || quality.equalsIgnoreCase("FHD")) {
                    backgroundRes = R.drawable.metadata_quality_fullhd; // Green for Full HD
                } else if (quality.equalsIgnoreCase("HD")) {
                    backgroundRes = R.drawable.metadata_quality_hd; // Orange for HD
                } else {
                    backgroundRes = R.drawable.metadata_quality_modern; // Default
                }
                
                heroQualityContainer.setBackgroundResource(backgroundRes);
                heroQualityContainer.setVisibility(View.VISIBLE);
            } else {
                heroQualityContainer.setVisibility(View.GONE);
            }
            
            // Update button text based on content type
            if (video.getIsTvseries() != null && video.getIsTvseries().equals("1")) {
                playButton.setText("▶");
            } else {
                playButton.setText("▶");
            }
            
            // Check favorite status
            checkFavoriteStatus();
            
            // Setup featured thumbnails
            setupFeaturedThumbnails();
            
            Log.d(TAG, "Hero Banner data bound: " + video.getTitle());
        }

        private void setupFeaturedThumbnails() {
            if (thumbnailsGrid == null) {
                Log.d(TAG, "Thumbnails grid not found in layout");
                return;
            }
            
            // Create adapter with thumbnail presenter
            ArrayObjectAdapter thumbnailsAdapter = new ArrayObjectAdapter(new HeroThumbnailPresenter());
            
            // Use featured content if available, otherwise use current video
            List<VideoContent> contentToShow = new ArrayList<>();
            if (featuredContent != null && !featuredContent.isEmpty()) {
                contentToShow.addAll(featuredContent);
            } else if (currentVideo != null) {
                // Fallback: show current video only
                contentToShow.add(currentVideo);
            }
            
            // Add content to adapter
            for (VideoContent video : contentToShow) {
                thumbnailsAdapter.add(video);
            }
            
            // HorizontalGridView needs ItemBridgeAdapter wrapper
            androidx.leanback.widget.ItemBridgeAdapter bridgeAdapter = 
                new androidx.leanback.widget.ItemBridgeAdapter(thumbnailsAdapter);
            thumbnailsGrid.setAdapter(bridgeAdapter);
            
            // Handle thumbnail selection - update hero banner when focused
            thumbnailsGrid.setOnChildViewHolderSelectedListener(new androidx.leanback.widget.OnChildViewHolderSelectedListener() {
                @Override
                public void onChildViewHolderSelected(androidx.recyclerview.widget.RecyclerView parent, 
                                                       androidx.recyclerview.widget.RecyclerView.ViewHolder child, 
                                                       int position, int subposition) {
                    // Update hero banner to show selected thumbnail's video
                    if (position >= 0 && position < thumbnailsAdapter.size()) {
                        final VideoContent selectedVideo = (VideoContent) thumbnailsAdapter.get(position);
                        
                        // Update hero banner content with selected video
                        if (selectedVideo != null) {
                            updateHeroBannerContent(selectedVideo);
                        }
                        
                        // Set click listener to open details
                        if (child != null) {
                            child.itemView.setOnClickListener(new View.OnClickListener() {
                                @Override
                                public void onClick(View v) {
                                    if (selectedVideo != null) {
                                        openVideoDetails(selectedVideo);
                                    }
                                }
                            });
                        }
                    }
                }
            });
            
            Log.d(TAG, "✅ Featured thumbnails setup complete with " + contentToShow.size() + " items");
        }

        private void updateHeroBannerContent(VideoContent video) {
            if (video == null) return;
            
            this.currentVideo = video;
            
            // Update background image
            String imageUrl = video.getPosterUrl();
            if (imageUrl == null || imageUrl.trim().isEmpty()) {
                imageUrl = video.getThumbnailUrl();
            }
            loadBackgroundImage(imageUrl);
            
            // Update title
            if (video.getTitle() != null) {
                String rawTitle = video.getTitle();
                java.util.regex.Pattern pattern = java.util.regex.Pattern.compile("^(.+?)\\s*\\((\\d{4})\\)\\s*(.+)$");
                java.util.regex.Matcher matcher = pattern.matcher(rawTitle);
                
                if (matcher.find()) {
                    String vietnameseTitle = matcher.group(1).trim();
                    String englishTitle = matcher.group(3).trim();
                    
                    heroTitle.setText(vietnameseTitle.toUpperCase());
                    
                    if (!englishTitle.isEmpty()) {
                        String spacedTitle = addLetterSpacing(englishTitle.toUpperCase());
                        heroTitleLine2.setText(spacedTitle);
                        heroTitleLine2.setVisibility(View.VISIBLE);
                    } else {
                        heroTitleLine2.setVisibility(View.GONE);
                    }
                } else {
                    heroTitle.setText(rawTitle.toUpperCase());
                    heroTitleLine2.setVisibility(View.GONE);
                }
            }
            
            // Update description
            if (video.getDescription() != null && !video.getDescription().isEmpty()) {
                heroDescription.setText(video.getDescription());
                heroDescription.setVisibility(View.VISIBLE);
            } else {
                heroDescription.setVisibility(View.GONE);
            }
            
            // Update IMDb rating
            if (video.getImdbRating() != null && !video.getImdbRating().isEmpty()) {
                heroImdbRating.setText(video.getImdbRating());
                heroImdbContainer.setVisibility(View.VISIBLE);
            } else {
                heroImdbContainer.setVisibility(View.GONE);
            }
            
            // Update year
            if (video.getRelease() != null && !video.getRelease().isEmpty()) {
                heroYear.setText(video.getRelease());
            }
            
            // Update quality
            if (video.getVideoQuality() != null && !video.getVideoQuality().isEmpty()) {
                String quality = video.getVideoQuality();
                heroQuality.setText(quality);
                
                int backgroundRes;
                if (quality.equalsIgnoreCase("4K") || quality.equalsIgnoreCase("2160p")) {
                    backgroundRes = R.drawable.metadata_quality_4k;
                } else if (quality.equalsIgnoreCase("Full HD") || quality.equalsIgnoreCase("1080p")) {
                    backgroundRes = R.drawable.metadata_quality_fullhd;
                } else if (quality.equalsIgnoreCase("HD")) {
                    backgroundRes = R.drawable.metadata_quality_hd;
                } else {
                    backgroundRes = R.drawable.metadata_quality_modern;
                }
                
                heroQualityContainer.setBackgroundResource(backgroundRes);
                heroQualityContainer.setVisibility(View.VISIBLE);
            } else {
                heroQualityContainer.setVisibility(View.GONE);
            }
            
            // Update favorite status
            checkFavoriteStatus();
            
            Log.d(TAG, "🔄 Hero Banner updated to: " + video.getTitle());
        }

        private void loadBackgroundImage(String url) {
            if (url == null || url.trim().isEmpty()) {
                // Use default placeholder
                heroBackground.setImageResource(R.drawable.logo);
                return;
            }
            
            // Get screen width for optimal image sizing
            android.util.DisplayMetrics displayMetrics = mContext.getResources().getDisplayMetrics();
            int screenWidth = displayMetrics.widthPixels;
            
            // Calculate 16:9 aspect ratio height based on screen width
            // This ensures Cloudflare returns image with correct aspect ratio
            int targetWidth = screenWidth;
            int targetHeight = (int) (screenWidth / 16.0 * 9.0); // 16:9 ratio
            
            Log.d(TAG, "📐 Screen dimensions: " + screenWidth + "x" + displayMetrics.heightPixels + " px");
            Log.d(TAG, "📐 Target 16:9 size: " + targetWidth + "x" + targetHeight + " px");
            
            // Use Cloudflare Image Resizing with fit=cover to fill the banner
            String optimizedUrl = url;
            try {
                java.net.URL urlObj = new java.net.URL(url);
                String domain = urlObj.getProtocol() + "://" + urlObj.getHost();
                String path = urlObj.getPath() + (urlObj.getQuery() != null ? "?" + urlObj.getQuery() : "");
                
                // Cloudflare resize: maintain 16:9 aspect ratio, fit=cover to fill
                // This preserves the entire image without cropping at Cloudflare level
                optimizedUrl = domain + "/cdn-cgi/image/width=" + targetWidth 
                             + ",height=" + targetHeight
                             + ",fit=cover,quality=90,format=auto" + path;
                
                Log.d(TAG, "🌐 ========== CLOUDFLARE URL ==========");
                Log.d(TAG, "🌐 Original: " + url);
                Log.d(TAG, "🌐 Cloudflare: " + optimizedUrl);
                Log.d(TAG, "🌐 Size: " + targetWidth + "x" + targetHeight + " px (fit=contain, no crop)");
                Log.d(TAG, "🌐 =====================================");
            } catch (Exception e) {
                optimizedUrl = url;
                Log.d(TAG, "⚠️ Using original URL: " + url);
            }
            
            // Load with Picasso
            Picasso.get()
                .load(optimizedUrl)
                // No placeholder - keep old image while loading new one
                .error(R.drawable.logo)
                .into(heroBackground);
        }

        private void openVideoDetails(VideoContent video) {
            Log.d(TAG, "HeroBannerPresenter - Opening video: " + video.getTitle() + " (ID: " + video.getId() + ")");
            Intent intent = new Intent(mContext, HeroStyleVideoDetailsActivity.class);
            intent.putExtra("id", video.getId());
            
            // Determine type from isTvseries field
            String type = "movie";
            if (video.getIsTvseries() != null && video.getIsTvseries().equals("1")) {
                type = "tvseries";
            } else if (video.getType() != null) {
                type = video.getType();
            }
            
            intent.putExtra("type", type);
            intent.putExtra("thumbImage", video.getThumbnailUrl() != null ? video.getThumbnailUrl() : "");
            
            mContext.startActivity(intent);
            
            Log.d(TAG, "Opening details for: " + video.getTitle() + " (Type: " + type + ")");
        }

        private void fadeInContent() {
            // Smooth fade in animation
            AlphaAnimation fadeIn = new AlphaAnimation(0.0f, 1.0f);
            fadeIn.setDuration(800);
            fadeIn.setFillAfter(true);
            
            heroTitle.startAnimation(fadeIn);
            heroDescription.startAnimation(fadeIn);
            playButton.startAnimation(fadeIn);
            favoriteButton.startAnimation(fadeIn);
        }

        private void animateButtonFocus(Button button, boolean hasFocus) {
            // No scale animation - border handles focus indication
        }
        
        /**
         * Check if video is in favorite list
         */
        private void checkFavoriteStatus() {
            if (currentVideo == null) return;
            
            String userId = PreferenceUtils.getUserId(mContext);
            if (userId == null || userId.isEmpty()) {
                isFavorite = false;
                updateFavoriteButton();
                return;
            }
            
            String videoId = currentVideo.getVideosId();
            if (videoId == null || videoId.isEmpty()) {
                videoId = currentVideo.getId();
            }
            
            if (videoId == null || videoId.isEmpty()) {
                isFavorite = false;
                updateFavoriteButton();
                return;
            }
            
            Retrofit retrofit = RetrofitClient.getRetrofitInstance();
            ApiService api = retrofit.create(ApiService.class);
            
            Call<FavoriteModel> call = api.verifyFavoriteList(AppConfig.API_KEY, userId, videoId);
            call.enqueue(new Callback<FavoriteModel>() {
                @Override
                public void onResponse(Call<FavoriteModel> call, Response<FavoriteModel> response) {
                    if (response.isSuccessful() && response.body() != null) {
                        String status = response.body().getStatus();
                        // API returns "success" if video IS in favorite list
                        isFavorite = status.equalsIgnoreCase("success");
                        updateFavoriteButton();
                        Log.e(TAG, "checkFavoriteStatus: isFavorite=" + isFavorite + " for " + currentVideo.getTitle());
                    } else {
                        isFavorite = false;
                        updateFavoriteButton();
                    }
                }
                
                @Override
                public void onFailure(Call<FavoriteModel> call, Throwable t) {
                    isFavorite = false;
                    updateFavoriteButton();
                    Log.e(TAG, "checkFavoriteStatus: Error - " + t.getMessage());
                }
            });
        }
        
        /**
         * Toggle Favorite - Add/Remove from favorite list
         */
        private void toggleFavorite() {
            if (currentVideo == null) {
                Log.e(TAG, "toggleFavorite: currentVideo is null");
                return;
            }
            
            String userId = PreferenceUtils.getUserId(mContext);
            if (userId == null || userId.isEmpty()) {
                Toast.makeText(mContext, "Vui lòng đăng nhập để thêm yêu thích", Toast.LENGTH_SHORT).show();
                Log.e(TAG, "toggleFavorite: User not logged in");
                return;
            }
            
            String videoId = currentVideo.getVideosId();
            if (videoId == null || videoId.isEmpty()) {
                // Try alternative ID fields
                videoId = currentVideo.getId();
                if (videoId == null || videoId.isEmpty()) {
                    Toast.makeText(mContext, "Không thể xác định ID phim", Toast.LENGTH_SHORT).show();
                    Log.e(TAG, "toggleFavorite: Video ID is null or empty");
                    return;
                }
            }
            
            Log.e(TAG, "toggleFavorite: userId=" + userId + ", videoId=" + videoId + ", title=" + currentVideo.getTitle() + ", isFavorite=" + isFavorite);
            
            Retrofit retrofit = RetrofitClient.getRetrofitInstance();
            ApiService api = retrofit.create(ApiService.class);
            
            // Use correct API based on current state
            Call<FavoriteModel> call;
            if (isFavorite) {
                // Already favorite -> Remove
                call = api.removeFromFavorite(AppConfig.API_KEY, userId, videoId);
            } else {
                // Not favorite -> Add
                call = api.addToFavorite(AppConfig.API_KEY, userId, videoId);
            }
            
            call.enqueue(new Callback<FavoriteModel>() {
                @Override
                public void onResponse(Call<FavoriteModel> call, Response<FavoriteModel> response) {
                    Log.e(TAG, "toggleFavorite: response code=" + response.code());
                    if (response.isSuccessful() && response.body() != null) {
                        String status = response.body().getStatus();
                        Log.e(TAG, "toggleFavorite: status=" + status);
                        if (status.equalsIgnoreCase("success")) {
                            isFavorite = !isFavorite;
                            updateFavoriteButton();
                            String message = isFavorite ? "Đã thêm vào yêu thích ❤" : "Đã xóa khỏi yêu thích 💔";
                            Toast.makeText(mContext, message, Toast.LENGTH_SHORT).show();
                        } else {
                            Toast.makeText(mContext, "Không thể cập nhật yêu thích", Toast.LENGTH_SHORT).show();
                        }
                    } else {
                        Toast.makeText(mContext, "Lỗi server: " + response.code(), Toast.LENGTH_SHORT).show();
                        Log.e(TAG, "toggleFavorite: Response not successful or body is null");
                    }
                }
                
                @Override
                public void onFailure(Call<FavoriteModel> call, Throwable t) {
                    Toast.makeText(mContext, "Lỗi kết nối: " + t.getMessage(), Toast.LENGTH_SHORT).show();
                    Log.e(TAG, "toggleFavorite: Error - " + t.getMessage(), t);
                }
            });
        }
        
        /**
         * Update favorite button text based on status
         */
        private void updateFavoriteButton() {
            if (isFavorite) {
                favoriteButton.setText("💔");
            } else {
                favoriteButton.setText("❤");
            }
        }

        public void cleanup() {
            // Clear image target to prevent memory leaks
            if (heroBackground != null) {
                heroBackground.setImageDrawable(null);
            }
        }

        /**
         * Picasso Target for loading images
         */
        class PicassoImageTarget implements Target {
            @Override
            public void onBitmapLoaded(Bitmap bitmap, Picasso.LoadedFrom from) {
                if (heroBackground != null) {
                    Drawable drawable = new BitmapDrawable(mContext.getResources(), bitmap);
                    
                    // Fade in the new image
                    AlphaAnimation fadeIn = new AlphaAnimation(0.0f, 1.0f);
                    fadeIn.setDuration(600);
                    heroBackground.setImageDrawable(drawable);
                    heroBackground.startAnimation(fadeIn);
                }
            }

            @Override
            public void onBitmapFailed(Exception e, Drawable errorDrawable) {
                if (heroBackground != null) {
                    heroBackground.setImageResource(R.drawable.logo);
                }
                Log.e(TAG, "Failed to load hero image: " + e.getMessage());
            }

            @Override
            public void onPrepareLoad(Drawable placeHolderDrawable) {
                // Keep current image while loading new one - don't replace with placeholder
            }
        }
    }

    /**
     * Enable/disable auto-cycling (for future implementation)
     */
    public void setAutoCycleEnabled(boolean enabled) {
        this.isAutoCycleEnabled = enabled;
    }

    /**
     * Set all content for cycling (for future implementation)
     */
    public void setAllContent(List<VideoContent> content) {
        this.allContent = content;
    }

    /**
     * Format movie title from "VietName (year) EngName" to styled text
     * Examples:
     * - "Thế Giới Không Lối Thoát (2020) Alice in Borderland" -> "Thế Giới Không Lối Thoát\nAlice in Borderland"
     * - "Sắc, Giới (2007) 色‧戒" -> "Sắc, Giới\n色‧戒"
     * - "Khoảnh Khắc Để Nhớ (2004)" -> "Khoảnh Khắc Để Nhớ"
     */
    private String formatMovieTitle(String rawTitle) {
        if (rawTitle == null || rawTitle.trim().isEmpty()) {
            return "";
        }
        
        // Strategy: Split by "(year)" pattern
        // Format is: "Vietnamese Name (year) Foreign Name"
        // We need to find the year pattern and split there
        
        // Pattern to match year: (2020), (2007), etc.
        java.util.regex.Pattern yearPattern = java.util.regex.Pattern.compile("\\s*\\((\\d{4})\\)\\s*");
        java.util.regex.Matcher matcher = yearPattern.matcher(rawTitle);
        
        if (matcher.find()) {
            // Found year pattern
            String vietnamesePart = rawTitle.substring(0, matcher.start()).trim();
            String foreignPart = rawTitle.substring(matcher.end()).trim();
            
            // If there's a foreign name after the year, split into 2 lines
            if (!foreignPart.isEmpty()) {
                return vietnamesePart + "\n" + foreignPart;
            } else {
                // No foreign name, just return Vietnamese part
                return vietnamesePart;
            }
        }
        
        // Fallback: no year pattern found, return as-is
        return rawTitle;
    }

    /**
     * Add letter spacing to text for elegant style
     * "TAKE ME HOME" -> "T A K E  M E  H O M E"
     */
    private String addLetterSpacing(String text) {
        if (text == null || text.trim().isEmpty()) {
            return text;
        }
        
        StringBuilder result = new StringBuilder();
        for (int i = 0; i < text.length(); i++) {
            result.append(text.charAt(i));
            // Add space after each character except the last one
            // Don't add extra space if current char is already a space
            if (i < text.length() - 1 && text.charAt(i) != ' ') {
                result.append(' ');
            }
        }
        
        return result.toString();
    }
}
