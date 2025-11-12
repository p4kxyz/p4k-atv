package com.files.codes.view;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.ProgressBar;

import com.files.codes.AppConfig;
import com.files.codes.R;
import com.files.codes.database.DatabaseHelper;
import com.files.codes.model.api.ApiService;
import com.files.codes.model.subscription.User;
import com.files.codes.service.SubscriptionStatusUpdateTask;
import com.files.codes.utils.Constants;
import com.files.codes.utils.RetrofitClient;
import com.files.codes.utils.ToastMsg;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;

public class LoginActivity extends Activity {
    private final String TAG = "LoginActivity";
    private EditText etEmail, etPass;
    private ProgressBar progressBar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);
        etEmail = findViewById(R.id.email_edit_text);
        etPass = findViewById(R.id.password_edit_text);
        progressBar = findViewById(R.id.progress_login);
    }

    public void loginBtn(View view) {
        if (!isValidEmailAddress(etEmail.getText().toString())) {
            new ToastMsg(LoginActivity.this).toastIconError("Please enter valid email");
        } else if (etPass.getText().toString().equals("")) {
            new ToastMsg(LoginActivity.this).toastIconError("Please enter password");
        } else {
            String email = etEmail.getText().toString();
            String pass = etPass.getText().toString();
            login(email, pass);
        }
    }

    private void login(String email, final String password) {
        progressBar.setVisibility(View.VISIBLE);
        Retrofit retrofit = RetrofitClient.getRetrofitInstance();
        ApiService api = retrofit.create(ApiService.class);
        Call<User> call = api.postLoginStatus(AppConfig.API_KEY, email, password);
        call.enqueue(new Callback<User>() {
            @Override
            public void onResponse(Call<User> call, Response<User> response) {
                Log.e(TAG, "onResponse: " + response.code() );
                if (response.code() == 200) {
                    assert response.body() != null;
                    if (response.body().getStatus().equalsIgnoreCase("success")) {
                        User user = response.body();
                        DatabaseHelper db = new DatabaseHelper(LoginActivity.this);
                        db.insertUserData(user);
                        SharedPreferences.Editor preferences = getSharedPreferences(Constants.USER_LOGIN_STATUS, MODE_PRIVATE).edit();
                        preferences.putBoolean(Constants.USER_LOGIN_STATUS, true);
                        preferences.apply();

                        //save user login time, expire time
                        updateSubscriptionStatus(user.getUserId());

                    } else {
                        new ToastMsg(LoginActivity.this).toastIconError(response.body().getData());
                        progressBar.setVisibility(View.GONE);
                    }
                }else {
                    new ToastMsg(LoginActivity.this).toastIconError(getString(R.string.something_went_wrong));
                    progressBar.setVisibility(View.GONE);
                }
            }

            @Override
            public void onFailure(Call<User> call, Throwable t) {
                progressBar.setVisibility(View.GONE);
                new ToastMsg(LoginActivity.this).toastIconError(getString(R.string.error_toast));
            }
        });
    }


    public boolean isValidEmailAddress(String email) {
        String ePattern = "^[a-zA-Z0-9.!#$%&'*+/=?^_`{|}~-]+@((\\[[0-9]{1,3}\\.[0-9]{1,3}\\.[0-9]{1,3}\\.[0-9]{1,3}\\])|(([a-zA-Z\\-0-9]+\\.)+[a-zA-Z]{2,}))$";
        java.util.regex.Pattern p = java.util.regex.Pattern.compile(ePattern);
        java.util.regex.Matcher m = p.matcher(email);
        return m.matches();
    }

    public void updateSubscriptionStatus(String userId) {
        SubscriptionStatusUpdateTask task = new SubscriptionStatusUpdateTask(userId, this, output -> {
            if (output) {
                Intent intent = new Intent(LoginActivity.this, MainActivity.class);
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK);
                intent.addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION);
                startActivity(intent);
                finish();
                progressBar.setVisibility(View.GONE);
            }
        });
        task.execute();


    }
}