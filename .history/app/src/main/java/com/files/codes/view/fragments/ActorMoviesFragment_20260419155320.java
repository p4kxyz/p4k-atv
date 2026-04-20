package com.files.codes.view.fragments;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityOptionsCompat;
import androidx.leanback.widget.ArrayObjectAdapter;
import androidx.leanback.widget.ImageCardView;
import androidx.leanback.widget.OnItemViewClickedListener;
import androidx.leanback.widget.OnItemViewSelectedListener;
import androidx.leanback.widget.Presenter;
import androidx.leanback.widget.Row;
import androidx.leanback.widget.RowPresenter;
import androidx.leanback.widget.VerticalGridPresenter;

import com.files.codes.AppConfig;
import com.files.codes.R;
import com.files.codes.model.Movie;

import com.files.codes.model.api.ApiService;
import com.files.codes.utils.BackgroundHelper;
import com.files.codes.utils.NetworkInst;
import com.files.codes.utils.RetrofitClient;
import com.files.codes.view.ErrorActivity;
import com.files.codes.view.HeroStyleVideoDetailsActivity;
import com.files.codes.view.fragments.testFolder.GridFragment;
import com.files.codes.view.presenter.VerticalCardPresenter;

import java.util.ArrayList;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;

public class ActorMoviesFragment extends GridFragment {
    private static final String TAG = "ActorMoviesFragment_Clone";
    private static final String ARG_ACTOR_NAME = "actor_name";
    private static final int NUM_COLUMNS = 5; // Same as MoviesFragment
    
    private String actorName;
    private BackgroundHelper bgHelper;
    private List<Movie> movies = new ArrayList<>();
    private ArrayObjectAdapter mAdapter;

    public static ActorMoviesFragment newInstance(String actorName) {
        ActorMoviesFragment fragment = new ActorMoviesFragment();
        Bundle args = new Bundle();
        args.putString(ARG_ACTOR_NAME, actorName);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        Log.d(TAG, "🟢 onCreate() called");
        super.onCreate(savedInstanceState);
        
        if (getArguments() != null) {
            actorName = getArguments().getString(ARG_ACTOR_NAME);
        }
        
        Log.d(TAG, "🟢 Actor name: " + actorName);
        
        setOnItemViewClickedListener(getDefaultItemViewClickedListener());
        setOnItemViewSelectedListener(getDefaultItemSelectedListener());

        // setup - same as MoviesFragment
        VerticalGridPresenter gridPresenter = new VerticalGridPresenter(androidx.leanback.widget.FocusHighlight.ZOOM_FACTOR_NONE, false);
        gridPresenter.setNumberOfColumns(NUM_COLUMNS);
        setGridPresenter(gridPresenter);

        mAdapter = new ArrayObjectAdapter(new VerticalCardPresenter(MoviesFragment.MOVIE));
        setAdapter(mAdapter);

        // Load actor movies instead of all movies
        loadActorMovies();
    }

    // click listener - same as MoviesFragment
    private OnItemViewClickedListener getDefaultItemViewClickedListener() {
        return new OnItemViewClickedListener() {
            @Override
            public void onItemClicked(Presenter.ViewHolder viewHolder, Object o,
                                      RowPresenter.ViewHolder viewHolder2, Row row) {
                Movie movie = (Movie) o;
                Intent intent = new Intent(getActivity(), HeroStyleVideoDetailsActivity.class);
                intent.putExtra("id", movie.getVideosId());
                // Detect if it's a TV series based on is_tvseries field
                String type = (movie.getIsTvseries() != null && "1".equals(movie.getIsTvseries())) ? "tvseries" : "movie";
                intent.putExtra("type", type);
                intent.putExtra("thumbImage", movie.getThumbnailUrl());

                ImageView imageView = null;
                if (viewHolder.view instanceof androidx.leanback.widget.ImageCardView) {
                    imageView = ((androidx.leanback.widget.ImageCardView) viewHolder.view).getMainImageView();
                } else {
                    imageView = viewHolder.view.findViewById(R.id.main_image);
                }
                Bundle bundle = ActivityOptionsCompat.makeSceneTransitionAnimation(getActivity(),
                        imageView, VideoDetailsFragment.TRANSITION_NAME).toBundle();
                startActivity(intent, bundle);
            }
        };
    }

    // selected listener - same as MoviesFragment
    protected OnItemViewSelectedListener getDefaultItemSelectedListener() {
        return new OnItemViewSelectedListener() {
            public void onItemSelected(Presenter.ViewHolder itemViewHolder, final Object item,
                                       RowPresenter.ViewHolder rowViewHolder, Row row) {
                if (item instanceof Movie) {
                    bgHelper = new BackgroundHelper(getActivity());
                    bgHelper.prepareBackgroundManager();
                    bgHelper.startBackgroundTimer(((Movie) item).getThumbnailUrl());
                }
            }
        };
    }

    private void loadActorMovies() {
        if (actorName == null || actorName.trim().isEmpty()) {
            Log.e(TAG, "❌ Actor name is null or empty");
            showEmptyState();
            return;
        }

        if (!new NetworkInst(getActivity()).isNetworkAvailable()) {
            Intent intent = new Intent(getActivity(), ErrorActivity.class);
            startActivity(intent);
            getActivity().finish();
            return;
        }

        Log.d(TAG, "🔍 Loading movies for actor: " + actorName);

        Retrofit retrofit = RetrofitClient.getRetrofitInstance();
        ApiService apiService = retrofit.create(ApiService.class);
        
        // Use search_by_actor endpoint - it returns a flat array of movies and series
        Call<List<Movie>> call = apiService.searchByActor(AppConfig.API_KEY, actorName, 1);
        call.enqueue(new Callback<List<Movie>>() {
            @Override
            public void onResponse(Call<List<Movie>> call, Response<List<Movie>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    List<Movie> allMovies = response.body();
                    
                    if (!allMovies.isEmpty()) {
                        populateView(allMovies);
                        Log.d(TAG, "🎬 Added " + allMovies.size() + " movies for actor: " + actorName);
                    } else {
                        Log.d(TAG, "❌ No movies found for actor: " + actorName);
                        showEmptyState();
                    }
                } else {
                    Log.e(TAG, "❌ API error: " + response.code());
                    handleApiError();
                }
            }

            @Override
            public void onFailure(Call<List<Movie>> call, Throwable t) {
                Log.e(TAG, "❌ API failure: " + t.getMessage(), t);
                handleApiError();
            }
        });
    }

    private void populateView(List<Movie> movieList) {
        if (movieList != null && movieList.size() > 0) {
            for (Movie movie : movieList) {
                mAdapter.add(movie);
            }
            mAdapter.notifyArrayItemRangeChanged(movieList.size() - 1, movieList.size() + movies.size());
            movies.addAll(movieList);
            setAdapter(mAdapter);
            
            // Add null check to prevent crash
            if (getMainFragmentAdapter() != null && getMainFragmentAdapter().getFragmentHost() != null) {
                getMainFragmentAdapter().getFragmentHost().notifyDataReady(getMainFragmentAdapter());
            }
        }
    }
    
    private void showEmptyState() {
        // Could show empty message or placeholder
        Toast.makeText(getActivity(), "Không tìm thấy phim của diễn viên " + actorName, Toast.LENGTH_SHORT).show();
    }

    private void handleApiError() {
        if (getActivity() != null) {
            Toast.makeText(getActivity(), "Không thể tải phim của diễn viên", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        movies = new ArrayList<>();
    }
}