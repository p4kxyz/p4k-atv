package com.files.codes.view.adapter;

import android.content.Context;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.files.codes.R;
import com.files.codes.model.movieDetails.RelatedMovie;
import com.files.codes.view.HeroStyleVideoDetailsActivity;
import com.squareup.picasso.Picasso;

import java.util.List;

public class RelatedMovieAdapter extends RecyclerView.Adapter<RelatedMovieAdapter.ViewHolder> {
    
    private List<RelatedMovie> movies;
    private Context context;

    public RelatedMovieAdapter(Context context, List<RelatedMovie> movies) {
        this.context = context;
        this.movies = movies;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_related_movie, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        RelatedMovie movie = movies.get(position);
        
        // Set title
        if (holder.titleTV != null) {
            holder.titleTV.setText(movie.getTitle());
        }
        
        // Load thumbnail
        if (holder.thumbnailIV != null && movie.getThumbnailUrl() != null) {
            Picasso.get()
                .load(movie.getThumbnailUrl())
                .placeholder(R.drawable.movie_placeholder)
                .error(R.drawable.movie_placeholder)
                .into(holder.thumbnailIV);
        }
        
        // Set click listener
        holder.itemView.setOnClickListener(v -> {
            Intent intent = new Intent(context, HeroStyleVideoDetailsActivity.class);
            intent.putExtra("id", movie.getVideosId());
            intent.putExtra("type", movie.getType());
            intent.putExtra("thumbImage", movie.getThumbnailUrl());
            context.startActivity(intent);
        });
    }

    @Override
    public int getItemCount() {
        return movies != null ? movies.size() : 0;
    }

    public void updateMovies(List<RelatedMovie> newMovies) {
        this.movies = newMovies;
        notifyDataSetChanged();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        ImageView thumbnailIV;
        TextView titleTV;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            thumbnailIV = itemView.findViewById(R.id.movie_thumbnail);
            titleTV = itemView.findViewById(R.id.movie_title);
        }
    }
}