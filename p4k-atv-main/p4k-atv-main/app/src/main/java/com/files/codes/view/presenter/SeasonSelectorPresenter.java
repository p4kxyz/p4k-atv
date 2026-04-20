package com.files.codes.view.presenter;

import android.content.Context;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.leanback.widget.Presenter;

import com.files.codes.R;
import com.files.codes.model.movieDetails.SeasonSelectorItem;
import com.files.codes.view.fragments.VideoDetailsFragment;

/**
 * Presenter for Season Selector button in TV Series details
 * Displays current season name and opens selection dialog on click
 */
public class SeasonSelectorPresenter extends Presenter {
    private static Context mContext;
    private VideoDetailsFragment fragment;
    private TextView currentTextView; // Store reference to TextView

    public SeasonSelectorPresenter(VideoDetailsFragment fragment) {
        this.fragment = fragment;
    }
    
    /**
     * Update the text directly on the TextView
     */
    public void updateSeasonText(String seasonName) {
        if (currentTextView != null) {
            String displayText = "🎬 " + seasonName + " ▼";
            currentTextView.setText(displayText);
        }
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent) {
        mContext = parent.getContext();
        
        TextView textView = new TextView(mContext);
        textView.setLayoutParams(new ViewGroup.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        ));
        textView.setPadding(40, 20, 40, 20);
        textView.setTextSize(17);
        textView.setTextColor(0xFFFFFFFF);
        textView.setBackgroundColor(0xFF2A2A3E);
        textView.setFocusable(true);
        textView.setFocusableInTouchMode(true);
        
        return new ViewHolder(textView);
    }

    @Override
    public void onBindViewHolder(ViewHolder viewHolder, Object item) {
        final SeasonSelectorItem seasonItem = (SeasonSelectorItem) item;
        TextView textView = (TextView) viewHolder.view;
        
        // Store reference for direct updates
        currentTextView = textView;
        
        // Display current season name only (without "Season:" prefix)
        String displayText = "🎬 " + seasonItem.getCurrentSeasonName() + " ▼";
        textView.setText(displayText);
        
        // On click, show season selection dialog
        textView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (fragment != null) {
                    fragment.showSeasonDialog();
                }
            }
        });
        
        // Focus change effect
        textView.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View v, boolean hasFocus) {
                if (hasFocus) {
                    textView.setBackgroundColor(0xFF1565C0);
                    textView.setTextColor(0xFFFFFFFF);
                } else {
                    textView.setBackgroundColor(0xFF2A2A3E);
                    textView.setTextColor(0xFFFFFFFF);
                }
            }
        });
    }

    @Override
    public void onUnbindViewHolder(ViewHolder viewHolder) {
        // Clean up if needed
    }
}
