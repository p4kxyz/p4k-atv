package com.files.codes.view.fragments;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.ActivityOptionsCompat;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.files.codes.AppConfig;
import com.files.codes.R;
import com.files.codes.model.Movie;
import com.files.codes.model.SearchModel;
import com.files.codes.model.api.ApiService;
import com.files.codes.view.VideoDetailsActivity;
import com.files.codes.view.adapter.MovieAdapter;

import java.util.ArrayList;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class ActorMoviesFragment extends Fragment {
    private static final String TAG = ActorMoviesFragment.class.getSimpleName();
    private static final String ARG_ACTOR_NAME = "actor_name";
    
    private String actorName;
    private RecyclerView recyclerView;
    private ProgressBar progressBar;
    private TextView emptyView;
    private MovieAdapter movieAdapter;
    private List<Movie> movieList = new ArrayList<>();

    public static ActorMoviesFragment newInstance(String actorName) {
        ActorMoviesFragment fragment = new ActorMoviesFragment();
        Bundle args = new Bundle();
        args.putString(ARG_ACTOR_NAME, actorName);
        fragment.setArguments(args);
        return fragment;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_actor_movies, container, false);
        
        recyclerView = view.findViewById(R.id.recyclerView);
        progressBar = view.findViewById(R.id.progressBar);
        emptyView = view.findViewById(R.id.emptyView);
        
        // Setup RecyclerView
        recyclerView.setLayoutManager(new GridLayoutManager(getContext(), 5));
        movieAdapter = new MovieAdapter(getContext(), movieList);
        recyclerView.setAdapter(movieAdapter);
        
        return view;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        
        if (getArguments() != null) {
            actorName = getArguments().getString(ARG_ACTOR_NAME);
            loadMovies();
        }
    }

    // Click listener
    private OnItemViewClickedListener getDefaultItemViewClickedListener() {
        return new OnItemViewClickedListener() {
            @Override
            public void onItemClicked(Presenter.ViewHolder itemViewHolder, Object item,
                                      RowPresenter.ViewHolder rowViewHolder, Row row) {
                if (item instanceof Movie) {
                    Movie movie = (Movie) item;
                    Log.d(TAG, "Item: " + item.toString());
                    Intent intent = new Intent(getActivity(), VideoDetailsActivity.class);
                    intent.putExtra("id", movie.getVideosId());
                    intent.putExtra("type", "movie");
                    
                    Bundle bundle = ActivityOptionsCompat.makeSceneTransitionAnimation(getActivity()).toBundle();
                    getActivity().startActivity(intent, bundle);
                } else {
                    Toast.makeText(getActivity(), ((Movie) item).getTitle(), Toast.LENGTH_SHORT).show();
                }
            }
        };
    }

    // Selected listener  
    private OnItemViewSelectedListener getDefaultItemSelectedListener() {
        return new OnItemViewSelectedListener() {
            @Override
            public void onItemSelected(Presenter.ViewHolder itemViewHolder, Object item,
                                       RowPresenter.ViewHolder rowViewHolder, Row row) {
                // Background update can be added here if needed
            }
        };
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
                                populateView(allMovies);
                            } else {
                                // Show empty state
                                Log.d(TAG, "No movies found for actor: " + actorName);
                            }
                        } else {
                            Log.e(TAG, "API response error: " + response.code());
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

    private void populateView(List<Movie> movieList) {
        movies.clear();
        movies.addAll(movieList);
        
        for (Movie movie : movies) {
            mAdapter.add(movie);
        }
        
        Log.d(TAG, "Added " + movies.size() + " movies for actor: " + actorName);
    }
}
