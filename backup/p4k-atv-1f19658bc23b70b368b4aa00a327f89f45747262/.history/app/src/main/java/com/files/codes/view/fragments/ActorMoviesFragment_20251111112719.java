package com.files.codes.view.fragments;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.files.codes.R;
import com.files.codes.model.Movie;
import com.files.codes.model.SearchModel;
import com.files.codes.model.api.ApiService;
import com.files.codes.AppConfig;
import com.files.codes.view.adapter.MovieAdapter;

import java.util.ArrayList;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class ActorMoviesFragment extends Fragment {
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
        recyclerView.setLayoutManager(new GridLayoutManager(getContext(), 3));
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

    private void loadMovies() {
        progressBar.setVisibility(View.VISIBLE);
        recyclerView.setVisibility(View.GONE);
        emptyView.setVisibility(View.GONE);

        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl(AppConfig.API_SERVER_URL)
                .addConverterFactory(GsonConverterFactory.create())
                .build();
        ApiService apiService = retrofit.create(ApiService.class);
        apiService.searchByActor(AppConfig.API_KEY, actorName, 1, "movieseries")
                .enqueue(new Callback<SearchModel>() {
                    @Override
                    public void onResponse(Call<SearchModel> call, Response<SearchModel> response) {
                        progressBar.setVisibility(View.GONE);
                        if (response.isSuccessful() && response.body() != null) {
                            List<Movie> movies = new ArrayList<>();
                            if (response.body().getMovie() != null) movies.addAll(response.body().getMovie());
                            if (response.body().getTvseries() != null) movies.addAll(response.body().getTvseries());
                            if (movies.isEmpty()) {
                                emptyView.setVisibility(View.VISIBLE);
                                recyclerView.setVisibility(View.GONE);
                            } else {
                                movieList.clear();
                                movieList.addAll(movies);
                                movieAdapter.notifyDataSetChanged();
                                recyclerView.setVisibility(View.VISIBLE);
                                emptyView.setVisibility(View.GONE);
                            }
                        } else {
                            emptyView.setVisibility(View.VISIBLE);
                        }
                    }

                    @Override
                    public void onFailure(Call<SearchModel> call, Throwable t) {
                        progressBar.setVisibility(View.GONE);
                        emptyView.setVisibility(View.VISIBLE);
                        Log.e("ActorMoviesFragment", "API error", t);
                    }
                });
    }
}
