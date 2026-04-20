package com.files.codes.utils;

import android.app.Dialog;
import android.content.Context;
import android.os.Bundle;
import android.view.View;
import android.view.Window;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;

import com.files.codes.R;


public class PaidDialog extends Dialog implements View.OnClickListener {

    private Context context;
    private Dialog d;
    private Button continueBtn;
    private TextView instruction_text_view;


    public PaidDialog(@NonNull Context context) {
        super(context);
        this.context = context;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.paid_alert_dialog);
        continueBtn = findViewById(R.id.continueBtn);
        instruction_text_view = findViewById(R.id.instruction_text_view);

        continueBtn.setOnClickListener(this);

    }


    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.continueBtn:
                dismiss();
                break;

            default:
                break;
        }
        dismiss();
    }

}

