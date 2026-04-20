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
    private static final float NAVIGATION_DRAWER_SCALE_FACTOR = 1.0f;

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
            
            settingsOrb.bringToFront();
            settingsOrb.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
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
                        
                        // Force visible just in case
                        settingsOrb.setVisibility(View.VISIBLE);
                        settingsOrb.bringToFront();
                    }
                }
            });
        } else {
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
                    // [FIX] Simplify Menu Opening Logic
                    // Removed isVerticalScrolling() check to prevent blocking menu on devices with sticky scroll states.
                    // If user presses LEFT, they intend to open the menu. period.

                    if (navigationDrawerOpen) {
                        // Already in menu, let system handle navigation within menu
                        return focused;
                    }
                    
                    // Ensure header container is visible so it can accept focus
                    if (headersFragment != null && headersFragment.getView() != null) {
                        View container = (View) headersFragment.getView().getParent();
                        if (container != null) {
                            container.setVisibility(View.VISIBLE);
                            container.bringToFront();
                        }
                    } else {
                         return focused; // Safety check
                    }

                    // [FIX] Hardware Focus Safety for Older Boxes
                    // Snap the menu OPEN immediately before transferring focus.
                    if (headersFragment != null && headersFragment.getView() != null) {
                         View headersContainer = (View) headersFragment.getView().getParent();
                         if (headersContainer != null) {
                             // Snap to onscreen position (Left Margin = 0)
                             ViewGroup.MarginLayoutParams headersParams = (ViewGroup.MarginLayoutParams) headersContainer.getLayoutParams();
                             headersParams.leftMargin = 0; 
                             headersContainer.setLayoutParams(headersParams);
                             
                             // Move content row
                             if (rowsFragment != null && rowsFragment.getView() != null) {
                                 View rowsContainer = (View) rowsFragment.getView().getParent();
                                 if (rowsContainer != null) {
                                     ViewGroup.MarginLayoutParams rowsParams = (ViewGroup.MarginLayoutParams) rowsContainer.getLayoutParams();
                                     rowsParams.leftMargin = Utils.dpToPx(150, HomeActivity.this);
                                     rowsContainer.setLayoutParams(rowsParams);
                                 }
                             }
                             navigationDrawerOpen = true;
                         }
                    }

                    // [FIX] Force focus onto the menu grid to resolve "cannot select items" issue.
                    // This ensures that after the menu appears, the system strictly hands over control to the menu items.
                    VerticalGridView menuGrid = getVerticalGridView(headersFragment);
                    if (menuGrid != null) {
                        menuGrid.requestFocus();
                        return menuGrid;
                    }
                    return focused;

                } else if (direction == View.FOCUS_RIGHT) {
                    if (focused == orbView) {
                        return settingsOrb != null && settingsOrb.getVisibility() == View.VISIBLE ? settingsOrb : focused;
                    }
                    if (isVerticalScrolling() || !navigationDrawerOpen) {
                        return focused;
                    }
                    toggleHeadersFragment(false);
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
            // [FIX] Specific handling for HeadersSupportFragment (CustomHeadersFragment)
            // Older Leanback versions might behave differently, so we search for the method directly on the class hierarchy
            if (fragment instanceof androidx.leanback.app.HeadersSupportFragment) {
                // HeadersSupportFragment usually has getVerticalGridView()
                try {
                     Method method = fragment.getClass().getMethod("getVerticalGridView");
                     return (VerticalGridView) method.invoke(fragment);
                } catch (NoSuchMethodException e) {
                     // Try protected method from parent
                     Class clazz = fragment.getClass();
                     while (clazz != null) {
                         try {
                             Method method = clazz.getDeclaredMethod("getVerticalGridView");
                             method.setAccessible(true);
                             return (VerticalGridView) method.invoke(fragment);
                         } catch (NoSuchMethodException ex) {
                             clazz = clazz.getSuperclass();
                         }
                     }
                }
                return null;
            }

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

            // Stop any in-flight animation before computing the next state.
            ((View) rowsContainer.getParent()).clearAnimation();

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
                        forceCloseHeadersState(headersContainer, rowsContainer);
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

            // Safety net: if the animation gets interrupted, force the fully-closed state.
            if (!doOpen) {
                ((View) rowsContainer.getParent()).postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        if (!navigationDrawerOpen) {
                            forceCloseHeadersState(headersContainer, rowsContainer);
                        }
                    }
                }, 260);
            }
        }
    }

    private void forceCloseHeadersState(View headersContainer, View rowsContainer) {
        if (headersContainer == null || rowsContainer == null) {
            return;
        }

        int headersWidth = headersContainer.getWidth();
        if (headersWidth <= 0) {
            headersWidth = Utils.dpToPx(300, this);
        }

        ViewGroup.MarginLayoutParams headersParams = (ViewGroup.MarginLayoutParams) headersContainer.getLayoutParams();
        headersParams.leftMargin = -headersWidth;
        headersContainer.setLayoutParams(headersParams);
        headersContainer.setVisibility(View.GONE);

        ViewGroup.MarginLayoutParams rowsParams = (ViewGroup.MarginLayoutParams) rowsContainer.getLayoutParams();
        rowsParams.leftMargin = 0;
        rowsContainer.setLayoutParams(rowsParams);

        navigationDrawerOpen = false;
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
        }
    }
    
    /**
     * Check for OTA updates when app starts
     */
    private void checkForUpdates() {
        try {
            OTAUpdateManager otaManager = OTAUpdateManager.getInstance(this);
            
            otaManager.checkForUpdatesSilently();
        } catch (Exception e) {
            if (e.getCause() != null) {
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

    private static final String PLAYER_PREFS = "player_settings";
    private static final String PREF_AUDIO_LANG    = "pref_audio_lang";
    private static final String PREF_SUBTITLE_LANG = "pref_subtitle_lang";

    // ── custom dark dialog (giống HomeNewActivity) ────────────────────────
    private int dp(int dp) {
        return Math.round(dp * getResources().getDisplayMetrics().density);
    }

    private android.app.Dialog buildCustomListDialog(
            String title, String[] items, int checkedIndex,
            Runnable[] actions, Runnable onCancel) {
        android.app.Dialog dialog = new android.app.Dialog(this);
        dialog.requestWindowFeature(android.view.Window.FEATURE_NO_TITLE);
        dialog.setCancelable(true);
        if (onCancel != null) dialog.setOnCancelListener(d -> onCancel.run());

        android.widget.LinearLayout root = new android.widget.LinearLayout(this);
        root.setOrientation(android.widget.LinearLayout.VERTICAL);
        root.setBackgroundColor(0xFF1E1E2E);
        root.setPadding(dp(20), dp(20), dp(20), dp(12));

        android.widget.TextView titleView = new android.widget.TextView(this);
        titleView.setText(title);
        titleView.setTextColor(0xFFFFFFFF);
        titleView.setTextSize(android.util.TypedValue.COMPLEX_UNIT_SP, 18);
        titleView.setTypeface(android.graphics.Typeface.DEFAULT_BOLD);
        titleView.setPadding(dp(4), 0, dp(4), dp(10));
        root.addView(titleView);

        android.view.View divider = new android.view.View(this);
        divider.setBackgroundColor(0x55FFFFFF);
        android.widget.LinearLayout.LayoutParams divLp =
                new android.widget.LinearLayout.LayoutParams(
                        android.widget.LinearLayout.LayoutParams.MATCH_PARENT, dp(1));
        divLp.bottomMargin = dp(6);
        root.addView(divider, divLp);

        android.widget.ScrollView sv = new android.widget.ScrollView(this);
        sv.setVerticalScrollBarEnabled(true);
        sv.setScrollBarStyle(android.view.View.SCROLLBARS_INSIDE_INSET);
        sv.setSmoothScrollingEnabled(true);
        android.widget.LinearLayout ll = new android.widget.LinearLayout(this);
        ll.setOrientation(android.widget.LinearLayout.VERTICAL);

        for (int i = 0; i < items.length; i++) {
            final int idx = i;
            boolean isSelected = (checkedIndex >= 0 && i == checkedIndex);
            android.widget.TextView tv = new android.widget.TextView(this);
            tv.setText(isSelected ? "✓  " + items[i] : "     " + items[i]);
            tv.setTextColor(isSelected ? 0xFF64B5F6 : 0xFFDDDDDD);
            tv.setTextSize(android.util.TypedValue.COMPLEX_UNIT_SP, 15);
            tv.setPadding(dp(10), dp(13), dp(10), dp(13));
            tv.setFocusable(true);
            tv.setFocusableInTouchMode(false);
            tv.setClickable(true);
            tv.setBackground(null);
            tv.setOnFocusChangeListener((v, hasFocus) -> {
                if (hasFocus) {
                    v.setBackgroundColor(0x664FC3F7);
                    ((android.widget.TextView) v).setTextColor(0xFFFFFFFF);
                    sv.post(() -> {
                        int targetY = Math.max(0, v.getTop() - dp(72));
                        sv.smoothScrollTo(0, targetY);
                    });
                } else {
                    v.setBackgroundColor(android.graphics.Color.TRANSPARENT);
                    boolean sel = (checkedIndex >= 0 && idx == checkedIndex);
                    ((android.widget.TextView) v).setTextColor(sel ? 0xFF64B5F6 : 0xFFDDDDDD);
                }
            });
            tv.setOnClickListener(v -> {
                dialog.dismiss();
                if (actions != null && idx < actions.length && actions[idx] != null)
                    actions[idx].run();
            });
            ll.addView(tv);
            android.view.View sep = new android.view.View(this);
            sep.setBackgroundColor(0x22FFFFFF);
            ll.addView(sep, new android.widget.LinearLayout.LayoutParams(
                    android.widget.LinearLayout.LayoutParams.MATCH_PARENT, 1));
        }
        sv.addView(ll);
        root.addView(sv, new android.widget.LinearLayout.LayoutParams(
            android.widget.LinearLayout.LayoutParams.MATCH_PARENT, dp(520)));

        dialog.setContentView(root);
        android.view.Window w = dialog.getWindow();
        if (w != null) {
            w.setLayout(dp(480), android.view.WindowManager.LayoutParams.WRAP_CONTENT);
            w.setGravity(android.view.Gravity.CENTER);
            w.setBackgroundDrawableResource(android.R.color.transparent);
        }
        return dialog;
    }

    private void showQuickSettingsDialog() {
        final android.content.SharedPreferences pref = getSharedPreferences(
                com.files.codes.utils.Constants.USER_LOGIN_STATUS, MODE_PRIVATE);
        final android.content.SharedPreferences playerPref = getSharedPreferences(PLAYER_PREFS, MODE_PRIVATE);

        final boolean useExternalPlayer = pref.getBoolean("use_external_player", false);
        final boolean useSoftwareAudio  = pref.getBoolean("audio_priority_sw", true);
        final String  prefAudioLang     = playerPref.getString(PREF_AUDIO_LANG, "");
        final String  prefSubLang       = playerPref.getString(PREF_SUBTITLE_LANG, "");
        final boolean logoFixed         = playerPref.getBoolean("logo_fixed", false);

        String[] items = {
            "🎞  Trình phát: " + (useExternalPlayer ? "Bên ngoài (VLC/MX)" : "Mặc định (ExoPlayer)"),
            "🔊 Ưu tiên Audio: " + (useSoftwareAudio ? "Software (Khuyên dùng)" : "Hardware"),
            "🔊 Ngôn ngữ âm thanh: " + getAudioLangLabel(prefAudioLang),
            "📝 Ngôn ngữ phụ đề: " + getSubtitleLangLabel(prefSubLang),
            "🖼️ Logo player: " + (logoFixed ? "Cố định (góc trên phải)" : "Di chuyển (chống burn-in)")
        };
        Runnable[] actions = {
            () -> {
                boolean newVal = !useExternalPlayer;
                pref.edit().putBoolean("use_external_player", newVal).apply();
                new com.files.codes.utils.ToastMsg(this).toastIconSuccess(
                        newVal ? "Đã chọn: Trình phát ngoài" : "Đã chọn: ExoPlayer");
                showQuickSettingsDialog();
            },
            () -> {
                boolean newVal = !useSoftwareAudio;
                pref.edit().putBoolean("audio_priority_sw", newVal).apply();
                new com.files.codes.utils.ToastMsg(this).toastIconSuccess(
                        newVal ? "Ưu tiên Audio Software" : "Ưu tiên Audio Hardware");
                showQuickSettingsDialog();
            },
            () -> showLangPickerDialog("🔊 Ngôn ngữ âm thanh ưu tiên", PREF_AUDIO_LANG,
                    new String[]{"Không đặt (tự động)", "Tiếng Việt (vi)", "Tiếng Anh (en)",
                         "Tiếng Nhật (ja)", "Tiếng Trung (zh)", "Tiếng Hàn (ko)",
                         "Tiếng Thái (th)", "Tiếng Indonesia (id)", "Tiếng Tây Ban Nha (es)",
                         "Tiếng Pháp (fr)", "Tiếng Đức (de)", "Tiếng Ý (it)",
                         "Tiếng Bồ Đào Nha (pt)", "Tiếng Nga (ru)", "Tiếng Ả Rập (ar)",
                         "Tiếng Hindi (hi)"},
                    new String[]{"", "vi", "en", "ja", "zh", "ko", "th", "id", "es", "fr",
                         "de", "it", "pt", "ru", "ar", "hi"}, prefAudioLang),
            () -> showLangPickerDialog("📝 Ngôn ngữ phụ đề ưu tiên", PREF_SUBTITLE_LANG,
                    new String[]{"Không đặt (tự động)", "Tiếng Việt (vi)", "Tiếng Anh (en)",
                         "Tiếng Nhật (ja)", "Tiếng Trung (zh)", "Tiếng Hàn (ko)",
                         "Tiếng Thái (th)", "Tiếng Indonesia (id)", "Tiếng Tây Ban Nha (es)",
                         "Tiếng Pháp (fr)", "Tiếng Đức (de)", "Tiếng Ý (it)",
                         "Tiếng Bồ Đào Nha (pt)", "Tiếng Nga (ru)", "Tiếng Ả Rập (ar)",
                         "Tiếng Hindi (hi)", "Tắt hoàn toàn"},
                    new String[]{"", "vi", "en", "ja", "zh", "ko", "th", "id", "es", "fr",
                         "de", "it", "pt", "ru", "ar", "hi", "off"}, prefSubLang),
            () -> {
                boolean newVal = !logoFixed;
                playerPref.edit().putBoolean("logo_fixed", newVal).apply();
                new com.files.codes.utils.ToastMsg(this).toastIconSuccess(
                        newVal ? "Logo: Cố định góc trên phải" : "Logo: Di chuyển (chống burn-in)");
                showQuickSettingsDialog();
            }
        };
        buildCustomListDialog("⚙️ Cài đặt nhanh", items, -1, actions, null).show();
    }

    private void showLangPickerDialog(String title, String prefKey,
            String[] displayNames, String[] values, String currentValue) {
        int checked = 0;
        for (int i = 0; i < values.length; i++) {
            if (currentValue.equals(values[i])) { checked = i; break; }
        }
        final int finalChecked = checked;
        android.content.SharedPreferences playerPref = getSharedPreferences(PLAYER_PREFS, MODE_PRIVATE);
        Runnable[] actions = new Runnable[displayNames.length];
        for (int i = 0; i < displayNames.length; i++) {
            final int idx = i;
            actions[i] = () -> {
                playerPref.edit().putString(prefKey, values[idx]).apply();
                new com.files.codes.utils.ToastMsg(this).toastIconSuccess("✅ Đã đặt: " + displayNames[idx]);
                showQuickSettingsDialog();
            };
        }
        buildCustomListDialog(title, displayNames, finalChecked, actions, this::showQuickSettingsDialog).show();
    }

    private String getAudioLangLabel(String value) {
        switch (value) {
            case "vi": return "Tiếng Việt";
            case "en": return "Tiếng Anh";
            case "ja": return "Tiếng Nhật";
            case "zh": return "Tiếng Trung";
            case "ko": return "Tiếng Hàn";
            case "th": return "Tiếng Thái";
            case "id": return "Tiếng Indonesia";
            case "es": return "Tiếng Tây Ban Nha";
            case "fr": return "Tiếng Pháp";
            case "de": return "Tiếng Đức";
            case "it": return "Tiếng Ý";
            case "pt": return "Tiếng Bồ Đào Nha";
            case "ru": return "Tiếng Nga";
            case "ar": return "Tiếng Ả Rập";
            case "hi": return "Tiếng Hindi";
            default:   return "Tự động";
        }
    }

    private String getSubtitleLangLabel(String value) {
        switch (value) {
            case "vi":  return "Tiếng Việt";
            case "en":  return "Tiếng Anh";
            case "ja":  return "Tiếng Nhật";
            case "zh":  return "Tiếng Trung";
            case "ko":  return "Tiếng Hàn";
            case "th":  return "Tiếng Thái";
            case "id":  return "Tiếng Indonesia";
            case "es":  return "Tiếng Tây Ban Nha";
            case "fr":  return "Tiếng Pháp";
            case "de":  return "Tiếng Đức";
            case "it":  return "Tiếng Ý";
            case "pt":  return "Tiếng Bồ Đào Nha";
            case "ru":  return "Tiếng Nga";
            case "ar":  return "Tiếng Ả Rập";
            case "hi":  return "Tiếng Hindi";
            case "off": return "Tắt";
            default:    return "Tự động";
        }
    }
}
