package com.files.codes.database.movie;

import android.app.Application;

import androidx.lifecycle.LiveData;

import com.files.codes.model.MovieList;


public class MovieRepository {
    private static final String TAG = "MovieRepository";
    private MovieDao movieDao;
    private LiveData<MovieList> movieLiveData;

    public MovieRepository(Application application) {
       MovieDatabase database = MovieDatabase.getInstance(application);
        movieDao = database.movieDao();
        movieLiveData = movieDao.getMoviesLiveData();
    }

    public void insert(MovieList movies){
       MovieDatabase.databaseExecutorsService.execute(() ->{
            movieDao.insert(movies);
        });
    }

    public void update(MovieList movie){
     MovieDatabase.databaseExecutorsService.execute(() ->{
            movieDao.update(movie);
        });
    }

    public void deleteAll(){
       MovieDatabase.databaseExecutorsService.execute(()->{
            movieDao.deleteAll();
        });
    }

    public LiveData<MovieList> getMovieLiveData(){
        return movieLiveData;
    }
}
