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

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.files.codes.AppConfig;
import com.files.codes.R;
import com.files.codes.database.DatabaseHelper;
import com.files.codes.model.Genre;
import com.files.codes.model.PlaybackModel;
import com.files.codes.model.Video;
import com.files.codes.model.movieDetails.CastAndCrew;
import com.files.codes.model.movieDetails.Director;
import com.files.codes.model.movieDetails.MovieSingleDetails;
import com.files.codes.model.api.ApiService;
import com.files.codes.utils.RetrofitClient;
import com.files.codes.utils.ToastMsg;
import com.files.codes.utils.Phim4kClient;
import com.files.codes.model.phim4k.Phim4kEpisodeServer;
import com.files.codes.model.phim4k.Phim4kEpisode;
import com.files.codes.model.phim4k.Phim4kMovie;
import com.files.codes.view.PlayerActivity;
import com.files.codes.view.adapter.ServerAdapter;
import com.squareup.picasso.Picasso;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

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
    private TextView releaseDate;
    private TextView movieDescription;
    private TextView directorTv;
    private TextView castTv;
    private LinearLayout genresLayout;
    private Button playButton;
    private Button favoriteButton;
    private TextView heroYear;
    private TextView heroImdbRating;
    private TextView heroQuality;
    
    // Data
    private String videoId;
    private String videoType;
    private MovieSingleDetails movieDetails;
    private DatabaseHelper databaseHelper;
    
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
        databaseHelper = new DatabaseHelper(getContext());
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
        releaseDate = view.findViewById(R.id.release_date_tv);
        movieDescription = view.findViewById(R.id.movie_description_tv);
        directorTv = view.findViewById(R.id.director_tv);
        castTv = view.findViewById(R.id.cast_tv);
        genresLayout = view.findViewById(R.id.genres);
        playButton = view.findViewById(R.id.play_button);
        favoriteButton = view.findViewById(R.id.favorite_button);
        heroYear = view.findViewById(R.id.hero_year);
        heroImdbRating = view.findViewById(R.id.hero_imdb_rating);
        heroQuality = view.findViewById(R.id.hero_quality);
    }
    
    private void setupButtonListeners() {
        playButton.setOnClickListener(v -> playMovie());
        favoriteButton.setOnClickListener(v -> toggleFavorite());
    }
    
    private void loadMovieDetails() {
        if (videoId == null) return;
        
        // Check if this is a phim4k movie/series
        if (videoId.startsWith("phim4k_")) {
            Log.d(TAG, "Detected phim4k content, ID: " + videoId);
            getPhim4kData(videoType, videoId);
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
        
        // Set movie title
        if (movieDetails.getTitle() != null) {
            movieTitle.setText(movieDetails.getTitle());
        }
        
        // Set hero info tags
        setHeroInfoTags();
        
        // Set release date
        if (movieDetails.getRelease() != null && movieDetails.getRelease().length() >= 4) {
            releaseDate.setText(movieDetails.getRelease().substring(0, 4));
        }
        
        // Set description
        if (movieDetails.getDescription() != null) {
            movieDescription.setText(movieDetails.getDescription());
        }
        
        // Set director
        setDirectorText();
        
        // Set cast
        setCastText();
        
        // Set genres
        setGenres();
        
        // Update favorite button state
        updateFavoriteButtonState();
    }
    
    private void loadMovieLogo() {
        if (videoId != null && !videoId.isEmpty()) {
            String logoUrl = "https://api.phim4k.lol/uploads/logo/" + videoId + ".jpg";
            
            Picasso.get()
                .load(logoUrl)
                .into(heroMovieLogo, new com.squareup.picasso.Callback() {
                    @Override
                    public void onSuccess() {
                        heroMovieLogo.setVisibility(View.VISIBLE);
                        movieTitle.setVisibility(View.GONE);
                    }
                    
                    @Override
                    public void onError(Exception e) {
                        heroMovieLogo.setVisibility(View.GONE);
                        movieTitle.setVisibility(View.VISIBLE);
                    }
                });
        }
    }
    
    private void setDirectorText() {
        if (movieDetails.getDirector() != null && !movieDetails.getDirector().isEmpty()) {
            StringBuilder director = new StringBuilder();
            for (int i = 0; i < movieDetails.getDirector().size(); i++) {
                Director dir = movieDetails.getDirector().get(i);
                director.append(dir.getName());
                if (i < movieDetails.getDirector().size() - 1) {
                    director.append(", ");
                }
            }
            directorTv.setText("Director: " + director.toString());
        } else {
            directorTv.setVisibility(View.GONE);
        }
    }
    
    private void setCastText() {
        if (movieDetails.getCastAndCrew() != null && !movieDetails.getCastAndCrew().isEmpty()) {
            StringBuilder cast = new StringBuilder();
            for (int i = 0; i < Math.min(movieDetails.getCastAndCrew().size(), 5); i++) {
                CastAndCrew castMember = movieDetails.getCastAndCrew().get(i);
                cast.append(castMember.getName());
                if (i < Math.min(movieDetails.getCastAndCrew().size(), 5) - 1) {
                    cast.append(", ");
                }
            }
            castTv.setText("Cast: " + cast.toString());
        } else {
            castTv.setVisibility(View.GONE);
        }
    }
    
    private void setGenres() {
        genresLayout.removeAllViews();
        if (movieDetails.getGenre() != null && !movieDetails.getGenre().isEmpty()) {
            for (int i = 0; i < Math.min(movieDetails.getGenre().size(), 3); i++) {
                Genre genre = movieDetails.getGenre().get(i);
                TextView genreTV = new TextView(getContext());
                genreTV.setText(genre.getName());
                genreTV.setTextSize(16);
                genreTV.setTextColor(getResources().getColor(android.R.color.white));
                genreTV.setShadowLayer(2, 0, 0, getResources().getColor(android.R.color.black));
                genresLayout.addView(genreTV);
                
                // Add separator
                if (i < Math.min(movieDetails.getGenre().size(), 3) - 1) {
                    TextView separator = new TextView(getContext());
                    separator.setText(" • ");
                    separator.setTextSize(16);
                    separator.setTextColor(getResources().getColor(android.R.color.white));
                    genresLayout.addView(separator);
                }
            }
        }
    }
    
    private void playMovie() {
        if (movieDetails != null) {
            Log.d(TAG, "Playing movie - ID: " + videoId + ", Title: " + movieDetails.getTitle());
            
            // Check if movie has video sources
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
        
        // Handle phim4k IDs differently since they're not numeric
        if (videoId.startsWith("phim4k_")) {
            // For phim4k content, use hash code as ID
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

        Intent intent = new Intent(getActivity(), PlayerActivity.class);
        intent.putExtra(com.files.codes.view.VideoPlaybackActivity.EXTRA_VIDEO, playbackModel);
        startActivity(intent, bundle);
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
                    
                    // Handle phim4k IDs differently since they're not numeric
                    if (videoId.startsWith("phim4k_")) {
                        // For phim4k content, use hash code as ID
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
                    startActivity(intent, bundle);
                    dialog.dismiss();
                }
            });
        }
    }
    
    private void toggleFavorite() {
        // Simplified favorite toggle - you can implement full database logic here
        Log.d(TAG, "Favorite button clicked!");
    }
    
    private void updateFavoriteButtonState() {
        // Update favorite button appearance based on current state
        // You can implement database check here
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
        
        // Set TMDB rating - use dummy value for now to test layout
        heroImdbRating.setText("8.5");
        heroImdbRating.setVisibility(View.VISIBLE);
        
        // Set quality
        if (movieDetails.getVideoQuality() != null && !movieDetails.getVideoQuality().isEmpty()) {
            heroQuality.setText(movieDetails.getVideoQuality());
            heroQuality.setVisibility(View.VISIBLE);
        } else {
            heroQuality.setText("HD");
            heroQuality.setVisibility(View.VISIBLE);
        }
    }
}