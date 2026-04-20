package com.files.codes.view.widget;

import android.content.Context;
import android.util.AttributeSet;
import android.util.Log;
import android.view.KeyEvent;
import android.widget.FrameLayout;

/**
 * Custom FrameLayout that intercepts UP/DOWN keys for category switching
 */
public class CategorySwitcherFrameLayout extends FrameLayout {
    private static final String TAG = "CategorySwitcher";
    private OnCategorySwitchListener listener;
    
    public interface OnCategorySwitchListener {
        void onSwitchNext();
        void onSwitchPrevious();
    }
    
    public CategorySwitcherFrameLayout(Context context) {
        super(context);
    }
    
    public CategorySwitcherFrameLayout(Context context, AttributeSet attrs) {
        super(context, attrs);
    }
    
    public CategorySwitcherFrameLayout(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }
    
    public void setCategorySwitchListener(OnCategorySwitchListener listener) {
        this.listener = listener;
    }
    
    @Override
    public boolean dispatchKeyEvent(KeyEvent event) {
        if (event.getAction() == KeyEvent.ACTION_DOWN) {
            Log.d(TAG, "🎮 dispatchKeyEvent: " + event.getKeyCode());
            
            if (event.getKeyCode() == KeyEvent.KEYCODE_DPAD_DOWN) {
                Log.d(TAG, "⬇️ DOWN key intercepted");
                if (listener != null) {
                    listener.onSwitchNext();
                    return true;
                }
            } else if (event.getKeyCode() == KeyEvent.KEYCODE_DPAD_UP) {
                Log.d(TAG, "⬆️ UP key intercepted");
                if (listener != null) {
                    listener.onSwitchPrevious();
                    return true;
                }
            }
        }
        
        return super.dispatchKeyEvent(event);
    }
}
