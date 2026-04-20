package com.files.codes.view.fragments.testFolder;

import android.os.Bundle;
import android.widget.Toast;

import androidx.leanback.widget.ArrayObjectAdapter;
import androidx.leanback.widget.FocusHighlight;
import androidx.leanback.widget.OnItemViewClickedListener;
import androidx.leanback.widget.Presenter;
import androidx.leanback.widget.Row;
import androidx.leanback.widget.RowPresenter;
import androidx.leanback.widget.VerticalGridPresenter;

public class MoviesNewFragment extends GridFragment {
    private static final int COLUMNS = 4;
    private final int ZOOM_FACTOR = FocusHighlight.ZOOM_FACTOR_SMALL;
    private ArrayObjectAdapter mAdapter;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setupAdapter();
        //loadData();
        getMainFragmentAdapter().getFragmentHost().notifyDataReady(getMainFragmentAdapter());

    }

    private void setupAdapter() {
        VerticalGridPresenter presenter = new VerticalGridPresenter(ZOOM_FACTOR);
        presenter.setNumberOfColumns(COLUMNS);
        setGridPresenter(presenter);

        CardPresenterSelector cardPresenter = new CardPresenterSelector(getActivity());
        mAdapter = new ArrayObjectAdapter(cardPresenter);
        setAdapter(mAdapter);
        setOnItemViewClickedListener(new OnItemViewClickedListener() {
            @Override
            public void onItemClicked(
                    Presenter.ViewHolder itemViewHolder,
                    Object item,
                    RowPresenter.ViewHolder rowViewHolder,
                    Row row) {
                HomeNewFragment.Card card = (HomeNewFragment.Card)item;
                Toast.makeText(getActivity(),
                        "Clicked on "+card.getTitle(),
                        Toast.LENGTH_SHORT).show();
            }
        });

    }
}
