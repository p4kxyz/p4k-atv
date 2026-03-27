with open('app/src/main/java/com/files/codes/view/PlayerActivity.java', 'r', encoding='utf-8') as f:
    text = f.read()

text = text.replace('khÃ´ng?\n\n\"', 'không?\\n\\n\"')
text = text.replace('khÃ´ng?\r\n\r\n\"', 'không?\\n\\n\"')
text = text.replace('khÃ´ng?\\r\\n\\r\\n\"', 'không?\\n\\n\"')
text = text.replace('khÃ´ng?\\n\\n\"', 'không?\\n\\n\"')
text = text.replace('khÃ¢Å’ng?\n\n\"', 'không?\\n\\n\"')

import re
text = re.sub(r'String msg = ".*?Bạn có muốn.*?\n\n" \+', 'String msg = "Bạn có muốn tiếp tục xem tập tiếp theo không?\\n\\n" +', text, flags=re.DOTALL)
text = re.sub(r'String msg = ".*?khÃ´ng\?\n\n" \+', 'String msg = "Bạn có muốn tiếp tục xem tập tiếp theo không?\\n\\n" +', text, flags=re.DOTALL)

with open('app/src/main/java/com/files/codes/view/PlayerActivity.java', 'w', encoding='utf-8') as f:
    f.write(text)
print('Done!')
