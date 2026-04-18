import re

with open('app/src/main/java/com/files/codes/view/PlayerActivity.java', 'r', encoding='utf-8') as f:
    text = f.read()

# Replace the entire showNextEpisodeDialog function perfectly
p_next = r'    private void showNextEpisodeDialog\(\) \{.*?\n    \}\n\n'
s_next = '''    private void showNextEpisodeDialog() {
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
            new Handler().postDelayed(() -> {
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

'''

text = re.sub(p_next, s_next, text, flags=re.DOTALL)

with open('app/src/main/java/com/files/codes/view/PlayerActivity.java', 'w', encoding='utf-8') as f:
    f.write(text)

print("Done complete replace.")
