package com.files.codes.viewmodel;

import android.content.Context;

import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;

import com.files.codes.model.config.Configuration;
import com.files.codes.viewmodel.config.ConfigDao;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Database(entities = {Configuration.class}, exportSchema = false,version = 1)
public abstract class CommonDatabase extends RoomDatabase {
    private static CommonDatabase instance;
    private static final int NUMBER_OF_THREADS = 4;

    public abstract ConfigDao configDao();
    static final ExecutorService databaseWriteExecutor = Executors.newFixedThreadPool(NUMBER_OF_THREADS);

    public static synchronized CommonDatabase getInstance(Context context){
        if (instance == null){
            instance = Room.databaseBuilder(context.getApplicationContext(),
                    CommonDatabase.class, "common_database")
                    .fallbackToDestructiveMigration()
                    .build();
        }
        return  instance;
    }
}
