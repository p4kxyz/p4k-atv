package com.files.codes.viewmodel;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;

import com.files.codes.model.HomeContentList;

public class HomeContentViewModel extends AndroidViewModel {
    private HomeContentRepository repository;
    private LiveData<HomeContentList> homeContentLiveData;

    public HomeContentViewModel(@NonNull Application application) {
        super(application);
        repository = new HomeContentRepository(application);
        homeContentLiveData = repository.getHomeContentLiveData();
    }

    public void insert(HomeContentList homeContent){
        repository.insert(homeContent);
    }

    public void update(HomeContentList homeContent){
        repository.update(homeContent);
    }

    public void deleteAll(){
        repository.delete();
    }

    public LiveData<HomeContentList> getHomeContentLiveData(){
        return homeContentLiveData;
    }

}
