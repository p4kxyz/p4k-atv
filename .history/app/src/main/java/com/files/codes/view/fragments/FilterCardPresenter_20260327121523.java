package com.files.codes.view.fragments;

import android.content.Context;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.text.TextUtils;
import android.view.Gravity;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.leanback.widget.Presenter;

public class FilterCardPresenter extends Presenter {

    private static final int WIDTH_DP  = 160;
    private static final int HEIGHT_DP = 80;
    private static final int RADIUS_DP = 12;

    private static final int COLOR_NORMAL  = 0xFF1C2B3A;
    private static final int COLOR_FOCUSED = 0xFF0277BD;
    private static final int COLOR_BORDER  = 0xFF37474F;
    private static final int COLOR_ACTIVE_BORDER = 0xFF0288D1;

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent) {
        Context ctx = parent.getContext();

        // outer: horizontal — icon | text column
        LinearLayout card = new LinearLayout(ctx);
        card.setLayoutParams(new ViewGroup.LayoutParams(dp(ctx, WIDTH_DP), dp(ctx, HEIGHT_DP)));
        card.setOrientation(LinearLayout.HORIZONTAL);
        card.setGravity(Gravity.CENTER_VERTICAL);
        card.setFocusable(true);
        card.setFocusableInTouchMode(true);
        card.setPadding(dp(ctx, 12), dp(ctx, 10), dp(ctx, 12), dp(ctx, 10));
        applyBackground(card, COLOR_NORMAL, COLOR_BORDER, dp(ctx, RADIUS_DP));

        card.setOnFocusChangeListener((v, hasFocus) -> {
            int fill   = hasFocus ? COLOR_FOCUSED : COLOR_NORMAL;
            int stroke = hasFocus ? COLOR_FOCUSED : COLOR_BORDER;
            applyBackground(card, fill, stroke, dp(ctx, RADIUS_DP));
            card.setScaleX(hasFocus ? 1.05f : 1f);
            card.setScaleY(hasFocus ? 1.05f : 1f);
            // Always keep fully visible (prevent Leanback dimming)
            card.setAlpha(1.0f);
        });

        // icon ☰
        TextView icon = new TextView(ctx);
        icon.setId(android.R.id.icon);
        icon.setText("\u2630");
        icon.setTextColor(0xFF90CAF9);
        icon.setTextSize(16);
        icon.setGravity(Gravity.CENTER);
        LinearLayout.LayoutParams iconLp = new LinearLayout.LayoutParams(
            dp(ctx, 26), ViewGroup.LayoutParams.WRAP_CONTENT);
        iconLp.setMargins(0, 0, dp(ctx, 8), 0);
        icon.setLayoutParams(iconLp);
        card.addView(icon);

        // text column: title + up to 3 filter rows
        LinearLayout col = new LinearLayout(ctx);
        col.setOrientation(LinearLayout.VERTICAL);
        col.setGravity(Gravity.CENTER_VERTICAL);
        col.setLayoutParams(new LinearLayout.LayoutParams(
            0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));

        TextView title = new TextView(ctx);
        title.setId(android.R.id.text1);
        title.setText("B\u1ed9 L\u1ecdc");
        title.setTextColor(0xFFECEFF1);
        title.setTextSize(12);
        title.setTypeface(null, Typeface.BOLD);
        title.setMaxLines(1);
        title.setEllipsize(TextUtils.TruncateAt.END);
        col.addView(title);

        // row genre
        TextView tvGenre = new TextView(ctx);
        tvGenre.setId(android.R.id.text2);
        tvGenre.setTextColor(0xFFFFD54F);
        tvGenre.setTextSize(10);
        tvGenre.setMaxLines(1);
        tvGenre.setEllipsize(TextUtils.TruncateAt.END);
        tvGenre.setVisibility(android.view.View.GONE);
        col.addView(tvGenre);

        // row country
        TextView tvCountry = new TextView(ctx);
        tvCountry.setId(android.R.id.summary);
        tvCountry.setTextColor(0xFF80CBC4);
        tvCountry.setTextSize(10);
        tvCountry.setMaxLines(1);
        tvCountry.setEllipsize(TextUtils.TruncateAt.END);
        tvCountry.setVisibility(android.view.View.GONE);
        col.addView(tvCountry);

        // row year
        TextView tvYear = new TextView(ctx);
        tvYear.setId(android.R.id.message);
        tvYear.setTextColor(0xFFCE93D8);
        tvYear.setTextSize(10);
        tvYear.setMaxLines(1);
        tvYear.setEllipsize(TextUtils.TruncateAt.END);
        tvYear.setVisibility(android.view.View.GONE);
        col.addView(tvYear);

        card.addView(col);

        return new ViewHolder(card);
    }

    @Override
    public void onBindViewHolder(ViewHolder viewHolder, Object item) {
        FilterItem fi = (FilterItem) item;
        bindRow(viewHolder.view.findViewById(android.R.id.text2),   fi.genreText);
        bindRow(viewHolder.view.findViewById(android.R.id.summary),  fi.countryText);
        bindRow(viewHolder.view.findViewById(android.R.id.message),  fi.yearText);
        boolean active = !fi.genreText.isEmpty() || !fi.countryText.isEmpty() || !fi.yearText.isEmpty();
        int stroke = active ? COLOR_ACTIVE_BORDER : COLOR_BORDER;
        applyBackground(viewHolder.view, COLOR_NORMAL, stroke,
            dp(viewHolder.view.getContext(), RADIUS_DP));
        // force RecyclerView to re-measure height after visibility changes
        viewHolder.view.requestLayout();
    }

    private static void bindRow(TextView tv, String text) {
        if (tv == null) return;
        if (text != null && !text.isEmpty()) {
            tv.setText(text);
            tv.setVisibility(android.view.View.VISIBLE);
        } else {
            tv.setVisibility(android.view.View.GONE);
        }
    }

    @Override
    public void onUnbindViewHolder(ViewHolder viewHolder) {}

    private static void applyBackground(android.view.View v, int fillColor, int strokeColor, int radius) {
        GradientDrawable bg = new GradientDrawable();
        bg.setColor(fillColor);
        bg.setCornerRadius(radius);
        bg.setStroke(2, strokeColor);
        v.setBackground(bg);
    }

    private static int dp(Context ctx, int dp) {
        return Math.round(dp * ctx.getResources().getDisplayMetrics().density);
    }
}
