package com.files.codes.view.presenter;

import android.content.res.Resources;
import android.graphics.Color;
import android.view.Gravity;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.leanback.widget.Presenter;

import com.files.codes.R;
import com.files.codes.model.CountryModel;


public class CountryPresenter extends Presenter {

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent) {
        TextView view = new TextView(parent.getContext());

        Resources res = parent.getResources();
        int width = res.getDimensionPixelSize(R.dimen.country_card_width);
        int height = res.getDimensionPixelSize(R.dimen.country_card_height);

        view.setLayoutParams(new ViewGroup.LayoutParams(width, height));
        view.setFocusable(true);
        view.setFocusableInTouchMode(true);
       // view.setBackgroundColor(ContextCompat.getColor(parent.getContext(), R.color.colorPrimary));
        view.setBackgroundResource(getColor());
        view.setTextColor(Color.WHITE);
        view.setGravity(Gravity.CENTER);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(ViewHolder viewHolder, Object item) {
        CountryModel countryModel = (CountryModel) item;
        ((TextView) viewHolder.view).setText(countryModel.getName());
    }

    @Override
    public void onUnbindViewHolder(ViewHolder viewHolder) {

    }

    int count;
    private int getColor() {

        //int colorList[] = {R.color.colorPrimary, R.color.blue_400, R.color.indigo_400, R.color.orange_400, R.color.light_green_400, R.color.blue_grey_400};
        int[] colorList2 = {R.drawable.gradient_1, R.drawable.gradient_2, R.drawable.gradient_3, R.drawable.gradient_4, R.drawable.gradient_5, R.drawable.gradient_6};

        if (count >= 6) {
            count = 0;
        }

        int color = colorList2[count];
        count++;

        return color;

    }
}
