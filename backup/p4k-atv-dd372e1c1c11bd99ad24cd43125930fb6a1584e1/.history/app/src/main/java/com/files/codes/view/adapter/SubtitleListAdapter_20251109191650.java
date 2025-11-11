package com.files.codes.view.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;


import com.files.codes.R;
import com.files.codes.model.movieDetails.Subtitle;

import java.util.List;

public class SubtitleListAdapter extends RecyclerView.Adapter<SubtitleListAdapter.SubtitleViewHolder>{
    private Context context;
    private List<Subtitle> subtitleList;
    private OnSubtitleItemClickListener listener;
    private SubtitleViewHolder viewHolder;

    public SubtitleListAdapter(Context context, List<Subtitle> subtitleList) {
        this.context = context;
        this.subtitleList = subtitleList;
        android.util.Log.d("SubtitleListAdapter", "🎬 Adapter created with " + 
            (subtitleList != null ? subtitleList.size() + " subtitles" : "NULL list"));
        if (subtitleList != null) {
            for (int i = 0; i < subtitleList.size(); i++) {
                Subtitle sub = subtitleList.get(i);
                android.util.Log.d("SubtitleListAdapter", "🎬 Item " + i + ": " + 
                    sub.getLanguage() + " - " + sub.getUrl());
            }
        }
    }

    @NonNull
    @Override
    public SubtitleViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.layout_server_tv_item, parent, false);
        return new SubtitleViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull SubtitleViewHolder holder, int position) {
        Subtitle subtitle = subtitleList.get(position);
        holder.subtitleNameTv.setText(subtitle.getLanguage());
        holder.subtitleNameTv.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                android.util.Log.d("SubtitleListAdapter", "🎬 Button clicked! Position: " + position);
                android.util.Log.d("SubtitleListAdapter", "🎬 Subtitle: " + subtitle.getLanguage());
                android.util.Log.d("SubtitleListAdapter", "🎬 Listener: " + (listener != null ? "SET" : "NULL"));
                
                if (listener != null){
                    android.util.Log.d("SubtitleListAdapter", "🎬 Calling listener...");
                    listener.onSubtitleItemClick(v, subtitle, position, holder);
                } else {
                    android.util.Log.e("SubtitleListAdapter", "🎬 ERROR: No listener set!");
                }
            }
        });
    }

    @Override
    public int getItemCount() {
        return subtitleList.size();
    }

    public static class SubtitleViewHolder extends RecyclerView.ViewHolder{
        Button subtitleNameTv;

        SubtitleViewHolder(@NonNull View itemView) {
            super(itemView);
            subtitleNameTv = itemView.findViewById(R.id.s_name_tv);
        }
    }

    public interface OnSubtitleItemClickListener {
        void onSubtitleItemClick(View view, Subtitle subtitle, int position, SubtitleViewHolder holder);
    }

    public void setListener(OnSubtitleItemClickListener listener) {
        this.listener = listener;
    }
}
