package com.files.codes.view.dialog;

import android.app.Dialog;
import android.content.Context;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Patterns;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import androidx.annotation.NonNull;

import com.files.codes.R;

/**
 * Dialog thiết lập đồng bộ lịch sử xem
 */
public class SyncSetupDialog extends Dialog {
    
    private EditText etEmail;
    private Button btnSetup;
    private Button btnCancel;
    private TextView tvDescription;
    
    private OnSyncSetupListener listener;

    public interface OnSyncSetupListener {
        void onSetupComplete(String email);
    }

    public SyncSetupDialog(@NonNull Context context, OnSyncSetupListener listener) {
        super(context);
        this.listener = listener;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.dialog_sync_setup);
        
        initViews();
        setupListeners();
    }

    private void initViews() {
        etEmail = findViewById(R.id.et_email);
        btnSetup = findViewById(R.id.btn_setup);
        btnCancel = findViewById(R.id.btn_cancel);
        tvDescription = findViewById(R.id.tv_description);

        // Set description text
        tvDescription.setText("Nhập email để tạo link đồng bộ lịch sử xem giữa các thiết bị. " +
                "Bạn sẽ có thể đồng bộ lịch sử xem trên TV, điện thoại và máy tính.");
    }

    private void setupListeners() {
        btnSetup.setOnClickListener(v -> {
            String email = etEmail.getText().toString().trim();
            
            if (validateEmail(email)) {
                if (listener != null) {
                    listener.onSetupComplete(email);
                }
                dismiss();
            }
        });

        btnCancel.setOnClickListener(v -> dismiss());
    }

    private boolean validateEmail(String email) {
        if (TextUtils.isEmpty(email)) {
            etEmail.setError("Vui lòng nhập email");
            etEmail.requestFocus();
            return false;
        }

        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            etEmail.setError("Email không hợp lệ");
            etEmail.requestFocus();
            return false;
        }

        return true;
    }
}