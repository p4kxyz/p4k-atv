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
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProvider;

import com.files.codes.AppConfig;
import com.files.codes.R;
import com.files.codes.database.TvSeries.TvSeriesViewModel;
import com.files.codes.model.Movie;
import com.files.codes.model.MovieList;
import com.files.codes.model.api.ApiService;
import com.files.codes.utils.BackgroundHelper;
import com.files.codes.utils.NetworkInst;
import com.files.codes.utils.RetrofitClient;
import com.files.codes.view.ErrorActivity;
import com.files.codes.view.VideoDetailsActivity;
import com.files.codes.view.fragments.testFolder.GridFragment;
import com.files.codes.view.fragments.testFolder.HomeNewActivity;
import com.files.codes.view.presenter.VerticalCardPresenter;

import java.util.ArrayList;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;

public class TvSeriesFragment extends GridFragment {
    public static final String TV_SERIES = "tv_series";
    private static final String TAG = TvSeriesFragment.class.getSimpleName();
    private static final int NUM_COLUMNS = 5;
    /*Page count is here 2
    Because for room database page 1 data is already loaded*/
    private int pageCount = 2;
    private boolean dataAvailable = true;
    private BackgroundHelper bgHelper;
    private List<Movie> movies = new ArrayList<>();
    private ArrayObjectAdapter mAdapter;
    private HomeNewActivity activity;
    private TvSeriesViewModel tvSeriesViewModel;


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        activity = (HomeNewActivity) getActivity();

        setOnItemViewClickedListener(getDefaultItemViewClickedListener());
        setOnItemViewSelectedListener(getDefaultItemSelectedListener());

        // setup
        VerticalGridPresenter gridPresenter = new VerticalGridPresenter();
        gridPresenter.setNumberOfColumns(NUM_COLUMNS);
        setGridPresenter(gridPresenter);

        mAdapter = new ArrayObjectAdapter(new VerticalCardPresenter(TV_SERIES));
        setAdapter(mAdapter);

        // fetchTvSereisData(pageCount);
        tvSeriesViewModel = new ViewModelProvider(getActivity()).get(TvSeriesViewModel.class);
        tvSeriesViewModel.getTvSeriesLiveData().observe(getActivity(), new Observer<MovieList>() {
            @Override
            public void onChanged(MovieList tvSeriesList) {
                if (tvSeriesList != null){
                    populateView(tvSeriesList.getMovieList());
                }
            }
        });
    }

    @Override
    public void onResume() {
        super.onResume();
        if (getActivity() instanceof com.files.codes.view.fragments.testFolder.HomeNewActivity) {
             ((com.files.codes.view.fragments.testFolder.HomeNewActivity) getActivity()).setOrbsVisibility(false);
        }
    }
}
        return new OnItemViewClickedListener() {
            @Override
            public void onItemClicked(Presenter.ViewHolder viewHolder, Object o,
                                      RowPresenter.ViewHolder viewHolder2, Row row) {

                Movie movie = (Movie) o;

                Intent intent = new Intent(getActivity(), com.files.codes.view.HeroStyleVideoDetailsActivity.class);
                intent.putExtra("id", movie.getVideosId());
                intent.putExtra("type", "tvseries");
                intent.putExtra("thumbImage", movie.getThumbnailUrl());
                
                Log.d("TvSeriesFragment", "Opening HeroStyleVideoDetails for TV series: " + movie.getTitle() + " (ID: " + movie.getVideosId() + ")");
                startActivity(intent);

            }
        };
    }

    // selected listener for setting blur background each time when the item will select.
    protected OnItemViewSelectedListener getDefaultItemSelectedListener() {
        return new OnItemViewSelectedListener() {
            public void onItemSelected(Presenter.ViewHolder itemViewHolder, final Object item,
                                       RowPresenter.ViewHolder rowViewHolder, Row row) {
                // pagination - load when reaching last row
                if (dataAvailable) {
                    int itemPos = mAdapter.indexOf(item);
                    if (itemPos >= movies.size() - NUM_COLUMNS) {
                        pageCount++;
                        dataAvailable = false;
                        fetchTvSereisData(pageCount);
                    }
                }

                //Log.d("iamge url: ------------------------------", itemPos+" : "+ movies.size());
                // change the background color when the item will select
                if (item instanceof Movie) {
                    bgHelper = new BackgroundHelper(getActivity());
                    bgHelper.prepareBackgroundManager();
                    bgHelper.startBackgroundTimer(((Movie) item).getThumbnailUrl());

                }

            }
        };
    }
    public void fetchTvSereisData(int pageCount) {
        if (!new NetworkInst(activity).isNetworkAvailable()) {
            Intent intent = new Intent(activity, ErrorActivity.class);
            startActivity(intent);
            activity.finish();
            return;
        }

        Retrofit retrofit = RetrofitClient.getRetrofitInstance();
        ApiService api = retrofit.create(ApiService.class);
        Call<List<Movie>> call = api.getTvSeries(AppConfig.API_KEY, pageCount);
        call.enqueue(new Callback<List<Movie>>() {
            @Override
            public void onResponse(Call<List<Movie>> call, Response<List<Movie>> response) {
                if (response.code() == 200) {
                    List<Movie> movieList = response.body();
                    if (movieList.size() == 0) {
                        dataAvailable = false;
                        if (pageCount != 2) {
                            Toast.makeText(activity, getResources().getString(R.string.no_data_found), Toast.LENGTH_SHORT).show();
                        }
                    } else {
                        dataAvailable = true;
                    }

                    populateView(movieList);


                }
            }

            @Override
            public void onFailure(Call<List<Movie>> call, Throwable t) {
                t.printStackTrace();

            }
        });
    }

    private void populateView(List<Movie> tvSeriesList) {
        if (tvSeriesList != null && tvSeriesList.size() > 0){
            for (Movie tvSeries : tvSeriesList){
                mAdapter.add(tvSeries);
            }
            mAdapter.notifyArrayItemRangeChanged(tvSeriesList.size() - 1, tvSeriesList.size() + movies.size());
            movies.addAll(tvSeriesList);
        }
    }

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);

        movies = new ArrayList<>();
        pageCount = 1;
        dataAvailable = true;

    }

}
