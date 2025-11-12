package com.files.codes.view;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.Toast;

import com.firebase.ui.auth.AuthUI;
import com.firebase.ui.auth.ErrorCodes;
import com.firebase.ui.auth.IdpResponse;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.files.codes.AppConfig;
import com.files.codes.R;
import com.files.codes.database.DatabaseHelper;
import com.files.codes.model.api.ApiService;
import com.files.codes.model.subscription.ActiveStatus;
import com.files.codes.model.subscription.User;
import com.files.codes.utils.Constants;
import com.files.codes.utils.RetrofitClient;
import com.files.codes.utils.ToastMsg;

import java.util.Arrays;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;

public class LoginChooserActivity extends Activity {
    private static final String TAG = "ActivityLoginChooser";
    private static final int RC_PHONE_SIGN_IN = 124;
    private FirebaseAuth firebaseAuth;
    private static int RC_GOOGLE_SIGN_IN = 123;
    private ProgressBar progressBar;
    private Button googleSignInButton, phoneSignInButton;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login_chooser);

        progressBar = findViewById(R.id.progress_bar);
        googleSignInButton = findViewById(R.id.google_signIn_button);
        phoneSignInButton = findViewById(R.id.phone_signIn_button);

        firebaseAuth = FirebaseAuth.getInstance();
        DatabaseHelper db = new DatabaseHelper(LoginChooserActivity.this);
        User user = db.getUserData();
        if (user != null && user.getUserId() != null) {
            updateSubscriptionStatus(user.getUserId());
        }
    }

    @Override
    protected void onStart() {
        super.onStart();
        if (!AppConfig.ENABLE_GOOGLE_LOGIN){
            googleSignInButton.setVisibility(View.GONE);
        }
        if (!AppConfig.ENABLE_PHONE_LOGIN){
            phoneSignInButton.setVisibility(View.GONE);
        }

        googleSignInButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                googleSignIn();
            }
        });

        phoneSignInButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                phoneSignIn();
            }
        });
    }

    public void signUpBtn(View view) {
        Intent intent = new Intent(LoginChooserActivity.this, SignUpActivity.class);
        startActivity(intent);
    }

    public void emailSignInBtn(View view) {
        Intent intent = new Intent(LoginChooserActivity.this, LoginActivity.class);
        startActivity(intent);
    }

    private void phoneSignIn() {
        progressBar.setVisibility(View.VISIBLE);
        if (firebaseAuth.getCurrentUser() != null) {
            /*if (!FirebaseAuth.getInstance().getCurrentUser().getUid().isEmpty()) {
                final String phone = FirebaseAuth.getInstance().getCurrentUser().getPhoneNumber();
                //already signed in
                if (phone != null)
                    sendPhoneSignInDataToServer();
            }*/

            sendPhoneSignInDataToServer();

        } else {
            progressBar.setVisibility(View.GONE);
            // Choose authentication providers
            List<AuthUI.IdpConfig> providers = Arrays.asList(
                    new AuthUI.IdpConfig.PhoneBuilder().build());
            // Create and launch sign-in intent
            startActivityForResult(
                    AuthUI.getInstance()
                            .createSignInIntentBuilder()
                            .setAvailableProviders(providers)
                            .build(),
                    RC_PHONE_SIGN_IN);
        }
    }

    private void googleSignIn() {
        progressBar.setVisibility(View.VISIBLE);
        if (firebaseAuth.getCurrentUser() != null) {
            if (!FirebaseAuth.getInstance().getCurrentUser().getUid().isEmpty()) {
                final String email = FirebaseAuth.getInstance().getCurrentUser().getEmail();
                //already signed in
                if (email != null)
                    sendGoogleSignInDataToServer();
            }

        } else {
            progressBar.setVisibility(View.GONE);
            // Choose authentication providers
            List<AuthUI.IdpConfig> providers = Arrays.asList(
                    new AuthUI.IdpConfig.GoogleBuilder().build());
            // Create and launch sign-in intent
            startActivityForResult(
                    AuthUI.getInstance()
                            .createSignInIntentBuilder()
                            .setAvailableProviders(providers)
                            .build(),
                    RC_GOOGLE_SIGN_IN);
        }
    }

    private void sendGoogleSignInDataToServer() {
        progressBar.setVisibility(View.VISIBLE);
        String email = FirebaseAuth.getInstance().getCurrentUser().getEmail();
        String uid = FirebaseAuth.getInstance().getCurrentUser().getUid();
        String name = FirebaseAuth.getInstance().getCurrentUser().getDisplayName();
        Log.e("FireAuth", email + ", " + uid + ", " + name);
        Retrofit retrofit = RetrofitClient.getRetrofitInstance();
        ApiService api = retrofit.create(ApiService.class);
        Call<User> call = api.getGoogleAuthStatus(AppConfig.API_KEY, uid, email, name);
        call.enqueue(new Callback<User>() {
            @Override
            public void onResponse(Call<User> call, Response<User> response) {
                if (response.code() == 200) {
                    if (response.body().getStatus().equalsIgnoreCase("success")) {
                        User user = response.body();
                        DatabaseHelper db = new DatabaseHelper(LoginChooserActivity.this);

                        db.deleteUserData();
                        db.insertUserData(user);

                        //save user login time, expire time
                        updateSubscriptionStatus(user.getUserId());
                    }
                }
            }

            @Override
            public void onFailure(Call<User> call, Throwable t) {
                Toast.makeText(LoginChooserActivity.this, "failed", Toast.LENGTH_SHORT).show();
                Log.e("LoginTV", "response: " + t.getLocalizedMessage());
            }
        });
    }

    private void sendPhoneSignInDataToServer() {
        progressBar.setVisibility(View.VISIBLE);
        String phone = "";
        String uid = "";
        try {
            phone = FirebaseAuth.getInstance().getCurrentUser().getPhoneNumber();
            uid = FirebaseAuth.getInstance().getCurrentUser().getUid();
        }catch (NullPointerException e){
            new ToastMsg(LoginChooserActivity.this).toastIconError(getResources().getString(R.string.something_went_wrong));
            return;
        }
        Retrofit retrofit = RetrofitClient.getRetrofitInstance();
        ApiService api = retrofit.create(ApiService.class);
        Call<User> call = api.getPhoneAuthStatus(AppConfig.API_KEY, uid, phone);
        call.enqueue(new Callback<User>() {
            @Override
            public void onResponse(Call<User> call, Response<User> response) {
                if (response.code() == 200) {
                    if (response.body().getStatus().equalsIgnoreCase("success")) {
                        User user = response.body();
                        DatabaseHelper db = new DatabaseHelper(LoginChooserActivity.this);
                        db.deleteUserData();
                        db.insertUserData(user);
                        //save user login time, expire time
                        updateSubscriptionStatus(user.getUserId());
                    }
                }
            }

            @Override
            public void onFailure(Call<User> call, Throwable t) {
                Toast.makeText(LoginChooserActivity.this, "failed", Toast.LENGTH_SHORT).show();
                Log.e("LoginTV", "response: " + t.getLocalizedMessage());
            }
        });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == RC_GOOGLE_SIGN_IN) {
            final IdpResponse response = IdpResponse.fromResultIntent(data);
            if (resultCode == RESULT_OK) {
                // Successfully signed in
                final FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
                if (user != null) {
                    sendGoogleSignInDataToServer();
                } else {
                    //empty
                    googleSignIn();
                }
            } else {
                // sign in failed
                if (response == null) {
                    Toast.makeText(this, "Canceled", Toast.LENGTH_SHORT).show();
                    return;
                }
                if (response.getError().getErrorCode() == ErrorCodes.NO_NETWORK) {
                    Toast.makeText(this, "No internet", Toast.LENGTH_SHORT).show();
                    return;
                }
                if (response.getError().getErrorCode() == ErrorCodes.UNKNOWN_ERROR) {
                    Toast.makeText(this, "Error !!", Toast.LENGTH_SHORT).show();
                }
            }
        } else if (requestCode == RC_PHONE_SIGN_IN) {
            final IdpResponse response = IdpResponse.fromResultIntent(data);
            if (resultCode == RESULT_OK) {
                // Successfully signed in
                final FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
                if (user != null) {
                    sendPhoneSignInDataToServer();
                } else {
                    //empty
                    phoneSignIn();
                }
            } else {
                // sign in failed
                if (response == null) {
                    Toast.makeText(this, "Canceled", Toast.LENGTH_SHORT).show();
                    return;
                }
                if (response.getError().getErrorCode() == ErrorCodes.NO_NETWORK) {
                    Toast.makeText(this, "No internet", Toast.LENGTH_SHORT).show();
                    return;
                }
                if (response.getError().getErrorCode() == ErrorCodes.UNKNOWN_ERROR) {
                    Toast.makeText(this, "Error !!", Toast.LENGTH_SHORT).show();
                }
            }
        }
    }

    public void updateSubscriptionStatus(String userId) {
        progressBar.setVisibility(View.VISIBLE);
        //get saved user id
        Retrofit retrofit = RetrofitClient.getRetrofitInstance();
        ApiService subscriptionApi = retrofit.create(ApiService.class);

        Call<ActiveStatus> call = subscriptionApi.getActiveStatus(AppConfig.API_KEY, userId);
        call.enqueue(new Callback<ActiveStatus>() {
            @Override
            public void onResponse(Call<ActiveStatus> call, Response<ActiveStatus> response) {
                if (response.code() == 200) {
                    if (response.body() != null) {
                        ActiveStatus activeStatus = response.body();
                        DatabaseHelper db = new DatabaseHelper(LoginChooserActivity.this);
                        db.insertActiveStatusData(activeStatus);

                        Intent intent = new Intent(LoginChooserActivity.this, MainActivity.class);
                        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK);
                        intent.addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION);

                        SharedPreferences.Editor sp = getSharedPreferences(Constants.USER_LOGIN_STATUS, MODE_PRIVATE).edit();
                        sp.putBoolean(Constants.USER_LOGIN_STATUS, true);
                        sp.apply();
                        sp.commit();

                        startActivity(intent);
                        finish();
                        progressBar.setVisibility(View.GONE);
                    }
                }
            }

            @Override
            public void onFailure(Call<ActiveStatus> call, Throwable t) {
                t.printStackTrace();
                progressBar.setVisibility(View.GONE);
                Toast.makeText(LoginChooserActivity.this, R.string.something_went_wrong, Toast.LENGTH_SHORT).show();
            }
        });
    }
}