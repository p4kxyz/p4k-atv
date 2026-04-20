package com.files.codes.view.fragments.testFolder;

import android.content.Context;

import androidx.leanback.widget.Presenter;
import androidx.leanback.widget.PresenterSelector;
import com.files.codes.view.fragments.testFolder.HomeNewFragment.Card;

import com.files.codes.R;

import java.util.HashMap;

public class CardPresenterSelector extends PresenterSelector {
    private final Context mContext;
    private final HashMap<Card.Type, Presenter> presenters = new HashMap<Card.Type, Presenter>();
    public CardPresenterSelector(Context context) {
        mContext = context;
    }

    @Override
    public Presenter getPresenter(Object item) {
        if (!(item instanceof Card)) throw new RuntimeException(
                String.format("The PresenterSelector only supports data items of type '%s'",
                        Card.class.getName()));
        Card card = (Card) item;
        Presenter presenter = presenters.get(card.getType());
        if (presenter == null) {
            switch (card.getType()) {
                case MOVIE:
                    int themeResId = R.style.MovieCardSimpleTheme;
                    presenter = new ImageCardViewPresenter(mContext, themeResId);
                case SETTINGS:
                    presenter = new SettingsIconPresenter(mContext);
                    break;

                default:
                    int themeResId2 = R.style.MovieCardSimpleTheme;
                    presenter = new ImageCardViewPresenter(mContext, themeResId2);
                    break;
            }
        }
        presenters.put(card.getType(), presenter);
        return presenter;
    }
}
