package com.files.codes.utils;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;


import com.files.codes.database.DatabaseHelper;
import com.files.codes.service.SubscriptionStatusUpdateTask;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;


public class PreferenceUtils {
    public static final String TAG = "PreferenceUtils";
    public static final String PREF_PREFERRED_AUDIO = "preferred_audio";
    public static final String PREF_PREFERRED_SUBTITLE = "preferred_subtitle";

    public static void setPreferredAudio(Context context, String languageCode) {
        SharedPreferences preferences = context.getSharedPreferences(Constants.USER_LOGIN_STATUS, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = preferences.edit();
        editor.putString(PREF_PREFERRED_AUDIO, languageCode);
        editor.apply();
    }

    public static String getPreferredAudio(Context context) {
        SharedPreferences preferences = context.getSharedPreferences(Constants.USER_LOGIN_STATUS, Context.MODE_PRIVATE);
        return preferences.getString(PREF_PREFERRED_AUDIO, "default");
    }

    public static void setPreferredSubtitle(Context context, String languageCode) {
        SharedPreferences preferences = context.getSharedPreferences(Constants.USER_LOGIN_STATUS, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = preferences.edit();
        editor.putString(PREF_PREFERRED_SUBTITLE, languageCode);
        editor.apply();
    }

    public static String getPreferredSubtitle(Context context) {
        SharedPreferences preferences = context.getSharedPreferences(Constants.USER_LOGIN_STATUS, Context.MODE_PRIVATE);
        return preferences.getString(PREF_PREFERRED_SUBTITLE, "default");
    }


    public static boolean isActivePlan(Context context) {
        String status = getSubscriptionStatus(context);
        Log.e("Status", status);
        return status.equals("active");
    }

    public static boolean isLoggedIn(Context context) {
        SharedPreferences preferences = context.getSharedPreferences(Constants.USER_LOGIN_STATUS, Context.MODE_PRIVATE);
        return preferences.getBoolean(Constants.USER_LOGIN_STATUS, false);
    }

    public static boolean isMandatoryLogin(Context context) {
        DatabaseHelper db = new DatabaseHelper(context);
        return db.getConfigurationData().getAppConfig().getMandatoryLogin();
    }

    public static String getSubscriptionStatus(Context context) {
        DatabaseHelper db = new DatabaseHelper(context);
        return db.getActiveStatusData() != null ?db.getActiveStatusData().getStatus() : "inactive";
    }

    public static long getCurrentTime() {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy:MM:dd:HH:mm");
        String currentDateAndTime = sdf.format(new Date());

        Date date = null;
        try {
            date = sdf.parse(currentDateAndTime);
        } catch (ParseException e) {
            e.printStackTrace();
        }
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(date);

        return calendar.getTimeInMillis();
    }

    public static long getExpireTime() {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy:MM:dd:HH:mm");
        String currentDateAndTime = sdf.format(new Date());

        Date date = null;
        try {
            date = sdf.parse(currentDateAndTime);
        } catch (ParseException e) {
            e.printStackTrace();
        }
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(date);
        calendar.add(Calendar.HOUR, 2);

        return calendar.getTimeInMillis();
    }

//    public static boolean isValid(Context context) {
//        String savedTime = getUpdatedTime(context);
//        long currentTime = getCurrentTime();
//        return Long.parseLong(savedTime) > currentTime;
//    }

    private static String getUpdatedTime(Context context) {
        DatabaseHelper db = new DatabaseHelper(context);
        return String.valueOf(db.getActiveStatusData().getExpireTime());
    }

    public static void updateSubscriptionStatus(final Context context) {
        SubscriptionStatusUpdateTask task = new SubscriptionStatusUpdateTask(context, getUserId(context));
        task.execute();
    }

    public static void clearSubscriptionSavedData(Context context) {
        //now save to sharedPreference
        DatabaseHelper db = new DatabaseHelper(context);
        db.deleteAllActiveStatusData();
    }

    public static String getUserId(Context context) {
        DatabaseHelper db = new DatabaseHelper(context);
        return db.getUserData() != null ? db.getUserData().getUserId() : null;
    }

    public static String getUserEmail(Context context) {
        DatabaseHelper db = new DatabaseHelper(context);
        return db.getUserData() != null ? db.getUserData().getEmail() : null;
    }
    
    // OTA Update helper methods
    public static void saveString(Context context, String key, String value) {
        SharedPreferences preferences = context.getSharedPreferences("ota_preferences", Context.MODE_PRIVATE);
        preferences.edit().putString(key, value).apply();
    }
    
    public static String getString(Context context, String key, String defaultValue) {
        SharedPreferences preferences = context.getSharedPreferences("ota_preferences", Context.MODE_PRIVATE);
        return preferences.getString(key, defaultValue);
    }

}
