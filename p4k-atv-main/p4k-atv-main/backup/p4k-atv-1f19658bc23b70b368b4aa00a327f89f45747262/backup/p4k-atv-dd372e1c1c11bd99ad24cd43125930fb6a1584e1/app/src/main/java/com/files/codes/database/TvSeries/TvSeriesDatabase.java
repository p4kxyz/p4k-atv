package com.files.codes.database.TvSeries;

import android.content.Context;

import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;


import com.files.codes.model.MovieList;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Database(entities = {MovieList.class}, exportSchema = false, version = 1)
public abstract class TvSeriesDatabase extends RoomDatabase {
    private static TvSeriesDatabase instance;
    private static final int NUMBER_OF_THREAD = 4;

    public abstract TvSeriesDao tvSeriesDao();
    static final ExecutorService databaseExecutorsService =
            Executors.newFixedThreadPool(NUMBER_OF_THREAD);

    public static synchronized TvSeriesDatabase getInstance(Context context){
        if (instance == null){
            instance = Room.databaseBuilder(context.getApplicationContext(),
                    TvSeriesDatabase.class, "tv_series_database")
                    .fallbackToDestructiveMigration()
                    .build();
        }
        return instance;
    }
}
