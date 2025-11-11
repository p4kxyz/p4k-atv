package com.files.codes.view;

import android.content.Context;
import android.graphics.drawable.GradientDrawable;
import android.os.Build;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.RequiresApi;
import androidx.core.content.ContextCompat;
import androidx.leanback.widget.Presenter;
import androidx.recyclerview.widget.RecyclerView;

import com.files.codes.R;
import com.files.codes.model.Genre;
import com.files.codes.model.movieDetails.CastAndCrew;
import com.files.codes.model.movieDetails.Director;
import com.files.codes.model.movieDetails.MovieSingleDetails;

import java.util.List;

public class VideoDetailsViewHolder extends Presenter.ViewHolder {
    TextView movieTitleTV;
    TextView descriptionTv;
    TextView releaseDateTv;
    TextView movieOverview;
    TextView mDirectorTv, castTv;
    TextView mOverviewLabelTV;
    LinearLayout mGenresLayout;
    LinearLayout detailsLayout;
    View divider;
    RecyclerView castCrewRv;
    Button bt1, bt2;
    private View itemView;


    public VideoDetailsViewHolder(View view) {
        super(view);
        itemView = view;

        movieTitleTV = itemView.findViewById(R.id.movie_title);
        descriptionTv = itemView.findViewById(R.id.movie_description_tv);
        releaseDateTv = itemView.findViewById(R.id.release_date_tv);
        mGenresLayout = itemView.findViewById(R.id.genres);
        mDirectorTv = itemView.findViewById(R.id.director_tv);
        castTv = itemView.findViewById(R.id.cast_tv);
        divider = itemView.findViewById(R.id.divider);
        detailsLayout = itemView.findViewById(R.id.details_layout);
        //castCrewRv = itemView.findViewById(R.id. cast_crew_rv);

    }

    @RequiresApi(api = Build.VERSION_CODES.KITKAT)
    public void bind(MovieSingleDetails movie, Context context) {


        if (movie != null && movie.getTitle() != null) {
            movieTitleTV.setText(movie.getTitle());
            if (!movie.getType().equals("tv")) {
                //mRuntimeTV.setText(String.format(Locale.getDefault(), "%d minutes", movie.getRuntime()));
                descriptionTv.setText(movie.getDescription());
                //releaseDateTv.setText(String.format(Locale.getDefault(), "(%s)", movie.getRelease().substring(0, 4)));
                releaseDateTv.setText(context.getString(R.string.release_date) + ": " + movie.getRelease());
                mGenresLayout.removeAllViews();

                String director = "";
                if (movie.getDirector() != null) {
                    int count = 1;
                    for (Director director1 : movie.getDirector()) {
                        if (count == movie.getDirector().size()) {
                            director += director1.getName();
                        } else {
                            director += director1.getName() + ", ";
                        }
                        count++;
                    }
                }
                mDirectorTv.setText(director);

                String cast = "";
                if (movie.getCastAndCrew() != null) {
                    int count = 1;
                    for (CastAndCrew c : movie.getCastAndCrew()) {
                        if (count == movie.getCastAndCrew().size()) {
                            cast += c.getName();
                        } else {
                            cast += c.getName() + ", ";
                        }
                        count++;
                    }
                }

                castTv.setText(cast);


                int _10dp = 10;
                int _5dp = 5;
                float corner = 5;

                // Adds each genre to the genre layout
                if (movie.getGenre() != null) {
                    for (Genre genre : movie.getGenre()) {
                        TextView tv = new TextView(itemView.getContext());

                        tv.setText(genre.getName());
                        GradientDrawable shape = new GradientDrawable();
                        shape.setShape(GradientDrawable.RECTANGLE);
                        shape.setCornerRadius(corner);
                        shape.setColor(ContextCompat.getColor(itemView.getContext(), R.color.colorPrimaryDark));
                        tv.setPadding(_5dp, 3, _5dp, 3);
                        //tv.setBackground(shape);
                        tv.setTextSize(10);

                        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(new LinearLayout.LayoutParams(
                                LinearLayout.LayoutParams.WRAP_CONTENT,
                                LinearLayout.LayoutParams.WRAP_CONTENT));
                        params.setMargins(0, 0, _10dp, 0);
                        tv.setLayoutParams(params);

                        mGenresLayout.addView(tv);
                    }
                }

            }

            if (movie.getType().equals("tv")) {
                detailsLayout.setVisibility(View.GONE);
                divider.setVisibility(View.GONE);
                descriptionTv.setText(context.getString(R.string.you_are_watching_on) + " " + context.getResources().getString(R.string.app_name));
                descriptionTv.setCompoundDrawables(context.getResources().getDrawable(R.drawable.ic_fiber_manual_record_red), null, null, null);
            }
        }
    }


    public void setupCastCrewRv(List<CastAndCrew> castAndCrews) {

        /*castCrewRv.setLayoutManager(new GridLayoutManager(itemView.getContext(), 5));
        castCrewRv.addItemDecoration(new SpacingItemDecoration(5, Tools.dpToPx(itemView.getContext(), 5), true));
        castCrewRv.setHasFixedSize(true);

        CastAndCrewAdapter adapter = new CastAndCrewAdapter(itemView.getContext(), castAndCrews);
        castCrewRv.setAdapter(adapter);*/

    }

}
