import re
import codecs

filepath = 'app/src/main/java/com/files/codes/view/PlayerActivity.java'

with codecs.open(filepath, 'r', 'utf-8') as f:
    text = f.read()

# Mappings of observed garbled utf-8 interpretations
mappings = [
    ("Cá»¡ chữ:", "Cỡ chữ:"),
    ("Kiá»ƒu chữ:", "Kiểu chữ:"),
    ("khÃ´i phá»¥c", "khôi phục"),
    ("KhÃ´i phá»¥c", "Khôi phục"),
    ("máº·c Ä‘á»‹nh", "mặc định"),
    ("CÃ¡i Ä‘áº·t", "Cài đặt"),
    ("phá»¥ Ä‘á»", "phụ đề"),
    ("Vá»‹ trÃ:", "Vị trí:"),
    ("Ná»n", "Nền"),
    ("Tá»‘c Ä‘á»™", "Tốc độ"),
    ("Dá»‹ch chuyá»ƒn", "Dịch chuyển"),
    ("Tráº¯ng", "Trắng"),
    ("Äỏ", "Đỏ"),
    ("VÃ ng", "Vàng"),
    ("Xanh lÃ¡", "Xanh lá"),
    ("Xanh dÆ°Æ¡ng", "Xanh dương"),
    ("Há»ng", "Hồng"),
    ("MÃ u viá»n", "Màu viền"),
    ("MÃ u chữ", "Màu chữ"),
    ("â—€", "◀"),
    ("â–¶", "▶"),
    ("Đã chá»n", "Đã chọn"),
    ("má»Ÿ", "mở"),
    ("Đã khôi phá»¥c cài đặt", "Đã khôi phục cài đặt"),
    ("Lỗi khi mở", "Lỗi khi mở"),
]

for k, v in mappings:
    text = text.replace(k, v)

with codecs.open(filepath, 'w', 'utf-8') as f:
    f.write(text)

print("Done python fix")
