package com.files.codes.view.presenter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.leanback.widget.Presenter;

import com.files.codes.R;
import com.files.codes.model.movieDetails.MovieSingleDetails;
import com.files.codes.view.VideoDetailsViewHolder;


public class MovieDetailsDescriptionPresenter extends Presenter {

    Context context;

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent) {
        context = parent.getContext();
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.vh_movie_details, parent, false);
        return new VideoDetailsViewHolder(v);
    }

    @Override
    public void onBindViewHolder(ViewHolder viewHolder, Object item) {

        MovieSingleDetails singleDetails = (MovieSingleDetails) item;
        VideoDetailsViewHolder holder = (VideoDetailsViewHolder) viewHolder;
        holder.bind(singleDetails, context);
    }

    @Override
    public void onUnbindViewHolder(ViewHolder viewHolder) {

    }
}
