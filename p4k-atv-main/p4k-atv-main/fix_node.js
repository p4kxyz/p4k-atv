const fs = require('fs');
const file = 'app/src/main/java/com/files/codes/view/PlayerActivity.java';
let txt = fs.readFileSync(file, 'utf8');

const mappings = {
    "Cá»¡ chữ:": "Cỡ chữ:",
    "Kiá»ƒu chữ:": "Kiểu chữ:",
    "khÃ´i phá»¥c": "khôi phục",
    "KhÃ´i phá»¥c": "Khôi phục",
    "máº·c Ä‘á»‹nh": "mặc định",
    "CÃ¡i Ä‘áº·t": "Cài đặt",
    "phá»¥ Ä‘á»": "phụ đề",
    "Vá»‹ trÃ:": "Vị trí:",
    "Ná»n": "Nền",
    "Tá»‘c Ä‘á»™": "Tốc độ",
    "Dá»‹ch chuyá»ƒn": "Dịch chuyển",
    "Tráº¯ng": "Trắng",
    "Äỏ": "Đỏ",
    "VÃ ng": "Vàng",
    "Xanh lÃ¡": "Xanh lá",
    "Xanh dÆ°Æ¡ng": "Xanh dương",
    "Há»ng": "Hồng",
    "MÃ u viá»n": "Màu viền",
    "MÃ u chữ": "Màu chữ",
    "â—€": "◀",
    "â–¶": "▶",
    "Đã chá»n": "Đã chọn",
    "má»Ÿ": "mở",
    "Đã khôi phá»¥c cài đặt": "Đã khôi phục cài đặt",
    "Lỗi khi mở": "Lỗi khi mở",
    "Thá»­": "Thử",
    "Tiáº¿p tá»¥c": "Tiếp tục",
    "Tua tá»›i": "Tua tới",
    "Ä‘á»ƒ": "để",
    "Cá»‘": "Cố",
    "chuyá»ƒn": "chuyển",
    "Ã¢m": "âm",
    "dáº¡ng": "dạng",
    "sáº½": "sẽ",
    "má»›i": "mới",
    "ðŸ”„": "🔄",
    "ðŸ”‡": "🔇"
};

for (const [k, v] of Object.entries(mappings)) {
    txt = txt.split(k).join(v);
}

fs.writeFileSync(file, txt, 'utf8');
console.log('Fixed all strings efficiently from JS buffer directly!');
