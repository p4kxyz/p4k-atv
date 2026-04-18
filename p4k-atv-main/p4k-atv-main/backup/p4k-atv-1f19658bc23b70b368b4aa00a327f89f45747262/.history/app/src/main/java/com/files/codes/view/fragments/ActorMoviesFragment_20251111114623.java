package com.files.codes.view.fragments;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityOptionsCompat;
import androidx.leanback.app.BrowseSupportFragment;
import androidx.leanback.widget.ArrayObjectAdapter;
import androidx.leanback.widget.HeaderItem;
import androidx.leanback.widget.ImageCardView;
import androidx.leanback.widget.ListRow;
import androidx.leanback.widget.ListRowPresenter;
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
import com.files.codes.view.presenter.CardPresenter;

import java.util.ArrayList;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class ActorMoviesFragment extends BrowseSupportFragment {
    private static final String TAG = ActorMoviesFragment.class.getSimpleName();
    private static final String ARG_ACTOR_NAME = "actor_name";
    
    private String actorName;
    private RecyclerView recyclerView;
    private ProgressBar progressBar;
    private TextView emptyView;
    private ActorMovieAdapter movieAdapter;
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
        movieAdapter = new ActorMovieAdapter(getContext(), movieList);
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
                    public void onResponse(@NonNull Call<SearchModel> call, @NonNull Response<SearchModel> response) {
                        progressBar.setVisibility(View.GONE);
                        
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
                                movieList.clear();
                                movieList.addAll(allMovies);
                                movieAdapter.notifyDataSetChanged();
                                recyclerView.setVisibility(View.VISIBLE);
                                emptyView.setVisibility(View.GONE);
                                Log.d(TAG, "Added " + allMovies.size() + " movies for actor: " + actorName);
                            } else {
                                emptyView.setText("Không có phim nào của " + actorName);
                                emptyView.setVisibility(View.VISIBLE);
                                recyclerView.setVisibility(View.GONE);
                            }
                        } else {
                            Log.e(TAG, "API response error: " + response.code());
                            emptyView.setText("Lỗi tải dữ liệu");
                            emptyView.setVisibility(View.VISIBLE);
                            recyclerView.setVisibility(View.GONE);
                        }
                    }

                    @Override
                    public void onFailure(@NonNull Call<SearchModel> call, @NonNull Throwable t) {
                        progressBar.setVisibility(View.GONE);
                        Log.e(TAG, "API call failed", t);
                        emptyView.setText("Lỗi kết nối");
                        emptyView.setVisibility(View.VISIBLE);
                        recyclerView.setVisibility(View.GONE);
                    }
                });
    }
}
