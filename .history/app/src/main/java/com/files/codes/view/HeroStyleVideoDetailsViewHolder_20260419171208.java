package com.files.codes.view;

import android.content.Context;
import android.graphics.drawable.GradientDrawable;
import android.os.Build;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.RequiresApi;
import androidx.core.content.ContextCompat;
import androidx.leanback.widget.Presenter;

import com.files.codes.R;
import com.files.codes.model.Genre;
import com.files.codes.model.movieDetails.CastAndCrew;
import com.files.codes.model.movieDetails.Director;
import com.files.codes.model.movieDetails.MovieSingleDetails;
import com.squareup.picasso.Picasso;

import java.util.List;

public class HeroStyleVideoDetailsViewHolder extends Presenter.ViewHolder {
    private static final int COLLAPSED_MAX_LINES = 4;
    private static final int MIN_DESCRIPTION_LENGTH_FOR_TOGGLE = 160;
    
    private TextView movieTitleTV;
    private TextView descriptionTv;
    private Button descriptionToggleButton;
    private TextView releaseDateTv;
    private TextView directorTv;
    private TextView castTv;
    private LinearLayout genresLayout;
    private ImageView backgroundPoster;
    private ImageView heroMovieLogo;
    private Button playButton;
    private Button favoriteButton;
    private View itemView;
    private MovieSingleDetails currentMovie;
    private String fullDescription = "";
    private boolean isDescriptionExpanded = false;
    
    // Listener interfaces
    public interface OnPlayClickListener {
        void onPlayClick(MovieSingleDetails movie);
    }
    
    public interface OnFavoriteClickListener {
        void onFavoriteClick(MovieSingleDetails movie);
    }
    
    private OnPlayClickListener playClickListener;
    private OnFavoriteClickListener favoriteClickListener;

    public HeroStyleVideoDetailsViewHolder(View view) {
        super(view);
        itemView = view;

        movieTitleTV = itemView.findViewById(R.id.movie_title);
        descriptionTv = itemView.findViewById(R.id.movie_description_tv);
        descriptionToggleButton = itemView.findViewById(R.id.description_toggle_btn);
        releaseDateTv = itemView.findViewById(R.id.release_date_tv);
        genresLayout = itemView.findViewById(R.id.genres);
        directorTv = itemView.findViewById(R.id.director_tv);
        castTv = itemView.findViewById(R.id.cast_tv);
        backgroundPoster = itemView.findViewById(R.id.background_poster);
        heroMovieLogo = itemView.findViewById(R.id.hero_movie_logo);
        playButton = itemView.findViewById(R.id.play_button);
        favoriteButton = itemView.findViewById(R.id.favorite_button);
        
        // Set up button click listeners
        setupButtonListeners();
        setupDescriptionToggle();
    }

    public void setOnPlayClickListener(OnPlayClickListener listener) {
        this.playClickListener = listener;
    }
    
    public void setOnFavoriteClickListener(OnFavoriteClickListener listener) {
        this.favoriteClickListener = listener;
    }
    
    private void setupButtonListeners() {
        if (playButton != null) {
            playButton.setOnClickListener(v -> {
                if (playClickListener != null && currentMovie != null) {
                    playClickListener.onPlayClick(currentMovie);
                }
            });
        }
        
        if (favoriteButton != null) {
            favoriteButton.setOnClickListener(v -> {
                if (favoriteClickListener != null && currentMovie != null) {
                    favoriteClickListener.onFavoriteClick(currentMovie);
                }
            });
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.KITKAT)
    public void bind(MovieSingleDetails movie, Context context) {
        if (movie != null && movie.getTitle() != null) {
            this.currentMovie = movie;
            
            // Set background poster
            if (movie.getPosterUrl() != null && !movie.getPosterUrl().isEmpty()) {
                Picasso.get()
                    .load(movie.getPosterUrl())
                    .placeholder(R.drawable.default_background)
                    .error(R.drawable.default_background)
                    .into(backgroundPoster);
            }

            // Load movie logo (similar to hero banner)
            loadMovieLogo(movie.getVideosId(), context);

            // Set movie title
            if (movieTitleTV != null) {
                movieTitleTV.setText(movie.getTitle());
            }

            // Set release date
            if (releaseDateTv != null && movie.getRelease() != null && movie.getRelease().length() >= 4) {
                releaseDateTv.setText(movie.getRelease().substring(0, 4));
            }

            // Set description
            bindDescription(movie.getDescription());

            // Set director
            String director = "";
            if (movie.getDirector() != null) {
                int count = 1;
                for (Director director1 : movie.getDirector()) {
                    if (count == movie.getDirector().size()) {
                        director += director1.getName();
                    } else {
                        director += director1.getName() + ", ";
                    }
                    count++;
                }
            }
            if (!director.isEmpty() && directorTv != null) {
                directorTv.setText("Director: " + director);
            }

            // Set cast
            String cast = "";
            if (movie.getCastAndCrew() != null) {
                int count = 1;
                for (CastAndCrew c : movie.getCastAndCrew()) {
                    if (count == movie.getCastAndCrew().size()) {
                        cast += c.getName();
                    } else {
                        cast += c.getName() + ", ";
                    }
                    count++;
                }
            }
            if (!cast.isEmpty() && castTv != null) {
                castTv.setText("Cast: " + cast);
            }

            // Set genres
            if (genresLayout != null) {
                genresLayout.removeAllViews();
            }
            if (movie.getGenre() != null && genresLayout != null) {
                for (int i = 0; i < Math.min(movie.getGenre().size(), 3); i++) {
                    Genre genre = movie.getGenre().get(i);
                    TextView genreTV = createGenreChip(context, genre.getName());
                    genresLayout.addView(genreTV);
                    
                    // Add separator except for last item
                    if (i < Math.min(movie.getGenre().size(), 3) - 1) {
                        TextView separator = new TextView(context);
                        separator.setText(" • ");
                        separator.setTextColor(ContextCompat.getColor(context, android.R.color.white));
                        genresLayout.addView(separator);
                    }
                }
            }
        }
    }

    private void loadMovieLogo(String videoId, Context context) {
        if (videoId != null && !videoId.isEmpty()) {
            String logoUrl = "https://api.phim4k.lol/uploads/logo/" + videoId + ".jpg";
            
            Picasso.get()
                .load(logoUrl)
                .into(heroMovieLogo, new com.squareup.picasso.Callback() {
                    @Override
                    public void onSuccess() {
                        heroMovieLogo.setVisibility(View.VISIBLE);
                        movieTitleTV.setVisibility(View.GONE); // Hide title when logo is available
                    }

                    @Override
                    public void onError(Exception e) {
                        heroMovieLogo.setVisibility(View.GONE);
                        movieTitleTV.setVisibility(View.VISIBLE); // Show title when no logo
                    }
                });
        }
    }

    private void setupDescriptionToggle() {
        if (descriptionToggleButton == null) {
            return;
        }

        descriptionToggleButton.setOnClickListener(v -> {
            if (fullDescription.isEmpty()) {
                return;
            }

            isDescriptionExpanded = !isDescriptionExpanded;
            updateDescriptionState();
        });
    }

    private void bindDescription(String description) {
        fullDescription = description != null ? description : "";

        if (descriptionTv == null) {
            return;
        }

        descriptionTv.setText(fullDescription);
        descriptionTv.setCompoundDrawables(null, null, null, null);

        if (descriptionToggleButton == null) {
            return;
        }

        if (fullDescription.isEmpty()) {
            descriptionToggleButton.setVisibility(View.GONE);
            return;
        }

        descriptionTv.post(() -> {
            boolean needsToggle = descriptionTv.getLineCount() > COLLAPSED_MAX_LINES
                    || fullDescription.length() > MIN_DESCRIPTION_LENGTH_FOR_TOGGLE;
            descriptionToggleButton.setVisibility(needsToggle ? View.VISIBLE : View.GONE);

            if (!needsToggle) {
                return;
            }

            isDescriptionExpanded = false;
            updateDescriptionState();
        });
    }

    private void updateDescriptionState() {
        if (descriptionTv == null) {
            return;
        }

        if (isDescriptionExpanded) {
            descriptionTv.setMaxLines(Integer.MAX_VALUE);
            descriptionTv.setEllipsize(null);
            if (descriptionToggleButton != null) {
                descriptionToggleButton.setText(R.string.collapse_description);
            }
            return;
        }

        descriptionTv.setMaxLines(COLLAPSED_MAX_LINES);
        descriptionTv.setEllipsize(TextUtils.TruncateAt.END);
        if (descriptionToggleButton != null) {
            descriptionToggleButton.setText(R.string.expand_description);
        }
    }

    private TextView createGenreChip(Context context, String genreName) {
        TextView genreTV = new TextView(context);
        genreTV.setText(genreName);
        genreTV.setTextSize(14);
        genreTV.setTextColor(ContextCompat.getColor(context, android.R.color.white));
        genreTV.setShadowLayer(2, 0, 0, ContextCompat.getColor(context, android.R.color.black));
        return genreTV;
    }
}