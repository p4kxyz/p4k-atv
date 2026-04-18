import re

with open('app/src/main/java/com/files/codes/view/PlayerActivity.java', 'r', encoding='utf-8') as f:
    text = f.read()

p_epi = r'        android\.app\.AlertDialog\.Builder builder = new android\.app\.AlertDialog\.Builder\(this,\s*android\.R\.style\.Theme_Material_Dialog_Alert\);\s*builder\.setTitle\("🎬 Tập đã kết thúc"\);.*?dialog\.show\(\);'
s_epi = '''        buildCustomAlertDialog(
            "🎬 Tập đã kết thúc",
            "Bạn có muốn tiếp tục xem tập tiếp theo không?\\n\\n" + (nextEpisodeTitle != null ? "📺 " + nextEpisodeTitle : ""),
            "Phát ngay", () -> {
                Log.d(TAG, "User clicked Next Episode in dialog");
                if (director != null && !director.isEmpty()) {
                    watchHistoryData();
                } else if (userId != null && !userId.equals("")) {
                    saveWatchHistoryWithData(player.getCurrentPosition(), player.getDuration(), true);
                }
                playNextEpisodeInternal();
            },
            "Thoát", () -> {
                finish();
            },
            null, null
        ).show();'''
text = re.sub(p_epi, s_epi, text, flags=re.DOTALL)

with open('app/src/main/java/com/files/codes/view/PlayerActivity.java', 'w', encoding='utf-8') as f:
    f.write(text)

print("Done episode ended patch.")
