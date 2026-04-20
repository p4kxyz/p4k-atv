package com.files.codes.view.fragments;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.leanback.app.VerticalGridSupportFragment;
import androidx.leanback.widget.ArrayObjectAdapter;
import androidx.leanback.widget.OnItemViewClickedListener;
import androidx.leanback.widget.OnItemViewSelectedListener;
import androidx.leanback.widget.Presenter;
import androidx.leanback.widget.Row;
import androidx.leanback.widget.RowPresenter;
import androidx.leanback.widget.VerticalGridPresenter;

import com.files.codes.AppConfig;
import com.files.codes.model.Movie;
import com.files.codes.model.api.ApiService;
import com.files.codes.utils.BackgroundHelper;
import com.files.codes.utils.RetrofitClient;
import com.files.codes.view.HeroStyleVideoDetailsActivity;
import com.files.codes.view.ItemGenreActivity;
import com.files.codes.view.presenter.VerticalCardPresenter;

import java.util.ArrayList;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;


public class ItemGenreFragment extends VerticalGridSupportFragment {

    private static final String TAG = ItemGenreFragment.class.getSimpleName();
    private static final int NUM_COLUMNS = 5;
    private List<Movie> movies = new ArrayList<>();
    private ArrayObjectAdapter mAdapter;
    private BackgroundHelper bgHelper;
    public static final String MOVIE = "movie";
    private int pageCount = 1;
    private boolean dataAvailable = true;

    private Context mContext;
    private String title;
    private String id = "";
    private ItemGenreActivity activity;


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mContext = getContext();
        title = getActivity().getIntent().getStringExtra("title");
        id = getActivity().getIntent().getStringExtra("id");
        activity = (ItemGenreActivity) getActivity();
        
        Log.d(TAG, "ItemGenreFragment created - Title: " + title + ", ID: " + id);

        setTitle(title);
        bgHelper = new BackgroundHelper(getActivity());

        setOnItemViewClickedListener(getDefaultItemViewClickedListener());
        setOnItemViewSelectedListener(getDefaultItemSelectedListener());

        setupFragment();

    }

    private void setupFragment() {
        VerticalGridPresenter gridPresenter = new VerticalGridPresenter();
        gridPresenter.setNumberOfColumns(NUM_COLUMNS);
        setGridPresenter(gridPresenter);
        // mAdapter = new ArrayObjectAdapter(new CardPresenter());
        mAdapter = new ArrayObjectAdapter(new VerticalCardPresenter(MOVIE));
        setAdapter(mAdapter);

        fetchMovieData(id, pageCount);

    }

    private void fetchMovieData(String id, int pageCount) {

        /*if (!new NetworkInst(mContext).isNetworkAvailable()) {
            Intent intent = new Intent(mContext, ErrorActivity.class);
            startActivity(intent);
            activity.finish();
            return;
        }*/
/*

        final SpinnerFragment mSpinnerFragment = new SpinnerFragment();
        final FragmentManager fm = getFragmentManager();
        fm.beginTransaction().add(R.id.custom_frame_layout, mSpinnerFragment).commit();
*/


        Retrofit retrofit = RetrofitClient.getRetrofitInstance();
        ApiService api = retrofit.create(ApiService.class);
        Call<List<Movie>> call = api.getMovieByGenre(AppConfig.API_KEY, id, pageCount);
        call.enqueue(new Callback<List<Movie>>() {
            @Override
            public void onResponse(Call<List<Movie>> call, Response<List<Movie>> response) {
                if (response.code() == 200) {
                    List<Movie> movieList = response.body();
                    if (movieList.size() <= 0) {
                        dataAvailable = false;
                        //Toast.makeText(activity, getResources().getString(R.string.no_data_found), Toast.LENGTH_SHORT).show();
                    } else {
                        dataAvailable = true;
                    }
                    for (Movie movie : movieList) {
                        mAdapter.add(movie);
                    }

                    mAdapter.notifyArrayItemRangeChanged(movieList.size() - 1, movieList.size() + movies.size());
                    movies.addAll(movieList);

                }
            }

            @Override
            public void onFailure(Call<List<Movie>> call, Throwable t) {
                Log.e("Genre Item", "code: " + t.getLocalizedMessage());
            }
        });
    }

    // click listener
    private OnItemViewClickedListener getDefaultItemViewClickedListener() {
        return new OnItemViewClickedListener() {
            @Override
            public void onItemClicked(Presenter.ViewHolder viewHolder, Object o,
                                      RowPresenter.ViewHolder viewHolder2, Row row) {

                Movie movie = (Movie) o;

                Intent intent = new Intent(getActivity(), HeroStyleVideoDetailsActivity.class);
                intent.putExtra("id", movie.getVideosId());
                
                // Fix: Check if it's TV series or movie
                String type = "movie"; // default
                if (movie.getIsTvseries() != null && movie.getIsTvseries().equals("1")) {
                    type = "tvseries";
                }
                intent.putExtra("type", type);
                
                intent.putExtra("thumbImage", movie.getThumbnailUrl());
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
                        fetchMovieData(id, pageCount);
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

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);

        movies = new ArrayList<>();
        pageCount = 1;
        dataAvailable = true;

    }

}
