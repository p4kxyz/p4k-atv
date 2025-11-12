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
    private TextView movieDescription;
    private Button playButton;
    private Button favoriteButton;
    private TextView heroYear;
    private TextView heroImdbRating;
    private TextView heroQuality;
    private RecyclerView genreTagsRecycler;
    private RecyclerView castCrewRecycler;
    
    // Data
    private String videoId;
    private String videoType;
    private MovieSingleDetails movieDetails;
    private boolean isFavorite = false;
    
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
        playButton = view.findViewById(R.id.play_button);
        favoriteButton = view.findViewById(R.id.favorite_button);
        heroYear = view.findViewById(R.id.hero_year);
        heroImdbRating = view.findViewById(R.id.hero_imdb_rating);
        heroQuality = view.findViewById(R.id.hero_quality);
        genreTagsRecycler = view.findViewById(R.id.genre_tags_recycler);
        castCrewRecycler = view.findViewById(R.id.cast_crew_recycler);
        
        // Setup RecyclerViews
        setupGenreTagsRecycler();
        setupCastCrewRecycler();
    }
    
    private void setupGenreTagsRecycler() {
        LinearLayoutManager layoutManager = new LinearLayoutManager(getContext(), LinearLayoutManager.HORIZONTAL, false);
        genreTagsRecycler.setLayoutManager(layoutManager);
    }
    
    private void setupCastCrewRecycler() {
        LinearLayoutManager layoutManager = new LinearLayoutManager(getContext(), LinearLayoutManager.HORIZONTAL, false);
        castCrewRecycler.setLayoutManager(layoutManager);
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
        Log.d(TAG, "Searching movies for actor: " + actorName);
        
        // Call API to search movies by actor
        retrofit2.Retrofit retrofit = new retrofit2.Retrofit.Builder()
                .baseUrl(AppConfig.getApiUrl())
                .addConverterFactory(retrofit2.converter.gson.GsonConverterFactory.create())
                .build();
        
        com.files.codes.model.api.ApiService apiService = retrofit.create(com.files.codes.model.api.ApiService.class);
        apiService.searchByActor(AppConfig.API_KEY, actorName, 1).enqueue(new retrofit2.Callback<com.files.codes.model.SearchModel>() {
            @Override
            public void onResponse(retrofit2.Call<com.files.codes.model.SearchModel> call, retrofit2.Response<com.files.codes.model.SearchModel> response) {
                if (response.isSuccessful() && response.body() != null) {
                    com.files.codes.model.SearchModel searchResult = response.body();
                    
                    // Show results in a dialog or navigate to search results
                    showActorMoviesDialog(actorName, searchResult);
                } else {
                    Log.e(TAG, "Failed to search actor movies: " + response.code());
                    ToastMsg toastMsg = new ToastMsg(getContext());
                    toastMsg.toastIconError("Không thể tải phim của diễn viên");
                }
            }
            
            @Override
            public void onFailure(retrofit2.Call<com.files.codes.model.SearchModel> call, Throwable t) {
                Log.e(TAG, "Error searching actor movies", t);
                ToastMsg toastMsg = new ToastMsg(getContext());
                toastMsg.toastIconError("Lỗi kết nối");
            }
        });
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
        if (movieDetails == null || videoId == null) return;
        
        try {
            ToastMsg toastMsg = new ToastMsg(getContext());
            if (isFavorite) {
                favoriteButton.setText("♡");
                toastMsg.toastIconSuccess("Removed from favorites");
                Log.d(TAG, "Removed from favorites: " + movieDetails.getTitle());
                isFavorite = false;
            } else {
                favoriteButton.setText("❤");
                toastMsg.toastIconSuccess("Added to favorites");
                Log.d(TAG, "Added to favorites: " + movieDetails.getTitle());
                isFavorite = true;
            }
        } catch (Exception e) {
            Log.e(TAG, "Error toggling favorite", e);
            ToastMsg toastMsg = new ToastMsg(getContext());
            toastMsg.toastIconError("Error updating favorites");
        }
    }
    
    private void updateFavoriteButtonState() {
        if (videoId == null) return;
        
        try {
            favoriteButton.setText(isFavorite ? "❤" : "♡");
        } catch (Exception e) {
            Log.e(TAG, "Error updating favorite state", e);
            favoriteButton.setText("♡");
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
        if (movieDetails.getTitle() != null) {
            String rawTitle = movieDetails.getTitle();
            
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
                
                // Use Vietnamese title as main title
                movieTitle.setText(vietnameseTitle);
            } else {
                // Fallback: if no pattern match, use formatted title
                String formattedTitle = formatMovieTitle(rawTitle);
                if (formattedTitle.contains("\n")) {
                    String[] parts = formattedTitle.split("\n", 2);
                    movieTitle.setText(parts[0]);
                } else {
                    movieTitle.setText(formattedTitle);
                }
            }
            movieTitle.setVisibility(View.VISIBLE);
        } else {
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
}