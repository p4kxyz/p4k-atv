package com.files.codes.view.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.resource.bitmap.RoundedCorners;
import com.bumptech.glide.request.RequestOptions;
import com.files.codes.R;
import com.files.codes.model.WatchHistoryItem;

import java.util.List;
import java.util.Locale;

/**
 * Adapter cho RecyclerView hiển thị lịch sử xem
 */
public class WatchHistoryAdapter extends RecyclerView.Adapter<WatchHistoryAdapter.ViewHolder> {
    
    private List<WatchHistoryItem> historyList;
    private OnItemClickListener listener;
    private Context context;

    public interface OnItemClickListener {
        void onItemClick(WatchHistoryItem item);
        void onRemoveClick(WatchHistoryItem item);
    }

    public WatchHistoryAdapter(List<WatchHistoryItem> historyList, OnItemClickListener listener) {
        this.historyList = historyList;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        context = parent.getContext();
        View view = LayoutInflater.from(context).inflate(R.layout.item_watch_history, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        WatchHistoryItem item = historyList.get(position);
        holder.bind(item);
    }

    @Override
    public int getItemCount() {
        return historyList.size();
    }

    public class ViewHolder extends RecyclerView.ViewHolder {
        private ImageView imgPoster;
        private TextView tvTitle;
        private TextView tvProgress;
        private TextView tvDuration;
        private ProgressBar progressWatched;
        private ImageView btnRemove;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            imgPoster = itemView.findViewById(R.id.img_poster);
            tvTitle = itemView.findViewById(R.id.tv_title);
            tvProgress = itemView.findViewById(R.id.tv_progress);
            tvDuration = itemView.findViewById(R.id.tv_duration);
            progressWatched = itemView.findViewById(R.id.progress_watched);
            btnRemove = itemView.findViewById(R.id.btn_remove);
        }

        public void bind(WatchHistoryItem item) {
            // Load poster image
            Glide.with(context)
                    .load(item.getPosterUrl())
                    .apply(new RequestOptions()
                            .placeholder(R.drawable.placeholder_poster)
                            .error(R.drawable.placeholder_poster)
                            .transform(new RoundedCorners(16)))
                    .into(imgPoster);

            // Set title
            tvTitle.setText(item.getTitle());

            // Calculate and set progress
            int progressPercent = 0;
            if (item.getTotalDuration() > 0) {
                progressPercent = (int) ((item.getWatchedPosition() * 100) / item.getTotalDuration());
            }
            
            progressWatched.setProgress(progressPercent);
            tvProgress.setText(progressPercent + "%");

            // Set duration info
            String watchedTime = formatTime(item.getWatchedPosition());
            String totalTime = formatTime(item.getTotalDuration());
            tvDuration.setText(watchedTime + " / " + totalTime);

            // Set click listeners
            itemView.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onItemClick(item);
                }
            });

            btnRemove.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onRemoveClick(item);
                }
            });

            // Set focus behavior for TV
            itemView.setFocusable(true);
            itemView.setClickable(true);
        }

        private String formatTime(long milliseconds) {
            long seconds = milliseconds / 1000;
            long minutes = seconds / 60;
            long hours = minutes / 60;
            
            if (hours > 0) {
                return String.format(Locale.getDefault(), "%d:%02d:%02d", hours, minutes % 60, seconds % 60);
            } else {
                return String.format(Locale.getDefault(), "%d:%02d", minutes, seconds % 60);
            }
        }
    }
}