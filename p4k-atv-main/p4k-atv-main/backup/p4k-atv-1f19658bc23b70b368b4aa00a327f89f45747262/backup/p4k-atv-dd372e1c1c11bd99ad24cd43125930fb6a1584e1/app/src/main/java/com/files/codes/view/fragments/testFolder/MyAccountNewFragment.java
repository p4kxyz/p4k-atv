package com.files.codes.view.fragments.testFolder;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.leanback.app.RowsSupportFragment;
import androidx.leanback.widget.ArrayObjectAdapter;
import androidx.leanback.widget.HeaderItem;
import androidx.leanback.widget.ListRow;
import androidx.leanback.widget.ListRowPresenter;
import androidx.leanback.widget.OnItemViewClickedListener;
import androidx.leanback.widget.Presenter;
import androidx.leanback.widget.Row;
import androidx.leanback.widget.RowPresenter;

import com.files.codes.R;

import java.util.ArrayList;
import java.util.List;

public class MyAccountNewFragment extends RowsSupportFragment {
    private final ArrayObjectAdapter rowsAdapter;

    public MyAccountNewFragment() {
        ListRowPresenter selector = new ListRowPresenter();
        selector.setNumRows(1);
        rowsAdapter = new ArrayObjectAdapter(selector);
        setAdapter(rowsAdapter);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        setOnItemViewClickedListener(new ItemViewClickListener());
        return super.onCreateView(inflater, container, savedInstanceState);

    }

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                loadData();
            }
        }, 200);
    }

    private void loadData() {
        if (isAdded()){
            HomeNewFragment.Card card = new HomeNewFragment.Card();
            card.setTitle(getResources().getString(R.string.myaccount));
            card.setType(HomeNewFragment.Card.Type.SETTINGS);
            card.setLocalImageResource("ic_baseline_account_circle_24");

            HomeNewFragment.CardRow cardRow = new HomeNewFragment.CardRow();
            List<HomeNewFragment.Card> cards = new ArrayList<>();
            cards.clear();
            cards.add(card);
            cardRow.setmTitle(getResources().getString(R.string.myaccount));
            cardRow.setmCards(cards);

            rowsAdapter.add(createListRow(cardRow));
            getMainFragmentAdapter().getFragmentHost().notifyDataReady(
                    getMainFragmentAdapter());
        }
    }

    private ListRow createListRow(HomeNewFragment.CardRow cardRow){
        //create presenter
        SettingsIconPresenter iconPresenter = new SettingsIconPresenter(getActivity());
        ArrayObjectAdapter adapter = new ArrayObjectAdapter(iconPresenter);
        for(HomeNewFragment.Card card : cardRow.getCards()) {
            adapter.add(card);
        }
        HeaderItem headerItem = new HeaderItem(cardRow.getTitle());
        return new CardListRow(headerItem, adapter, cardRow);

    }

    private final class ItemViewClickListener implements OnItemViewClickedListener {

        @Override
        public void onItemClicked(Presenter.ViewHolder itemViewHolder, Object item, RowPresenter.ViewHolder rowViewHolder, Row row) {

            Intent intent = new Intent(getActivity(), ProfileActivity.class);
            startActivity(intent);
        }
    }
}
