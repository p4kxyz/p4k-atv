package com.files.codes.database;

import static android.content.Context.MODE_PRIVATE;
import static com.files.codes.utils.Constants.ACTIVE_STATUS_PREF;
import static com.files.codes.utils.Constants.ACTIVE_STATUS_PREF_DATA;
import static com.files.codes.utils.Constants.CONFIG_PREF;
import static com.files.codes.utils.Constants.CONFIG_PREF_DATA;
import static com.files.codes.utils.Constants.USER_PREF;
import static com.files.codes.utils.Constants.USER_PREF_DATA;

import android.content.Context;
import android.content.SharedPreferences;

import com.google.gson.Gson;
import com.files.codes.model.config.Configuration;
import com.files.codes.model.subscription.ActiveStatus;
import com.files.codes.model.subscription.User;

public class DatabaseHelper {
    private Context context;

    public DatabaseHelper(Context context) {
        this.context = context;
    }

    public Configuration getConfigurationData(){
        //get data from local database
        SharedPreferences prefs = this.context.getSharedPreferences(CONFIG_PREF, Context.MODE_PRIVATE);
        String json = prefs.getString(CONFIG_PREF_DATA, null);
        //convert this into object
        Gson gson = new Gson();
        return gson.fromJson(json, Configuration.class);
    }

    public void deleteAllAppConfig() {
        SharedPreferences.Editor editor = this.context.getSharedPreferences(CONFIG_PREF, MODE_PRIVATE).edit();
        editor.putString(CONFIG_PREF_DATA, null);
        editor.apply();
    }

    public void insertConfigurationData(Configuration configuration) {
        //convert object data to json string
        String data = new Gson().toJson(configuration);
        SharedPreferences.Editor editor = this.context.getSharedPreferences(CONFIG_PREF, MODE_PRIVATE).edit();
        editor.putString(CONFIG_PREF_DATA, data);
        editor.apply();
    }

    public void insertActiveStatusData(ActiveStatus activeStatus) {
        String data = new Gson().toJson(activeStatus);
        SharedPreferences.Editor editor = this.context.getSharedPreferences(ACTIVE_STATUS_PREF, MODE_PRIVATE).edit();
        editor.putString(ACTIVE_STATUS_PREF_DATA, data);
        editor.apply();
    }

    public ActiveStatus getActiveStatusData() {
        //get data from local database
        SharedPreferences prefs = this.context.getSharedPreferences(ACTIVE_STATUS_PREF, Context.MODE_PRIVATE);
        String json = prefs.getString(ACTIVE_STATUS_PREF_DATA, null);
        //convert this into object
        Gson gson = new Gson();
        ActiveStatus activeStatus = gson.fromJson(json, ActiveStatus.class);
        return gson.fromJson(json, ActiveStatus.class);
    }

    public void deleteAllActiveStatusData() {
        SharedPreferences.Editor editor = this.context.getSharedPreferences(ACTIVE_STATUS_PREF, MODE_PRIVATE).edit();
        editor.putString(ACTIVE_STATUS_PREF_DATA, null);
        editor.apply();
    }

    public void insertUserData(User user) {
        //convert user data to json response
        String data = new Gson().toJson(user);
        SharedPreferences.Editor editor = this.context.getSharedPreferences(USER_PREF, MODE_PRIVATE).edit();
        editor.putString(USER_PREF_DATA, data);
        editor.apply();
    }

    public User getUserData() {
        //get data from local database
        SharedPreferences prefs = this.context.getSharedPreferences(USER_PREF, Context.MODE_PRIVATE);
        String json = prefs.getString(USER_PREF_DATA, null);
        //convert this into object
        Gson gson = new Gson();
        return gson.fromJson(json, User.class);
    }

    public void deleteUserData() {
        SharedPreferences.Editor editor = this.context.getSharedPreferences(USER_PREF, MODE_PRIVATE).edit();
        editor.putString(USER_PREF_DATA, null);
        editor.apply();
    }
}
