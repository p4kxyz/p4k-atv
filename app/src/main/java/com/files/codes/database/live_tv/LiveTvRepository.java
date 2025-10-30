package com.files.codes.database.live_tv;
import android.app.Application;
import androidx.lifecycle.LiveData;

public class LiveTvRepository {
  private LiveTvDao liveTvDao;
  private LiveData<LiveTvList> tvLiveData;

  public LiveTvRepository(Application application){
     LiveTvDatabase database = LiveTvDatabase.getInstance(application);
      liveTvDao = database.liveTvDao();
      tvLiveData = liveTvDao.getAllLiveTv();
  }

  public void insert(LiveTvList liveTvList){
     LiveTvDatabase.databaseExecutorsService.execute(()->{
          liveTvDao.insert(liveTvList);
      });
  }

    public void update(LiveTvList liveTvList){
       LiveTvDatabase.databaseExecutorsService.execute(() ->{
            liveTvDao.update(liveTvList);
        });
    }

    public void deleteAll(){
        LiveTvDatabase.databaseExecutorsService.execute(()->{
            liveTvDao.deleteAll();
        });
    }

    public LiveData<LiveTvList> getLiveTvLiveData(){
        return tvLiveData;
    }
}
