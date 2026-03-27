const fs = require('fs');

let fragmentPath = 'app/src/main/java/com/files/codes/view/fragments/WatchHistoryPageFragment.java';
let content = fs.readFileSync(fragmentPath, 'utf8');

let regexClick = /private OnItemViewClickedListener getDefaultItemViewClickedListener\(\) \{[\s\S]*?private void playVideo\(VideoContent vc\) \{/m;

let newClickListener = `private OnItemViewClickedListener getDefaultItemViewClickedListener() {
        return (viewHolder, item, rowViewHolder, row) -> {
            if (!(item instanceof VideoContent)) return;
            VideoContent vc = (VideoContent) item;

            String[] options = {"▶  Xem tiếp", "🗑  Xóa lịch sử phim này"};
            
            Runnable[] actions = new Runnable[] {
                () -> playVideo(vc),
                () -> deleteHistoryItem(vc)
            };

            android.app.Dialog dialog = buildCustomListDialog(vc.getTitle(), options, -1, actions, null);
            dialog.show();
        };
    }

    private void playVideo(VideoContent vc) {`;

content = content.replace(regexClick, newClickListener);

let regexDelete = /private void deleteHistoryItem\(VideoContent vc\) \{[\s\S]*?Toast\.makeText\(getContext\(\), "Đã xóa khỏi lịch sử", Toast\.LENGTH_SHORT\)\.show\(\);\s*loadWatchHistory\(\); \/\/ Reload to refresh grid\s*\}\);[\s\S]*?\}\s*\}\);\s*\}\s*\}/m;

let newDelete = `private void deleteHistoryItem(VideoContent vc) {
        if (syncManager != null) {
            syncManager.deleteWatchHistoryItem(vc.getId(), new WatchHistorySyncManager.SyncCallback() {
                @Override
                public void onSuccess(String message) {
                    if (getActivity() != null) {
                        getActivity().runOnUiThread(() -> {
                            Toast.makeText(getContext(), "Đã xóa khỏi lịch sử", Toast.LENGTH_SHORT).show();
                            // Update UI instantly without full reload
                            if (mAdapter != null) {
                                mAdapter.remove(vc);
                                mAdapter.notifyArrayItemRangeChanged(0, mAdapter.size());
                            }
                        });
                    }
                }

                @Override
                public void onError(String error) {
                    if (getActivity() != null) {
                        getActivity().runOnUiThread(() -> {
                            Toast.makeText(getContext(), "Lỗi khi xóa: " + error, Toast.LENGTH_SHORT).show();
                        });
                    }
                }
            });
        }
    }`;

content = content.replace(regexDelete, newDelete);

let methodsToAdd = `
    private int dp(int dp) {
        if (getContext() == null) return dp;
        return Math.round(dp * getResources().getDisplayMetrics().density);
    }

    private android.app.Dialog buildCustomListDialog(
            String title, String[] items, int checkedIndex,
            Runnable[] actions, Runnable onCancel) {

        android.app.Dialog dialog = new android.app.Dialog(requireContext());
        dialog.requestWindowFeature(android.view.Window.FEATURE_NO_TITLE);
        dialog.setCancelable(true);
        if (onCancel != null) {
            dialog.setOnCancelListener(d -> onCancel.run());
        }

        android.widget.LinearLayout root = new android.widget.LinearLayout(requireContext());
        root.setOrientation(android.widget.LinearLayout.VERTICAL);
        root.setBackgroundColor(0xFF1E1E2E);
        root.setPadding(dp(20), dp(20), dp(20), dp(12));

        android.widget.TextView titleView = new android.widget.TextView(requireContext());
        titleView.setText(title);
        titleView.setTextColor(0xFFFFFFFF);
        titleView.setTextSize(android.util.TypedValue.COMPLEX_UNIT_SP, 18);
        titleView.setTypeface(android.graphics.Typeface.DEFAULT_BOLD);
        titleView.setPadding(dp(4), 0, dp(4), dp(10));
        root.addView(titleView);

        android.view.View divider = new android.view.View(requireContext());
        divider.setBackgroundColor(0x55FFFFFF);
        android.widget.LinearLayout.LayoutParams divLp =
                new android.widget.LinearLayout.LayoutParams(
                        android.widget.LinearLayout.LayoutParams.MATCH_PARENT, dp(1));
        divLp.bottomMargin = dp(6);
        root.addView(divider, divLp);

        android.widget.ScrollView sv = new android.widget.ScrollView(requireContext());
        sv.setVerticalScrollBarEnabled(false);
        android.widget.LinearLayout ll = new android.widget.LinearLayout(requireContext());
        ll.setOrientation(android.widget.LinearLayout.VERTICAL);

        for (int i = 0; i < items.length; i++) {
            final int idx = i;
            boolean isSelected = (checkedIndex >= 0 && i == checkedIndex);

            android.widget.TextView tv = new android.widget.TextView(requireContext());
            tv.setText(isSelected ? "✓  " + items[i] : "     " + items[i]);
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

            android.view.View sep = new android.view.View(requireContext());
            sep.setBackgroundColor(0x22FFFFFF);
            ll.addView(sep, new android.widget.LinearLayout.LayoutParams(
                    android.widget.LinearLayout.LayoutParams.MATCH_PARENT, 1));
        }
        sv.addView(ll);

        android.widget.LinearLayout.LayoutParams svLp =
                new android.widget.LinearLayout.LayoutParams(
                        android.widget.LinearLayout.LayoutParams.MATCH_PARENT,
                        android.view.ViewGroup.LayoutParams.WRAP_CONTENT);
        root.addView(sv, svLp);

        dialog.setContentView(root);
        android.view.Window w = dialog.getWindow();
        if (w != null) {
            w.setLayout(dp(480), android.view.WindowManager.LayoutParams.WRAP_CONTENT);
            w.setGravity(android.view.Gravity.CENTER);
            w.setBackgroundDrawableResource(android.R.color.transparent);
        }
        return dialog;
    }
}
`;

content = content.replace(/}\s*$/m, methodsToAdd);

fs.writeFileSync(fragmentPath, content, 'utf8');
console.log("SUCCESS FORMATTING");