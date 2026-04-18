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

import com.files.codes.R;
import com.files.codes.database.DatabaseHelper;
import com.files.codes.model.Genre;
import com.files.codes.model.movieDetails.CastAndCrew;
import com.files.codes.model.movieDetails.Director;
import com.files.codes.model.movieDetails.MovieSingleDetails;
import com.files.codes.model.api.ApiService;
import com.files.codes.utils.RetrofitClient;
import com.files.codes.utils.ToastMsg;
import com.files.codes.view.PlayerActivity;
import com.squareup.picasso.Picasso;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

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
    }
    
    private void setupButtonListeners() {
        playButton.setOnClickListener(v -> playMovie());
        favoriteButton.setOnClickListener(v -> toggleFavorite());
    }
    
    private void loadMovieDetails() {
        if (videoId == null) return;
        
        ApiService apiService = RetrofitClient.getRetrofitInstance().create(ApiService.class);
        
        // Use getSingleDetail API with type parameter
        String type = ("tvseries".equals(videoType) || "tv".equals(videoType)) ? "tvseries" : "movie";
        Call<MovieSingleDetails> call = apiService.getSingleDetail("", type, videoId);
        
        call.enqueue(new Callback<MovieSingleDetails>() {
            @Override
            public void onResponse(Call<MovieSingleDetails> call, Response<MovieSingleDetails> response) {
                if (response.isSuccessful() && response.body() != null) {
                    movieDetails = response.body();
                    displayMovieDetails();
                } else {
                    Log.e(TAG, "Failed to load movie details");
                    ToastMsg.toastIconError(getContext(), "Failed to load movie details", R.drawable.ic_favorite_border_white_24dp);
                }
            }
            
            @Override
            public void onFailure(Call<MovieSingleDetails> call, Throwable t) {
                Log.e(TAG, "Error loading movie details: " + t.getMessage());
                ToastMsg.toastIconError(getContext(), "Network error", R.drawable.ic_favorite_border_white_24dp);
            }
        });
    }
    
    private void displayMovieDetails() {
        if (movieDetails == null) return;
        
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
            Intent intent = new Intent(getActivity(), PlayerActivity.class);
            intent.putExtra("id", videoId);
            intent.putExtra("type", videoType != null ? videoType : "movie");
            startActivity(intent);
        }
    }
    
    private void toggleFavorite() {
        // Simplified favorite toggle - you can implement full database logic here
        ToastMsg.toastIconSuccess(getActivity(), "Favorite feature clicked!", R.drawable.ic_favorite);
    }
    
    private void updateFavoriteButtonState() {
        // Update favorite button appearance based on current state
        // You can implement database check here
    }
}