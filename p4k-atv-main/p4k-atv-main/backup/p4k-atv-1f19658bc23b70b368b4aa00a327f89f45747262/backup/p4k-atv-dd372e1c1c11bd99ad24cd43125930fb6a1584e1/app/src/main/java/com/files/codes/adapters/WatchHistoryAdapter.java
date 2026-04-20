package com.files.codes.adapters;

import android.content.Context;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.cardview.widget.CardView;
import androidx.recyclerview.widget.RecyclerView;

import com.files.codes.R;
import com.files.codes.model.sync.WatchHistorySyncItem;
import com.squareup.picasso.Picasso;

import java.util.ArrayList;
import java.util.List;

public class WatchHistoryAdapter extends RecyclerView.Adapter<WatchHistoryAdapter.ViewHolder> {

    private Context context;
    private List<WatchHistorySyncItem.WatchHistoryItem> items;
    private OnItemClickListener clickListener;
    private OnRemoveClickListener removeListener;

    public interface OnItemClickListener {
        void onItemClick(WatchHistorySyncItem.WatchHistoryItem item);
    }

    public interface OnRemoveClickListener {
        void onRemoveClick(WatchHistorySyncItem.WatchHistoryItem item, int position);
    }

    public WatchHistoryAdapter(Context context) {
        this.context = context;
        this.items = new ArrayList<>();
    }

    public void setItems(List<WatchHistorySyncItem.WatchHistoryItem> items) {
        this.items = items != null ? items : new ArrayList<>();
        notifyDataSetChanged();
    }

    public void setOnItemClickListener(OnItemClickListener listener) {
        this.clickListener = listener;
    }

    public void setOnRemoveClickListener(OnRemoveClickListener listener) {
        this.removeListener = listener;
    }

    public void removeItem(int position) {
        if (position >= 0 && position < items.size()) {
            items.remove(position);
            notifyItemRemoved(position);
            notifyItemRangeChanged(position, items.size());
        }
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_watch_history, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        WatchHistorySyncItem.WatchHistoryItem item = items.get(position);
        
        // Set title
        holder.tvTitle.setText(item.getTitle());
        
        // Load poster image
        if (!TextUtils.isEmpty(item.getPosterUrl())) {
            Picasso.get()
                    .load(item.getPosterUrl())
                    .placeholder(R.drawable.placeholder_poster)
                    .error(R.drawable.placeholder_poster)
                    .into(holder.imgPoster);
        } else {
            holder.imgPoster.setImageResource(R.drawable.placeholder_poster);
        }
        
        // Set progress
        int progress = (int) (item.getProgressPercentage() * 100);
        holder.progressWatched.setProgress(progress);
        holder.tvProgress.setText(progress + "%");
        
        // Set duration
        String currentTime = formatTime(item.getPosition());
        String totalTime = formatTime(item.getDuration());
        holder.tvDuration.setText(currentTime + " / " + totalTime);
        
        // Click listeners
        holder.cardView.setOnClickListener(v -> {
            if (clickListener != null) {
                clickListener.onItemClick(item);
            }
        });
        
        holder.btnRemove.setOnClickListener(v -> {
            if (removeListener != null) {
                removeListener.onRemoveClick(item, position);
            }
        });
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    private String formatTime(long timeMs) {
        if (timeMs <= 0) return "0:00";
        
        long seconds = timeMs / 1000;
        long minutes = seconds / 60;
        long hours = minutes / 60;
        
        seconds = seconds % 60;
        minutes = minutes % 60;
        
        if (hours > 0) {
            return String.format("%d:%02d:%02d", hours, minutes, seconds);
        } else {
            return String.format("%d:%02d", minutes, seconds);
        }
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        CardView cardView;
        ImageView imgPoster;
        ProgressBar progressWatched;
        TextView tvProgress;
        TextView tvDuration;
        TextView tvTitle;
        ImageView btnRemove;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            cardView = itemView.findViewById(R.id.card_view);
            imgPoster = itemView.findViewById(R.id.img_poster);
            progressWatched = itemView.findViewById(R.id.progress_watched);
            tvProgress = itemView.findViewById(R.id.tv_progress);
            tvDuration = itemView.findViewById(R.id.tv_duration);
            tvTitle = itemView.findViewById(R.id.tv_title);
            btnRemove = itemView.findViewById(R.id.btn_remove);
            
            // Make sure card view is the root element
            if (cardView == null && itemView instanceof CardView) {
                cardView = (CardView) itemView;
            }
        }
    }
    
    /**
     * Update adapter with new items
     */
    public void updateItems(List<WatchHistorySyncItem.WatchHistoryItem> newItems) {
        this.items.clear();
        this.items.addAll(newItems);
        notifyDataSetChanged();
    }
}