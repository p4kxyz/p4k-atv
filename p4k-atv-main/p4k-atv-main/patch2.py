import re

with open('app/src/main/java/com/files/codes/view/PlayerActivity.java', 'r', encoding='utf-8') as f:
    text = f.read()

# showAudioTrackSwitchedNotification
p7 = r'    private void showAudioTrackSwitchedNotification\(\) \{\s*new android\.app\.AlertDialog\.Builder\(this\).*?\.show\(\);\s*\}'
s7 = '''    private void showAudioTrackSwitchedNotification() {
        buildCustomAlertDialog(
            "🔄 Đã chuyển đổi âm thanh",
            "Đã tự động chuyển từ EAC3 sang định dạng âm thanh tương thích khác. Video sẽ phát với âm thanh mới.",
            "OK", null, null, null, null, null
        ).show();
    }'''
text = re.sub(p7, s7, text, flags=re.DOTALL)

# showAudioCodecErrorDialog
p8 = r'    private void showAudioCodecErrorDialog\(\) \{\s*new android\.app\.AlertDialog\.Builder\(this\).*?\.show\(\);\s*\}'
s8 = '''    private void showAudioCodecErrorDialog() {
        buildCustomAlertDialog(
            "⚠️ Lỗi âm thanh",
            "Định dạng âm thanh không được hỗ trợ trên thiết bị này. Video sẽ phát không có tiếng hoặc với âm thanh thay thế.\\n\\nBạn có muốn tiếp tục không?",
            "Tiếp tục", null,
            "Thử Server khác", () -> showServerDialog(),
            null, null
        ).show();
    }'''
text = re.sub(p8, s8, text, flags=re.DOTALL)

# showVideoCodecErrorDialog
p9 = r'    private void showVideoCodecErrorDialog\(\) \{\s*new android\.app\.AlertDialog\.Builder\(this\).*?\.show\(\);\s*\}'
s9 = '''    private void showVideoCodecErrorDialog() {
        buildCustomAlertDialog(
            "⚠️ Lỗi video codec",
            "Định dạng video không được hỗ trợ đầy đủ trên thiết bị này. Video có thể phát chậm hoặc có lỗi hiển thị.\\n\\nBạn có muốn thử chất lượng thấp hơn không?",
            "Thử Server khác", () -> showServerDialog(),
            "Tiếp tục", null,
            null, null
        ).show();
    }'''
text = re.sub(p9, s9, text, flags=re.DOTALL)


with open('app/src/main/java/com/files/codes/view/PlayerActivity.java', 'w', encoding='utf-8') as f:
    f.write(text)
print("Done codec alerts.")
