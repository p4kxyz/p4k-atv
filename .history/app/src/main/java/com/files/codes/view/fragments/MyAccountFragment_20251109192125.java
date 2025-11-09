package com.files.codes.view.fragments;

import static android.content.Context.MODE_PRIVATE;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.appcompat.widget.SwitchCompat;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.files.codes.R;
import com.files.codes.database.DatabaseHelper;
import com.files.codes.model.subscription.ActiveStatus;
import com.files.codes.utils.Constants;
import com.files.codes.utils.PreferenceUtils;
import com.files.codes.view.LoginChooserActivity;
import com.files.codes.view.MainActivity;
import com.files.codes.view.fragments.WatchHistoryFragment;

import androidx.fragment.app.Fragment;

public class MyAccountFragment extends Fragment  {
    private final String TAG = "MyAccountFragment";
    private static final String PREF_USE_EXTERNAL_PLAYER = "use_external_player";
    
    private Button sign_out, login;
    private TextView user_name;
    private TextView user_email;
    private TextView expire_date;
    private TextView active_plan;
    private TextView status_tv;
    private SwitchCompat externalPlayerSwitch;
    private DatabaseHelper db;
    private LinearLayout userDataLayout;


    public MyAccountFragment() {
        // Required empty public constructor
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // IMPORTANT: Don't call super.onCreateView() because GridFragment uses different layout
        // Inflate the layout for this fragment directly
        View view = inflater.inflate(R.layout.fragment_my_account, container, false);
        db = new DatabaseHelper(getContext());
        
        // FORCE hiển thị external player container ngay từ đầu
        View externalPlayerContainer = view.findViewById(R.id.external_player_setting_container);
        
        if (externalPlayerContainer != null) {
            externalPlayerContainer.setVisibility(View.VISIBLE);
        }
        
        initViews(view);

        sign_out.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                signOut();
            }
        });
        login.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(getContext(), LoginChooserActivity.class));
                //getActivity().finish();
            }
        });
        
        // Setup external player switch
        setupExternalPlayerSwitch();
        
        return view;
    }

    private void initViews(View view) {
        sign_out = view.findViewById(R.id.sign_out_button);
        login = view.findViewById(R.id.login_button);
        user_name = view.findViewById(R.id.userNameTv);
        user_email = view.findViewById(R.id.userEmailTv);
        active_plan = view.findViewById(R.id.activePlanTv);
        expire_date = view.findViewById(R.id.expireDateTv);
        userDataLayout = view.findViewById(R.id.user_data_layout);
        status_tv = view.findViewById(R.id.status_tv);
        externalPlayerSwitch = view.findViewById(R.id.external_player_switch);
        
        // Tìm và hiển thị external player container - LUÔN HIỂN THỊ
        View externalPlayerContainer = view.findViewById(R.id.external_player_setting_container);
        if (externalPlayerContainer != null) {
            externalPlayerContainer.setVisibility(View.VISIBLE);
        }
        if (PreferenceUtils.isLoggedIn(getContext())) {
            login.setVisibility(View.GONE);
            user_name.setText(db.getUserData().getName());
            user_email.setText(db.getUserData().getEmail());
            ActiveStatus activeStatus = db.getActiveStatusData();
            active_plan.setText(activeStatus.getPackageTitle());
            expire_date.setText(activeStatus.getExpireDate());
        } else {
            userDataLayout.setVisibility(View.GONE);
            login.setVisibility(View.VISIBLE);
            user_name.setVisibility(View.GONE);
            user_email.setVisibility(View.GONE);
            sign_out.setVisibility(View.GONE);
            status_tv.setText(R.string.you_are_not_logged_in);
        }
        
        // Setup external player switch sau khi init views - LUÔN GỌI
        setupExternalPlayerSwitch();
    }

    private void signOut() {
        DatabaseHelper databaseHelper = new DatabaseHelper(getContext());
        String userId = databaseHelper.getUserData().getUserId();
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user != null) {
            FirebaseAuth.getInstance().signOut();
        }
        if (userId != null) {
            SharedPreferences.Editor editor = getContext().getSharedPreferences(Constants.USER_LOGIN_STATUS, MODE_PRIVATE).edit();
            editor.putBoolean(Constants.USER_LOGIN_STATUS, false);
            editor.apply();

            databaseHelper.deleteUserData();
            PreferenceUtils.clearSubscriptionSavedData(getContext());
            
            // CRITICAL: Clear watch history cache to prevent user data leakage
            WatchHistoryFragment.clearWatchHistoryCache(getContext());

            startActivity(new Intent(getContext(), MainActivity.class));
            getActivity().finish();
        }
    }
    
    private void setupExternalPlayerSwitch() {
        if (externalPlayerSwitch == null) {
            return;
        }
        
        // Load saved preference
        SharedPreferences prefs = getContext().getSharedPreferences(Constants.USER_LOGIN_STATUS, MODE_PRIVATE);
        boolean useExternalPlayer = prefs.getBoolean(PREF_USE_EXTERNAL_PLAYER, false);
        externalPlayerSwitch.setChecked(useExternalPlayer);
        
        // Handle switch changes
        externalPlayerSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                SharedPreferences.Editor editor = getContext().getSharedPreferences(Constants.USER_LOGIN_STATUS, MODE_PRIVATE).edit();
                editor.putBoolean(PREF_USE_EXTERNAL_PLAYER, isChecked);
import android.util.Log;
                editor.apply();
            }
        });
    }
    
    public static boolean shouldUseExternalPlayer(android.content.Context context) {
        SharedPreferences prefs = context.getSharedPreferences(Constants.USER_LOGIN_STATUS, MODE_PRIVATE);
        return prefs.getBoolean(PREF_USE_EXTERNAL_PLAYER, false);
    }

}
