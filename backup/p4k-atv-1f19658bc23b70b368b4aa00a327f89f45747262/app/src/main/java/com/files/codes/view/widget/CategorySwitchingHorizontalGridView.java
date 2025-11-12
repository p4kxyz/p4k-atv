package com.files.codes.view.widget;

import android.content.Context;
import android.util.AttributeSet;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.FocusFinder;
import androidx.leanback.widget.HorizontalGridView;

public class CategorySwitchingHorizontalGridView extends HorizontalGridView {
    private static final String TAG = "CategorySwitchGrid";
    private OnCategorySwitchListener listener;
    
    public interface OnCategorySwitchListener {
        boolean onSwitchNext();
        boolean onSwitchPrevious();
    }
    
    public CategorySwitchingHorizontalGridView(Context context) {
        super(context);
    }
    
    public CategorySwitchingHorizontalGridView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }
    
    public CategorySwitchingHorizontalGridView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }
    
    public void setCategorySwitchListener(OnCategorySwitchListener listener) {
        this.listener = listener;
    }
    
    @Override
    public View focusSearch(View focused, int direction) {
        // 🎯 Override focusSearch to allow LEFT escape at first position
        if (direction == View.FOCUS_LEFT) {
            int position = getSelectedPosition();
            if (position == 0) {
                Log.d(TAG, "⬅️ focusSearch LEFT at position 0 - use FocusFinder");
                // Use FocusFinder to search outside this grid
                ViewGroup rootView = (ViewGroup) getRootView();
                if (rootView != null) {
                    View result = FocusFinder.getInstance().findNextFocus(rootView, this, direction);
                    if (result != null && result != this) {
                        Log.d(TAG, "✅ FocusFinder found target: " + result.getClass().getSimpleName());
                        return result;
                    }
                }
                Log.d(TAG, "⚠️ No focus target found, returning null");
                return null;
            }
        }
        
        return super.focusSearch(focused, direction);
    }
    
    @Override
    public boolean dispatchKeyEvent(KeyEvent event) {
        if (event.getAction() == KeyEvent.ACTION_DOWN) {
            Log.d(TAG, "🎮 dispatchKeyEvent: " + event.getKeyCode() + " at position: " + getSelectedPosition());
            
            if (event.getKeyCode() == KeyEvent.KEYCODE_DPAD_DOWN) {
                Log.d(TAG, "⬇️ DOWN key intercepted - switching category");
                if (listener != null) {
                    boolean consumed = listener.onSwitchNext();
                    if (consumed) {
                        return true; // Consume event
                    }
                }
            } else if (event.getKeyCode() == KeyEvent.KEYCODE_DPAD_UP) {
                Log.d(TAG, "⬆️ UP key intercepted - checking if can switch");
                if (listener != null) {
                    boolean consumed = listener.onSwitchPrevious();
                    if (consumed) {
                        Log.d(TAG, "✅ Category switched - event consumed");
                        return true; // Consume event
                    } else {
                        Log.d(TAG, "🔓 At first category - allow focus to escape UP");
                        // Don't consume event - let it propagate to allow focus to escape to search
                    }
                }
            } else if (event.getKeyCode() == KeyEvent.KEYCODE_DPAD_LEFT) {
                // ⬅️ LEFT: Allow escape to menu when at first item
                int position = getSelectedPosition();
                Log.d(TAG, "⬅️ LEFT key at position: " + position);
                if (position == 0) {
                    Log.d(TAG, "⬅️ At first thumbnail - attempting to transfer focus to menu");
                    
                    // Try to find and request focus on menu
                    ViewGroup rootView = (ViewGroup) getRootView();
                    if (rootView != null) {
                        View menuView = FocusFinder.getInstance().findNextFocus(rootView, this, View.FOCUS_LEFT);
                        if (menuView != null && menuView != this) {
                            Log.d(TAG, "✅ Found menu view: " + menuView.getClass().getSimpleName());
                            // Explicitly request focus on the menu
                            boolean focusRequested = menuView.requestFocus();
                            Log.d(TAG, focusRequested ? "✅ Focus request succeeded" : "❌ Focus request failed");
                            if (focusRequested) {
                                return true; // Consume event since we handled it
                            }
                        } else {
                            Log.d(TAG, "⚠️ Could not find menu view");
                        }
                    }
                    
                    // If we couldn't transfer focus manually, don't consume event
                    return false;
                }
            }
        }
        
        return super.dispatchKeyEvent(event);
    }
}
