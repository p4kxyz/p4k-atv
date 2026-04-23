package com.files.codes.view.presenter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.leanback.widget.Presenter;

import com.files.codes.R;
import com.files.codes.utils.SearchHistoryManager;

/**
 * Presenter for recent search items.
 */
public class SearchHistoryPresenter extends Presenter {

    public interface ActionListener {
        void onSearch(String query);

        void onDelete(String query);
    }

    private final ActionListener actionListener;

    public SearchHistoryPresenter(ActionListener actionListener) {
        this.actionListener = actionListener;
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_search_history, parent, false);
        return new SearchHistoryViewHolder(view);
    }

    @Override
    public void onBindViewHolder(ViewHolder viewHolder, Object item) {
        SearchHistoryViewHolder holder = (SearchHistoryViewHolder) viewHolder;
        if (!(item instanceof SearchHistoryManager.SearchHistoryEntry)) {
            return;
        }

        SearchHistoryManager.SearchHistoryEntry entry = (SearchHistoryManager.SearchHistoryEntry) item;
        holder.bind(entry);
    }

    @Override
    public void onUnbindViewHolder(ViewHolder viewHolder) {
        SearchHistoryViewHolder holder = (SearchHistoryViewHolder) viewHolder;
        holder.clear();
    }

    private class SearchHistoryViewHolder extends ViewHolder {
        private final View rootView;
        private final TextView queryView;
        private final ImageButton deleteButton;

        private SearchHistoryManager.SearchHistoryEntry currentItem;

        SearchHistoryViewHolder(View view) {
            super(view);
            rootView = view;
            queryView = view.findViewById(R.id.tv_history_query);
            deleteButton = view.findViewById(R.id.btn_history_delete);

            rootView.setFocusable(true);
            rootView.setClickable(true);
            rootView.setOnFocusChangeListener(new View.OnFocusChangeListener() {
                @Override
                public void onFocusChange(View v, boolean hasFocus) {
                    v.setSelected(hasFocus);
                    queryView.setAlpha(hasFocus ? 1.0f : 0.9f);
                    queryView.setScaleX(hasFocus ? 1.03f : 1.0f);
                    queryView.setScaleY(hasFocus ? 1.03f : 1.0f);
                    v.animate().scaleX(hasFocus ? 1.1f : 1.0f).scaleY(hasFocus ? 1.1f : 1.0f).setDuration(140).start();
                }
            });

            rootView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (actionListener != null && currentItem != null) {
                        actionListener.onSearch(currentItem.getQuery());
                    }
                }
            });

            deleteButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (actionListener != null && currentItem != null) {
                        actionListener.onDelete(currentItem.getQuery());
                    }
                }
            });
        }

        void bind(SearchHistoryManager.SearchHistoryEntry item) {
            currentItem = item;
            queryView.setText(item != null && item.getQuery() != null ? item.getQuery() : "");
            rootView.setSelected(false);
            queryView.setAlpha(0.9f);
            queryView.setScaleX(1.0f);
            queryView.setScaleY(1.0f);
            rootView.setScaleX(1.0f);
            rootView.setScaleY(1.0f);
        }

        void clear() {
            currentItem = null;
            queryView.setText(null);
        }
    }
}