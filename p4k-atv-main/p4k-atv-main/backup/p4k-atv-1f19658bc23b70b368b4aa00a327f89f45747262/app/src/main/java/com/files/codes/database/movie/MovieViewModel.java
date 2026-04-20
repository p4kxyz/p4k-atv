package com.files.codes.database.movie;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;

import com.files.codes.model.MovieList;

public class MovieViewModel extends AndroidViewModel {
    private static final String TAG = "MovieViewModel";

    private MovieRepository repository;
    private LiveData<MovieList> movieLiveData;

    public MovieViewModel(@NonNull Application application) {
        super(application);
        repository = new MovieRepository(application);
        movieLiveData = repository.getMovieLiveData();
    }

    public void insert(MovieList movies){
        repository.insert(movies);
    }

    public void update(MovieList movieList){
        repository.update(movieList);
    }

    public void delete(){
        repository.deleteAll();
    }

    public LiveData<MovieList> getMovieLiveData(){
        return movieLiveData;
    }

}
