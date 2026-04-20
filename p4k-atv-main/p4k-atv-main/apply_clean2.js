const fs = require('fs');

const path = 'app/src/main/java/com/files/codes/view/fragments/WatchHistoryPageFragment.java';
let content = fs.readFileSync(path, 'utf8');

const clickRegex = /\/\/ Click [^\n]*\n\s*private OnItemViewClickedListener getDefaultItemViewClickedListener\(\) \{[\s\S]*?private OnItemViewSelectedListener/;

const newClickAndHelpers = `// Click -> show custom dialog
    private OnItemViewClickedListener getDefaultItemViewClickedListener() {
        return (viewHolder, item, rowViewHolder, row) -> {
            if (!(item instanceof VideoContent)) return;
            VideoContent vc = (VideoContent) item;

            String[] options = {"\\u25B6  Xem ti\\u1EBFp", "\\uD83D\\uDDD1  X\\u00F3a kh\\u1ECFi l\\u1ECBch s\\u1EED"};
            
            Runnable[] actions = new Runnable[] {
                () -> playVideo(vc),
                () -> deleteHistoryItem(vc)
            };

            android.app.Dialog dialog = buildCustomListDialog(vc.getTitle(), options, -1, actions, null);
            dialog.show();
        };
    }

    private void playVideo(VideoContent vc) {
        long currentPosition = 0;
        String desc = vc.getDescription();
        if (desc != null && desc.startsWith("WATCH_HISTORY:")) {
            try {
                currentPosition = Long.parseLong(desc.substring("WATCH_HISTORY:".length()));
            } catch (NumberFormatException e) {
                currentPosition = 0;
            }
        }
        
        android.content.Intent intent = new android.content.Intent(getActivity(), com.files.codes.view.PlayerActivity.class);
        intent.putExtra("vType", vc.getType());
        intent.putExtra("id", vc.getId());
        if (currentPosition > 0) {
            intent.putExtra("resume_position", currentPosition);
        }
        intent.setFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK | android.content.Intent.FLAG_ACTIVITY_CLEAR_TOP);
        startActivity(intent);
    }

    private void deleteHistoryItem(VideoContent vc) {
        if (syncManager != null) {
            syncManager.deleteWatchHistoryItem(vc.getId(), new WatchHistorySyncManager.SyncCallback() {
                @Override
                public void onSuccess(String message) {
                    if (getActivity() != null) {
                        getActivity().runOnUiThread(() -> {
                            android.widget.Toast.makeText(getContext(), "Đã xóa khỏi lịch sử", android.widget.Toast.LENGTH_SHORT).show();
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
                            android.widget.Toast.makeText(getContext(), "Lỗi khi xóa: " + error, android.widget.Toast.LENGTH_SHORT).show();
                        });
                    }
                }
            });
        }
    }

    private OnItemViewSelectedListener`;

content = content.replace(clickRegex, newClickAndHelpers);

const customDialogCode = `
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
`;

let lastBracePos = content.lastIndexOf('}');
if (lastBracePos !== -1) {
    content = content.substring(0, lastBracePos) + customDialogCode + "\n}\n";
}

fs.writeFileSync(path, content, 'utf8');
console.log("CLEAN APPLY SUCCESS");
