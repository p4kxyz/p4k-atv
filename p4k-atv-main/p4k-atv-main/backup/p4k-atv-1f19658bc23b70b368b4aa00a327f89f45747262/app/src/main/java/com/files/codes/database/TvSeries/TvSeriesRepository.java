package com.files.codes.database.TvSeries;

import android.app.Application;

import androidx.lifecycle.LiveData;

import com.files.codes.model.MovieList;


public class TvSeriesRepository {
    private static final String TAG = "TvSeriesRepository";
    private TvSeriesDao tvSeriesDao;
    private LiveData<MovieList> tvSeriesLiveData;

    public TvSeriesRepository(Application application) {
        TvSeriesDatabase database = TvSeriesDatabase.getInstance(application);
        tvSeriesDao = database.tvSeriesDao();
        tvSeriesLiveData = tvSeriesDao.getTvSeriesLiveData();
    }

    public void insert(MovieList tvSeriesList){
        TvSeriesDatabase.databaseExecutorsService.execute(() -> {
            tvSeriesDao.insert(tvSeriesList);
        });
    }

    public void update(MovieList tvseries){
        TvSeriesDatabase.databaseExecutorsService.execute(() ->{
            tvSeriesDao.update(tvseries);
        });
    }

    public void deleteAll(){
        TvSeriesDatabase.databaseExecutorsService.execute(() ->{
            tvSeriesDao.deleteAll();
        });
    }

    public LiveData<MovieList> getTvSeriesLiveData(){
        return tvSeriesLiveData;
    }
}
