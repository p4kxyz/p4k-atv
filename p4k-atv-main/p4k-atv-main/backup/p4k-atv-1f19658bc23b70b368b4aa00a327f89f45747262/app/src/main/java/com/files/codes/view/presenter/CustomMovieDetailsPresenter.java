package com.files.codes.view.presenter;

import android.view.View;

import androidx.leanback.widget.DetailsOverviewLogoPresenter;
import androidx.leanback.widget.FullWidthDetailsOverviewRowPresenter;
import androidx.leanback.widget.Presenter;
import androidx.leanback.widget.RowPresenter;

public class CustomMovieDetailsPresenter extends FullWidthDetailsOverviewRowPresenter {

    public CustomMovieDetailsPresenter(Presenter detailsPresenter, DetailsOverviewLogoPresenter logoPresenter) {
        super(detailsPresenter, logoPresenter);

        setInitialState(FullWidthDetailsOverviewRowPresenter.ALIGN_MODE_MIDDLE);
    }


    @Override
    protected void onBindRowViewHolder(RowPresenter.ViewHolder holder, Object item) {
        super.onBindRowViewHolder(holder, item);
        ViewHolder vh = (ViewHolder) holder;
        View v = vh.getOverviewView();
        v.setBackgroundColor(getBackgroundColor());
        //v.findViewById(androidx.leanback.R.id.details_overview_actions_background).setBackgroundColor(getActionsBackgroundColor());
        v.findViewById(androidx.leanback.R.id.details_overview_actions_background).setBackgroundColor(getBackgroundColor());
    }
}
