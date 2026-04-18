package com.files.codes.view.presenter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;

import androidx.leanback.widget.Action;
import androidx.leanback.widget.Presenter;

import com.files.codes.R;


public class ActionButtonPresenter extends Presenter {

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.tv_action_button_layout, parent, false);
        return new ViewHolder(v);
    }

    @Override
    public void onBindViewHolder(ViewHolder viewHolder, Object item) {
        Button button = (Button) viewHolder.view;
        Action action = (Action) item;
        action.setLabel2("");

        button.setText(action.getLabel1());

        if (action.getId() == 2) {
            button.setCompoundDrawables(null, null, null, null);
        }

    }

    @Override
    public void onUnbindViewHolder(ViewHolder viewHolder) {

    }
}
