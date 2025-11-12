package com.files.codes.viewmodel;

import android.content.Context;

import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;

import com.files.codes.model.HomeContentList;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Database(entities = {HomeContentList.class}, exportSchema = false, version = 1)
public abstract class HomeContentDatabase extends RoomDatabase {
    private static HomeContentDatabase instance;
    private static final int NUM_OF_THREADS = 4;

    public abstract HomeContentDao homeContentDao();
    static final ExecutorService databaseWritableExecutable =
            Executors.newFixedThreadPool(NUM_OF_THREADS);

    public static synchronized HomeContentDatabase getInstance(Context context){
        if (instance == null){
            instance = Room.databaseBuilder(context.getApplicationContext(),
                    HomeContentDatabase.class, "home_content_database")
                    .fallbackToDestructiveMigration()
                    .build();
        }
        return instance;
    }
}
