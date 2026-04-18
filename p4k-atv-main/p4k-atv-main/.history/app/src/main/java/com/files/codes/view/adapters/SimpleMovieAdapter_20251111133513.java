package com.files.codes.view.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.files.codes.R;
import com.files.codes.model.Movie;

import java.util.List;

public class SimpleMovieAdapter extends RecyclerView.Adapter<SimpleMovieAdapter.MovieViewHolder> {
    
    private List<Movie> movieList;
    private OnMovieClickListener clickListener;
    
    public interface OnMovieClickListener {
        void onMovieClick(Movie movie);
    }
    
    public SimpleMovieAdapter(List<Movie> movieList, OnMovieClickListener clickListener) {
        this.movieList = movieList;
        this.clickListener = clickListener;
    }
    
    @NonNull
    @Override
    public MovieViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_simple_movie_card, parent, false);
        return new MovieViewHolder(view);
    }
    
    @Override
    public void onBindViewHolder(@NonNull MovieViewHolder holder, int position) {
        Movie movie = movieList.get(position);
        
        holder.titleText.setText(movie.getTitle());
        if (movie.getRelease() != null && !movie.getRelease().isEmpty()) {
            holder.yearText.setText(movie.getRelease());
            holder.yearText.setVisibility(View.VISIBLE);
        } else {
            holder.yearText.setVisibility(View.GONE);
        }
        
        // Load thumbnail
        if (movie.getThumbnailUrl() != null && !movie.getThumbnailUrl().isEmpty()) {
            Glide.with(holder.itemView.getContext())
                    .load(movie.getThumbnailUrl())
                    .placeholder(R.drawable.album_art)
                    .error(R.drawable.album_art)
                    .into(holder.thumbnailImage);
        }
        
        holder.itemView.setOnClickListener(v -> {
            if (clickListener != null) {
                clickListener.onMovieClick(movie);
            }
        });
        
        // TV focus handling
        holder.itemView.setFocusable(true);
        holder.itemView.setFocusableInTouchMode(true);
    }
    
    @Override
    public int getItemCount() {
        return movieList.size();
    }
    
    static class MovieViewHolder extends RecyclerView.ViewHolder {
        ImageView thumbnailImage;
        TextView titleText;
        TextView yearText;
        
        public MovieViewHolder(@NonNull View itemView) {
            super(itemView);
            thumbnailImage = itemView.findViewById(R.id.thumbnail_image);
            titleText = itemView.findViewById(R.id.title_text);
            yearText = itemView.findViewById(R.id.year_text);
        }
    }
}