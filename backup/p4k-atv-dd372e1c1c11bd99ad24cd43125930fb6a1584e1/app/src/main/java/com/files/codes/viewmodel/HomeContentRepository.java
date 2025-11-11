package com.files.codes.viewmodel;

import android.app.Application;

import androidx.lifecycle.LiveData;

import com.files.codes.model.HomeContentList;

public class HomeContentRepository {
    private HomeContentDao homeContentDao;
    private LiveData<HomeContentList> homeContentLiveData;
    private final HomeContentList homeContentList = null;

    public HomeContentRepository(Application application) {
        HomeContentDatabase database = HomeContentDatabase.getInstance(application);
        homeContentDao = database.homeContentDao();
        homeContentLiveData = homeContentDao.getHomeContentLiveData();
    }

    public void insert(HomeContentList homeContentList) {
        HomeContentDatabase.databaseWritableExecutable.execute(() -> {
            homeContentDao.insert(homeContentList);
        });

    }

    public void update(HomeContentList homeContentList) {
        HomeContentDatabase.databaseWritableExecutable.execute(() -> {
            homeContentDao.update(homeContentList);
        });
    }

    public LiveData<HomeContentList> getHomeContentLiveData() {
        return homeContentLiveData;
    }

    public void delete() {
        HomeContentDatabase.databaseWritableExecutable.execute(() -> {
            homeContentDao.deleteAllData();
        });
    }
}
