package com.files.codes.view.presenter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.leanback.widget.Presenter;

import com.files.codes.R;
import com.files.codes.model.movieDetails.MovieSingleDetails;
import com.files.codes.view.HeroStyleVideoDetailsViewHolder;

public class HeroStyleMovieDetailsPresenter extends Presenter {

    Context context;
    private HeroStyleVideoDetailsViewHolder.OnPlayClickListener playClickListener;
    private HeroStyleVideoDetailsViewHolder.OnFavoriteClickListener favoriteClickListener;

    public void setOnPlayClickListener(HeroStyleVideoDetailsViewHolder.OnPlayClickListener listener) {
        this.playClickListener = listener;
    }
    
    public void setOnFavoriteClickListener(HeroStyleVideoDetailsViewHolder.OnFavoriteClickListener listener) {
        this.favoriteClickListener = listener;
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent) {
        context = parent.getContext();
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.hero_style_movie_details, parent, false);
        HeroStyleVideoDetailsViewHolder holder = new HeroStyleVideoDetailsViewHolder(v);
        
        // Set listeners
        holder.setOnPlayClickListener(playClickListener);
        holder.setOnFavoriteClickListener(favoriteClickListener);
        
        return holder;
    }

    @Override
    public void onBindViewHolder(ViewHolder viewHolder, Object item) {
        MovieSingleDetails singleDetails = (MovieSingleDetails) item;
        HeroStyleVideoDetailsViewHolder holder = (HeroStyleVideoDetailsViewHolder) viewHolder;
        holder.bind(singleDetails, context);
    }

    @Override
    public void onUnbindViewHolder(ViewHolder viewHolder) {

    }
}