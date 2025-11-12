package com.files.codes.view;

import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.RequiresApi;
import androidx.fragment.app.FragmentActivity;

import com.files.codes.AppConfig;
import com.files.codes.R;
import com.files.codes.database.DatabaseHelper;
import com.files.codes.model.api.ApiService;
import com.files.codes.model.config.Configuration;
import com.files.codes.utils.PreferenceUtils;
import com.files.codes.utils.RetrofitClient;

import com.files.codes.view.fragments.SplashFragment;
import com.files.codes.view.fragments.testFolder.HomeNewActivity;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;

/*
 * Main Activity class that loads {@link MainFragment}.
 */
public class MainActivity extends FragmentActivity {
    private static final int REQUEST_CODE = 123;
    private static final String TAG = "TvSplashScreen";
    public static boolean COMPLETED_SPLASH = false;
    private static final long SPLASH_DURATION_MS = 1000;


    @RequiresApi(api = Build.VERSION_CODES.N)
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        getConfigData();

            getSupportFragmentManager().beginTransaction()
                    .replace(R.id.main_browse_fragment, new SplashFragment())
                    .commitNow();
    }


    @Override
    protected void onResume() {
        super.onResume();
    }

    private void getConfigData(){
        Retrofit retrofit = RetrofitClient.getRetrofitInstance();
        ApiService api = retrofit.create(ApiService.class);
        Call<Configuration> call = api.getConfiguration(AppConfig.API_KEY);
        call.enqueue(new Callback<Configuration>() {
            @Override
            public void onResponse(Call<Configuration> call, Response<Configuration> response) {
                if (response.code() == 200 && response.body() != null){
                    Configuration configuration = response.body();
                    DatabaseHelper db = new DatabaseHelper(getApplicationContext());
                    db.insertConfigurationData(configuration);
                    checkUserData(configuration);
                }else {
                    Toast.makeText(getApplicationContext(), getApplicationContext().getString(R.string.something_went_wrong), Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<Configuration> call, Throwable t) {
                Log.e(TAG, "onResponse: "+ t.getLocalizedMessage() );
                Toast.makeText(getApplicationContext(), getApplicationContext().getString(R.string.something_went_wrong), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void checkUserData(Configuration configuration) {
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                if (configuration.getAppConfig().getMandatoryLogin()) {
                    if (PreferenceUtils.isLoggedIn(getApplicationContext())) {
                        Intent intent = new Intent(getApplicationContext(), HomeNewActivity.class);
                        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK);
                        intent.addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION);
                        startActivity(intent);
                    } else {
                        startActivity(new Intent(getApplicationContext(), LoginChooserActivity.class));
                    }
                }else {
                    Intent intent = new Intent(getApplicationContext(), HomeNewActivity.class);
                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK);
                    intent.addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION);
                    startActivity(intent);
                }
                finish();
            }

        }, SPLASH_DURATION_MS);
    }
}