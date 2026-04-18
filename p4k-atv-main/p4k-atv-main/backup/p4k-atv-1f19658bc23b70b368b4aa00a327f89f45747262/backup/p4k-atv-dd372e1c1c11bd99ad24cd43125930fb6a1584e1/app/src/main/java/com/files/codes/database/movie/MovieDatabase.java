package com.files.codes.database.movie;

import android.content.Context;

import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;


import com.files.codes.model.MovieList;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Database(entities = {MovieList.class}, exportSchema = false, version = 1)
public abstract class MovieDatabase extends RoomDatabase {
    private static final String TAG = "MovieDatabase";

    private static MovieDatabase instance;
    private static final int NUMBER_OF_THREAD = 4;

    public abstract MovieDao movieDao();
    static final ExecutorService databaseExecutorsService =
            Executors.newFixedThreadPool(NUMBER_OF_THREAD);

    public static synchronized MovieDatabase getInstance(Context context){
        if (instance == null){
            instance = Room.databaseBuilder(context.getApplicationContext(),
                    MovieDatabase.class, "Movie_database")
                    .fallbackToDestructiveMigration()
                    .build();
        }
        return instance;
    }

}
