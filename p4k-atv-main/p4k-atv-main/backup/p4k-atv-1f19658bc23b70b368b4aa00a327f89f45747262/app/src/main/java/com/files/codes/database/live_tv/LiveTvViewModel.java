package com.files.codes.database.live_tv;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;

public class LiveTvViewModel extends AndroidViewModel {
    private static final String TAG = "LiveTvViewModel";
    private LiveTvRepository repository;
    private LiveData<LiveTvList> liveTvListLiveData;

    public LiveTvViewModel(@NonNull Application application){
        super(application);
        repository = new LiveTvRepository(application);
        liveTvListLiveData = repository.getLiveTvLiveData();
    }

    public void insert(LiveTvList liveTvList){
        repository.insert(liveTvList);
    }

    public void update(LiveTvList liveTvList){
        repository.update(liveTvList);
    }

    public void delete(){
        repository.deleteAll();
    }

    public LiveData<LiveTvList> getLiveTvListLiveData(){
        return liveTvListLiveData;
    }
}
