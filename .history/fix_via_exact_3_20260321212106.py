import sys

def replace_method(source_code, method_declaration, new_method_code):
    start_idx = source_code.find(method_declaration)
    if start_idx == -1:
        print(f"Could not find method declaration: {method_declaration[:50]}...")
        return source_code
        
    brace_count = 0
    in_method = False
    end_idx = -1
    
    for i in range(start_idx, len(source_code)):
        if source_code[i] == '{':
            brace_count += 1
            in_method = True
        elif source_code[i] == '}':
            brace_count -= 1
            
        if in_method and brace_count == 0:
            end_idx = i + 1
            break
            
    if end_idx == -1:
        print("Could not find end of method.")
        return source_code
        
    if end_idx < len(source_code) and source_code[end_idx] == '\n':
        end_idx += 1
        
    return source_code[:start_idx] + new_method_code + source_code[end_idx:]

with open('app/src/main/java/com/files/codes/view/PlayerActivity.java', 'r', encoding='utf-8') as f:
    content = f.read()

helper_code = """
    // =================================================================================
    // CUSTOM DIALOG HELPERS (MATCHING HOME ACTIVITY QUICK SETTINGS STYLE)
    // =================================================================================
    
    private int dp(int dp) {
        return Math.round(dp * getResources().getDisplayMetrics().density);
    }

    private android.app.Dialog buildCustomListDialog(
            String title, String[] items, int checkedIndex,
            Runnable[] actions, Runnable onCancel) {
        android.app.Dialog dialog = new android.app.Dialog(this);
        dialog.requestWindowFeature(android.view.Window.FEATURE_NO_TITLE);
        dialog.setCancelable(true);
        if (onCancel != null) dialog.setOnCancelListener(d -> onCancel.run());

        android.widget.LinearLayout root = new android.widget.LinearLayout(this);
        root.setOrientation(android.widget.LinearLayout.VERTICAL);
        root.setBackgroundColor(0xFF1E1E2E);
        root.setPadding(dp(20), dp(20), dp(20), dp(12));

        android.widget.TextView titleView = new android.widget.TextView(this);
        titleView.setText(title);
        titleView.setTextColor(0xFFFFFFFF);
        titleView.setTextSize(android.util.TypedValue.COMPLEX_UNIT_SP, 18);
        titleView.setTypeface(android.graphics.Typeface.DEFAULT_BOLD);
        titleView.setPadding(dp(4), 0, dp(4), dp(10));
        root.addView(titleView);

        android.view.View divider = new android.view.View(this);
        divider.setBackgroundColor(0x55FFFFFF);
        android.widget.LinearLayout.LayoutParams divLp = 
                new android.widget.LinearLayout.LayoutParams(
                        android.widget.LinearLayout.LayoutParams.MATCH_PARENT, dp(1));
        divLp.bottomMargin = dp(6);
        root.addView(divider, divLp);

        android.widget.ScrollView sv = new android.widget.ScrollView(this);
        sv.setVerticalScrollBarEnabled(false);
        android.widget.LinearLayout ll = new android.widget.LinearLayout(this);
        ll.setOrientation(android.widget.LinearLayout.VERTICAL);

        for (int i = 0; i < items.length; i++) {
            final int idx = i;
            boolean isSelected = (checkedIndex >= 0 && i == checkedIndex);
            android.widget.TextView tv = new android.widget.TextView(this);
            tv.setText(isSelected ? "-  " + items[i] : "   " + items[i]);
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
                if (actions != null && idx < actions.length && actions[idx] != null)
                    actions[idx].run();
            });
            ll.addView(tv);
        }
        sv.addView(ll);
        
        android.widget.LinearLayout.LayoutParams svLp = 
            new android.widget.LinearLayout.LayoutParams(
                android.widget.LinearLayout.LayoutParams.MATCH_PARENT, 
                android.widget.LinearLayout.LayoutParams.WRAP_CONTENT);
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

    private android.app.Dialog buildCustomAlertDialog(
            String title, String message, 
            String posText, Runnable onPositive, 
            String negText, Runnable onNegative,
            String neutralText, Runnable onNeutral) {
        android.app.Dialog dialog = new android.app.Dialog(this);
        dialog.requestWindowFeature(android.view.Window.FEATURE_NO_TITLE);
        dialog.setCancelable(true);

        android.widget.LinearLayout root = new android.widget.LinearLayout(this);
        root.setOrientation(android.widget.LinearLayout.VERTICAL);
        root.setBackgroundColor(0xFF1E1E2E);
        root.setPadding(dp(24), dp(24), dp(24), dp(16));

        if (title != null && !title.isEmpty()) {
            android.widget.TextView titleView = new android.widget.TextView(this);
            titleView.setText(title);
            titleView.setTextColor(0xFFFFFFFF);
            titleView.setTextSize(android.util.TypedValue.COMPLEX_UNIT_SP, 18);
            titleView.setTypeface(android.graphics.Typeface.DEFAULT_BOLD);
            titleView.setPadding(0, 0, 0, dp(12));
            root.addView(titleView);
        }

        if (message != null && !message.isEmpty()) {
            android.widget.TextView msgView = new android.widget.TextView(this);
            msgView.setText(message);
            msgView.setTextColor(0xFFE0E0E0);
            msgView.setTextSize(android.util.TypedValue.COMPLEX_UNIT_SP, 15);
            msgView.setPadding(0, 0, 0, dp(24));
            root.addView(msgView);
        }

        android.widget.LinearLayout btnLayout = new android.widget.LinearLayout(this);
        btnLayout.setOrientation(android.widget.LinearLayout.HORIZONTAL);
        btnLayout.setGravity(android.view.Gravity.END);
        
        java.util.List<android.widget.TextView> btns = new java.util.ArrayList<>();
        
        if (neutralText != null) {
            android.widget.TextView btnNeu = new android.widget.TextView(this);
            btnNeu.setText(neutralText);
            btnNeu.setTextColor(0xFFB0BEC5);
            btnNeu.setPadding(dp(12), dp(10), dp(12), dp(10));
            btnNeu.setOnClickListener(v -> { dialog.dismiss(); if (onNeutral != null) onNeutral.run(); });
            btns.add(btnNeu);
        }
        
        if (negText != null) {
            android.widget.TextView btnNeg = new android.widget.TextView(this);
            btnNeg.setText(negText);
            btnNeg.setTextColor(0xFFB0BEC5);
            btnNeg.setPadding(dp(12), dp(10), dp(12), dp(10));
            btnNeg.setOnClickListener(v -> { dialog.dismiss(); if (onNegative != null) onNegative.run(); });
            btns.add(btnNeg);
        }

        if (posText != null) {
            android.widget.TextView btnPos = new android.widget.TextView(this);
            btnPos.setText(posText);
            btnPos.setTextColor(0xFF64B5F6);
            btnPos.setTypeface(android.graphics.Typeface.DEFAULT_BOLD);
            btnPos.setPadding(dp(12), dp(10), dp(12), dp(10));
            btnPos.setOnClickListener(v -> { dialog.dismiss(); if (onPositive != null) onPositive.run(); });
            btns.add(btnPos);
        }
        
        for(android.widget.TextView btn : btns) {
            btn.setTextSize(android.util.TypedValue.COMPLEX_UNIT_SP, 14);
            btn.setFocusable(true);
            btn.setClickable(true);
            btnLayout.addView(btn);
            btn.setOnFocusChangeListener((v, has) -> {
                if(has) v.setBackgroundColor(0x3364B5F6);
                else v.setBackgroundColor(android.graphics.Color.TRANSPARENT);
            });
        }
        
        root.addView(btnLayout);

        dialog.setContentView(root);
        android.view.Window w = dialog.getWindow();
        if (w != null) {
            w.setLayout(dp(420), android.view.WindowManager.LayoutParams.WRAP_CONTENT);
            w.setGravity(android.view.Gravity.CENTER);
            w.setBackgroundDrawableResource(android.R.color.transparent);
        }
        return dialog;
    }
"""

if "private int dp(int dp)" not in content:
    last_brace_idx = content.rfind('}')
    content = content[:last_brace_idx] + helper_code + content[last_brace_idx:]

r1 = r'''    private void showPlayerInstallDialog(final String playerName, final String packageName) {
        buildCustomAlertDialog(
                getString(R.string.player_not_found),
                playerName + " chưa được cài đặt trên thiết bị của bạn. Bạn có muốn cài đặt không?",
                getString(R.string.install_player), () -> {
                    try {
                        Intent intent = new Intent(Intent.ACTION_VIEW);
                        intent.setData(Uri.parse("market://details?id=" + packageName));
                        startActivity(intent);
                    } catch (Exception e) {
                        Intent intent = new Intent(Intent.ACTION_VIEW);
                        intent.setData(Uri.parse("https://play.google.com/store/apps/details?id=" + packageName));
                        startActivity(intent);
                    }
                    finish();
                },
                "Hủy", () -> {
                    finish();
                },
                null, null
        ).show();
    }
'''
content = replace_method(content, "private void showPlayerInstallDialog(final String playerName, final String packageName)", r1)

r2 = r'''    private void showUrlIssueDialog(String videoUrl) {
        buildCustomAlertDialog(
                "Vấn đề URL Video",
                "URL video này có thể gây lỗi với trình phát ngoài. Bạn muốn:\n\n• Thử internal player\n• Hoặc thử external player khác?",
                "Internal Player", () -> {
                    Log.d(TAG, "User chose internal player for problematic URL");
                    fallbackToInternalPlayer();
                },
                "Thử External Player", () -> {
                    Log.d(TAG, "User chose to try external player anyway");
                    continueExternalPlayerLaunch();
                },
                "Hủy", () -> {
                    finish();
                }
        ).show();
    }
'''
content = replace_method(content, "private void showUrlIssueDialog(String videoUrl)", r2)

r4 = r'''    private void showFormatErrorDialog() {
        buildCustomAlertDialog(
            "Định dạng không hỗ trợ",
            "Video này có định dạng không được hỗ trợ. Bạn có muốn thử chất lượng khác không?",
            "Thử lại", () -> {
                recreatePlayer();
            },
            "Quay lại", () -> {
                finish();
            },
            null, null
        ).show();
    }
'''
content = replace_method(content, "private void showFormatErrorDialog()", r4)

r5_2 = r'''    private void showErrorDialog(String title, String message) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                buildCustomAlertDialog(
                    title, message,
                    "OK", null,
                    null, null, null, null
                ).show();
            }
        });
    }
'''
content = replace_method(content, "private void showErrorDialog(String title, String message)", r5_2)

r6_1 = r'''    private void showAudioDisabledNotification() {
        buildCustomAlertDialog(
            "Âm thanh đã bị tắt",
            "Định dạng âm thanh EAC3 không được hỗ trợ trên thiết bị này. Video sẽ phát không có tiếng.\n\nBạn có muốn tiếp tục xem không?",
            "Xem không tiếng", () -> {
                Log.d("PlayerActivity", "User chose to continue watching without audio");
            },
            "Quay lại", () -> finish(),
            null, null
        ).show();
    }
'''
content = replace_method(content, "private void showAudioDisabledNotification()", r6_1)

r6_2 = r'''    private void showAudioTrackSwitchedNotification() {
        buildCustomAlertDialog(
            "Đã chuyển đổi âm thanh",
            "Đã tự động chuyển từ EAC3 sang định dạng âm thanh tương thích khác. Video sẽ phát với âm thanh mới.",
            "OK", () -> {
                Log.d("PlayerActivity", "User acknowledged audio track switch");
            },
            null, null, null, null
        ).show();
    }
'''
content = replace_method(content, "private void showAudioTrackSwitchedNotification()", r6_2)

r6_3 = r'''    private void showAudioCodecErrorDialog() {
        buildCustomAlertDialog(
            "Lỗi âm thanh",
            "Định dạng âm thanh không được hỗ trợ trên thiết bị này. Video sẽ phát không có tiếng hoặc với âm thanh thay thế.\n\nBạn có muốn tiếp tục không?",
            "Tiếp tục", () -> {
                Log.d("PlayerActivity", "User chose to continue with audio codec issues");
            },
            "Quay lại", () -> finish(),
            null, null
        ).show();
    }
'''
content = replace_method(content, "private void showAudioCodecErrorDialog()", r6_3)

r6_4 = r'''    private void showVideoCodecErrorDialog() {
        buildCustomAlertDialog(
            "Lỗi video codec",
            "Định dạng video không được hỗ trợ đầy đủ trên thiết bị này. Video có thể phát chậm hoặc có lỗi hiển thị.\n\nBạn có muốn thử chất lượng thấp hơn không?",
            "Thử chất lượng thấp", () -> {
                Log.d("PlayerActivity", "User chose to try lower quality for video codec issues");
                if (trackSelector != null) {
                    trackSelector.setParameters(
                        trackSelector.buildUponParameters()
                            .setForceLowestBitrate(true)
                            .setMaxVideoSize(854, 480)
                            .setExceedVideoConstraintsIfNecessary(true)
                    );
                }
            },
            "Quay lại", () -> finish(),
            null, null
        ).show();
    }
'''
content = replace_method(content, "private void showVideoCodecErrorDialog()", r6_4)

r10 = r'''    private void showNextEpisodeDialog() {
        if (this.isFinishing() || this.isDestroyed()) {
            return;
        }

        try {
            String nextEpisodeTitle = getNextEpisodeTitle();
            String message = "Bạn có muốn tiếp tục xem tập tiếp theo không?";
            if (nextEpisodeTitle != null && !nextEpisodeTitle.isEmpty()) {
                message = "Bạn có muốn tiếp tục xem tập tiếp theo?\n\n- " + nextEpisodeTitle;
            }

            android.app.Dialog dialog = buildCustomAlertDialog(
                "Tập đã kết thúc",
                message,
                "Xem ngay", () -> {
                    Log.d(TAG, "User chose to watch next episode");
                    navigateToNextEpisode();
                },
                "Chọn tập khác", () -> {
                    Log.d(TAG, "User chose to select different episode");
                    finish();
                },
                null, null
            );
            dialog.setOnCancelListener(d -> {
                Log.d(TAG, "Next episode dialog cancelled");
                finish();
            });
            dialog.show();
            
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
'''
content = replace_method(content, "private void showNextEpisodeDialog()", r10)

with open('app/src/main/java/com/files/codes/view/PlayerActivity.java', 'w', encoding='utf-8') as f:
    f.write(content)
print("Methods successfully replaced v3!")
