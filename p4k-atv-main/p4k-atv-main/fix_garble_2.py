import re
import codecs

filepath = 'app/src/main/java/com/files/codes/view/PlayerActivity.java'

with codecs.open(filepath, 'r', 'utf-8') as f:
    text = f.read()

mappings = {
    "Đã khÃ´i phá»¥c": "Đã khôi phục",
    "Thá»­ lại": "Thử lại",
    "Tiáº¿p tá»¥c": "Tiếp tục",
    "Thá»­ chất lượng thấp": "Thử chất lượng thấp",
    "Thá»­ External Player": "Thử External Player",
    "Đã chá»\x8dn": "Đã chọn",
    "má»Ÿ": "mở",
    "Tua tá»›i": "Tua tới",
    "â—€â–¶": "◀▶",
    "Ä‘á»ƒ": "để",
    "âš\xa0ï¸\x8f": "⚠️",
    "âœ“": "✓"
}

for k, v in mappings.items():
    text = text.replace(k, v)

with codecs.open(filepath, 'w', 'utf-8') as f:
    f.write(text)

print("Done fixing remaining texts!")
