package com.files.codes.view.fragments;

import android.view.View;
import android.view.ViewGroup;

import androidx.leanback.widget.Presenter;

public class SpacerPresenter extends Presenter {

    // Must match FilterCardPresenter.HEIGHT_DP so row-1 columns align
    private static final int HEIGHT_DP = 80;

    private static int dp(android.content.Context ctx, int dp) {
        return Math.round(dp * ctx.getResources().getDisplayMetrics().density);
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent) {
        View v = new View(parent.getContext());
        v.setLayoutParams(new ViewGroup.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, dp(parent.getContext(), HEIGHT_DP)));
        v.setFocusable(false);
        v.setFocusableInTouchMode(false);
        v.setClickable(false);
        v.setAlpha(1.0f);  // Ensure spacers stay fully visible
        
        // Continuous monitoring
        v.addOnAttachStateChangeListener(new View.OnAttachStateChangeListener() {
            @Override
            public void onViewAttachedToWindow(View v) {
                v.setAlpha(1.0f);
                v.getViewTreeObserver().addOnGlobalLayoutListener(() -> {
                    v.setAlpha(1.0f);
                });
            }
            @Override
            public void onViewDetachedFromWindow(View v) {}
        });
        
        return new ViewHolder(v);
    }

    @Override
    public void onBindViewHolder(ViewHolder viewHolder, Object item) {}

    @Override
    public void onUnbindViewHolder(ViewHolder viewHolder) {}
}
