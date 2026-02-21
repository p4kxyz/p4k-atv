package com.files.codes.view.fragments;

import android.os.Bundle;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.leanback.app.RowsSupportFragment;
import androidx.leanback.widget.ArrayObjectAdapter;
import androidx.leanback.widget.ListRowPresenter;
import androidx.leanback.widget.OnItemViewClickedListener;

/**
 * Simple RowsSupportFragment for displaying search results.
 * Uses Leanback's ListRow/ListRowPresenter for TV-optimized horizontal scrolling rows.
 * Search logic is managed by the parent SearchActivity.
 */
public class SearchResultsFragment extends RowsSupportFragment {
    private static final String TAG = "SearchResultsFragment";
    private ArrayObjectAdapter rowsAdapter;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        rowsAdapter = new ArrayObjectAdapter(new ListRowPresenter());
        setAdapter(rowsAdapter);
    }
    
    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        // Set focus search direction: up goes to search bar
        view.setNextFocusUpId(com.files.codes.R.id.search_bar_container);
    }

    public ArrayObjectAdapter getRowsAdapter() {
        return rowsAdapter;
    }

    public void clearResults() {
        if (rowsAdapter != null) {
            rowsAdapter.clear();
        }
    }

    public void setItemClickListener(OnItemViewClickedListener listener) {
        setOnItemViewClickedListener(listener);
    }
}
