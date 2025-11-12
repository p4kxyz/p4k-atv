package com.files.codes.view;

import android.content.Context;
import android.content.res.Configuration;
import android.os.Build;
import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;

import java.util.Locale;

public class BaseActivity extends AppCompatActivity {
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setVietnameseLocale();
    }
    
    @Override
    protected void attachBaseContext(Context base) {
        super.attachBaseContext(setLocale(base));
    }
    
    private void setVietnameseLocale() {
        Locale vietnamese = new Locale("vi", "VN");
        Locale.setDefault(vietnamese);
        
        Configuration config = getResources().getConfiguration();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            config.setLocale(vietnamese);
        } else {
            config.locale = vietnamese;
        }
        getResources().updateConfiguration(config, getResources().getDisplayMetrics());
    }
    
    private Context setLocale(Context context) {
        Locale vietnamese = new Locale("vi", "VN");
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            Configuration configuration = new Configuration(context.getResources().getConfiguration());
            configuration.setLocale(vietnamese);
            return context.createConfigurationContext(configuration);
        } else {
            Configuration configuration = context.getResources().getConfiguration();
            configuration.locale = vietnamese;
            context.getResources().updateConfiguration(configuration, 
                context.getResources().getDisplayMetrics());
            return context;
        }
    }
}