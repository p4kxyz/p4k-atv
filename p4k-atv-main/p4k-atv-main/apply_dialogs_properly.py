import re

def safe_replace(content):
    # Add Helper Code
    helper_code = \
"""    // =================================================================================
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
}"""
    
    last_brace_idx = content.rfind('}')
    content = content[:last_brace_idx] + helper_code + content[last_brace_idx+1:]

    # 1. showPlayerInstallDialog
    content = re.sub(
        r'    private void showPlayerInstallDialog\(final String playerName, final String packageName\) \{[\s\S]*?dialog\.show\(\);\s*\}',
        '''    private void showPlayerInstallDialog(final String playerName, final String packageName) {
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
    }''', content)

    # 2. showUrlIssueDialog
    content = re.sub(
        r'    private void showUrlIssueDialog\(String videoUrl\) \{[\s\S]*?dialog\.show\(\);\s*\}',
        '''    private void showUrlIssueDialog(String videoUrl) {
        buildCustomAlertDialog(
                "Vấn đề URL Video",
                "URL video này có thể gây lỗi với trình phát ngoài. Bạn muốn:\\n\\n• Thử internal player\\n• Hoặc thử external player khác?",
                "Internal Player", () -> {
                    Log.d(TAG, "🎯 User chose internal player for problematic URL");
                    fallbackToInternalPlayer();
                },
                "Thử External Player", () -> {
                    Log.d(TAG, "🎯 User chose to try external player anyway");
                    continueExternalPlayerLaunch();
                },
                "Hủy", () -> {
                    finish();
                }
        ).show();
    }''', content)

    # 3. Aspect ratio dialog
    content = re.sub(
        r'        AlertDialog\.Builder builder = new AlertDialog\.Builder\(this\);\s*builder\.setTitle\("Chọn tỷ lệ màn hình"\);\s*builder\.setItems\(aspectRatioOptions, \(dialog, which\) -> \{[\s\S]*?        \}\);\s*builder\.show\(\);',
        '''        Runnable[] actions = new Runnable[aspectRatioOptions.length];
        for (int i = 0; i < aspectRatioOptions.length; i++) {
            final int index = i;
            actions[i] = () -> {
                if (index == 0) {
                    exoPlayerView.setResizeMode(com.google.android.exoplayer2.ui.AspectRatioFrameLayout.RESIZE_MODE_FIT);
                    exoPlayerView.setUseArtwork(false);
                    player.setVideoScalingMode(com.google.android.exoplayer2.C.VIDEO_SCALING_MODE_SCALE_TO_FIT);
                } else {
                    exoPlayerView.setResizeMode(aspectRatioModes[index]);
                    if (index == 2) {
                        player.setVideoScalingMode(com.google.android.exoplayer2.C.VIDEO_SCALING_MODE_SCALE_TO_FIT_WITH_CROPPING);
                    } else {
                        player.setVideoScalingMode(com.google.android.exoplayer2.C.VIDEO_SCALING_MODE_SCALE_TO_FIT);
                    }
                }
                new ToastMsg(PlayerActivity.this).toastIconSuccess("Đã chuyển: " + aspectRatioOptions[index]);
            };
        }
        buildCustomListDialog("Chọn tỷ lệ màn hình", aspectRatioOptions, -1, actions, null).show();''', content)

    # 4. showFormatErrorDialog
    content = re.sub(
        r'    private void showFormatErrorDialog\(\) \{[\s\S]*?\.show\(\);\s*\}',
        '''    private void showFormatErrorDialog() {
        buildCustomAlertDialog(
            "⚠️ Định dạng không hỗ trợ",
            "Video này có định dạng không được hỗ trợ. Bạn có muốn thử chất lượng khác không?",
            "Thử Chất lượng khác", () -> {
                showServerDialog();
            },
            "Bỏ qua", null,
            null, null
        ).show();
    }''', content)

    # 5. showErrorDialog variants
    content = re.sub(
        r'    private void showErrorDialog\(String title, String message, boolean finishOnClose\) \{[\s\S]*?\}\s*\}\);\s*\}',
        '''    private void showErrorDialog(String title, String message, boolean finishOnClose) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                buildCustomAlertDialog(
                    title,
                    message,
                    "Đóng", () -> {
                        if (finishOnClose) {
                            finish();
                        }
                    },
                    null, null, null, null
                ).show();
            }
        });
    }''', content)

    content = re.sub(
        r'    private void showErrorDialog\(String title, String message\) \{[\s\S]*?\}\s*\}\);\s*\}',
        '''    private void showErrorDialog(String title, String message) {
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
    }''', content)

    # 6. Audio / Codec error dialogs
    content = re.sub(
        r'    private void showAudioDisabledNotification\(\) \{[\s\S]*?\.show\(\);\s*\}',
        '''    private void showAudioDisabledNotification() {
        buildCustomAlertDialog(
            "🔇 Âm thanh đã bị tắt",
            "Định dạng âm thanh EAC3 không được hỗ trợ trên thiết bị này. Video sẽ phát không có tiếng.\\n\\nBạn có muốn tiếp tục xem không?",
            "Tiếp tục", null,
            "Thử Server khác", () -> showServerDialog(),
            "Thoát", () -> finish()
        ).show();
    }''', content)

    content = re.sub(
        r'    private void showAudioTrackSwitchedNotification\(\) \{[\s\S]*?\.show\(\);\s*\}',
        '''    private void showAudioTrackSwitchedNotification() {
        buildCustomAlertDialog(
            "🔄 Đã chuyển đổi âm thanh",
            "Đã tự động chuyển từ EAC3 sang định dạng âm thanh tương thích khác. Video sẽ phát với âm thanh mới.",
            "OK", null, null, null, null, null
        ).show();
    }''', content)

    content = re.sub(
        r'    private void showAudioCodecErrorDialog\(\) \{[\s\S]*?\.show\(\);\s*\}',
        '''    private void showAudioCodecErrorDialog() {
        buildCustomAlertDialog(
            "⚠️ Lỗi âm thanh",
            "Định dạng âm thanh không được hỗ trợ trên thiết bị này. Video sẽ phát không có tiếng hoặc với âm thanh thay thế.\\n\\nBạn có muốn tiếp tục không?",
            "Tiếp tục", null,
            "Thử Server khác", () -> showServerDialog(),
            null, null
        ).show();
    }''', content)

    content = re.sub(
        r'    private void showVideoCodecErrorDialog\(\) \{[\s\S]*?\.show\(\);\s*\}',
        '''    private void showVideoCodecErrorDialog() {
        buildCustomAlertDialog(
            "⚠️ Lỗi video codec",
            "Định dạng video không được hỗ trợ đầy đủ trên thiết bị này. Video có thể phát chậm hoặc có lỗi hiển thị.\\n\\nBạn có muốn thử chất lượng thấp hơn không?",
            "Thử Server khác", () -> showServerDialog(),
            "Tiếp tục", null,
            null, null
        ).show();
    }''', content)

    # 7. Track Selection (Subtitles)
    content = re.sub(
        r'        AlertDialog\.Builder builder = new AlertDialog\.Builder\(this\);\s*builder\.setTitle\("Chọn phụ đề \(" \+ totalTracks \+ " có sẵn\)"\);\s*builder\.setItems\(labels\.toArray\(new String\[0\]\), \(dialog, which\) -> \{[\s\S]*?        \}\);\s*builder\.show\(\);',
        '''        Runnable[] actions = new Runnable[labels.size()];
        for (int i = 0; i < labels.size(); i++) {
            final int which = i;
            actions[i] = () -> {
                int[] pair = selectionPairs.get(which);
                long currentPosition = player.getCurrentPosition();
                boolean wasPlaying = player.getPlayWhenReady();
                try {
                    if (pair[0] == -1) {
                        trackSelector.setParameters(trackSelector.buildUponParameters().setRendererDisabled(renderer, true));
                    } else {
                        int group = pair[0];
                        int track = pair[1];
                        trackSelector.setParameters(
                                trackSelector.buildUponParameters()
                                        .setRendererDisabled(renderer, false)
                                        .clearSelectionOverrides(renderer)
                                        .setSelectionOverride(renderer, mappedTrackInfo.getTrackGroups(renderer),
                                                new com.google.android.exoplayer2.trackselection.DefaultTrackSelector.SelectionOverride(group, track))
                        );
                    }
                } catch (Exception e) {
                    Log.e("PlayerActivity", "Error setting subtitle track", e);
                }
            };
        }
        buildCustomListDialog("Chọn phụ đề (" + totalTracks + " có sẵn)", labels.toArray(new String[0]), -1, actions, null).show();''', content)

    # 8. Track Selection (Audio)
    content = re.sub(
        r'        AlertDialog\.Builder builder = new AlertDialog\.Builder\(this\);\s*builder\.setTitle\("Chọn âm thanh \(" \+ labels\.size\(\) \+ " có sẵn\)"\);\s*builder\.setItems\(labels\.toArray\(new String\[0\]\), \(dialog, which\) -> \{[\s\S]*?        \}\);\s*builder\.show\(\);',
        '''        Runnable[] actions = new Runnable[labels.size()];
        for (int i = 0; i < labels.size(); i++) {
            final int which = i;
            actions[i] = () -> {
                int[] pair = selectionPairs.get(which);
                int rendererIndex = pair[0];
                int groupIndex = pair[1];
                int trackIndex = pair[2];
                long currentPosition = player.getCurrentPosition();
                boolean wasPlaying = player.getPlayWhenReady();
                try {
                    com.google.android.exoplayer2.trackselection.DefaultTrackSelector.Parameters.Builder parametersBuilder = trackSelector.buildUponParameters();
                    for (int j = 0; j < mappedTrackInfo.getRendererCount(); j++) {
                        if (mappedTrackInfo.getRendererType(j) == com.google.android.exoplayer2.C.TRACK_TYPE_AUDIO) {
                            parametersBuilder.clearSelectionOverrides(j).setRendererDisabled(j, true);
                        }
                    }
                    parametersBuilder.setRendererDisabled(rendererIndex, false)
                            .setSelectionOverride(rendererIndex,
                                    mappedTrackInfo.getTrackGroups(rendererIndex),
                                    new com.google.android.exoplayer2.trackselection.DefaultTrackSelector.SelectionOverride(groupIndex, trackIndex));
                    trackSelector.setParameters(parametersBuilder);
                } catch (Exception e) {
                    Log.e("PlayerActivity", "Error setting audio track", e);
                }
            };
        }
        buildCustomListDialog("Chọn âm thanh (" + labels.size() + " có sẵn)", labels.toArray(new String[0]), -1, actions, null).show();''', content)

    # 9. Subtitles off layout (the small option)
    content = re.sub(
        r'                    AlertDialog\.Builder builder = new AlertDialog\.Builder\(this\);\s*builder\.setTitle\("Subtitles"\);\s*String\[\] options = \{"On", "Off"\};\s*builder\.setItems\(options, \(dialog, which\) -> \{[\s\S]*?                    \}\);\s*builder\.show\(\);',
        '''                    Runnable[] actions = new Runnable[2];
                    actions[0] = () -> {
                        com.google.android.exoplayer2.trackselection.MappingTrackSelector.MappedTrackInfo info = trackSelector.getCurrentMappedTrackInfo();
                        if (info != null) {
                            for (int i = 0; i < info.getRendererCount(); i++) {
                                if (info.getRendererType(i) == com.google.android.exoplayer2.C.TRACK_TYPE_TEXT) {
                                    trackSelector.setParameters(trackSelector.buildUponParameters().setRendererDisabled(i, false));
                                }
                            }
                        }
                    };
                    actions[1] = () -> {
                        com.google.android.exoplayer2.trackselection.MappingTrackSelector.MappedTrackInfo info = trackSelector.getCurrentMappedTrackInfo();
                        if (info != null) {
                            for (int i = 0; i < info.getRendererCount(); i++) {
                                if (info.getRendererType(i) == com.google.android.exoplayer2.C.TRACK_TYPE_TEXT) {
                                    trackSelector.setParameters(trackSelector.buildUponParameters().setRendererDisabled(i, true));
                                }
                            }
                        }
                    };
                    buildCustomListDialog("Subtitles", new String[]{"On", "Off"}, -1, actions, null).show();''', content)

    # 10. showNextEpisodeDialog
    content = re.sub(
        r'    private void showNextEpisodeDialog\(\) \{[\s\S]*?\n    \}\n',
        '''    private void showNextEpisodeDialog() {
        if (this.isFinishing() || this.isDestroyed()) {
            return; // Don't show dialog if activity is finishing
        }

        try {
            String nextEpisodeTitle = getNextEpisodeTitle();
            String msg = "Bạn có muốn tiếp tục xem tập tiếp theo không?\\n\\n" + (nextEpisodeTitle != null ? "📺 " + nextEpisodeTitle : "");

            android.app.Dialog dialog = buildCustomAlertDialog(
                "🎬 Tập đã kết thúc",
                msg,
                "Phát ngay", () -> {
                    Log.d(TAG, "User clicked Next Episode in dialog");
                    if (director != null && !director.isEmpty()) {
                        watchHistoryData();
                    } else if (userId != null && !userId.equals("")) {
                        saveWatchHistoryWithData(player != null ? player.getCurrentPosition() : 0, player != null ? player.getDuration() : 0, true);
                    }
                    playNextEpisodeInternal();
                },
                "Thoát", () -> {
                    finish();
                },
                null, null
            );
            dialog.show();
            
            // Auto-dismiss after 30 seconds if no action
            new android.os.Handler().postDelayed(() -> {
                if (dialog.isShowing()) {
                    Log.d(TAG, "Auto-dismissing next episode dialog after 30s");
                    dialog.dismiss();
                    finish();
                }
            }, 30000); 

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
''', content)
    
    return content

file_path = 'app/src/main/java/com/files/codes/view/PlayerActivity.java'
with open(file_path, 'r', encoding='utf-8') as f:
    content = f.read()

new_content = safe_replace(content)

with open(file_path, 'w', encoding='utf-8') as f:
    f.write(new_content)

print(f"Update applied to {file_path} with proper UTF-8 encoding.")
