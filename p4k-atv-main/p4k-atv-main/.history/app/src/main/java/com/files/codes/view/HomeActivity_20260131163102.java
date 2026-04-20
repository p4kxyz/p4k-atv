package com.files.codes.view;


import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;
import androidx.leanback.widget.HorizontalGridView;
import androidx.leanback.widget.SearchOrbView;
import androidx.leanback.widget.VerticalGridView;

import android.content.Context;
import android.content.Intent;
import android.content.res.Configuration;
import android.graphics.Rect;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.Transformation;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;
import android.graphics.Color;

import java.util.Locale;

import com.files.codes.R;
import com.files.codes.utils.DataProvider;
import com.files.codes.utils.NetworkInst;
import com.files.codes.utils.PreferenceUtils;
import com.files.codes.utils.Utils;
import com.files.codes.view.fragments.CountryFragment;
import com.files.codes.view.fragments.CustomHeadersFragment;
import com.files.codes.view.fragments.CustomRowsFragment;
import com.files.codes.view.fragments.FavouriteFragment;
import com.files.codes.view.fragments.GenreFragment;
import com.files.codes.view.fragments.HomeFragment;
import com.files.codes.view.fragments.MoviesFragment;
import com.files.codes.view.fragments.MoviesWithFilterFragment;
import com.files.codes.view.fragments.MyAccountFragment;
import com.files.codes.view.fragments.TvSeriesFragment;
import com.files.codes.view.fragments.WatchHistoryFragment;
import com.files.codes.view.OTAUpdateManager;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.LinkedHashMap;

import io.reactivex.disposables.CompositeDisposable;

public class HomeActivity extends FragmentActivity {
    public static final String INTENT_EXTRA_VIDEO = "intentExtraVideo";
    private CustomHeadersFragment headersFragment;
    private Fragment rowsFragment;
    private LinkedHashMap<Integer, Fragment> fragments;
    private SearchOrbView orbView;
    private ImageView settingsOrb; // NEW: Settings Orb reference
    private boolean navigationDrawerOpen;
    private static final float NAVIGATION_DRAWER_SCALE_FACTOR = 0.9f;

    private CustomFrameLayout customFrameLayout;
    private boolean rowsContainerFocused;
    private CompositeDisposable disposable;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        setVietnameseLocale();
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);
        
        // CRITICAL TEST: Verify onCreate is running
        // Log.d("HomeActivity", "🔴 onCreate() STARTED - Basic setup complete");

        orbView = findViewById(R.id.custom_search_orb);
        orbView.setOrbColor(getResources().getColor(R.color.colorPrimary));
        orbView.bringToFront();
        orbView.setOnOrbClickedListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(getApplicationContext(), SearchActivity.class);
                startActivity(intent);
            }
        });

        // Initialize Settings Orb
        settingsOrb = findViewById(R.id.settings_orb);
        if (settingsOrb != null) {
            Log.e("HomeActivity", "✅ FOUND Settings Orb!");
            Log.e("HomeActivity", "   - Visibility: " + (settingsOrb.getVisibility() == View.VISIBLE ? "VISIBLE" : "GONE/INVISIBLE"));
            Log.e("HomeActivity", "   - Elevation: " + settingsOrb.getElevation());
            
            settingsOrb.bringToFront();
            settingsOrb.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    Log.e("HomeActivity", "⚙️ Settings Orb CLICKED!");
                    showQuickSettingsDialog();
                }
            });
            
            // Post log to check actual layout position
            settingsOrb.post(new Runnable() {
                @Override
                public void run() {
                    if (settingsOrb != null) {
                        int[] location = new int[2];
                        settingsOrb.getLocationOnScreen(location);
                        Log.e("HomeActivity", "📍 Settings Orb Post-Layout:");
                        Log.e("HomeActivity", "   - Screen X: " + location[0] + ", Y: " + location[1]);
                        Log.e("HomeActivity", "   - Width: " + settingsOrb.getWidth() + ", Height: " + settingsOrb.getHeight());
                        
                        // Force visible just in case
                        settingsOrb.setVisibility(View.VISIBLE);
                        settingsOrb.bringToFront();
                    }
                }
            });
        } else {
             Log.e("HomeActivity", "❌ FATAL: Settings Orb (R.id.settings_orb) NOT FOUND in View Hierarchy!");
        }

        //load home content data and save it to database
        // Log.d("HomeActivity", "🔴 BEFORE creating disposable");
        disposable = new CompositeDisposable();
        // Log.d("HomeActivity", "🔴 BEFORE creating DataProvider");
        DataProvider provider = new DataProvider(this, disposable);
        // Log.d("HomeActivity", "🔴 DataProvider created successfully");

        // TEST: Add immediate log to verify this code path executes
        // Log.d("HomeActivity", "🟢 BEFORE Handler - About to start background tasks");
        
        // Log.d("HomeActivity", "🔴 CREATING Handler now...");
        new Handler().post(new Runnable() {
            @Override
            public void run() {
                // Log.d("HomeActivity", "🟢 INSIDE Handler - Starting background tasks");
                
                provider.getAndSaveHomeContentDataFromServer(HomeActivity.this);
                provider.getMoviesFromServer(HomeActivity.this);
                provider.getTvSeriesDataFromServer(HomeActivity.this);
                // provider.getLiveTvDataFromServer(HomeActivity.this); // Removed Live TV
                
                // AUTO SYNC: Tự động sync watch history khi vào app
                autoSyncWatchHistoryOnAppStart();
                
                // Log.d("HomeActivity", "🟢 BEFORE OTA Check - About to check for updates");
                
                // Check for OTA updates
                checkForUpdates();
                
                // Log.d("HomeActivity", "🟢 AFTER OTA Check - OTA check completed");
            }
        });

        //get subscription status and save to sharedPreference
        PreferenceUtils.updateSubscriptionStatus(this);

        fragments = new LinkedHashMap<>();

        int CATEGORIES_NUMBER = 7; // Back to 7 (removed Watch History from menu)
        for (int i = 0; i < CATEGORIES_NUMBER; i++) {
            if (i == 0) {
                HomeFragment fragment = new HomeFragment();
                fragments.put(i, fragment);
            } else if (i == 1) {
                // ✅ Use MoviesWithFilterFragment (includes filter panel + MoviesFragment)
                try {
                    // Log.e("HomeActivity", "🔴🔴🔴 Creating MoviesWithFilterFragment...");
                    MoviesWithFilterFragment fragment = new MoviesWithFilterFragment();
                    // Log.e("HomeActivity", "🔴🔴🔴 MoviesWithFilterFragment created successfully!");
                    Bundle bundle = new Bundle();
                    bundle.putInt("menu", i);
                    fragment.setArguments(bundle);
                    fragments.put(i, fragment);
                    // Log.e("HomeActivity", "🔴🔴🔴 MoviesWithFilterFragment added to fragments map!");
                } catch (Exception e) {
                    Log.e("HomeActivity", "❌❌❌ FAILED to create MoviesWithFilterFragment: " + e.getMessage(), e);
                    // Fallback to old MoviesFragment
                    MoviesFragment fragment = new MoviesFragment();
                    Bundle bundle = new Bundle();
                    bundle.putInt("menu", i);
                    fragment.setArguments(bundle);
                    fragments.put(i, fragment);
                }
            } else if (i == 2) {
                TvSeriesFragment fragment = new TvSeriesFragment();
                Bundle bundle = new Bundle();
                bundle.putInt("menu", i);
                fragment.setArguments(bundle);
                fragments.put(i, fragment);
            } else if (i == 3) { // Changed from i == 4 to i == 3
                GenreFragment fragment = new GenreFragment();
                Bundle bundle = new Bundle();
                bundle.putInt("menu", i);
                fragment.setArguments(bundle);
                fragments.put(i, fragment);
            } else if (i == 4) { // Changed from i == 5 to i == 4
                CountryFragment fragment = new CountryFragment();
                Bundle bundle = new Bundle();
                bundle.putInt("menu", i);
                fragment.setArguments(bundle);
                fragments.put(i, fragment);
            } else if (i == 5) { // Favorite Fragment
                FavouriteFragment fragment = new FavouriteFragment();
                Bundle bundle = new Bundle();
                bundle.putInt("menu", i);
                fragment.setArguments(bundle);
                fragments.put(i, fragment);
            } else if (i == 6) { // My Account Fragment (moved from 7 to 6)
                MyAccountFragment fragment = new MyAccountFragment();
                Bundle bundle = new Bundle();
                bundle.putInt("menu", i);
                fragment.setArguments(bundle);
                fragments.put(i, fragment);
            } else {
                CustomRowsFragment fragment = new CustomRowsFragment();
                Bundle bundle = new Bundle();
                bundle.putInt("menu", i);
                fragment.setArguments(bundle);
                fragments.put(i, fragment);
            }
        }


        headersFragment = new CustomHeadersFragment();
        rowsFragment = (HomeFragment) fragments.get(0);
        customFrameLayout = (CustomFrameLayout) ((ViewGroup) this.findViewById(android.R.id.content)).getChildAt(0);
        setupCustomFrameLayout();


        if (new NetworkInst(this).isNetworkAvailable()) {
            FragmentManager fragmentManager = getSupportFragmentManager();
            FragmentTransaction transaction = fragmentManager.beginTransaction();
            transaction
                    .replace(R.id.header_container, headersFragment, "CustomHeadersFragment")
                    .replace(R.id.rows_container, rowsFragment, "CustomRowsFragment");
            transaction.commit();
        } else {
            // show no internet page
            Intent intent = new Intent(this, ErrorActivity.class);
            startActivity(intent);
            finish();
        }
    }
    public LinkedHashMap<Integer, Fragment> getFragments() {
        return fragments;
    }



    private void setupCustomFrameLayout() {
        customFrameLayout.setOnChildFocusListener(new CustomFrameLayout.OnChildFocusListener() {
            @Override
            public boolean onRequestFocusInDescendants(int direction, Rect previouslyFocusedRect) {
                if (headersFragment.getView() != null && headersFragment.getView().requestFocus(direction, previouslyFocusedRect)) {
                    return true;
                }
                return rowsFragment.getView() != null && rowsFragment.getView().requestFocus(direction, previouslyFocusedRect);
            }

            @Override
            public void onRequestChildFocus(View child, View focused) {
                int childId = child.getId();
                if (childId == R.id.rows_container) {
                    rowsContainerFocused = true;
                    toggleHeadersFragment(false);

                } else if (childId == R.id.header_container) {
                    rowsContainerFocused = false;
                    toggleHeadersFragment(true);

                }
            }
        });

        customFrameLayout.setOnFocusSearchListener(new CustomFrameLayout.OnFocusSearchListener() {
            @Override
            public View onFocusSearch(View focused, int direction) {
                // 🎯 Debug logging
                String focusedViewName = focused != null ? focused.getClass().getSimpleName() : "null";
                int focusedId = focused != null ? focused.getId() : -1;
                // Log.d("HomeActivity", "🔍 onFocusSearch - Focused: " + focusedViewName + " (ID: " + focusedId + "), Direction: " + direction);
                
                if (direction == View.FOCUS_LEFT) {
                    // Log.d("HomeActivity", "⬅️ LEFT pressed - isVerticalScrolling: " + isVerticalScrolling() + ", navDrawerOpen: " + navigationDrawerOpen);
                    
                    // 🎯 Special handling for hero thumbnails grid
                    if (focusedId == R.id.hero_thumbnails_grid || isViewInsideHeroThumbnails(focused)) {
                        // Log.d("HomeActivity", "🎬 Focus is in hero thumbnails - force go to menu");
                        return getVerticalGridView(headersFragment);
                    }
                    
                    if (isVerticalScrolling() || navigationDrawerOpen) {
                        return focused;
                    }
                    return getVerticalGridView(headersFragment);
                } else if (direction == View.FOCUS_RIGHT) {
                    if (focused == orbView) {
                        return settingsOrb != null && settingsOrb.getVisibility() == View.VISIBLE ? settingsOrb : focused;
                    }
                    if (isVerticalScrolling() || !navigationDrawerOpen) {
                        return focused;
                    }
                    return getVerticalGridView(rowsFragment);
                } else if (focused == orbView && direction == View.FOCUS_DOWN) {
                    return navigationDrawerOpen ? getVerticalGridView(headersFragment) : getVerticalGridView(rowsFragment);
                } else if (focused == settingsOrb && direction == View.FOCUS_DOWN) {
                    return navigationDrawerOpen ? getVerticalGridView(headersFragment) : getVerticalGridView(rowsFragment);
                } else if (focused == settingsOrb && direction == View.FOCUS_LEFT) {
                    return orbView != null && orbView.getVisibility() == View.VISIBLE ? orbView : focused;
                } else if (focused != orbView && focused != settingsOrb && direction == View.FOCUS_UP) {
                    // Smart navigation for UP:
                    // If focusing on left side -> Search Orb
                    // If focusing on right side -> Settings Orb
                    int[] location = new int[2];
                    focused.getLocationOnScreen(location);
                    int screenWidth = getResources().getDisplayMetrics().widthPixels;
                    if (location[0] + focused.getWidth()/2 > screenWidth / 2) {
                        return settingsOrb != null && settingsOrb.getVisibility() == View.VISIBLE ? settingsOrb : orbView;
                    }
                    return orbView != null && orbView.getVisibility() == View.VISIBLE ? orbView : null;
                } else {
                    return null;
                }
            }
            
            private boolean isViewInsideHeroThumbnails(View view) {
                // Check if view is a child of hero_thumbnails_grid
                View parent = view;
                while (parent != null) {
                    if (parent.getId() == R.id.hero_thumbnails_grid) {
                        return true;
                    }
                    if (parent.getParent() instanceof View) {
                        parent = (View) parent.getParent();
                    } else {
                        break;
                    }
                }
                return false;
            }
        });
    }

    public VerticalGridView getVerticalGridView(Fragment fragment) {

        try {
            if (fragment instanceof TvSeriesFragment) {
                // TvSeriesFragment extends GridFragment -> Fragment (NOT BaseRowSupportFragment)
                // We need to access the grid view via reflection on GridFragment properties if possible,
                // or assume it exposes a method.
                // For now, let's try the correct class name format, but this likely needs a custom method in GridFragment.
                Class baseRowFragmentClass = getClassLoader().loadClass("androidx.leanback.app.BaseSupportFragment");
                Method getVerticalGridViewMethod = baseRowFragmentClass.getDeclaredMethod("getVerticalGridView");
                getVerticalGridViewMethod.setAccessible(true);
                VerticalGridView gridView = (VerticalGridView) getVerticalGridViewMethod.invoke(fragment, (Object[]) null);

                return gridView;
            } else if (fragment instanceof MoviesFragment) {

                Class baseRowFragmentClass = getClassLoader().loadClass("androidx.leanback.app.BaseSupportFragment");
                Method getVerticalGridViewMethod = baseRowFragmentClass.getDeclaredMethod("getVerticalGridView");
                getVerticalGridViewMethod.setAccessible(true);
                VerticalGridView gridView = (VerticalGridView) getVerticalGridViewMethod.invoke(fragment, (Object[]) null);
                return gridView;

            } else if (fragment instanceof MoviesWithFilterFragment) {
                // MoviesWithFilterFragment wraps MoviesFragment inside, need to get inner fragment
                // For now, return null to prevent LEFT key interference (filter panel handles its own navigation)
                return null;

            } else if (fragment instanceof FavouriteFragment) {

                Class baseRowFragmentClass = getClassLoader().loadClass("androidx.leanback.app.BaseSupportFragment");
                Method getVerticalGridViewMethod = baseRowFragmentClass.getDeclaredMethod("getVerticalGridView");
                getVerticalGridViewMethod.setAccessible(true);
                VerticalGridView gridView = (VerticalGridView) getVerticalGridViewMethod.invoke(fragment, (Object[]) null);
                return gridView;

            } else if (fragment instanceof GenreFragment) {

                Class baseRowFragmentClass = getClassLoader().loadClass("androidx.leanback.app.BaseSupportFragment");
                Method getVerticalGridViewMethod = baseRowFragmentClass.getDeclaredMethod("getVerticalGridView");
                getVerticalGridViewMethod.setAccessible(true);
                VerticalGridView gridView = (VerticalGridView) getVerticalGridViewMethod.invoke(fragment, (Object[]) null);
                return gridView;

            } else if (fragment instanceof CountryFragment) {
                Class baseRowFragmentClass = getClassLoader().loadClass("androidx.leanback.app.BaseSupportFragment");
                Method getVerticalGridViewMethod = baseRowFragmentClass.getDeclaredMethod("getVerticalGridView");
                getVerticalGridViewMethod.setAccessible(true);
                VerticalGridView gridView = (VerticalGridView) getVerticalGridViewMethod.invoke(fragment, (Object[]) null);
                return gridView;

            } else if (fragment instanceof MyAccountFragment) {
                Class baseRowFragmentClass = getClassLoader().loadClass("androidx.leanback.app.BaseSupportFragment");
                Method getVerticalGridViewMethod = baseRowFragmentClass.getDeclaredMethod("getVerticalGridView");
                getVerticalGridViewMethod.setAccessible(true);
                VerticalGridView gridView = (VerticalGridView) getVerticalGridViewMethod.invoke(fragment, (Object[]) null);
                return gridView;

            } else {
                Class baseRowFragmentClass = getClassLoader().loadClass("androidx.leanback.app.BaseRowSupportFragment");
                Method getVerticalGridViewMethod = baseRowFragmentClass.getDeclaredMethod("getVerticalGridView");
                getVerticalGridViewMethod.setAccessible(true);
                VerticalGridView gridView = (VerticalGridView) getVerticalGridViewMethod.invoke(fragment, (Object[]) null);

                return gridView;
            }


        } catch (NoSuchMethodException e) {
            e.printStackTrace();
        } catch (InvocationTargetException e) {
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }

        return null;
    }

    public synchronized void toggleHeadersFragment(final boolean doOpen) {
        boolean condition = (doOpen != isNavigationDrawerOpen());
        if (condition) {
            final View headersContainer = (View) headersFragment.getView().getParent();
            final View rowsContainer = (View) rowsFragment.getView().getParent();

            final float delta = headersContainer.getWidth() * NAVIGATION_DRAWER_SCALE_FACTOR;

            // Show sidebar immediately when opening
            if (doOpen) {
                headersContainer.setVisibility(View.VISIBLE);
            }

            // get current margin (a previous animation might have been interrupted)
            final int currentHeadersMargin = (((ViewGroup.MarginLayoutParams) headersContainer.getLayoutParams()).leftMargin);
            final int currentRowsMargin = (((ViewGroup.MarginLayoutParams) rowsContainer.getLayoutParams()).leftMargin);

            // calculate destination
            final int headersDestination = (doOpen ? 0 : (int) (0 - delta));
            final int rowsDestination = (doOpen ? (Utils.dpToPx(150, this)) : 0);

            // calculate the delta (destination - current)
            final int headersDelta = headersDestination - currentHeadersMargin;
            final int rowsDelta = rowsDestination - currentRowsMargin;

            Animation animation = new Animation() {
                @Override
                protected void applyTransformation(float interpolatedTime, Transformation t) {
                    ViewGroup.MarginLayoutParams headersParams = (ViewGroup.MarginLayoutParams) headersContainer.getLayoutParams();
                    headersParams.leftMargin = (int) (currentHeadersMargin + headersDelta * interpolatedTime);
                    headersContainer.setLayoutParams(headersParams);

                    ViewGroup.MarginLayoutParams rowsParams = (ViewGroup.MarginLayoutParams) rowsContainer.getLayoutParams();
                    rowsParams.leftMargin = (int) (currentRowsMargin + rowsDelta * interpolatedTime);
                    rowsContainer.setLayoutParams(rowsParams);
                }
            };

            animation.setAnimationListener(new Animation.AnimationListener() {
                @Override
                public void onAnimationStart(Animation animation) {
                    navigationDrawerOpen = doOpen;
                }

                @Override
                public void onAnimationEnd(Animation animation) {
                    // Hide sidebar after closing animation completes
                    if (!doOpen) {
                        headersContainer.setVisibility(View.GONE);
                        if (rowsFragment instanceof CustomRowsFragment) {
                           ((CustomRowsFragment) rowsFragment).refresh();
                        }
                    }
                }

                @Override
                public void onAnimationRepeat(Animation animation) {
                }

            });

            animation.setDuration(200);
            ((View) rowsContainer.getParent()).startAnimation(animation);
        }
    }

    private boolean isVerticalScrolling() {
        try {
            // don't run transition
            return getVerticalGridView(headersFragment).getScrollState()
                    != HorizontalGridView.SCROLL_STATE_IDLE
                    || getVerticalGridView(rowsFragment).getScrollState()
                    != HorizontalGridView.SCROLL_STATE_IDLE;
        } catch (Exception e) {
            e.printStackTrace();
        }

        return false;
    }

    public synchronized boolean isNavigationDrawerOpen() {
        return navigationDrawerOpen;
    }

    public void updateCurrentRowsFragment(Fragment fragment) {
        rowsFragment = fragment;
    }


    @Override
    public void onBackPressed() {

        if (rowsContainerFocused) {
            toggleHeadersFragment(true);
            rowsContainerFocused = false;
        } else {
            super.onBackPressed();
        }

    }



    @Override
    protected void onDestroy() {
        disposable.dispose();
        super.onDestroy();
    }

    @Override
    protected void onPause() {
        super.onPause();
        disposable.dispose();
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
    
    /**
     * AUTO SYNC: Tự động sync watch history khi vào app
     */
    private void autoSyncWatchHistoryOnAppStart() {
        try {
            com.files.codes.utils.sync.WatchHistorySyncManager syncManager = 
                com.files.codes.utils.sync.WatchHistorySyncManager.getInstance(this);
            syncManager.autoSyncOnAppStart();
        } catch (Exception e) {
            Log.e("HomeActivity", "Error auto syncing watch history", e);
        }
    }
    
    /**
     * Check for OTA updates when app starts
     */
    private void checkForUpdates() {
        Log.d("HomeActivity", "🚀 Starting OTA update check...");
        try {
            Log.d("HomeActivity", "� Getting OTAUpdateManager instance...");
            OTAUpdateManager otaManager = OTAUpdateManager.getInstance(this);
            Log.d("HomeActivity", "✅ OTAUpdateManager instance created: " + (otaManager != null ? "SUCCESS" : "NULL"));
            
            Log.d("HomeActivity", "🔍 Calling checkForUpdates...");
            otaManager.checkForUpdates();
            Log.d("HomeActivity", "✅ checkForUpdates called successfully");
        } catch (Exception e) {
            Log.e("HomeActivity", "❌ Error checking for updates", e);
            Log.e("HomeActivity", "❌ Exception details: " + e.getClass().getSimpleName() + " - " + e.getMessage());
            if (e.getCause() != null) {
                Log.e("HomeActivity", "❌ Cause: " + e.getCause().getMessage());
            }
        }
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

    private void showQuickSettingsDialog() {
        final android.content.SharedPreferences pref = getSharedPreferences(com.files.codes.utils.Constants.USER_LOGIN_STATUS, MODE_PRIVATE);
        final boolean useExternalPlayer = pref.getBoolean("use_external_player", false);
        final boolean useSoftwareAudio = pref.getBoolean("audio_priority_sw", true); // Default to True (SW)

        String[] items = new String[3];
        items[0] = "Trình phát: " + (useExternalPlayer ? "Bên ngoài (VLC/MX)" : "Mặc định (ExoPlayer)");
        items[1] = "Ưu tiên Audio: " + (useSoftwareAudio ? "Phạm mềm (SW - Khuyên dùng)" : "Phần cứng (HW)");
        items[2] = "Cài đặt hiển thị phụ đề";

        new android.app.AlertDialog.Builder(this)
            .setTitle("Cài đặt nhanh")
            .setItems(items, new android.content.DialogInterface.OnClickListener() {
                @Override
                public void onClick(android.content.DialogInterface dialog, int which) {
                    android.content.SharedPreferences.Editor editor = pref.edit();
                    if (which == 0) {
                        boolean newValue = !useExternalPlayer;
                        editor.putBoolean("use_external_player", newValue);
                        editor.apply();
                        new com.files.codes.utils.ToastMsg(HomeActivity.this).toastIconSuccess(newValue ? "Đã chọn: Trình phát ngoài" : "Đã chọn: ExoPlayer");
                        dialog.dismiss();
                        showQuickSettingsDialog();
                    } else if (which == 1) {
                        boolean newValue = !useSoftwareAudio;
                        editor.putBoolean("audio_priority_sw", newValue);
                        editor.apply();
                        new com.files.codes.utils.ToastMsg(HomeActivity.this).toastIconSuccess(newValue ? "Đã chọn: Ưu tiên Audio Software" : "Đã chọn: Ưu tiên Audio Hardware");
                        dialog.dismiss();
                        showQuickSettingsDialog();
                    } else if (which == 2) {
                        dialog.dismiss();
                        openSubtitleSettingsDialog();
                    }
                }
            })
            .setNegativeButton("Đóng", null)
            .show();
    }

    private void saveSubtitleSetting(String key, int value) {
        android.content.SharedPreferences.Editor editor = getSharedPreferences("subtitle_settings", MODE_PRIVATE).edit();
        editor.putInt(key, value);
        editor.apply();
    }

    private void saveSubtitleSetting(String key, boolean value) {
        android.content.SharedPreferences.Editor editor = getSharedPreferences("subtitle_settings", MODE_PRIVATE).edit();
        editor.putBoolean(key, value);
        editor.apply();
    }

    private void saveSubtitleSetting(String key, float value) {
        android.content.SharedPreferences.Editor editor = getSharedPreferences("subtitle_settings", MODE_PRIVATE).edit();
        editor.putFloat(key, value);
        editor.apply();
    }

    private void openSubtitleSettingsDialog() {
        // Subtitle settings dialog with all customization options
        android.app.AlertDialog.Builder builder = new android.app.AlertDialog.Builder(this);
        builder.setTitle("Cài đặt phụ đề");
        
        // Create a ScrollView to handle long content
        ScrollView scrollView = new ScrollView(this);
        
        // Create a vertical layout
        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(50, 40, 50, 10);
        
        android.content.SharedPreferences prefs = getSharedPreferences("subtitle_settings", MODE_PRIVATE);
        
        // Font size controls
        TextView fontSizeLabel = new TextView(this);
        fontSizeLabel.setText("Cỡ chữ:");
        fontSizeLabel.setTextSize(16);
        fontSizeLabel.setPadding(0, 10, 0, 10);
        
        LinearLayout fontSizeLayout = new LinearLayout(this);
        fontSizeLayout.setOrientation(LinearLayout.HORIZONTAL);
        
        Button fontSizeMinusBtn = new Button(this);
        fontSizeMinusBtn.setText("-");
        Button fontSizePlusBtn = new Button(this);
        fontSizePlusBtn.setText("+");
        TextView fontSizeTV = new TextView(this);
        fontSizeTV.setText(prefs.getInt("font_size", 20) + "sp");
        fontSizeTV.setGravity(android.view.Gravity.CENTER);
        fontSizeTV.setPadding(20, 0, 20, 0);
        
        fontSizeLayout.addView(fontSizeMinusBtn);
        fontSizeLayout.addView(fontSizeTV);
        fontSizeLayout.addView(fontSizePlusBtn);
        
        // Font type controls
        TextView fontTypeLabel = new TextView(this);
        fontTypeLabel.setText("Kiểu chữ:");
        fontTypeLabel.setTextSize(16);
        fontTypeLabel.setPadding(0, 20, 0, 10);
        
        LinearLayout fontTypeLayout = new LinearLayout(this);
        fontTypeLayout.setOrientation(LinearLayout.HORIZONTAL);
        
        Button fontTypePrevBtn = new Button(this);
        fontTypePrevBtn.setText("◀");
        Button fontTypeNextBtn = new Button(this);
        fontTypeNextBtn.setText("▶");
        TextView fontTypeTV = new TextView(this);
        
        String[] fontNames = {"Mặc định", "Sans Serif", "Serif", "Monospace", "Tiếng Việt"};
        int currentFontType = prefs.getInt("font_type", 4); // Default to Vietnamese
        // Bounds checking to prevent crash
        if (currentFontType >= fontNames.length) {
            currentFontType = 4; // Default to Vietnamese
            prefs.edit().putInt("font_type", currentFontType).apply();
        }
        fontTypeTV.setText(fontNames[currentFontType]);
        fontTypeTV.setGravity(android.view.Gravity.CENTER);
        fontTypeTV.setPadding(20, 0, 20, 0);
        
        fontTypeLayout.addView(fontTypePrevBtn);
        fontTypeLayout.addView(fontTypeTV);
        fontTypeLayout.addView(fontTypeNextBtn);
        
        // Position controls
        TextView positionLabel = new TextView(this);
        positionLabel.setText("Vị trí:");
        positionLabel.setTextSize(16);
        positionLabel.setPadding(0, 20, 0, 10);
        
        LinearLayout positionLayout = new LinearLayout(this);
        positionLayout.setOrientation(LinearLayout.HORIZONTAL);
        
        Button positionUpBtn = new Button(this);
        positionUpBtn.setText("Lên");
        Button positionDownBtn = new Button(this);
        positionDownBtn.setText("Xuống");
        TextView positionTV = new TextView(this);
        
        // Position logic - offset from default position (negative = closer to bottom, positive = further up)
        int currentOffset = prefs.getInt("vertical_offset", 0);
        positionTV.setText("Dịch chuyển (" + (currentOffset > 0 ? "+" : "") + currentOffset + "%)");
        positionTV.setGravity(android.view.Gravity.CENTER);
        positionTV.setPadding(20, 0, 20, 0);
        
        positionLayout.addView(positionUpBtn);
        positionLayout.addView(positionTV);
        positionLayout.addView(positionDownBtn);
        
        // Background switch
        LinearLayout backgroundLayout = new LinearLayout(this);
        backgroundLayout.setOrientation(LinearLayout.HORIZONTAL);
        backgroundLayout.setPadding(0, 20, 0, 10);
        TextView backgroundLabel = new TextView(this);
        backgroundLabel.setText("Nền: ");
        backgroundLabel.setTextSize(16);
        Switch backgroundSwitch = new Switch(this);
        backgroundSwitch.setChecked(prefs.getBoolean("background", false));
        
        backgroundLayout.addView(backgroundLabel);
        backgroundLayout.addView(backgroundSwitch);
        
        // Text Color controls
        TextView textColorLabel = new TextView(this);
        textColorLabel.setText("Màu chữ:");
        textColorLabel.setTextSize(16);
        textColorLabel.setPadding(0, 20, 0, 10);
        
        LinearLayout textColorLayout = new LinearLayout(this);
        textColorLayout.setOrientation(LinearLayout.HORIZONTAL);
        
        Button textColorPrevBtn = new Button(this);
        textColorPrevBtn.setText("◀");
        Button textColorNextBtn = new Button(this);
        textColorNextBtn.setText("▶");
        TextView textColorTV = new TextView(this);
        
        String[] colorNames = {"Trắng", "Vàng", "Đỏ", "Xanh lá", "Xanh dương", "Cam", "Hồng", "Xanh lơ"};
        int[] colorValues = {Color.WHITE, Color.YELLOW, Color.RED, Color.GREEN, 
                            Color.BLUE, 0xFFFF8C00, 0xFFFFC0CB, Color.CYAN}; // Orange, Pink
        int currentTextColor = prefs.getInt("text_color", 0);
        // Bounds checking to prevent crash
        if (currentTextColor >= colorNames.length) {
            currentTextColor = 0; // Default to White
            prefs.edit().putInt("text_color", currentTextColor).apply();
        }
        textColorTV.setText(colorNames[currentTextColor]);
        textColorTV.setTextColor(colorValues[currentTextColor]);
        textColorTV.setGravity(android.view.Gravity.CENTER);
        textColorTV.setPadding(20, 0, 20, 0);
        
        textColorLayout.addView(textColorPrevBtn);
        textColorLayout.addView(textColorTV);
        textColorLayout.addView(textColorNextBtn);
        
        // Outline Color controls
        TextView outlineColorLabel = new TextView(this);
        outlineColorLabel.setText("Màu viền:");
        outlineColorLabel.setTextSize(16);
        outlineColorLabel.setPadding(0, 20, 0, 10);
        
        LinearLayout outlineColorLayout = new LinearLayout(this);
        outlineColorLayout.setOrientation(LinearLayout.HORIZONTAL);
        
        Button outlineColorPrevBtn = new Button(this);
        outlineColorPrevBtn.setText("◀");
        Button outlineColorNextBtn = new Button(this);
        outlineColorNextBtn.setText("▶");
        TextView outlineColorTV = new TextView(this);
        
        String[] outlineColorNames = {"Trong suốt", "Đen", "Trắng", "Đỏ", "Xanh dương", "Vàng"};
        int[] outlineColorValues = {Color.TRANSPARENT, Color.BLACK, Color.WHITE, Color.RED, Color.BLUE, Color.YELLOW};
        int currentOutlineColor = prefs.getInt("outline_color", 1);
        // Bounds checking to prevent crash
        if (currentOutlineColor >= outlineColorNames.length) {
            currentOutlineColor = 1; // Default to Black
            prefs.edit().putInt("outline_color", currentOutlineColor).apply();
        }
        outlineColorTV.setText(outlineColorNames[currentOutlineColor]);
        // Show the actual color like text color does
        if (currentOutlineColor == 0) {
            outlineColorTV.setTextColor(Color.GRAY); // Show gray for "None"
        } else {
            outlineColorTV.setTextColor(outlineColorValues[currentOutlineColor]);
        }
        outlineColorTV.setGravity(android.view.Gravity.CENTER);
        outlineColorTV.setPadding(20, 0, 20, 0);
        outlineColorTV.setGravity(android.view.Gravity.CENTER);
        
        outlineColorLayout.addView(outlineColorPrevBtn);
        outlineColorLayout.addView(outlineColorTV);
        outlineColorLayout.addView(outlineColorNextBtn);
        
        // Playback Speed controls
        TextView playbackSpeedLabel = new TextView(this);
        playbackSpeedLabel.setText("Tốc độ phát mặc định:");
        playbackSpeedLabel.setTextSize(16);
        playbackSpeedLabel.setPadding(0, 20, 0, 10);
        
        LinearLayout playbackSpeedLayout = new LinearLayout(this);
        playbackSpeedLayout.setOrientation(LinearLayout.HORIZONTAL);
        playbackSpeedLayout.setGravity(android.view.Gravity.CENTER);
        
        float currentSpeed = prefs.getFloat("playback_speed", 1.0f);
        
        Button speed05 = new Button(this);
        speed05.setText("0.5x");
        speed05.setTextSize(12);
        if (currentSpeed == 0.5f) speed05.setBackgroundColor(Color.GREEN);
        
        Button speed075 = new Button(this);
        speed075.setText("0.75x");
        speed075.setTextSize(12);
        if (currentSpeed == 0.75f) speed075.setBackgroundColor(Color.GREEN);
        
        Button speed10 = new Button(this);
        speed10.setText("1.0x");
        speed10.setTextSize(12);
        if (currentSpeed == 1.0f) speed10.setBackgroundColor(Color.GREEN);
        
        Button speed15 = new Button(this);
        speed15.setText("1.5x");
        speed15.setTextSize(12);
        if (currentSpeed == 1.5f) speed15.setBackgroundColor(Color.GREEN);
        
        Button speed20 = new Button(this);
        speed20.setText("2.0x");
        speed20.setTextSize(12);
        if (currentSpeed == 2.0f) speed20.setBackgroundColor(Color.GREEN);
        
        Button speed30 = new Button(this);
        speed30.setText("3.0x");
        speed30.setTextSize(12);
        if (currentSpeed == 3.0f) speed30.setBackgroundColor(Color.GREEN);
        
        playbackSpeedLayout.addView(speed05);
        playbackSpeedLayout.addView(speed075);
        playbackSpeedLayout.addView(speed10);
        playbackSpeedLayout.addView(speed15);
        playbackSpeedLayout.addView(speed20);
        playbackSpeedLayout.addView(speed30);
        
        // Reset button
        Button resetBtn = new Button(this);
        resetBtn.setText("Khôi phục mặc định");
        resetBtn.setPadding(0, 30, 0, 0);
        
        // Add all views to layout
        layout.addView(fontSizeLabel);
        layout.addView(fontSizeLayout);
        layout.addView(fontTypeLabel);
        layout.addView(fontTypeLayout);
        layout.addView(positionLabel);
        layout.addView(positionLayout);
        layout.addView(backgroundLayout);
        layout.addView(textColorLabel);
        layout.addView(textColorLayout);
        layout.addView(outlineColorLabel);
        layout.addView(outlineColorLayout);
        layout.addView(playbackSpeedLabel);
        layout.addView(playbackSpeedLayout);
        layout.addView(resetBtn);
        
        // Set up button listeners
        fontSizeMinusBtn.setOnClickListener(v -> {
            int currentSize = Integer.parseInt(fontSizeTV.getText().toString().replace("sp", ""));
            if (currentSize > 12) {
                currentSize -= 2;
                fontSizeTV.setText(currentSize + "sp");
                saveSubtitleSetting("font_size", currentSize);
            }
        });
        
        fontSizePlusBtn.setOnClickListener(v -> {
            int currentSize = Integer.parseInt(fontSizeTV.getText().toString().replace("sp", ""));
            if (currentSize < 40) {
                currentSize += 2;
                fontSizeTV.setText(currentSize + "sp");
                saveSubtitleSetting("font_size", currentSize);
            }
        });
        
        fontTypePrevBtn.setOnClickListener(v -> {
            int currentType = prefs.getInt("font_type", 0);
            currentType = (currentType - 1 + fontNames.length) % fontNames.length;
            fontTypeTV.setText(fontNames[currentType]);
            saveSubtitleSetting("font_type", currentType);
        });
        
        fontTypeNextBtn.setOnClickListener(v -> {
            int currentType = prefs.getInt("font_type", 0);
            currentType = (currentType + 1) % fontNames.length;
            fontTypeTV.setText(fontNames[currentType]);
            saveSubtitleSetting("font_type", currentType);
        });
        
        positionUpBtn.setOnClickListener(v -> {
            int offset = prefs.getInt("vertical_offset", 0);
            offset += 5; // Move up (increase offset from bottom - higher value = further from bottom)
            offset = Math.min(offset, 80); // Max 80% from bottom
            String newPositionText = offset == 0 ? "Giữa" : "Lên +" + offset + "%";
            positionTV.setText(newPositionText);
            saveSubtitleSetting("vertical_offset", offset);
        });
        
        positionDownBtn.setOnClickListener(v -> {
            int offset = prefs.getInt("vertical_offset", 0);
            offset -= 5; // Move down (decrease offset - negative values = closer to bottom edge)
            offset = Math.max(offset, -10); // Min -10% (very close to bottom edge)
            String newPositionText = offset == 0 ? "Giữa" : "Xuống " + offset + "%";
            positionTV.setText(newPositionText);
            saveSubtitleSetting("vertical_offset", offset);
        });
        
        backgroundSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
            saveSubtitleSetting("background", isChecked);
        });
        
        // Text color controls
        textColorPrevBtn.setOnClickListener(v -> {
            int index = (prefs.getInt("text_color", 0) - 1 + colorNames.length) % colorNames.length;
            saveSubtitleSetting("text_color", index);
            textColorTV.setText(colorNames[index]);
            textColorTV.setTextColor(colorValues[index]);
        });
        
        textColorNextBtn.setOnClickListener(v -> {
            int index = (prefs.getInt("text_color", 0) + 1) % colorNames.length;
            saveSubtitleSetting("text_color", index);
            textColorTV.setText(colorNames[index]);
            textColorTV.setTextColor(colorValues[index]);
        });
        
        // Outline color controls
        outlineColorPrevBtn.setOnClickListener(v -> {
            int index = (prefs.getInt("outline_color", 1) - 1 + outlineColorNames.length) % outlineColorNames.length;
            saveSubtitleSetting("outline_color", index);
            outlineColorTV.setText(outlineColorNames[index]);
            // Show the actual color like text color does
            if (index == 0) {
                outlineColorTV.setTextColor(Color.GRAY); // Show gray for "None"
            } else {
                outlineColorTV.setTextColor(outlineColorValues[index]);
            }
        });
        
        outlineColorNextBtn.setOnClickListener(v -> {
            int index = (prefs.getInt("outline_color", 1) + 1) % outlineColorNames.length;
            saveSubtitleSetting("outline_color", index);
            outlineColorTV.setText(outlineColorNames[index]);
            // Show the actual color like text color does
            if (index == 0) {
                outlineColorTV.setTextColor(Color.GRAY); // Show gray for "None"
            } else {
                outlineColorTV.setTextColor(outlineColorValues[index]);
            }
        });
        
        // Playback speed controls
        View.OnClickListener speedClickListener = v -> {
            float speed = 1.0f;
            if (v == speed05) speed = 0.5f;
            else if (v == speed075) speed = 0.75f;
            else if (v == speed10) speed = 1.0f;
            else if (v == speed15) speed = 1.5f;
            else if (v == speed20) speed = 2.0f;
            else if (v == speed30) speed = 3.0f;
            
            // Reset all button backgrounds
            speed05.setBackgroundColor(Color.TRANSPARENT);
            speed075.setBackgroundColor(Color.TRANSPARENT);
            speed10.setBackgroundColor(Color.TRANSPARENT);
            speed15.setBackgroundColor(Color.TRANSPARENT);
            speed20.setBackgroundColor(Color.TRANSPARENT);
            speed30.setBackgroundColor(Color.TRANSPARENT);
            
            // Highlight selected button
            ((Button)v).setBackgroundColor(Color.GREEN);
            
            // Save speed
            saveSubtitleSetting("playback_speed", speed);
        };
        
        speed05.setOnClickListener(speedClickListener);
        speed075.setOnClickListener(speedClickListener);
        speed10.setOnClickListener(speedClickListener);
        speed15.setOnClickListener(speedClickListener);
        speed20.setOnClickListener(speedClickListener);
        speed30.setOnClickListener(speedClickListener);
        
        resetBtn.setOnClickListener(v -> {
            android.content.SharedPreferences.Editor editor = prefs.edit();
            editor.putInt("font_size", 20);
            editor.putInt("font_type", 4); // Default to Vietnamese
            editor.putInt("vertical_offset", 0);
            editor.putBoolean("background", false);
            editor.putInt("text_color", 0); // White
            editor.putInt("outline_color", 1); // Black
            editor.putFloat("playback_speed", 1.0f); // Normal speed
            editor.apply();
            
            fontSizeTV.setText("20sp");
            fontTypeTV.setText(fontNames[4]); // Default to Vietnamese
            positionTV.setText("Giữa");
            backgroundSwitch.setChecked(false);
            textColorTV.setText(colorNames[0]);
            textColorTV.setTextColor(colorValues[0]);
            outlineColorTV.setText(outlineColorNames[1]);
            outlineColorTV.setTextColor(outlineColorValues[1]); // Show black color for reset
            
            Toast.makeText(HomeActivity.this, "Đã khôi phục cài đặt gốc", Toast.LENGTH_SHORT).show();
        });
        
        // Add layout to ScrollView and ScrollView to dialog
        scrollView.addView(layout);
        builder.setView(scrollView);
        builder.setPositiveButton("Đóng", (dialog, which) -> {
            dialog.dismiss();
            showQuickSettingsDialog(); // Re-open quick settings
        });
        builder.show();
    }
}