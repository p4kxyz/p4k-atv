package com.files.codes.database.live_tv;

import android.content.Context;

import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Database(entities = {LiveTvList.class}, exportSchema = false, version = 1)
public abstract class LiveTvDatabase extends RoomDatabase {
    private static final int NUMBER_OF_THREADS = 4;
    private static LiveTvDatabase instance;

    public abstract LiveTvDao liveTvDao();
    static final ExecutorService databaseExecutorsService =
            Executors.newFixedThreadPool(NUMBER_OF_THREADS);

    public static synchronized LiveTvDatabase getInstance(Context context){
        if (instance == null){
            instance = Room.databaseBuilder(context.getApplicationContext(),
                    LiveTvDatabase.class, "live_tv_database")
                    .fallbackToDestructiveMigration()
                    .build();
        }
        return instance;
    }


}
