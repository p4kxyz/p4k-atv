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
import android.widget.ImageView;

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

        String[] items = new String[2];
        items[0] = "Trình phát: " + (useExternalPlayer ? "Bên ngoài (VLC/MX)" : "Mặc định (ExoPlayer)");
        items[1] = "Ưu tiên Audio: " + (useSoftwareAudio ? "Phần mềm (SW - Khuyên dùng)" : "Phần cứng (HW)");

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
                    } else if (which == 1) {
                        boolean newValue = !useSoftwareAudio;
                        editor.putBoolean("audio_priority_sw", newValue);
                        editor.apply();
                        new com.files.codes.utils.ToastMsg(HomeActivity.this).toastIconSuccess(newValue ? "Đã chọn: Ưu tiên Audio Software" : "Đã chọn: Ưu tiên Audio Hardware");
                    }
                    dialog.dismiss();
                    // Re-show dialog to reflect changes
                    showQuickSettingsDialog();
                }
            })
            .setNegativeButton("Đóng", null)
            .show();
    }
}