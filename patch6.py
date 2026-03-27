import re

with open('app/src/main/java/com/files/codes/view/PlayerActivity.java', 'r', encoding='utf-8') as f:
    text = f.read()

text = text.replace('tiếng.\\n\\nBạn', 'tiếng.\\n\\nBạn')
text = text.replace('tiếng.\\n\\nBạn'.replace('\\n', '\n'), 'tiếng.\\n\\nBạn')
text = text.replace('thay thế.\\n\\nBạn'.replace('\\n', '\n'), 'thay thế.\\n\\nBạn')
text = text.replace('hiển thị.\\n\\nBạn'.replace('\\n', '\n'), 'hiển thị.\\n\\nBạn')

with open('app/src/main/java/com/files/codes/view/PlayerActivity.java', 'w', encoding='utf-8') as f:
    f.write(text)
print("Done fix newlines.")
