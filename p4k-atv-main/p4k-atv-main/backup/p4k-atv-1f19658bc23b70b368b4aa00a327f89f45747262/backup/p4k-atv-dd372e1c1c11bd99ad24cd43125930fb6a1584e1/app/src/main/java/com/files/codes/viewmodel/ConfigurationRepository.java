package com.files.codes.viewmodel;

import android.app.Application;

import com.files.codes.model.config.Configuration;
import com.files.codes.viewmodel.config.ConfigDao;


public class ConfigurationRepository {
    private ConfigDao configDao;
    private Configuration configuration;

    public ConfigurationRepository(Application application){
        CommonDatabase database = CommonDatabase.getInstance(application);
        configDao = database.configDao();
    }

    public void insertConfigData(Configuration configuration){
        CommonDatabase.databaseWriteExecutor.execute(() -> {
            configDao.inset(configuration);
        });
    }

    public void update(Configuration configuration){
        CommonDatabase.databaseWriteExecutor.execute(()->{
            configDao.update(configuration);
        });
    }

    public void deleteConfigData(){
        CommonDatabase.databaseWriteExecutor.execute(()->{
            configDao.deleteAll();
        });
    }

    public Configuration getConfiguration(){
        CommonDatabase.databaseWriteExecutor.execute(()->{
           configuration = configDao.getConfigData();
        });

        return configuration;
    }
}
