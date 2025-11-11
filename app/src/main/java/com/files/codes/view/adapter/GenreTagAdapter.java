package com.files.codes.view.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.files.codes.R;
import com.files.codes.model.Genre;

import java.util.List;

public class GenreTagAdapter extends RecyclerView.Adapter<GenreTagAdapter.GenreViewHolder> {
    
    private List<Genre> genres;
    
    public GenreTagAdapter(List<Genre> genres) {
        this.genres = genres;
    }
    
    @NonNull
    @Override
    public GenreViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_genre_tag, parent, false);
        return new GenreViewHolder(view);
    }
    
    @Override
    public void onBindViewHolder(@NonNull GenreViewHolder holder, int position) {
        Genre genre = genres.get(position);
        holder.genreText.setText(genre.getName());
    }
    
    @Override
    public int getItemCount() {
        return genres != null ? genres.size() : 0;
    }
    
    public void updateGenres(List<Genre> newGenres) {
        this.genres = newGenres;
        notifyDataSetChanged();
    }
    
    static class GenreViewHolder extends RecyclerView.ViewHolder {
        TextView genreText;
        
        GenreViewHolder(@NonNull View itemView) {
            super(itemView);
            genreText = itemView.findViewById(android.R.id.text1);
            // Since we're using TextView as root in item_genre_tag.xml
            if (genreText == null) {
                genreText = (TextView) itemView;
            }
        }
    }
}