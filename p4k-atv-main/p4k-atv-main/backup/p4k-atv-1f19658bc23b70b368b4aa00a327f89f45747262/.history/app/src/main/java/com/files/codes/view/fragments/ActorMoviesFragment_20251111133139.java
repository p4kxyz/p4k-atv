package com.files.codes.view.fragments;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityOptionsCompat;
import androidx.leanback.app.GridFragment;
import androidx.leanback.widget.VerticalGridPresenter;
import androidx.leanback.widget.ArrayObjectAdapter;
import androidx.leanback.widget.ImageCardView;

import androidx.leanback.widget.OnItemViewClickedListener;
import androidx.leanback.widget.OnItemViewSelectedListener;
import androidx.leanback.widget.Presenter;
import androidx.leanback.widget.Row;
import androidx.leanback.widget.RowPresenter;

import com.files.codes.AppConfig;
import com.files.codes.R;
import com.files.codes.model.Movie;
import com.files.codes.model.SearchModel;
import com.files.codes.model.api.ApiService;
import com.files.codes.utils.NetworkInst;
import com.files.codes.view.ErrorActivity;
import com.files.codes.view.VideoDetailsActivity;
import com.files.codes.view.fragments.VideoDetailsFragment;
import com.files.codes.view.fragments.MoviesFragment;
import com.files.codes.view.presenter.VerticalCardPresenter;

import java.util.ArrayList;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class ActorMoviesFragment extends VerticalGridSupportFragment {
    private static final String TAG = "ActorMoviesFragment_Custom";
    private static final String ARG_ACTOR_NAME = "actor_name";
    private static final int NUM_COLUMNS = 6;
    
    private String actorName;
    private ArrayObjectAdapter mAdapter;
    private List<Movie> movieList = new ArrayList<>();

    public static ActorMoviesFragment newInstance(String actorName) {
        ActorMoviesFragment fragment = new ActorMoviesFragment();
        Bundle args = new Bundle();
        args.putString(ARG_ACTOR_NAME, actorName);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        if (getArguments() != null) {
            actorName = getArguments().getString(ARG_ACTOR_NAME);
        }
        
        setupUIElements();
        setupEventListeners();
        loadMovies();
    }
    
    private void setupUIElements() {
        setTitle("Phim của " + (actorName != null ? actorName : "Diễn viên"));
        
        VerticalGridPresenter gridPresenter = new VerticalGridPresenter();
        gridPresenter.setNumberOfColumns(NUM_COLUMNS);
        setGridPresenter(gridPresenter);
        
        mAdapter = new ArrayObjectAdapter(new VerticalCardPresenter(MoviesFragment.MOVIE));
        setAdapter(mAdapter);
    }

    private void setupEventListeners() {
        setOnItemViewClickedListener(new ItemViewClickedListener());
        setOnItemViewSelectedListener(new ItemViewSelectedListener());
    }



    private void loadMovies() {
        // Network check can be added later if needed

        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl(AppConfig.API_SERVER_URL)
                .addConverterFactory(GsonConverterFactory.create())
                .build();
        
        ApiService apiService = retrofit.create(ApiService.class);
        apiService.searchByActor(AppConfig.API_KEY, actorName, 1, "movieseries")
                .enqueue(new Callback<SearchModel>() {
                    @Override
                    public void onResponse(@NonNull Call<SearchModel> call, @NonNull Response<SearchModel> response) {
                        if (response.isSuccessful() && response.body() != null) {
                            SearchModel searchResult = response.body();
                            List<Movie> allMovies = new ArrayList<>();
                            
                            // Add movies
                            if (searchResult.getMovie() != null) {
                                allMovies.addAll(searchResult.getMovie());
                            }
                            
                            // Add TV series
                            if (searchResult.getTvseries() != null) {
                                allMovies.addAll(searchResult.getTvseries());
                            }
                            
                            if (!allMovies.isEmpty()) {
                                populateMovies(allMovies);
                                Log.d(TAG, "Added " + allMovies.size() + " movies for actor: " + actorName);
                            } else {
                                Log.d(TAG, "No movies found for actor: " + actorName);
                                // Show empty state
                                showEmptyState();
                            }
                        } else {
                            Log.e(TAG, "API response error: " + response.code());
                            showEmptyState();
                        }
                    }

                    @Override
                    public void onFailure(@NonNull Call<SearchModel> call, @NonNull Throwable t) {
                        Log.e(TAG, "API call failed", t);
                        Intent intent = new Intent(getActivity(), ErrorActivity.class);
                        startActivity(intent);
                    }
                });
    }

    private void populateMovies(List<Movie> movies) {
        movieList.clear();
        movieList.addAll(movies);
        
        mAdapter.clear();
        for (Movie movie : movies) {
            mAdapter.add(movie);
        }
        Log.d(TAG, "Added " + movies.size() + " movies for actor: " + actorName);
    }
    
    private void showEmptyState() {
        mAdapter.clear();
        // Could add a placeholder item or just leave empty
    }

    private final class ItemViewClickedListener implements OnItemViewClickedListener {
        @Override
        public void onItemClicked(Presenter.ViewHolder itemViewHolder, Object item,
                                  RowPresenter.ViewHolder rowViewHolder, Row row) {
            if (item instanceof Movie) {
                Movie movie = (Movie) item;
                Intent intent = new Intent(getActivity(), VideoDetailsActivity.class);
                intent.putExtra("id", movie.getVideosId());
                intent.putExtra("type", movie.getIsTvseries().equals("1") ? "tvseries" : "movie");

                ImageView imageView = ((ImageCardView) itemViewHolder.view).getMainImageView();
                Bundle bundle = ActivityOptionsCompat.makeSceneTransitionAnimation(getActivity(),
                        imageView, VideoDetailsFragment.TRANSITION_NAME).toBundle();
                startActivity(intent, bundle);
            } else {
                Toast.makeText(getActivity(), item.toString(), Toast.LENGTH_SHORT).show();
            }
        }
    }

    private final class ItemViewSelectedListener implements OnItemViewSelectedListener {
        @Override
        public void onItemSelected(Presenter.ViewHolder itemViewHolder, Object item,
                                   RowPresenter.ViewHolder rowViewHolder, Row row) {
            // Background update can be added here if needed
        }
    }
}
