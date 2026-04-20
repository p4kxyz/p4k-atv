package com.files.codes.view.presenter;

import android.content.Context;
import android.graphics.Color;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.leanback.widget.Presenter;

import com.files.codes.model.movieDetails.PaginationItem;
import com.files.codes.view.fragments.VideoDetailsFragment;

/**
 * Presenter for Pagination controls in TV Series episode list
 * Displays page numbers and navigation buttons (Prev/Next)
 */
public class PaginationPresenter extends Presenter {
    private static Context mContext;
    private VideoDetailsFragment fragment;

    public PaginationPresenter(VideoDetailsFragment fragment) {
        this.fragment = fragment;
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent) {
        mContext = parent.getContext();
        
        LinearLayout container = new LinearLayout(mContext);
        container.setOrientation(LinearLayout.HORIZONTAL);
        container.setGravity(Gravity.CENTER);
        container.setLayoutParams(new ViewGroup.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        ));
        container.setPadding(40, 20, 40, 20);
        
        return new ViewHolder(container);
    }

    @Override
    public void onBindViewHolder(ViewHolder viewHolder, Object item) {
        final PaginationItem paginationItem = (PaginationItem) item;
        LinearLayout container = (LinearLayout) viewHolder.view;
        container.removeAllViews();
        
        int currentPage = paginationItem.getCurrentPage();
        int totalPages = paginationItem.getTotalPages();
        
        if (totalPages <= 1) {
            // No pagination needed
            TextView infoText = createPageButton("Page 1 of 1", false, -1);
            container.addView(infoText);
            return;
        }
        
        // Prev button
        TextView prevButton = createPageButton("◄ Prev", true, currentPage - 1);
        prevButton.setEnabled(currentPage > 0);
        prevButton.setAlpha(currentPage > 0 ? 1.0f : 0.3f);
        if (currentPage > 0) {
            prevButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (fragment != null) {
                        fragment.onPageChanged(currentPage - 1);
                    }
                }
            });
        }
        container.addView(prevButton);
        
        // Page numbers (show max 5 pages)
        int startPage = Math.max(0, currentPage - 2);
        int endPage = Math.min(totalPages - 1, startPage + 4);
        
        // Adjust startPage if we're near the end
        if (endPage - startPage < 4) {
            startPage = Math.max(0, endPage - 4);
        }
        
        for (int i = startPage; i <= endPage; i++) {
            final int pageNum = i;
            TextView pageButton = createPageButton(String.valueOf(i + 1), true, i);
            
            if (i == currentPage) {
                pageButton.setBackgroundColor(Color.parseColor("#FF6B35"));
                pageButton.setTextColor(Color.WHITE);
            }
            
            pageButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (fragment != null) {
                        fragment.onPageChanged(pageNum);
                    }
                }
            });
            
            container.addView(pageButton);
        }
        
        // Next button
        TextView nextButton = createPageButton("Next ►", true, currentPage + 1);
        nextButton.setEnabled(currentPage < totalPages - 1);
        nextButton.setAlpha(currentPage < totalPages - 1 ? 1.0f : 0.3f);
        if (currentPage < totalPages - 1) {
            nextButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (fragment != null) {
                        fragment.onPageChanged(currentPage + 1);
                    }
                }
            });
        }
        container.addView(nextButton);
    }
    
    private TextView createPageButton(String text, boolean focusable, final int targetPage) {
        TextView button = new TextView(mContext);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.WRAP_CONTENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        );
        params.setMargins(10, 0, 10, 0);
        button.setLayoutParams(params);
        
        button.setText(text);
        button.setTextSize(18);
        button.setTextColor(Color.WHITE);
        button.setBackgroundColor(Color.parseColor("#1A1A1A"));
        button.setPadding(30, 20, 30, 20);
        button.setGravity(Gravity.CENTER);
        button.setFocusable(focusable);
        button.setFocusableInTouchMode(focusable);
        
        if (focusable) {
            button.setOnFocusChangeListener(new View.OnFocusChangeListener() {
                @Override
                public void onFocusChange(View v, boolean hasFocus) {
                    if (hasFocus && targetPage != -1) {
                        button.setBackgroundColor(Color.parseColor("#FF6B35"));
                    } else if (!hasFocus) {
                        button.setBackgroundColor(Color.parseColor("#1A1A1A"));
                    }
                }
            });
        }
        
        return button;
    }

    @Override
    public void onUnbindViewHolder(ViewHolder viewHolder) {
        // Clean up if needed
    }
}
