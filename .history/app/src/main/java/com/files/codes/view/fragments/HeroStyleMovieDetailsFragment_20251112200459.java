package com.files.codes.view.fragments;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.io.Serializable;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.files.codes.AppConfig;
import com.files.codes.R;

import com.files.codes.model.FavoriteModel;
import com.files.codes.model.Genre;
import com.files.codes.model.PlaybackModel;
import com.files.codes.model.Video;
import com.files.codes.model.movieDetails.CastAndCrew;
import com.files.codes.model.movieDetails.Director;
import com.files.codes.model.movieDetails.MovieSingleDetails;
import com.files.codes.model.movieDetails.Season;
import com.files.codes.model.movieDetails.Episode;
import com.files.codes.model.api.ApiService;
import com.files.codes.model.SearchModel;
import com.files.codes.utils.RetrofitClient;
import com.files.codes.utils.ToastMsg;
import com.files.codes.utils.Phim4kClient;
import com.files.codes.utils.Free1Client;
import com.files.codes.utils.PreferenceUtils;
import com.files.codes.model.phim4k.Phim4kEpisodeServer;
import com.files.codes.model.phim4k.Phim4kEpisode;
import com.files.codes.model.phim4k.Phim4kMovie;
import com.files.codes.view.PlayerActivity;
import com.files.codes.view.adapter.ServerAdapter;
import com.squareup.picasso.Picasso;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;

import java.util.ArrayList;
import java.util.List;

public class HeroStyleMovieDetailsFragment extends Fragment {
    
    private static final String TAG = "HeroStyleMovieDetails";
    private static final String ARG_VIDEO_ID = "video_id";
    private static final String ARG_VIDEO_TYPE = "video_type";
    
    // UI Elements
    private ImageView backgroundPoster;
    private ImageView heroMovieLogo;
    private TextView movieTitle;
    private TextView movieDescription;
    private Button playButton;
    private Button favoriteButton;
    private TextView heroYear;
    private TextView heroImdbRating;
    private TextView heroQuality;
    private RecyclerView genreTagsRecycler;
    private RecyclerView castCrewRecycler;
    
    // Season/Episodes UI Elements (TV Series)
    private LinearLayout seasonSelectorLayout;
    private Button seasonSelectorButton;
    private RecyclerView episodesRecycler;
    private LinearLayout episodesPaginationLayout;
    private Button episodesPrevButton;
    private Button episodesNextButton;
    private TextView episodesPageInfo;
    
    // Data
    private String videoId;
    private String videoType;
    private MovieSingleDetails movieDetails;
    private boolean isFavorite = false;
    private String userId = "";
    
    // Season/Episodes Data (TV Series)
    private List<Season> allSeasons;
    private int currentSeasonIndex = 0;
    private int currentEpisodePage = 0;
    private static final int EPISODES_PER_PAGE = 4; // Optimized for 4K screen display
    
    // UI Animation State
    private LinearLayout hideableContentContainer;
    private boolean isHideableContentHidden = false;
    
    public static HeroStyleMovieDetailsFragment newInstance(String videoId, String videoType) {
        HeroStyleMovieDetailsFragment fragment = new HeroStyleMovieDetailsFragment();
        Bundle args = new Bundle();
        args.putString(ARG_VIDEO_ID, videoId);
        args.putString(ARG_VIDEO_TYPE, videoType);
        fragment.setArguments(args);
        return fragment;
    }
    
    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            videoId = getArguments().getString(ARG_VIDEO_ID);
            videoType = getArguments().getString(ARG_VIDEO_TYPE);
        }
        
        // Get userId from preferences
        if (PreferenceUtils.isLoggedIn(getContext())) {
            this.userId = PreferenceUtils.getUserId(getContext());
            Log.d(TAG, "User is logged in. UserId: " + this.userId);
        } else {
            this.userId = "1";
            Log.d(TAG, "User not logged in. Using default UserId: " + this.userId);
        }

    }
    
    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.hero_style_movie_details, container, false);
    }
    
    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        
        initViews(view);
        setupButtonListeners();
        loadMovieDetails();
    }
    
    private void initViews(View view) {
        backgroundPoster = view.findViewById(R.id.background_poster);
        heroMovieLogo = view.findViewById(R.id.hero_movie_logo);
        movieTitle = view.findViewById(R.id.movie_title);
        movieDescription = view.findViewById(R.id.movie_description_tv);
        
        Log.d(TAG, "Views initialized - movieTitle: " + (movieTitle != null ? "found" : "NOT FOUND"));
        playButton = view.findViewById(R.id.play_button);
        favoriteButton = view.findViewById(R.id.favorite_button);
        heroYear = view.findViewById(R.id.hero_year);
        heroImdbRating = view.findViewById(R.id.hero_imdb_rating);
        heroQuality = view.findViewById(R.id.hero_quality);
        genreTagsRecycler = view.findViewById(R.id.genre_tags_recycler);
        castCrewRecycler = view.findViewById(R.id.cast_crew_recycler);
        
        // UI Animation Containers
        hideableContentContainer = view.findViewById(R.id.hideable_content_container);
        
        // Season/Episodes UI Elements (TV Series)
        seasonSelectorLayout = view.findViewById(R.id.season_selector_layout);
        seasonSelectorButton = view.findViewById(R.id.season_selector_button);
        episodesRecycler = view.findViewById(R.id.episodes_recycler);
        episodesPaginationLayout = view.findViewById(R.id.episodes_pagination_layout);
        episodesPrevButton = view.findViewById(R.id.episodes_prev_button);
        episodesNextButton = view.findViewById(R.id.episodes_next_button);
        episodesPageInfo = view.findViewById(R.id.episodes_page_info);
        
        // Setup RecyclerViews
        setupGenreTagsRecycler();
        setupCastCrewRecycler();
        setupEpisodesRecycler();
    }
    
    private void setupGenreTagsRecycler() {
        LinearLayoutManager layoutManager = new LinearLayoutManager(getContext(), LinearLayoutManager.HORIZONTAL, false);
        genreTagsRecycler.setLayoutManager(layoutManager);
    }
    
    private void setupCastCrewRecycler() {
        LinearLayoutManager layoutManager = new LinearLayoutManager(getContext(), LinearLayoutManager.HORIZONTAL, false);
        castCrewRecycler.setLayoutManager(layoutManager);
    }
    
    private void setupEpisodesRecycler() {
        // Vertical linear layout for episode list items (like old page)
        androidx.recyclerview.widget.LinearLayoutManager linearLayoutManager =
            new androidx.recyclerview.widget.LinearLayoutManager(getContext(), androidx.recyclerview.widget.LinearLayoutManager.VERTICAL, false);
        episodesRecycler.setLayoutManager(linearLayoutManager);
        episodesRecycler.setHasFixedSize(true);
        episodesRecycler.setNestedScrollingEnabled(false);
    }
    
    private void setupButtonListeners() {
        playButton.setOnClickListener(v -> playMovie());
        favoriteButton.setOnClickListener(v -> toggleFavorite());
        seasonSelectorButton.setOnClickListener(v -> showSeasonSelectorDialog());
        
        // Episode pagination listeners
        episodesPrevButton.setOnClickListener(v -> goToPreviousEpisodesPage());
        episodesNextButton.setOnClickListener(v -> goToNextEpisodesPage());
        
        // Setup focus listeners for hiding/showing movie info
        setupFocusListeners();
    }
    
    private void setupFocusListeners() {
        // When focusing on season selector or episodes, hide hideable content
        View.OnFocusChangeListener hideContentListener = (v, hasFocus) -> {
            if (hasFocus) {
                hideHideableContent();
            }
        };
        
        // When focusing on main buttons, show hideable content
        View.OnFocusChangeListener showContentListener = (v, hasFocus) -> {
            if (hasFocus) {
                showHideableContent();
            }
        };
        
        // Apply listeners
        seasonSelectorButton.setOnFocusChangeListener(hideContentListener);
        episodesRecycler.setOnFocusChangeListener(hideContentListener);
        episodesPrevButton.setOnFocusChangeListener(hideContentListener);
        episodesNextButton.setOnFocusChangeListener(hideContentListener);
        
        playButton.setOnFocusChangeListener(showContentListener);
        favoriteButton.setOnFocusChangeListener(showContentListener);
        
        // Add focus listeners to other focusable elements that should show content
        movieTitle.setOnFocusChangeListener(showContentListener);
        genreTagsRecycler.setOnFocusChangeListener(showContentListener);
    }
    
    private void loadMovieDetails() {
        if (videoId == null) return;
        
        // Check if this is a phim4k movie/series
        if (videoId.startsWith("phim4k_")) {
            Log.d(TAG, "Detected phim4k content, ID: " + videoId);
            getPhim4kData(videoType, videoId);
            return;
        }
        
        // Check if this is a free1 movie/series
        if (videoId.startsWith("free1_")) {
            Log.d(TAG, "Detected free1 content, ID: " + videoId);
            getFree1Data(videoType, videoId);
            return;
        }
        
        ApiService apiService = RetrofitClient.getRetrofitInstance().create(ApiService.class);
        
        // Use getSingleDetail API with type parameter
        String type = ("tvseries".equals(videoType) || "tv".equals(videoType)) ? "tvseries" : "movie";
        Log.d(TAG, "API call - Key: " + com.files.codes.AppConfig.API_KEY + ", Type: " + type + ", ID: " + videoId);
        Call<MovieSingleDetails> call = apiService.getSingleDetail(com.files.codes.AppConfig.API_KEY, type, videoId);
        
        Log.d(TAG, "Loading movie details - ID: " + videoId + ", Type: " + type);
        
        call.enqueue(new Callback<MovieSingleDetails>() {
            @Override
            public void onResponse(Call<MovieSingleDetails> call, Response<MovieSingleDetails> response) {
                Log.d(TAG, "API Response Code: " + response.code());
                if (response.isSuccessful() && response.body() != null) {
                    movieDetails = response.body();
                    Log.d(TAG, "Movie loaded: " + movieDetails.getTitle());
                    displayMovieDetails();
                } else {
                    Log.e(TAG, "Failed to load movie details - Code: " + response.code());
                    if (response.errorBody() != null) {
                        try {
                            Log.e(TAG, "Error body: " + response.errorBody().string());
                        } catch (Exception e) {
                            Log.e(TAG, "Error reading error body: " + e.getMessage());
                        }
                    }
                }
            }
            
            @Override
            public void onFailure(Call<MovieSingleDetails> call, Throwable t) {
                Log.e(TAG, "Network error: " + t.getMessage());
            }
        });
    }
    
    private void getFree1Data(String vtype, final String vId) {
        Log.d(TAG, "Getting free1 data for: " + vId);
        
        // Extract actual free1 ID from full ID (remove "free1_" prefix)
        String actualId = vId.replace("free1_", "");
        Log.d(TAG, "Getting free1 data for actual ID: " + actualId);

        Free1Client free1Client = Free1Client.getInstance();
        free1Client.getMovieDetailWithMovie(actualId, new Free1Client.Free1DetailWithMovieCallback() {
            @Override
            public void onSuccess(com.files.codes.model.VideoContent videoContent, Phim4kMovie phim4kMovie) {
                if (getActivity() == null) return;
                
                Log.d(TAG, "Free1 details received: " + videoContent.getTitle());
                
                getActivity().runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        // Create a MovieSingleDetails object with free1 data
                        movieDetails = new MovieSingleDetails();
                        movieDetails.setTitle(videoContent.getTitle());
                        movieDetails.setDescription(videoContent.getDescription());
                        movieDetails.setPosterUrl(videoContent.getThumbnailUrl());
                        movieDetails.setRelease(videoContent.getRelease());
                        movieDetails.setType(vtype);
                        movieDetails.setVideosId(vId);
                        movieDetails.setIsPaid("0"); // Free1 content is always free
                        
                        // Convert free1 episodes to Video objects for playback
                        if (phim4kMovie != null && phim4kMovie.getEpisodes() != null) {
                            List<Video> videos = new ArrayList<>();
                            for (Phim4kEpisodeServer episodeServer : phim4kMovie.getEpisodes()) {
                                if (episodeServer.getItems() != null) {
                                    for (Phim4kEpisode episode : episodeServer.getItems()) {
                                        Video video = new Video();
                                        video.setLabel(episodeServer.getServerName() + " - " + episode.getName());
                                        video.setFileUrl(episode.getLink());
                                        video.setVideoFileId(episode.getSlug());
                                        video.setFileType("m3u8"); // Assuming HLS format
                                        video.setSubtitle(new ArrayList<>()); // Initialize empty subtitle list
                                        videos.add(video);
                                    }
                                }
                            }
                            movieDetails.setVideos(videos);
                        }
                        
                        displayMovieDetails();
                    }
                });
            }

            @Override
            public void onError(String error) {
                Log.e(TAG, "Free1 API error: " + error);
                if (getActivity() != null) {
                    getActivity().runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            // Show error or fallback
                        }
                    });
                }
            }
        });
    }
    
    private void displayMovieDetails() {
        if (movieDetails == null) return;
        
        Log.d(TAG, "Displaying movie details:");
        Log.d(TAG, "- Title: " + movieDetails.getTitle());
        Log.d(TAG, "- Videos count: " + (movieDetails.getVideos() != null ? movieDetails.getVideos().size() : "null"));
        if (movieDetails.getVideos() != null) {
            for (int i = 0; i < movieDetails.getVideos().size(); i++) {
                Video video = movieDetails.getVideos().get(i);
                Log.d(TAG, "  Video " + i + ": " + video.getLabel() + " - " + video.getFileType() + " - " + video.getFileUrl());
            }
        }
        
        // Set background poster
        if (movieDetails.getPosterUrl() != null) {
            Picasso.get()
                .load(movieDetails.getPosterUrl())
                .placeholder(R.drawable.default_background)
                .error(R.drawable.default_background)
                .into(backgroundPoster);
        }
        
        // Load movie logo
        loadMovieLogo();
        
        // Set movie title with hero banner logic
        setMovieTitle();
        
        // Set hero info tags
        setHeroInfoTags();
        
        // Set genre tags
        setupGenreTags();
        
        // Set description with smart truncation like hero banner
        setMovieDescription();
        
        // Setup cast and crew images
        setupCastCrewImages();
        
        // Setup seasons and episodes for TV series
        setupSeasonsAndEpisodes();
        
        // Check and update favorite status
        getFavStatus();
        
        // Auto-focus on Play button when page loads
        if (playButton != null) {
            playButton.post(() -> {
                playButton.requestFocus();
                Log.d(TAG, "Auto-focused on Play button");
            });
        }
    }
    
    private void loadMovieLogo() {
        if (videoId != null && !videoId.isEmpty()) {
            String logoUrl = "https://api.phim4k.lol/uploads/logo/" + videoId + ".jpg";
            
            Log.d(TAG, "Trying to load logo: " + logoUrl);
            
            Picasso.get()
                .load(logoUrl)
                .into(heroMovieLogo, new com.squareup.picasso.Callback() {
                    @Override
                    public void onSuccess() {
                        Log.d(TAG, "Logo loaded successfully, showing both logo and title");
                        heroMovieLogo.setVisibility(View.VISIBLE);
                        // Keep title visible too - both logo and title can coexist
                        movieTitle.setVisibility(View.VISIBLE);
                    }
                    
                    @Override
                    public void onError(Exception e) {
                        Log.d(TAG, "Logo load failed: " + e.getMessage() + ", showing title only");
                        heroMovieLogo.setVisibility(View.GONE);
                        movieTitle.setVisibility(View.VISIBLE);
                    }
                });
        } else {
            Log.d(TAG, "No video ID for logo, showing title only");
            heroMovieLogo.setVisibility(View.GONE);
            movieTitle.setVisibility(View.VISIBLE);
        }
    }
    
    private void setupCastCrewImages() {
        // Only show cast members (no directors)
        List<CastCrewItem> castList = new ArrayList<>();
        
        // Add cast members only (limit to 10 for better display)
        if (movieDetails.getCastAndCrew() != null && !movieDetails.getCastAndCrew().isEmpty()) {
            for (int i = 0; i < Math.min(movieDetails.getCastAndCrew().size(), 10); i++) {
                CastAndCrew cast = movieDetails.getCastAndCrew().get(i);
                castList.add(new CastCrewItem(cast.getName(), "Cast", cast.getImageUrl()));
            }
        }
        
        if (!castList.isEmpty()) {
            CastCrewAdapter adapter = new CastCrewAdapter(castList, new OnCastClickListener() {
                @Override
                public void onCastClick(CastCrewItem cast) {
                    // Show cast movies when clicked
                    showCastMovies(cast.name);
                }
            });
            castCrewRecycler.setAdapter(adapter);
            castCrewRecycler.setVisibility(View.VISIBLE);
        } else {
            castCrewRecycler.setVisibility(View.GONE);
        }
    }



    private void showCastMovies(String actorName) {
        // Chuyển sang Activity mới để hiển thị danh sách phim của diễn viên
        Intent intent = new Intent(getActivity(), com.files.codes.view.ActorMoviesActivity.class);
        intent.putExtra("actor_name", actorName);
        startActivity(intent);
    }

    private void showActorMoviesDialog(String actorName, com.files.codes.model.SearchModel searchResult) {
        android.app.AlertDialog.Builder builder = new android.app.AlertDialog.Builder(getActivity());
        builder.setTitle("Phim của " + actorName);
        
        StringBuilder content = new StringBuilder();
        
        // Add movies
        if (searchResult.getMovie() != null && !searchResult.getMovie().isEmpty()) {
            content.append("🎬 PHIM LẺ:\n");
            for (int i = 0; i < Math.min(searchResult.getMovie().size(), 10); i++) {
                com.files.codes.model.Movie movie = searchResult.getMovie().get(i);
                content.append("• ").append(movie.getTitle()).append("\n");
            }
            content.append("\n");
        }
        
        // Add TV series
        if (searchResult.getTvseries() != null && !searchResult.getTvseries().isEmpty()) {
            content.append("📺 PHIM BỘ:\n");
            for (int i = 0; i < Math.min(searchResult.getTvseries().size(), 10); i++) {
                com.files.codes.model.Movie series = searchResult.getTvseries().get(i);
                content.append("• ").append(series.getTitle()).append("\n");
            }
        }
        
        if (content.length() == 0) {
            content.append("Không tìm thấy phim nào của diễn viên này.");
        }
        
        builder.setMessage(content.toString());
        builder.setPositiveButton("Đóng", (dialog, which) -> dialog.dismiss());
        builder.show();
    }
    
    private void playMovie() {
        if (movieDetails != null) {
            Log.d(TAG, "Playing movie - ID: " + videoId + ", Title: " + movieDetails.getTitle());
            
            // Check if this is a TV series with seasons/episodes
            if (movieDetails.getSeason() != null && !movieDetails.getSeason().isEmpty()) {
                // TV Series - auto-play first episode of first season
                Season firstSeason = movieDetails.getSeason().get(0);
                if (firstSeason.getEpisodes() != null && !firstSeason.getEpisodes().isEmpty()) {
                    Episode firstEpisode = firstSeason.getEpisodes().get(0);
                    Log.d(TAG, "Auto-playing first episode of TV series: " + firstEpisode.getEpisodesName());
                    playEpisode(firstEpisode, 0);
                    return;
                } else {
                    Log.e(TAG, "TV series has no episodes in first season");
                }
            }
            
            // Regular Movie - check if movie has video sources
            if (movieDetails.getVideos() != null && movieDetails.getVideos().size() > 0) {
                if (movieDetails.getVideos().size() == 1) {
                    // Only one video source, play directly
                    playVideoDirectly(movieDetails.getVideos().get(0));
                } else {
                    // Multiple video sources, show server selection dialog
                    openServerDialog(movieDetails.getVideos());
                }
            } else {
                Log.e(TAG, "No video sources available");
            }
        } else {
            Log.e(TAG, "Cannot play movie - movieDetails is null");
        }
    }
    
    private void playVideoDirectly(Video video) {
        Log.d(TAG, "Playing video directly: " + video.getLabel());
        
        Bundle bundle = androidx.core.app.ActivityOptionsCompat.makeSceneTransitionAnimation(getActivity()).toBundle();
        
        com.files.codes.model.PlaybackModel playbackModel = new com.files.codes.model.PlaybackModel();
        
        // Set movieId for watch history tracking
        playbackModel.setMovieId(videoId);
        
        // Handle phim4k and free1 IDs differently since they're not numeric
        if (videoId.startsWith("phim4k_") || videoId.startsWith("free1_")) {
            // For phim4k/free1 content, use hash code as ID
            playbackModel.setId((long) videoId.hashCode());
        } else {
            // For regular content, parse as Long
            try {
                playbackModel.setId(Long.parseLong(videoId));
            } catch (NumberFormatException e) {
                Log.e(TAG, "Failed to parse ID as Long: " + videoId + ", using hashCode");
                playbackModel.setId((long) videoId.hashCode());
            }
        }
        
        // Set complete movie metadata
        playbackModel.setTitle(movieDetails.getTitle());
        playbackModel.setDescription(movieDetails.getDescription());
        playbackModel.setCategory("movie");
        playbackModel.setVideo(video);
        
        // Create video list for intent
        ArrayList<Video> videoListForIntent = new ArrayList<>();
        if (movieDetails.getVideos() != null) {
            videoListForIntent.addAll(movieDetails.getVideos());
        }
        playbackModel.setVideoList(videoListForIntent);
        
        playbackModel.setVideoUrl(video.getFileUrl());
        playbackModel.setVideoType(video.getFileType());
        playbackModel.setBgImageUrl(movieDetails.getPosterUrl());
        playbackModel.setCardImageUrl(movieDetails.getThumbnailUrl());
        playbackModel.setIsPaid(movieDetails.getIsPaid());
        
        // Set available metadata
        if (movieDetails.getRelease() != null) {
            playbackModel.setReleaseDate(movieDetails.getRelease());
        }
        if (movieDetails.getRuntime() != null) {
            playbackModel.setRuntime(movieDetails.getRuntime());
        }
        if (movieDetails.getVideoQuality() != null) {
            playbackModel.setVideoQuality(movieDetails.getVideoQuality());
        }
        playbackModel.setIsTvSeries(movieDetails.getIsTvseries());
        
        // Set genre info if available
        if (movieDetails.getGenre() != null) {
            playbackModel.setGenreList(movieDetails.getGenre());
        }
        
        // Debug logging for external player issue
        Log.e(TAG, "🎬 PLAYBACK DEBUG - Video ID: " + videoId);
        Log.e(TAG, "🎬 PLAYBACK DEBUG - Title: " + (playbackModel.getTitle() != null ? playbackModel.getTitle() : "NULL"));
        Log.e(TAG, "🎬 PLAYBACK DEBUG - VideoUrl: " + (playbackModel.getVideoUrl() != null ? playbackModel.getVideoUrl() : "NULL"));
        Log.e(TAG, "🎬 PLAYBACK DEBUG - VideoType: " + (playbackModel.getVideoType() != null ? playbackModel.getVideoType() : "NULL"));
        Log.e(TAG, "🎬 PLAYBACK DEBUG - Video Object: " + (video != null ? video.toString() : "NULL"));

        Intent intent = new Intent(getActivity(), PlayerActivity.class);
        intent.putExtra(com.files.codes.view.VideoPlaybackActivity.EXTRA_VIDEO, playbackModel);
        
        // 🔥 Add intent extras for enhanced watch history saving (same as VideoDetailsFragment)
        if (movieDetails != null) {
            Log.e(TAG, "🎬 HeroStyleMovieDetailsFragment adding intent extras:");
            Log.e(TAG, "  - ID: '" + videoId + "'");
            Log.e(TAG, "  - Title: '" + movieDetails.getTitle() + "'");
            Log.e(TAG, "  - Description: '" + movieDetails.getDescription() + "'");
            Log.e(TAG, "  - Type: '" + movieDetails.getType() + "'");
            
            intent.putExtra("id", videoId);
            intent.putExtra("type", movieDetails.getType() != null ? movieDetails.getType() : "movie");
            intent.putExtra("title", movieDetails.getTitle());
            intent.putExtra("description", movieDetails.getDescription());
            intent.putExtra("poster", movieDetails.getPosterUrl());
            intent.putExtra("thumbnail", movieDetails.getThumbnailUrl() != null ? movieDetails.getThumbnailUrl() : movieDetails.getPosterUrl());
            intent.putExtra("release_date", movieDetails.getRelease());
            intent.putExtra("imdb_rating", movieDetails.getImdbRating());
            intent.putExtra("runtime", movieDetails.getRuntime());
        } else {
            Log.e(TAG, "🎬 ERROR: movieDetails is NULL, cannot add intent extras!");
        }
        
        // TEMP: Remove bundle animation to match backup behavior
        startActivity(intent);
    }
    
    public void openServerDialog(final List<com.files.codes.model.Video> videos) {
        if (videos.size() != 0) {
            List<com.files.codes.model.Video> videoList = new ArrayList<>();
            videoList.clear();

            // Filter out embed videos
            for (com.files.codes.model.Video video : videos) {
                if (video.getFileType() != null && !video.getFileType().equalsIgnoreCase("embed")) {
                    videoList.add(video);
                }
            }

            android.app.AlertDialog.Builder builder = new android.app.AlertDialog.Builder(getActivity());
            View view = LayoutInflater.from(getActivity()).inflate(R.layout.layout_server_tv, null);
            androidx.recyclerview.widget.RecyclerView serverRv = view.findViewById(R.id.serverRv);
            com.files.codes.view.adapter.ServerAdapter serverAdapter = new com.files.codes.view.adapter.ServerAdapter(getActivity(), videoList, "movie");
            serverRv.setLayoutManager(new androidx.recyclerview.widget.LinearLayoutManager(getActivity()));
            serverRv.setHasFixedSize(true);
            serverRv.setAdapter(serverAdapter);

            Button closeBt = view.findViewById(R.id.close_bt);

            builder.setView(view);
            final android.app.AlertDialog dialog = builder.create();
            dialog.show();

            closeBt.setOnClickListener(v -> dialog.dismiss());

            serverAdapter.setOnItemClickListener(new com.files.codes.view.adapter.ServerAdapter.OnItemClickListener() {
                @Override
                public void onItemClick(View view, com.files.codes.model.Video obj, int position, com.files.codes.view.adapter.ServerAdapter.OriginalViewHolder holder) {
                    Bundle bundle = androidx.core.app.ActivityOptionsCompat.makeSceneTransitionAnimation(getActivity()).toBundle();

                    com.files.codes.model.PlaybackModel playbackModel = new com.files.codes.model.PlaybackModel();
                    
                    // Set movieId for watch history tracking
                    playbackModel.setMovieId(videoId);
                    
                    // Handle phim4k and free1 IDs differently since they're not numeric
                    if (videoId.startsWith("phim4k_") || videoId.startsWith("free1_")) {
                        // For phim4k/free1 content, use hash code as ID
                        playbackModel.setId((long) videoId.hashCode());
                    } else {
                        // For regular content, parse as Long
                        try {
                            playbackModel.setId(Long.parseLong(videoId));
                        } catch (NumberFormatException e) {
                            Log.e(TAG, "Failed to parse ID as Long: " + videoId + ", using hashCode");
                            playbackModel.setId((long) videoId.hashCode());
                        }
                    }
                    
                    // Set complete movie metadata
                    playbackModel.setTitle(movieDetails.getTitle());
                    playbackModel.setDescription(movieDetails.getDescription());
                    playbackModel.setCategory("movie");
                    playbackModel.setVideo(obj);
                    
                    // Create video list for intent
                    ArrayList<Video> videoListForIntent = new ArrayList<>();
                    if (movieDetails.getVideos() != null) {
                        videoListForIntent.addAll(movieDetails.getVideos());
                    }
                    playbackModel.setVideoList(videoListForIntent);
                    
                    playbackModel.setVideoUrl(obj.getFileUrl());
                    playbackModel.setVideoType(obj.getFileType());
                    playbackModel.setBgImageUrl(movieDetails.getPosterUrl());
                    playbackModel.setCardImageUrl(movieDetails.getThumbnailUrl());
                    playbackModel.setIsPaid(movieDetails.getIsPaid());
                    
                    // Set available metadata
                    if (movieDetails.getRelease() != null) {
                        playbackModel.setReleaseDate(movieDetails.getRelease());
                    }
                    if (movieDetails.getRuntime() != null) {
                        playbackModel.setRuntime(movieDetails.getRuntime());
                    }
                    if (movieDetails.getVideoQuality() != null) {
                        playbackModel.setVideoQuality(movieDetails.getVideoQuality());
                    }
                    playbackModel.setIsTvSeries(movieDetails.getIsTvseries());
                    
                    // Set genre info if available
                    if (movieDetails.getGenre() != null) {
                        playbackModel.setGenreList(movieDetails.getGenre());
                    }

                    Intent intent = new Intent(getActivity(), PlayerActivity.class);
                    intent.putExtra(com.files.codes.view.VideoPlaybackActivity.EXTRA_VIDEO, playbackModel);
                    
                    // 🔥 Add intent extras for enhanced watch history saving (server selection)
                    if (movieDetails != null) {
                        Log.e(TAG, "🎬 HeroStyleMovieDetailsFragment (server selection) adding intent extras:");
                        Log.e(TAG, "  - ID: '" + videoId + "'");
                        Log.e(TAG, "  - Title: '" + movieDetails.getTitle() + "'");
                        Log.e(TAG, "  - Server: '" + obj.getLabel() + "'");
                        Log.e(TAG, "  - Type: '" + movieDetails.getType() + "'");
                        
                        intent.putExtra("id", videoId);
                        intent.putExtra("type", movieDetails.getType() != null ? movieDetails.getType() : "movie");
                        intent.putExtra("title", movieDetails.getTitle() + " - " + obj.getLabel());
                        intent.putExtra("description", movieDetails.getDescription());
                        intent.putExtra("poster", movieDetails.getPosterUrl());
                        intent.putExtra("thumbnail", movieDetails.getThumbnailUrl() != null ? movieDetails.getThumbnailUrl() : movieDetails.getPosterUrl());
                        intent.putExtra("release_date", movieDetails.getRelease());
                        intent.putExtra("imdb_rating", movieDetails.getImdbRating());
                        intent.putExtra("runtime", movieDetails.getRuntime());
                    } else {
                        Log.e(TAG, "🎬 ERROR: movieDetails is NULL in server selection, cannot add intent extras!");
                    }
                    
                    // TEMP: Remove bundle animation to match backup behavior
                    startActivity(intent);
                    dialog.dismiss();
                }
            });
        }
    }
    
    private void toggleFavorite() {
        // Check if user is logged in before allowing favorites
        if (!PreferenceUtils.isLoggedIn(getContext())) {
            new ToastMsg(getActivity()).toastIconError("Bạn cần đăng nhập để sử dụng tính năng yêu thích");
            return;
        }
        
        if (isFavorite) {
            removeFromFav();
        } else {
            addToFav();
        }
    }
    

    
    private void getPhim4kData(String vtype, final String vId) {
        Log.d(TAG, "Getting phim4k data for: " + vId);
        
        // Extract actual phim4k ID from full ID (remove "phim4k_" prefix)
        String actualId = vId.replace("phim4k_", "");
        Log.d(TAG, "Getting phim4k data for actual ID: " + actualId);

        Phim4kClient phim4kClient = Phim4kClient.getInstance();
        phim4kClient.getMovieDetailWithMovie(actualId, new Phim4kClient.Phim4kDetailWithMovieCallback() {
            @Override
            public void onSuccess(com.files.codes.model.VideoContent videoContent, Phim4kMovie phim4kMovie) {
                if (getActivity() == null) return;
                
                Log.d(TAG, "Phim4k details received: " + videoContent.getTitle());
                
                getActivity().runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        // Create a MovieSingleDetails object with phim4k data
                        movieDetails = new MovieSingleDetails();
                        movieDetails.setTitle(videoContent.getTitle());
                        movieDetails.setDescription(videoContent.getDescription());
                        movieDetails.setPosterUrl(videoContent.getThumbnailUrl());
                        movieDetails.setRelease(videoContent.getRelease());
                        movieDetails.setType(vtype);
                        movieDetails.setVideosId(vId);
                        movieDetails.setIsPaid("0"); // Phim4k content is always free
                        
                        // Convert phim4k episodes to Video objects for playback
                        if (phim4kMovie != null && phim4kMovie.getEpisodes() != null) {
                            List<Video> videos = new ArrayList<>();
                            for (Phim4kEpisodeServer episodeServer : phim4kMovie.getEpisodes()) {
                                if (episodeServer.getItems() != null) {
                                    for (Phim4kEpisode episode : episodeServer.getItems()) {
                                        Video video = new Video();
                                        video.setLabel(episodeServer.getServerName() + " - " + episode.getName());
                                        video.setFileUrl(episode.getLink());
                                        video.setVideoFileId(episode.getSlug());
                                        video.setFileType("m3u8"); // Assuming HLS format
                                        video.setSubtitle(new ArrayList<>()); // Initialize empty subtitle list
                                        videos.add(video);
                                    }
                                }
                            }
                            movieDetails.setVideos(videos);
                            Log.d(TAG, "Created " + videos.size() + " video links for phim4k content");
                        }
                        
                        displayMovieDetails();
                    }
                });
            }

            @Override
            public void onError(String error) {
                Log.e(TAG, "Phim4k API error: " + error);
                if (getActivity() != null) {
                    getActivity().runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            // Show error or fallback
                        }
                    });
                }
            }
        });
    }
    
    private void setHeroInfoTags() {
        // Set year
        if (movieDetails.getRelease() != null && movieDetails.getRelease().length() >= 4) {
            heroYear.setText(movieDetails.getRelease().substring(0, 4));
            heroYear.setVisibility(View.VISIBLE);
        } else {
            heroYear.setVisibility(View.GONE);
        }
        
        // Set TMDB rating from API
        if (movieDetails.getImdbRating() != null && !movieDetails.getImdbRating().isEmpty()) {
            heroImdbRating.setText(movieDetails.getImdbRating());
            heroImdbRating.setVisibility(View.VISIBLE);
        } else {
            heroImdbRating.setVisibility(View.GONE);
        }
        
        // Set quality
        if (movieDetails.getVideoQuality() != null && !movieDetails.getVideoQuality().isEmpty()) {
            heroQuality.setText(movieDetails.getVideoQuality());
            heroQuality.setVisibility(View.VISIBLE);
        } else {
            heroQuality.setText("HD");
            heroQuality.setVisibility(View.VISIBLE);
        }
    }

    private void setupGenreTags() {
        if (movieDetails.getGenre() != null && !movieDetails.getGenre().isEmpty()) {
            // Create simple adapter for genres
            GenreAdapter adapter = new GenreAdapter(movieDetails.getGenre());
            genreTagsRecycler.setAdapter(adapter);
            genreTagsRecycler.setVisibility(View.VISIBLE);
        } else {
            genreTagsRecycler.setVisibility(View.GONE);
        }
    }

    // Simple Genre Adapter class
    private class GenreAdapter extends RecyclerView.Adapter<GenreAdapter.GenreViewHolder> {
        private List<com.files.codes.model.Genre> genres;
        
        GenreAdapter(List<com.files.codes.model.Genre> genres) {
            this.genres = genres;
        }
        
        @NonNull
        @Override
        public GenreViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_genre_tag, parent, false);
            return new GenreViewHolder(view);
        }
        
        @Override
        public void onBindViewHolder(@NonNull GenreViewHolder holder, int position) {
            com.files.codes.model.Genre genre = genres.get(position);
            holder.genreText.setText(genre.getName());
        }
        
        @Override
        public int getItemCount() {
            return genres != null ? genres.size() : 0;
        }
        
        class GenreViewHolder extends RecyclerView.ViewHolder {
            TextView genreText;
            
            GenreViewHolder(@NonNull View itemView) {
                super(itemView);
                // Since we're using TextView as root in item_genre_tag.xml
                genreText = (TextView) itemView;
                
                // Add click listener to open genre activity
                itemView.setOnClickListener(v -> {
                    Log.d(TAG, "Genre tag clicked!");
                    int position = getAdapterPosition();
                    Log.d(TAG, "Adapter position: " + position);
                    
                    if (position != RecyclerView.NO_POSITION && genres != null && position < genres.size()) {
                        com.files.codes.model.Genre genre = genres.get(position);
                        Log.d(TAG, "Genre at position " + position + ": " + (genre != null ? genre.getName() : "null"));
                        openGenreActivity(genre);
                    } else {
                        Log.e(TAG, "Invalid position or genres list. Position: " + position + ", Genres size: " + (genres != null ? genres.size() : "null"));
                    }
                });
                
                // Add focus change listener for debugging
                itemView.setOnFocusChangeListener((v, hasFocus) -> {
                    Log.d(TAG, "Genre tag focus changed: " + hasFocus + " at position: " + getAdapterPosition());
                });
            }
        }
    }

    // Cast/Crew data class
    private static class CastCrewItem {
        String name;
        String role;
        String imageUrl;
        
        CastCrewItem(String name, String role, String imageUrl) {
            this.name = name;
            this.role = role;
            this.imageUrl = imageUrl;
        }
    }

    interface OnCastClickListener {
        void onCastClick(CastCrewItem cast);
    }
    
    // Cast/Crew Adapter
    private class CastCrewAdapter extends RecyclerView.Adapter<CastCrewAdapter.CastCrewViewHolder> {
        private List<CastCrewItem> items;
        private OnCastClickListener listener;
        
        CastCrewAdapter(List<CastCrewItem> items, OnCastClickListener listener) {
            this.items = items;
            this.listener = listener;
        }
        
        @NonNull
        @Override
        public CastCrewViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_cast_member, parent, false);
            return new CastCrewViewHolder(view);
        }
        
        @Override
        public void onBindViewHolder(@NonNull CastCrewViewHolder holder, int position) {
            CastCrewItem item = items.get(position);
            holder.castName.setText(item.name);
            
            // Load image if available
            if (item.imageUrl != null && !item.imageUrl.isEmpty()) {
                Picasso.get()
                    .load(item.imageUrl)
                    .placeholder(R.drawable.circular_image_background)
                    .error(R.drawable.circular_image_background)
                    .transform(new CircleTransform())
                    .into(holder.castPhoto);
            } else {
                // Use default avatar for directors or when no image available
                holder.castPhoto.setImageResource(R.drawable.circular_image_background);
            }
        }
        
        @Override
        public int getItemCount() {
            return items != null ? items.size() : 0;
        }
        
        class CastCrewViewHolder extends RecyclerView.ViewHolder {
            ImageView castPhoto;
            TextView castName;
            
            CastCrewViewHolder(@NonNull View itemView) {
                super(itemView);
                castPhoto = itemView.findViewById(R.id.cast_photo);
                castName = itemView.findViewById(R.id.cast_name);
                
                // Set click listener for the whole item
                itemView.setOnClickListener(v -> {
                    if (listener != null && getAdapterPosition() != RecyclerView.NO_POSITION) {
                        listener.onCastClick(items.get(getAdapterPosition()));
                    }
                });
                
                // Focus listener to show content when focusing on cast member
                itemView.setOnFocusChangeListener((v, hasFocus) -> {
                    if (hasFocus) {
                        showHideableContent();
                    }
                });
            }
        }
    }

    // Circle Transform for Picasso
    private static class CircleTransform implements com.squareup.picasso.Transformation {
        @Override
        public android.graphics.Bitmap transform(android.graphics.Bitmap source) {
            int size = Math.min(source.getWidth(), source.getHeight());
            
            int x = (source.getWidth() - size) / 2;
            int y = (source.getHeight() - size) / 2;
            
            android.graphics.Bitmap squaredBitmap = android.graphics.Bitmap.createBitmap(source, x, y, size, size);
            if (squaredBitmap != source) {
                source.recycle();
            }
            
            android.graphics.Bitmap bitmap = android.graphics.Bitmap.createBitmap(size, size, source.getConfig());
            
            android.graphics.Canvas canvas = new android.graphics.Canvas(bitmap);
            android.graphics.Paint paint = new android.graphics.Paint();
            android.graphics.BitmapShader shader = new android.graphics.BitmapShader(squaredBitmap, 
                android.graphics.Shader.TileMode.CLAMP, android.graphics.Shader.TileMode.CLAMP);
            paint.setShader(shader);
            paint.setAntiAlias(true);
            
            float r = size / 2f;
            canvas.drawCircle(r, r, r, paint);
            
            squaredBitmap.recycle();
            return bitmap;
        }
        
        @Override
        public String key() {
            return "circle";
        }
    }

    private void setMovieTitle() {
        Log.d(TAG, "setMovieTitle called - movieDetails: " + (movieDetails != null ? "not null" : "null"));
        if (movieDetails != null && movieDetails.getTitle() != null) {
            String rawTitle = movieDetails.getTitle();
            Log.d(TAG, "Raw title: " + rawTitle);
            
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
                
                Log.d(TAG, "Pattern matched - Vietnamese: " + vietnameseTitle + ", English: " + englishTitle);
                // Use Vietnamese title as main title
                movieTitle.setText(vietnameseTitle);
            } else {
                // Fallback: if no pattern match, use formatted title
                String formattedTitle = formatMovieTitle(rawTitle);
                Log.d(TAG, "Pattern not matched - Formatted title: " + formattedTitle);
                if (formattedTitle.contains("\n")) {
                    String[] parts = formattedTitle.split("\n", 2);
                    movieTitle.setText(parts[0]);
                    Log.d(TAG, "Using first part: " + parts[0]);
                } else {
                    movieTitle.setText(formattedTitle);
                    Log.d(TAG, "Using formatted title: " + formattedTitle);
                }
            }
            movieTitle.setVisibility(View.VISIBLE);
            Log.d(TAG, "Title set and visible");
        } else {
            Log.d(TAG, "No title available - hiding title view");
            movieTitle.setVisibility(View.GONE);
        }
    }

    private void setMovieDescription() {
        if (movieDetails.getDescription() != null && !movieDetails.getDescription().isEmpty()) {
            String description = movieDetails.getDescription().trim();
            
            // Smart truncate: cut at word boundary for better readability (max 150 chars for hero style)
            if (description.length() > 150) {
                description = description.substring(0, 150);
                int lastSpace = description.lastIndexOf(' ');
                if (lastSpace > 100) { // Ensure we don't cut too early
                    description = description.substring(0, lastSpace);
                }
                description += "...";
            }
            
            movieDescription.setText(description);
            movieDescription.setVisibility(View.VISIBLE);
        } else {
            movieDescription.setVisibility(View.GONE);
        }
    }

    /**
     * Format movie title like hero banner
     * Splits "Vietnamese Name (year) Foreign Name" into two lines
     */
    private String formatMovieTitle(String rawTitle) {
        if (rawTitle == null || rawTitle.trim().isEmpty()) {
            return "";
        }
        
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

    /**
     * Add movie to favorites
     */
    private void addToFav() {
        if (videoId == null) return;
        
        Log.d(TAG, "Adding to favorites - VideoId: " + videoId + ", UserId: " + userId);
        ApiService apiService = RetrofitClient.getRetrofitInstance().create(ApiService.class);
        Call<FavoriteModel> call = apiService.addToFavorite(AppConfig.API_KEY, userId, videoId);

        call.enqueue(new Callback<FavoriteModel>() {
            @Override
            public void onResponse(Call<FavoriteModel> call, Response<FavoriteModel> response) {
                Log.d(TAG, "AddToFav response code: " + response.code());
                if (getActivity() != null && isAdded()) {
                    if (response.code() == 200 && response.body() != null) {
                        Log.d(TAG, "AddToFav response status: " + response.body().getStatus());
                        if (response.body().getStatus().equalsIgnoreCase("success")) {
                            isFavorite = true;
                            updateFavoriteButton();
                            new ToastMsg(getActivity()).toastIconSuccess(response.body().getMessage());
                        } else {
                            // Show server error message or default
                            String errorMsg = response.body().getMessage();
                            if (errorMsg == null || errorMsg.isEmpty()) {
                                errorMsg = "Bạn chưa đăng nhập";
                            }
                            new ToastMsg(getActivity()).toastIconError(errorMsg);
                        }
                    } else {
                        Log.e(TAG, "AddToFav failed - Code: " + response.code() + ", Body: " + (response.body() != null));
                        new ToastMsg(getActivity()).toastIconError("Không thể thêm vào yêu thích");
                    }
                }
            }

            @Override
            public void onFailure(Call<FavoriteModel> call, Throwable t) {
                Log.e(TAG, "AddToFav API failure", t);
                if (getActivity() != null && isAdded()) {
                    new ToastMsg(getActivity()).toastIconError("Không thể thêm vào yêu thích");
                }
            }
        });
    }

    /**
     * Remove movie from favorites
     */
    private void removeFromFav() {
        if (videoId == null) return;
        
        Log.d(TAG, "Removing from favorites - VideoId: " + videoId + ", UserId: " + userId);
        ApiService apiService = RetrofitClient.getRetrofitInstance().create(ApiService.class);
        Call<FavoriteModel> call = apiService.removeFromFavorite(AppConfig.API_KEY, userId, videoId);

        call.enqueue(new Callback<FavoriteModel>() {
            @Override
            public void onResponse(Call<FavoriteModel> call, Response<FavoriteModel> response) {
                Log.d(TAG, "RemoveFromFav response code: " + response.code());
                if (getActivity() != null && isAdded()) {
                    if (response.code() == 200 && response.body() != null) {
                        Log.d(TAG, "RemoveFromFav response status: " + response.body().getStatus());
                        if (response.body().getStatus().equalsIgnoreCase("success")) {
                            isFavorite = false;
                            updateFavoriteButton();
                            new ToastMsg(getActivity()).toastIconSuccess(response.body().getMessage());
                        } else {
                            // Show server error message or default
                            String errorMsg = response.body().getMessage();
                            if (errorMsg == null || errorMsg.isEmpty()) {
                                errorMsg = "Bạn chưa đăng nhập";
                            }
                            new ToastMsg(getActivity()).toastIconError(errorMsg);
                        }
                    } else {
                        Log.e(TAG, "RemoveFromFav failed - Code: " + response.code() + ", Body: " + (response.body() != null));
                        new ToastMsg(getActivity()).toastIconError("Không thể xóa khỏi yêu thích");
                    }
                }
            }

            @Override
            public void onFailure(Call<FavoriteModel> call, Throwable t) {
                Log.e(TAG, "RemoveFromFav API failure", t);
                if (getActivity() != null && isAdded()) {
                    new ToastMsg(getActivity()).toastIconError("Không thể xóa khỏi yêu thích");
                }
            }
        });
    }

    /**
     * Check favorite status
     */
    private void getFavStatus() {
        if (videoId == null) return;
        
        // Only check favorite status if user is logged in
        if (!PreferenceUtils.isLoggedIn(getContext())) {
            Log.d(TAG, "User not logged in, skipping favorite status check");
            isFavorite = false;
            updateFavoriteButton();
            return;
        }
        
        Log.d(TAG, "Checking favorite status - VideoId: " + videoId + ", UserId: " + userId);
        ApiService apiService = RetrofitClient.getRetrofitInstance().create(ApiService.class);
        Call<FavoriteModel> call = apiService.verifyFavoriteList(AppConfig.API_KEY, userId, videoId);

        call.enqueue(new Callback<FavoriteModel>() {
            @Override
            public void onResponse(Call<FavoriteModel> call, Response<FavoriteModel> response) {
                Log.d(TAG, "GetFavStatus response code: " + response.code());
                if (getActivity() != null && isAdded()) {
                    if (response.code() == 200 && response.body() != null) {
                        Log.d(TAG, "GetFavStatus response status: " + response.body().getStatus());
                        isFavorite = response.body().getStatus().equalsIgnoreCase("success");
                        Log.d(TAG, "Set isFavorite to: " + isFavorite);
                        updateFavoriteButton();
                    } else {
                        Log.e(TAG, "GetFavStatus failed - Code: " + response.code() + ", Body: " + (response.body() != null));
                        // Silent failure, just set default state
                        isFavorite = false;
                        updateFavoriteButton();
                    }
                }
            }

            @Override
            public void onFailure(Call<FavoriteModel> call, Throwable t) {
                // Silent failure for status check
                Log.e(TAG, "Failed to check favorite status", t);
                if (getActivity() != null && isAdded()) {
                    isFavorite = false;
                    updateFavoriteButton();
                }
            }
        });
    }

    /**
     * Update favorite button appearance
     */
    private void updateFavoriteButton() {
        if (favoriteButton != null) {
            if (isFavorite) {
                favoriteButton.setText("❤");
            } else {
                favoriteButton.setText("♡");
            }
        }
    }

    /**
     * Setup seasons and episodes for TV series
     */
    private void setupSeasonsAndEpisodes() {
        if (movieDetails == null) return;
        
        // Check if this is a TV series and has seasons
        if ("1".equals(movieDetails.getIsTvseries()) && movieDetails.getSeason() != null && !movieDetails.getSeason().isEmpty()) {
            allSeasons = movieDetails.getSeason();
            currentSeasonIndex = 0;
            currentEpisodePage = 0;
            
            // Show season selector and episodes
            seasonSelectorLayout.setVisibility(View.VISIBLE);
            episodesRecycler.setVisibility(View.VISIBLE);
            episodesPaginationLayout.setVisibility(View.VISIBLE);
            
            // Update season selector button
            updateSeasonSelectorButton();
            
            // Load episodes for current season
            loadEpisodesForCurrentSeason();
        } else {
            // Hide season/episodes UI for movies
            seasonSelectorLayout.setVisibility(View.GONE);
            episodesRecycler.setVisibility(View.GONE);
            episodesPaginationLayout.setVisibility(View.GONE);
        }
    }
    
    /**
     * Update season selector button text
     */
    private void updateSeasonSelectorButton() {
        if (allSeasons != null && currentSeasonIndex < allSeasons.size()) {
            String seasonName = allSeasons.get(currentSeasonIndex).getSeasonsName();
            seasonSelectorButton.setText(seasonName);
        }
    }
    
    /**
     * Show season selector dialog
     */
    private void showSeasonSelectorDialog() {
        if (allSeasons == null || allSeasons.isEmpty()) return;
        
        // Create season names array for dialog
        String[] seasonNames = new String[allSeasons.size()];
        for (int i = 0; i < allSeasons.size(); i++) {
            seasonNames[i] = allSeasons.get(i).getSeasonsName();
        }
        
        // Show selection dialog
        android.app.AlertDialog.Builder builder = new android.app.AlertDialog.Builder(getActivity());
        builder.setTitle("Chọn Mùa Phim");
        builder.setItems(seasonNames, (dialog, which) -> {
            // User selected a season
            currentSeasonIndex = which;
            currentEpisodePage = 0;
            
            // Update UI
            updateSeasonSelectorButton();
            loadEpisodesForCurrentSeason();
        });
        
        builder.show();
    }
    
    /**
     * Load episodes for current season
     */
    private void loadEpisodesForCurrentSeason() {
        if (allSeasons == null || currentSeasonIndex >= allSeasons.size()) return;
        
        Season currentSeason = allSeasons.get(currentSeasonIndex);
        List<Episode> allEpisodes = currentSeason.getEpisodes();
        
        if (allEpisodes == null || allEpisodes.isEmpty()) {
            new ToastMsg(getActivity()).toastIconError("Không tìm thấy tập phim nào cho mùa này");
            return;
        }
        
        // Calculate pagination
        int startIndex = currentEpisodePage * EPISODES_PER_PAGE;
        int endIndex = Math.min(startIndex + EPISODES_PER_PAGE, allEpisodes.size());
        
        // Get episodes for current page
        List<Episode> pageEpisodes = allEpisodes.subList(startIndex, endIndex);
        
        // Set up adapter
        EpisodesAdapter adapter = new EpisodesAdapter(pageEpisodes, new OnEpisodeClickListener() {
            @Override
            public void onEpisodeClick(Episode episode, int position) {
                playEpisode(episode, position);
            }
        });
        
        episodesRecycler.setAdapter(adapter);
        
        // Update pagination UI
        updateEpisodesPaginationUI(allEpisodes.size());
    }
    
    /**
     * Play selected episode
     */
    private void playEpisode(Episode episode, int position) {
        Log.d(TAG, "Playing episode: " + episode.getEpisodesName());
        
        Bundle bundle = androidx.core.app.ActivityOptionsCompat.makeSceneTransitionAnimation(getActivity()).toBundle();
        
        PlaybackModel playbackModel = new PlaybackModel();
        
        // Set episode metadata
        playbackModel.setId(Long.parseLong(episode.getEpisodesId()));
        playbackModel.setMovieId(videoId); // Main series ID for watch history
        playbackModel.setTitle(movieDetails.getTitle() + " - " + episode.getEpisodesName());
        playbackModel.setDescription(movieDetails.getDescription());
        playbackModel.setCategory("tvseries");
        
        // Create video object from episode
        Video episodeVideo = new Video();
        episodeVideo.setFileUrl(episode.getFileUrl());
        episodeVideo.setFileType(episode.getFileType() != null ? episode.getFileType() : "mp4");
        episodeVideo.setLabel(episode.getEpisodesName());
        // TEMP: Disable subtitle to fix external player black screen
        // episodeVideo.setSubtitle(episode.getSubtitle());
        episodeVideo.setSubtitle(new ArrayList<>());
        
        playbackModel.setVideo(episodeVideo);
        playbackModel.setVideoUrl(episode.getFileUrl());
        playbackModel.setVideoType(episode.getFileType() != null ? episode.getFileType() : "mp4");
        playbackModel.setBgImageUrl(movieDetails.getPosterUrl());
        playbackModel.setCardImageUrl(episode.getImageUrl() != null ? episode.getImageUrl() : movieDetails.getThumbnailUrl());
        playbackModel.setIsPaid(movieDetails.getIsPaid());
        playbackModel.setIsTvSeries("1");
        
        // Set episode navigation data for next/previous episode buttons
        if (movieDetails.getSeason() != null && !movieDetails.getSeason().isEmpty()) {
            playbackModel.setAllSeasons(movieDetails.getSeason());
            Log.d(TAG, "Set allSeasons, total seasons: " + movieDetails.getSeason().size());
            
            // Find current season and episode index
            boolean foundEpisode = false;
            for (int seasonIndex = 0; seasonIndex < movieDetails.getSeason().size(); seasonIndex++) {
                Season season = movieDetails.getSeason().get(seasonIndex);
                if (season.getEpisodes() != null) {
                    for (int episodeIndex = 0; episodeIndex < season.getEpisodes().size(); episodeIndex++) {
                        Episode ep = season.getEpisodes().get(episodeIndex);
                        if (ep.getEpisodesId().equals(episode.getEpisodesId())) {
                            playbackModel.setCurrentSeasonIndex(seasonIndex);
                            playbackModel.setCurrentEpisodeIndex(episodeIndex);
                            playbackModel.setTotalEpisodesInSeason(season.getEpisodes().size());
                            foundEpisode = true;
                            Log.d(TAG, "Found episode in season " + seasonIndex + ", episode " + episodeIndex + 
                                    ", total episodes in season: " + season.getEpisodes().size());
                            break;
                        }
                    }
                    if (foundEpisode) break;
                }
            }
            
            if (!foundEpisode) {
                Log.w(TAG, "Episode not found in seasons data for navigation: " + episode.getEpisodesId());
            }
        } else {
            Log.w(TAG, "No season data available for episode navigation");
        }
        
        Intent intent = new Intent(getActivity(), PlayerActivity.class);
        intent.putExtra(com.files.codes.view.VideoPlaybackActivity.EXTRA_VIDEO, playbackModel);
        
        // 🔥 Add intent extras for enhanced watch history saving (episode playback)
        if (movieDetails != null) {
            Log.e(TAG, "🎬 HeroStyleMovieDetailsFragment (playEpisode) adding intent extras:");
            Log.e(TAG, "  - ID: '" + videoId + "'");
            Log.e(TAG, "  - Title: '" + movieDetails.getTitle() + "'");
            Log.e(TAG, "  - Episode: '" + episode.getEpisodesName() + "'");
            Log.e(TAG, "  - Type: '" + movieDetails.getType() + "'");
            
            intent.putExtra("id", videoId);
            intent.putExtra("type", movieDetails.getType() != null ? movieDetails.getType() : "tvseries");
            intent.putExtra("title", movieDetails.getTitle() + " - " + episode.getEpisodesName());
            intent.putExtra("description", movieDetails.getDescription());
            intent.putExtra("poster", movieDetails.getPosterUrl());
            intent.putExtra("thumbnail", episode.getImageUrl() != null ? episode.getImageUrl() : movieDetails.getThumbnailUrl());
            intent.putExtra("release_date", movieDetails.getRelease());
            intent.putExtra("imdb_rating", movieDetails.getImdbRating());
            intent.putExtra("runtime", movieDetails.getRuntime());
        } else {
            Log.e(TAG, "🎬 ERROR: movieDetails is NULL in playEpisode, cannot add intent extras!");
        }
        
        // TEMP: Remove bundle animation to match backup behavior
        startActivity(intent);
    }

    // Episodes Adapter
    private class EpisodesAdapter extends RecyclerView.Adapter<EpisodesAdapter.EpisodeViewHolder> {
        private List<Episode> episodes;
        private OnEpisodeClickListener listener;
        
        EpisodesAdapter(List<Episode> episodes, OnEpisodeClickListener listener) {
            this.episodes = episodes;
            this.listener = listener;
        }
        
        @NonNull
        @Override
        public EpisodeViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_episode_list, parent, false);
            return new EpisodeViewHolder(view);
        }
        
        @Override
        public void onBindViewHolder(@NonNull EpisodeViewHolder holder, int position) {
            Episode episode = episodes.get(position);
            holder.episodeName.setText(episode.getEpisodesName());
            
            // Load episode thumbnail if available
            if (episode.getImageUrl() != null && !episode.getImageUrl().isEmpty()) {
                Picasso.get()
                    .load(episode.getImageUrl())
                    .placeholder(R.drawable.default_background)
                    .error(R.drawable.default_background)
                    .into(holder.episodeThumbnail);
            } else {
                // Use series poster as fallback
                Picasso.get()
                    .load(movieDetails.getPosterUrl())
                    .placeholder(R.drawable.default_background)
                    .error(R.drawable.default_background)
                    .into(holder.episodeThumbnail);
            }
        }
        
        @Override
        public int getItemCount() {
            return episodes != null ? episodes.size() : 0;
        }
        
        class EpisodeViewHolder extends RecyclerView.ViewHolder {
            ImageView episodeThumbnail;
            TextView episodeName;
            
            EpisodeViewHolder(@NonNull View itemView) {
                super(itemView);
                episodeThumbnail = itemView.findViewById(R.id.episode_thumbnail);
                episodeName = itemView.findViewById(R.id.episode_name);
                
                itemView.setOnClickListener(v -> {
                    if (listener != null && getAdapterPosition() != RecyclerView.NO_POSITION) {
                        listener.onEpisodeClick(episodes.get(getAdapterPosition()), getAdapterPosition());
                    }
                });
                
                // Focus listener for hiding hideable content when focusing on episodes
                itemView.setOnFocusChangeListener((v, hasFocus) -> {
                    if (hasFocus) {
                        hideHideableContent();
                    }
                });
            }
        }
    }

    // Episode click listener interface
    interface OnEpisodeClickListener {
        void onEpisodeClick(Episode episode, int position);
    }

    /**
     * Hide hideable content (description, buttons, cast) with animation
     * Keep movie title, year, rating, quality, genre tags visible
     */
    private void hideHideableContent() {
        if (isHideableContentHidden) return;
        
        isHideableContentHidden = true;
        
        // Animate hideable content container out
        hideableContentContainer.animate()
            .alpha(0f)
            .translationY(-50f)
            .setDuration(300)
            .withEndAction(() -> hideableContentContainer.setVisibility(View.GONE));
    }

    /**
     * Show hideable content (description, buttons, cast) with animation
     */
    private void showHideableContent() {
        if (!isHideableContentHidden) return;
        
        isHideableContentHidden = false;
        
        // Animate hideable content container in
        hideableContentContainer.setVisibility(View.VISIBLE);
        hideableContentContainer.setAlpha(0f);
        hideableContentContainer.setTranslationY(-50f);
        hideableContentContainer.animate()
            .alpha(1f)
            .translationY(0f)
            .setDuration(300);
    }
    
    /**
     * Update episodes pagination UI
     */
    private void updateEpisodesPaginationUI(int totalEpisodes) {
        if (totalEpisodes <= EPISODES_PER_PAGE) {
            // Hide pagination if all episodes fit in one page
            episodesPaginationLayout.setVisibility(View.GONE);
            return;
        }
        
        episodesPaginationLayout.setVisibility(View.VISIBLE);
        
        int totalPages = (int) Math.ceil((double) totalEpisodes / EPISODES_PER_PAGE);
        int currentPage = currentEpisodePage + 1; // Display as 1-based
        
        // Update page info
        episodesPageInfo.setText(currentPage + " / " + totalPages);
        
        // Update button states
        episodesPrevButton.setEnabled(currentEpisodePage > 0);
        episodesNextButton.setEnabled(currentEpisodePage < totalPages - 1);
    }
    
    /**
     * Go to previous episodes page
     */
    private void goToPreviousEpisodesPage() {
        if (currentEpisodePage > 0) {
            currentEpisodePage--;
            loadEpisodesForCurrentSeason();
        }
    }
    
    /**
     * Go to next episodes page
     */
    private void goToNextEpisodesPage() {
        if (allSeasons != null && currentSeasonIndex < allSeasons.size()) {
            Season currentSeason = allSeasons.get(currentSeasonIndex);
            List<Episode> allEpisodes = currentSeason.getEpisodes();
            
            if (allEpisodes != null) {
                int totalPages = (int) Math.ceil((double) allEpisodes.size() / EPISODES_PER_PAGE);
                if (currentEpisodePage < totalPages - 1) {
                    currentEpisodePage++;
                    loadEpisodesForCurrentSeason();
                }
            }
        }
    }
    
    /**
     * Open genre activity to show movies of selected genre
     */
    private void openGenreActivity(com.files.codes.model.Genre genre) {
        try {
            Log.d(TAG, "Genre clicked - Name: " + genre.getName() + ", ID: " + genre.getGenreId());
            
            // Check if genre has valid data
            if (genre.getName() == null || genre.getName().isEmpty()) {
                Log.e(TAG, "Genre name is null or empty");
                new ToastMsg(getActivity()).toastIconError("Thông tin thể loại không hợp lệ");
                return;
            }
            
            Intent intent = new Intent(getActivity(), com.files.codes.view.ItemGenreActivity.class);
            intent.putExtra("id", genre.getGenreId());
            intent.putExtra("title", genre.getName());
            
            Log.d(TAG, "Starting ItemGenreActivity with genre: " + genre.getName() + " (ID: " + genre.getGenreId() + ")");
            
            // Try simple startActivity first without bundle
            startActivity(intent);
            
        } catch (Exception e) {
            Log.e(TAG, "Error opening genre activity", e);
            new ToastMsg(getActivity()).toastIconError("Không thể mở danh sách thể loại: " + e.getMessage());
        }
    }
}