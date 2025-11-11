package com.files.codes.view.adapter;

import android.content.Context;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityOptionsCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.files.codes.R;
import com.files.codes.model.Movie;
import com.files.codes.view.VideoDetailsActivity;
import com.files.codes.view.HeroStyleVideoDetailsActivity;
import com.squareup.picasso.Picasso;

import java.util.List;

public class ActorMovieAdapter extends RecyclerView.Adapter<ActorMovieAdapter.ViewHolder> {
    private Context context;
    private List<Movie> movieList;

    public ActorMovieAdapter(Context context, List<Movie> movieList) {
        this.context = context;
        this.movieList = movieList;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_movie_card, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Movie movie = movieList.get(position);
        
        holder.titleText.setText(movie.getTitle());
        
        // Load movie poster
        if (movie.getThumbnailUrl() != null && !movie.getThumbnailUrl().isEmpty()) {
            Picasso.get()
                .load(movie.getThumbnailUrl())
                .placeholder(R.drawable.poster_placeholder)
                .error(R.drawable.poster_placeholder)
                .into(holder.posterImage);
        } else {
            holder.posterImage.setImageResource(R.drawable.poster_placeholder);
        }
        
        // Click listener
        holder.itemView.setOnClickListener(v -> {
            android.util.Log.d("ActorMovieAdapter", "Opening movie: " + movie.getTitle() + " (ID: " + movie.getVideosId() + ")");
            Intent intent = new Intent(context, HeroStyleVideoDetailsActivity.class);
            intent.putExtra("id", movie.getVideosId());
            intent.putExtra("type", movie.getIsTvseries().equals("1") ? "tvseries" : "movie");
            
            // Optional transition animation
            if (context instanceof androidx.fragment.app.FragmentActivity) {
                androidx.fragment.app.FragmentActivity activity = (androidx.fragment.app.FragmentActivity) context;
                android.os.Bundle bundle = ActivityOptionsCompat.makeSceneTransitionAnimation(activity).toBundle();
                context.startActivity(intent, bundle);
            } else {
                context.startActivity(intent);
            }
        });
    }

    @Override
    public int getItemCount() {
        return movieList != null ? movieList.size() : 0;
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        ImageView posterImage;
        TextView titleText;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            posterImage = itemView.findViewById(R.id.poster_image);
            titleText = itemView.findViewById(R.id.title_text);
        }
    }
}