const fs = require('fs');
const path = 'app/src/main/java/com/files/codes/view/fragments/WatchHistoryPageFragment.java';
let content = fs.readFileSync(path, 'utf8');

// 1. Remove the previously injected dp() and buildCustomListDialog() entirely.
// They were injected around line 48 right after setupFragment();
const badBlockRegex = /\s*private int dp\(int dp\) \{[\s\S]*?return dialog;\s*\}/;
content = content.replace(badBlockRegex, '');

// 2. We inject the proper dp() and buildCustomListDialog() at the VERY END of the file (before the last '}')
// We will use getActivity() instead of requireContext() to ensure it matches HomeActivity's Theme correctly!
const properMethods = `
    private int dp(int dp) {
        if (getActivity() == null) return dp;
        return Math.round(dp * getResources().getDisplayMetrics().density);
    }

    private android.app.Dialog buildCustomListDialog(
            String title, String[] items, int checkedIndex,
            Runnable[] actions, Runnable onCancel) {

        android.app.Dialog dialog = new android.app.Dialog(getActivity());
        dialog.requestWindowFeature(android.view.Window.FEATURE_NO_TITLE);
        dialog.setCancelable(true);
        if (onCancel != null) {
            dialog.setOnCancelListener(d -> onCancel.run());
        }

        android.widget.LinearLayout root = new android.widget.LinearLayout(getActivity());
        root.setOrientation(android.widget.LinearLayout.VERTICAL);
        root.setBackgroundColor(0xFF1E1E2E);
        root.setPadding(dp(20), dp(20), dp(20), dp(12));

        android.widget.TextView titleView = new android.widget.TextView(getActivity());
        titleView.setText(title);
        titleView.setTextColor(0xFFFFFFFF);
        titleView.setTextSize(android.util.TypedValue.COMPLEX_UNIT_SP, 18);
        titleView.setTypeface(android.graphics.Typeface.DEFAULT_BOLD);
        titleView.setPadding(dp(4), 0, dp(4), dp(10));
        root.addView(titleView);

        android.view.View divider = new android.view.View(getActivity());
        divider.setBackgroundColor(0x55FFFFFF);
        android.widget.LinearLayout.LayoutParams divLp =
                new android.widget.LinearLayout.LayoutParams(
                        android.widget.LinearLayout.LayoutParams.MATCH_PARENT, dp(1));
        divLp.bottomMargin = dp(6);
        root.addView(divider, divLp);

        android.widget.ScrollView sv = new android.widget.ScrollView(getActivity());
        sv.setVerticalScrollBarEnabled(false);
        android.widget.LinearLayout ll = new android.widget.LinearLayout(getActivity());
        ll.setOrientation(android.widget.LinearLayout.VERTICAL);

        for (int i = 0; i < items.length; i++) {
            final int idx = i;
            boolean isSelected = (checkedIndex >= 0 && i == checkedIndex);

            android.widget.TextView tv = new android.widget.TextView(getActivity());
            // Remove the hardcoded 5 spaces for UI consistency when there's no checkmark
            tv.setText(isSelected ? "✓  " + items[i] : items[i]);
            tv.setTextColor(isSelected ? 0xFF64B5F6 : 0xFFDDDDDD);
            tv.setTextSize(android.util.TypedValue.COMPLEX_UNIT_SP, 15);
            tv.setPadding(dp(10), dp(13), dp(10), dp(13));
            tv.setFocusable(true);
            tv.setFocusableInTouchMode(false);
            tv.setClickable(true);
            tv.setBackground(null);

            tv.setOnFocusChangeListener((v, hasFocus) -> {
                if (hasFocus) {
                    v.setBackgroundColor(0x664FC3F7);
                    ((android.widget.TextView) v).setTextColor(0xFFFFFFFF);
                } else {
                    v.setBackgroundColor(android.graphics.Color.TRANSPARENT);
                    boolean sel = (checkedIndex >= 0 && idx == checkedIndex);
                    ((android.widget.TextView) v).setTextColor(sel ? 0xFF64B5F6 : 0xFFDDDDDD);
                }
            });

            tv.setOnClickListener(v -> {
                dialog.dismiss();
                if (actions != null && idx < actions.length && actions[idx] != null) {
                    actions[idx].run();
                }
            });

            ll.addView(tv);

            android.view.View sep = new android.view.View(getActivity());
            sep.setBackgroundColor(0x22FFFFFF);
            ll.addView(sep, new android.widget.LinearLayout.LayoutParams(
                    android.widget.LinearLayout.LayoutParams.MATCH_PARENT, 1));
        }
        sv.addView(ll);

        // Fix height: wrap_content makes it adapt perfectly without making a massive empty box
        android.widget.LinearLayout.LayoutParams svLp =
                new android.widget.LinearLayout.LayoutParams(
                        android.widget.LinearLayout.LayoutParams.MATCH_PARENT,
                        android.view.ViewGroup.LayoutParams.WRAP_CONTENT);
        root.addView(sv, svLp);

        // Cancel button
        android.view.View btnDivider = new android.view.View(getActivity());
        btnDivider.setBackgroundColor(0x33FFFFFF);
        android.widget.LinearLayout.LayoutParams bdLp =
                new android.widget.LinearLayout.LayoutParams(
                        android.widget.LinearLayout.LayoutParams.MATCH_PARENT, dp(1));
        bdLp.topMargin = dp(6);
        root.addView(btnDivider, bdLp);

        android.widget.TextView cancelBtn = new android.widget.TextView(getActivity());
        cancelBtn.setText("Đóng");
        cancelBtn.setTextColor(0xFF90CAF9);
        cancelBtn.setTextSize(android.util.TypedValue.COMPLEX_UNIT_SP, 15);
        cancelBtn.setTypeface(android.graphics.Typeface.DEFAULT_BOLD);
        cancelBtn.setPadding(dp(10), dp(12), dp(10), dp(4));
        cancelBtn.setFocusable(true);
        cancelBtn.setFocusableInTouchMode(false);
        cancelBtn.setClickable(true);
        cancelBtn.setBackground(null);
        cancelBtn.setGravity(android.view.Gravity.END);
        cancelBtn.setOnFocusChangeListener((v, hasFocus) ->
                v.setBackgroundColor(hasFocus ? 0x33FFFFFF : android.graphics.Color.TRANSPARENT));
        cancelBtn.setOnClickListener(v -> { dialog.dismiss(); if (onCancel != null) onCancel.run(); });
        android.widget.LinearLayout.LayoutParams cbLp =
                new android.widget.LinearLayout.LayoutParams(
                        android.widget.LinearLayout.LayoutParams.MATCH_PARENT,
                        android.widget.LinearLayout.LayoutParams.WRAP_CONTENT);
        root.addView(cancelBtn, cbLp);

        dialog.setContentView(root);
        android.view.Window w = dialog.getWindow();
        if (w != null) {
            w.setLayout(dp(480), android.view.WindowManager.LayoutParams.WRAP_CONTENT);
            w.setGravity(android.view.Gravity.CENTER);
            w.setBackgroundDrawableResource(android.R.color.transparent);
        }
        return dialog;
    }
`;

content = content.replace(/}\s*$/m, properMethods + "\n}");

// 3. Update the click listener to use the proper unicode escapes to avoid any encoding corruption during JS execution
const clickListenerRegex = /private OnItemViewClickedListener getDefaultItemViewClickedListener\(\) \{[\s\S]*?private void playVideo\(VideoContent vc\) \{/;
const newClickListener = `private OnItemViewClickedListener getDefaultItemViewClickedListener() {
        return (viewHolder, item, rowViewHolder, row) -> {
            if (!(item instanceof VideoContent)) return;
            VideoContent vc = (VideoContent) item;

            // Using Unicode escapes to guarantee correct display of emojis and Vietnamese text
            String[] options = {"\\u25B6  Xem ti\\u1EBFp", "\\uD83D\\uDDD1  X\\u00F3a kh\\u1ECFi l\\u1ECBch s\\u1EED"};
            
            Runnable[] actions = new Runnable[] {
                () -> playVideo(vc),
                () -> deleteHistoryItem(vc)
            };

            android.app.Dialog dialog = buildCustomListDialog(vc.getTitle(), options, -1, actions, null);
            dialog.show();
        };
    }

    private void playVideo(VideoContent vc) {`;

content = content.replace(clickListenerRegex, newClickListener);

fs.writeFileSync(path, content, 'utf8');
console.log("SUCCESS APPLYING UI FIX");
