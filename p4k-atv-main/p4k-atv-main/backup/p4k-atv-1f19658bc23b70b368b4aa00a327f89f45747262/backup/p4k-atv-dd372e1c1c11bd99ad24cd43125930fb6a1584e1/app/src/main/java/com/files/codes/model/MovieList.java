package com.files.codes.model;

import androidx.annotation.NonNull;
import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.PrimaryKey;
import androidx.room.TypeConverters;

import com.files.codes.database.movie.MovieConverter;

import java.io.Serializable;
import java.util.List;

@Entity(tableName = "movie_table")
public class MovieList implements Serializable {
    @PrimaryKey()
    @NonNull
    private int id;

    @ColumnInfo(name = "movie_list")
    @TypeConverters(MovieConverter.class)
    private List<Movie> movieList;

    public MovieList() {
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public List<Movie> getMovieList() {
        return movieList;
    }

    public void setMovieList(List<Movie> movieList) {
        this.movieList = movieList;
    }
}
