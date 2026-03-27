import re

with open('app/src/main/java/com/files/codes/view/PlayerActivity.java', 'r', encoding='utf-8') as f:
    text = f.read()

# 1. showPlayerInstallDialog
p1 = r'    private void showPlayerInstallDialog.*?dialog\.show\(\);\s*\}'
s1 = '''    private void showPlayerInstallDialog(final String playerName, final String packageName) {
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
    }'''
text = re.sub(p1, s1, text, flags=re.DOTALL)

# 2. showUrlIssueDialog
p2 = r'    private void showUrlIssueDialog.*?dialog\.show\(\);\s*\}'
s2 = '''    private void showUrlIssueDialog(String videoUrl) {
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
    }'''
text = re.sub(p2, s2, text, flags=re.DOTALL)

# 3. showAspectRatioDialog
p3 = r'        AlertDialog\.Builder builder = new AlertDialog\.Builder\(this\);\s*builder\.setTitle\("Chọn tỷ lệ màn hình"\);\s*builder\.setItems\(aspectRatioOptions, \(dialog, which\) -> \{.*?\n        \}\);\s*builder\.show\(\);'
s3 = '''        Runnable[] actions = new Runnable[aspectRatioOptions.length];
        for (int i = 0; i < aspectRatioOptions.length; i++) {
            final int index = i;
            actions[i] = () -> {
                currentAspectRatio = aspectRatios[index];
                if (playerView != null) {
                    playerView.setResizeMode(currentAspectRatio);
                }
            };
        }
        
        // Find current checked index
        int checkedIndex = -1;
        for (int i = 0; i < aspectRatios.length; i++) {
            if (aspectRatios[i] == currentAspectRatio) {
                checkedIndex = i;
                break;
            }
        }
        
        buildCustomListDialog("Chọn tỷ lệ màn hình", aspectRatioOptions, checkedIndex, actions, null).show();'''
text = re.sub(p3, s3, text, flags=re.DOTALL)


# 4. showFormatErrorDialog
p4_search = r'    private void showFormatErrorDialog\(\) \{\s*new android\.app\.AlertDialog\.Builder\(this\).*?\.show\(\);\s*\}'
s4_repl = '''    private void showFormatErrorDialog() {
        buildCustomAlertDialog(
            "⚠️ Định dạng không hỗ trợ",
            "Video này có định dạng không được hỗ trợ. Bạn có muốn thử chất lượng khác không?",
            "Thử Chất lượng khác", () -> {
                showServerDialog();
            },
            "Bỏ qua", null,
            null, null
        ).show();
    }'''
text = re.sub(p4_search, s4_repl, text, flags=re.DOTALL)

# 5. showErrorDialog
p5_search = r'    private void showErrorDialog\(String title, String message, boolean finishOnClose\) \{\s*runOnUiThread\(new Runnable\(\) \{\s*@Override\s*public void run\(\) \{\s*new android\.app\.AlertDialog\.Builder\(PlayerActivity\.this\).*?\.show\(\);\s*\}\s*\}\);\s*\}'
s5_repl = '''    private void showErrorDialog(String title, String message, boolean finishOnClose) {
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
    }'''
text = re.sub(p5_search, s5_repl, text, flags=re.DOTALL)

# 6. Audio Notifications
p6_search = r'    private void showAudioDisabledNotification\(\) \{\s*new android\.app\.AlertDialog\.Builder\(this\).*?\.show\(\);\s*\}'
s6_repl = '''    private void showAudioDisabledNotification() {
        buildCustomAlertDialog(
            "🔇 Âm thanh đã bị tắt",
            "Định dạng âm thanh EAC3 không được hỗ trợ trên thiết bị này. Video sẽ phát không có tiếng.\\n\\nBạn có muốn tiếp tục xem không?",
            "Tiếp tục", null,
            "Thử Server khác", () -> showServerDialog(),
            "Thoát", () -> finish()
        ).show();
    }'''
text = re.sub(p6_search, s6_repl, text, flags=re.DOTALL)

with open('app/src/main/java/com/files/codes/view/PlayerActivity.java', 'w', encoding='utf-8') as f:
    f.write(text)

print("Done alerts.")
