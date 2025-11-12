package com.files.codes.view.fragments;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.files.codes.AppConfig;
import com.files.codes.R;
import com.files.codes.model.Movie;
import com.files.codes.model.SearchModel;
import com.files.codes.model.api.ApiService;
import com.files.codes.utils.NetworkInst;
import com.files.codes.view.ErrorActivity;
import com.files.codes.view.VideoDetailsActivity;
import com.files.codes.view.adapters.SimpleMovieAdapter;

import java.util.ArrayList;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;

public class ActorMoviesSimpleFragment extends Fragment {
    private static final String TAG = "ActorMoviesSimple";
    private static final String ARG_ACTOR_NAME = "actor_name";
    private static final int NUM_COLUMNS = 6;
    
    private String actorName;
    private RecyclerView recyclerView;
    private SimpleMovieAdapter adapter;
    private List<Movie> movieList = new ArrayList<>();
    private TextView titleText;
    private String instanceId = "Simple_" + System.currentTimeMillis();

    public static ActorMoviesSimpleFragment newInstance(String actorName) {
        ActorMoviesSimpleFragment fragment = new ActorMoviesSimpleFragment();
        Bundle args = new Bundle();
        args.putString(ARG_ACTOR_NAME, actorName);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        Log.d(TAG, "🟢 onCreate() called for " + instanceId);
        
        if (getArguments() != null) {
            actorName = getArguments().getString(ARG_ACTOR_NAME);
        }
        
        Log.d(TAG, "🟢 Actor name: " + actorName + " for " + instanceId);
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_actor_movies_simple, container, false);
        
        titleText = view.findViewById(R.id.title_text);
        recyclerView = view.findViewById(R.id.movies_recycler_view);
        
        setupRecyclerView();
        updateTitle();
        loadMovies();
        
        return view;
    }

    private void setupRecyclerView() {
        GridLayoutManager layoutManager = new GridLayoutManager(getContext(), NUM_COLUMNS);
        recyclerView.setLayoutManager(layoutManager);
        
        adapter = new SimpleMovieAdapter(movieList, new SimpleMovieAdapter.OnMovieClickListener() {
            @Override
            public void onMovieClick(Movie movie) {
                Intent intent = new Intent(getActivity(), VideoDetailsActivity.class);
                intent.putExtra("id", movie.getId());
                intent.putExtra("vType", movie.getType());
                startActivity(intent);
            }
        });
        
        recyclerView.setAdapter(adapter);
    }

    private void updateTitle() {
        if (titleText != null) {
            String title = "Phim của " + (actorName != null ? actorName : "Diễn viên");
            titleText.setText(title);
        }
    }

    private void loadMovies() {
        if (actorName == null || actorName.trim().isEmpty()) {
            Log.e(TAG, "❌ Actor name is null or empty for " + instanceId);
            showEmptyState();
            return;
        }

        Log.d(TAG, "🔍 [" + instanceId + "] Loading movies for actor: " + actorName);

        Retrofit retrofit = NetworkInst.getInstance().getRetrofitInstance();
        ApiService apiService = retrofit.create(ApiService.class);
        
        Call<SearchModel> call = apiService.searchByActor(AppConfig.API_KEY, actorName);
        call.enqueue(new Callback<SearchModel>() {
            @Override
            public void onResponse(Call<SearchModel> call, Response<SearchModel> response) {
                if (response.isSuccessful() && response.body() != null) {
                    SearchModel searchModel = response.body();
                    if (searchModel.getMovies() != null && !searchModel.getMovies().isEmpty()) {
                        populateMovies(searchModel.getMovies());
                    } else {
                        Log.d(TAG, "❌ [" + instanceId + "] No movies found for actor: " + actorName);
                        showEmptyState();
                    }
                } else {
                    Log.e(TAG, "❌ [" + instanceId + "] API error: " + response.code());
                    handleApiError();
                }
            }

            @Override
            public void onFailure(Call<SearchModel> call, Throwable t) {
                Log.e(TAG, "❌ [" + instanceId + "] API failure: " + t.getMessage(), t);
                handleApiError();
            }
        });
    }

    private void populateMovies(List<Movie> movies) {
        movieList.clear();
        movieList.addAll(movies);
        
        if (adapter != null) {
            adapter.notifyDataSetChanged();
        }
        
        updateTitle();
        Log.d(TAG, "🎬 [" + instanceId + "] Added " + movies.size() + " movies for actor: " + actorName);
    }
    
    private void showEmptyState() {
        movieList.clear();
        if (adapter != null) {
            adapter.notifyDataSetChanged();
        }
        updateTitle();
    }

    private void handleApiError() {
        if (getActivity() != null) {
            Toast.makeText(getActivity(), "Không thể tải phim của diễn viên", Toast.LENGTH_SHORT).show();
        }
    }
}