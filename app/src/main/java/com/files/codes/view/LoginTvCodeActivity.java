package com.files.codes.view;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.EncodeHintType;
import com.google.zxing.WriterException;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;

import com.files.codes.AppConfig;
import com.files.codes.R;
import com.files.codes.database.DatabaseHelper;
import com.files.codes.model.api.ApiService;
import com.files.codes.model.subscription.TvCodeCheckResponse;
import com.files.codes.model.subscription.TvCodeResponse;
import com.files.codes.model.subscription.User;
import com.files.codes.service.SubscriptionStatusUpdateTask;
import com.files.codes.utils.Constants;
import com.files.codes.utils.RetrofitClient;
import com.files.codes.utils.ToastMsg;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class LoginTvCodeActivity extends Activity {
    private static final String TAG = "LoginTvCodeActivity";
    private static final int POLL_INTERVAL_MS = 4000;

    private TextView tvCode, tvCountdown, tvStatus;
    private ImageView ivQrCode;
    private ProgressBar progressBar;
    private Button btnRefresh;

    private String currentCode;
    private int remainingSeconds;

    private final Handler handler = new Handler(Looper.getMainLooper());
    private final Runnable pollRunnable = this::pollCode;
    private final Runnable countdownRunnable = this::tickCountdown;

    private boolean isPolling = false;
    private boolean isFinishing = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login_tv_code);

        tvCode      = findViewById(R.id.tv_code);
        tvCountdown = findViewById(R.id.tv_countdown);
        tvStatus    = findViewById(R.id.tv_status);
        progressBar = findViewById(R.id.progress_bar);
        btnRefresh  = findViewById(R.id.btn_refresh);
        ivQrCode    = findViewById(R.id.iv_qr_code);

        btnRefresh.setOnClickListener(v -> generateCode());

        generateCode();
    }

    private void generateCode() {
        stopPolling();
        btnRefresh.setVisibility(View.GONE);
        progressBar.setVisibility(View.VISIBLE);
        tvCode.setText("------");
        tvStatus.setText("Đang tạo mã...");
        tvCountdown.setText("");

        ApiService api = RetrofitClient.getRetrofitInstance().create(ApiService.class);
        api.generateTvCode(AppConfig.API_KEY).enqueue(new Callback<TvCodeResponse>() {
            @Override
            public void onResponse(Call<TvCodeResponse> call, Response<TvCodeResponse> response) {
                progressBar.setVisibility(View.GONE);
                if (response.isSuccessful() && response.body() != null
                        && "success".equals(response.body().getStatus())) {
                    currentCode = response.body().getCode();
                    remainingSeconds = response.body().getExpiresIn();
                    tvCode.setText(currentCode);
                    showQrCode(currentCode);
                    tvStatus.setText("Đang chờ xác nhận từ điện thoại...");
                    startCountdown();
                    startPolling();
                } else {
                    showError("Không thể tạo mã. Thử lại sau.");
                }
            }

            @Override
            public void onFailure(Call<TvCodeResponse> call, Throwable t) {
                progressBar.setVisibility(View.GONE);
                Log.e(TAG, "generateCode failed", t);
                showError("Lỗi kết nối. Kiểm tra mạng và thử lại.");
            }
        });
    }

    private void startPolling() {
        isPolling = true;
        handler.postDelayed(pollRunnable, POLL_INTERVAL_MS);
    }

    private void stopPolling() {
        isPolling = false;
        handler.removeCallbacks(pollRunnable);
        handler.removeCallbacks(countdownRunnable);
    }

    private void pollCode() {
        if (!isPolling || currentCode == null || isFinishing) return;

        ApiService api = RetrofitClient.getRetrofitInstance().create(ApiService.class);
        api.checkTvCode(AppConfig.API_KEY, currentCode).enqueue(new Callback<TvCodeCheckResponse>() {
            @Override
            public void onResponse(Call<TvCodeCheckResponse> call, Response<TvCodeCheckResponse> response) {
                if (!isPolling || isFinishing) return;

                if (!response.isSuccessful() || response.body() == null) {
                    scheduleNextPoll();
                    return;
                }

                String status = response.body().getStatus();
                if ("confirmed".equals(status)) {
                    onLoginConfirmed(response.body().getUserInfo());
                } else if ("expired".equals(status)) {
                    onCodeExpired();
                } else {
                    // pending — keep polling
                    scheduleNextPoll();
                }
            }

            @Override
            public void onFailure(Call<TvCodeCheckResponse> call, Throwable t) {
                Log.w(TAG, "pollCode failed", t);
                scheduleNextPoll();
            }
        });
    }

    private void scheduleNextPoll() {
        if (isPolling && !isFinishing) {
            handler.postDelayed(pollRunnable, POLL_INTERVAL_MS);
        }
    }

    private void onLoginConfirmed(User user) {
        stopPolling();
        if (user == null || user.getUserId() == null) {
            showError("Lỗi dữ liệu tài khoản. Thử lại.");
            return;
        }
        tvStatus.setText("Đăng nhập thành công! Đang vào app...");
        tvCode.setTextColor(0xFF66BB6A);

        DatabaseHelper db = new DatabaseHelper(LoginTvCodeActivity.this);
        db.insertUserData(user);
        SharedPreferences.Editor prefs = getSharedPreferences(Constants.USER_LOGIN_STATUS, MODE_PRIVATE).edit();
        prefs.putBoolean(Constants.USER_LOGIN_STATUS, true);
        prefs.apply();

        updateSubscriptionStatus(user.getUserId());
    }

    private void onCodeExpired() {
        stopPolling();
        tvStatus.setText("Mã đã hết hạn.");
        tvCode.setTextColor(0xFFEF5350);
        tvCountdown.setText("Mã hết hạn");
        btnRefresh.setVisibility(View.VISIBLE);
    }

    private void showError(String msg) {
        tvStatus.setText(msg);
        btnRefresh.setVisibility(View.VISIBLE);
    }

    // ── Countdown ─────────────────────────────────────────────────────────────

    private void startCountdown() {
        handler.removeCallbacks(countdownRunnable);
        tickCountdown();
    }

    private void tickCountdown() {
        if (remainingSeconds <= 0) {
            tvCountdown.setText("Mã hết hạn");
            return;
        }
        int min = remainingSeconds / 60;
        int sec = remainingSeconds % 60;
        tvCountdown.setText(String.format("Hết hạn sau %d:%02d", min, sec));
        remainingSeconds--;
        handler.postDelayed(countdownRunnable, 1000);
    }

    // ── Post-login ─────────────────────────────────────────────────────────────

    private void updateSubscriptionStatus(String userId) {
        SubscriptionStatusUpdateTask task = new SubscriptionStatusUpdateTask(userId, this, success -> {
            if (success) {
                Intent intent = new Intent(LoginTvCodeActivity.this, MainActivity.class);
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK
                        | Intent.FLAG_ACTIVITY_NO_ANIMATION);
                startActivity(intent);
                finish();
            } else {
                new ToastMsg(LoginTvCodeActivity.this).toastIconError("Không thể kiểm tra đăng ký.");
            }
        });
        task.execute();
    }

    @Override
    protected void onDestroy() {
        isFinishing = true;
        stopPolling();
        super.onDestroy();
    }

    private void showQrCode(String code) {
        try {
            java.util.Map<EncodeHintType, Object> hints = new java.util.HashMap<>();
            hints.put(EncodeHintType.MARGIN, 2);
            hints.put(EncodeHintType.CHARACTER_SET, "UTF-8");
            hints.put(EncodeHintType.ERROR_CORRECTION, com.google.zxing.qrcode.decoder.ErrorCorrectionLevel.H);
            BitMatrix bitMatrix = new QRCodeWriter().encode(code, BarcodeFormat.QR_CODE, 400, 400, hints);
            int width = bitMatrix.getWidth();
            int height = bitMatrix.getHeight();
            int[] pixels = new int[width * height];
            for (int y = 0; y < height; y++) {
                for (int x = 0; x < width; x++) {
                    pixels[y * width + x] = bitMatrix.get(x, y) ? Color.BLACK : Color.WHITE;
                }
            }
            Bitmap qrBitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
            qrBitmap.setPixels(pixels, 0, width, 0, 0, width, height);

            // Overlay logo vào giữa QR
            Bitmap mutable = qrBitmap.copy(Bitmap.Config.ARGB_8888, true);
            Canvas canvas = new Canvas(mutable);
            Bitmap logo = BitmapFactory.decodeResource(getResources(), R.drawable.logo);
            if (logo != null) {
                int logoSize = width / 5; // logo chiếm 20% QR
                Bitmap scaledLogo = Bitmap.createScaledBitmap(logo, logoSize, logoSize, true);
                int logoX = (width - logoSize) / 2;
                int logoY = (height - logoSize) / 2;
                // Vẽ nền trắng bo góc cho logo
                Paint bgPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
                bgPaint.setColor(Color.WHITE);
                int pad = 6;
                canvas.drawRoundRect(new RectF(logoX - pad, logoY - pad,
                        logoX + logoSize + pad, logoY + logoSize + pad), 12, 12, bgPaint);
                canvas.drawBitmap(scaledLogo, logoX, logoY, null);
                scaledLogo.recycle();
                logo.recycle();
            }

            ivQrCode.setImageBitmap(mutable);
            ivQrCode.setVisibility(View.VISIBLE);
        } catch (WriterException e) {
            Log.e(TAG, "QR generate failed", e);
            ivQrCode.setVisibility(View.GONE);
        }
    }
}
