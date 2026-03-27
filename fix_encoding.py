import os

replacements = {
    'Lá»—i: KhÃ´ng cÃ³ dá»¯ liá»‡u video': 'Lỗi: Không có dữ liệu video',
    'Lá»—i khi má»Ÿ trÃ¬nh phÃ¡t ngoÃ i: ': 'Lỗi khi mở trình phát ngoài: ',
    'KhÃ´ng thá»ƒ má»Ÿ Kodi trÃªn Google TV. HÃ£y thá»­ trÃ¬nh phÃ¡t khÃ¡c.': 'Không thể mở Kodi trên Google TV. Hãy thử trình phát khác.',
    'Just Player chÆ°a Ä‘Æ°á»£c cÃ i Ä‘áº·t hoáº·c khÃ´ng tÆ°Æ¡ng thÃ­ch': 'Just Player chưa được cài đặt hoặc không tương thích',
    ' chÆ°a Ä‘Æ°á»£c cÃ i Ä‘áº·t trÃªn thiáº¿t bá»‹ cá»§a báº¡n. Báº¡n cÃ³ muá»‘n cÃ i Ä‘áº·t khÃ´ng?': ' chưa được cài đặt trên thiết bị của bạn. Bạn có muốn cài đặt không?',
    'URL video nÃ y cÃ³ thá»ƒ gÃ¢y lá»—i vá»›i trÃ¬nh phÃ¡t ngoÃ i. Báº¡n muá»‘n:\\n\\nâ€¢ Thá»­ internal player\\nâ€¢ Hoáº·c thá»­ external player khÃ¡c?': 'URL video này có thể gây lỗi với trình phát ngoài. Bạn muốn:\\n\\n• Thử internal player\\n• Hoặc thử external player khác?',
    'Chuyá»ƒn sang trÃ¬nh phÃ¡t ná»™i bá»™': 'Chuyển sang trình phát nội bộ',
    'Tá»‘c Ä‘á»™ phÃ¡t: ': 'Tốc độ phát: ',
    'KhÃ´ng tÃ¬m tháº¥y phá»¥ Ä‘á»': 'Không tìm thấy phụ đề',
    'Ã‚m thanh: ': 'Âm thanh: ',
    'Lá»—i káº¿t ná»‘i máº¡ng': 'Lỗi kết nối mạng',
    'KhÃ´ng thá»ƒ káº¿t ná»‘i Ä‘áº¿n server. Vui lÃ²ng kiá»ƒm tra káº¿t ná»‘i internet.': 'Không thể kết nối đến server. Vui lòng kiểm tra kết nối internet.',
    'Lá»—i phÃ¡t video': 'Lỗi phát video',
    'KhÃ´ng thá»ƒ phÃ¡t video nÃ y. Vui lÃ²ng thá»­ láº¡i sau.': 'Không thể phát video này. Vui lòng thử lại sau.',
    'KhÃ´ng thá»ƒ tua trong ná»™i dung trá»±c tiáº¿p': 'Không thể tua trong nội dung trực tiếp',
    'Tua lÃ¹i -': 'Tua lùi -',
    'Chá»n phá»¥ Ä‘á» (': 'Chọn phụ đề (',
    ' cÃ³ sáºµn)': ' có sẵn)',
    'Ã‚m thanh ': 'Âm thanh ',
    'Chá»‰ cÃ³ má»™t luá»“ng Ã¢m thanh': 'Chỉ có một luồng âm thanh',
    'Lá»—i khi Ä‘á»•i Ã¢m thanh': 'Lỗi khi đổi âm thanh',
    'Chá»n Ã¢m thanh (': 'Chọn âm thanh (',
    'CÃ i Ä‘áº·t phá»¥ Ä‘á»': 'Cài đặt phụ đề',
    'Vá»‹ trÃ­:': 'Vị trí:',
    'LÃªn': 'Lên',
    'MÃ u chá»¯:': 'Màu chữ:',
    '"Tráº¯ng", "VÃ ng", "Äá»", "Xanh lÃ¡", "Xanh dÆ°Æ¡ng", "Cam", "Há»“ng", "Xanh lÆ¡"': '"Trắng", "Vàng", "Đỏ", "Xanh lá", "Xanh dương", "Cam", "Hồng", "Xanh lơ"',
    'MÃ u viá»n:': 'Màu viền:',
    '"Trong suá»‘t", "Äen", "Tráº¯ng", "Äá»", "Xanh dÆ°Æ¡ng", "VÃ ng"': '"Trong suốt", "Đen", "Trắng", "Đỏ", "Xanh dương", "Vàng"',
    'Tá»‘c Ä‘á»™ phÃ¡t:': 'Tốc độ phát:',
    'KhÃ´i phá»¥c máº·c Ä‘á»‹nh': 'Khôi phục mặc định',
    'Giá»¯a': 'Giữa',
    'LÃªn +': 'Lên +',
    'ÄÃ³ng': 'Đóng',
    'Vá»«a mÃ n hÃ¬nh': 'Vừa màn hình',
    'TrÃ n mÃ n hÃ¬nh': 'Tràn màn hình',
    'PhÃ³ng to': 'Phóng to',
    'Chá»n tá»· lá»‡ mÃ n hÃ¬nh': 'Chọn tỷ lệ màn hình',
    'KhÃ´ng tÃ¬m tháº¥y phá»¥ Ä‘á». Báº¡n cÃ³ thá»ƒ tÃ¹y chá»‰nh subtitle trong Settings.': 'Không tìm thấy phụ đề. Bạn có thể tùy chỉnh subtitle trong Settings.',
    'âš ï¸ Äá»‹nh dáº¡ng khÃ´ng há»— trá»£': '⚠️ Định dạng không hỗ trợ',
    'Video nÃ y cÃ³ Ä‘á»‹nh dáº¡ng khÃ´ng Ä‘Æ°á»£c há»— trá»£. Báº¡n cÃ³ muá»‘n thá»­ cháº¥t lÆ°á»£ng khÃ¡c khÃ´ng?': 'Video này có định dạng không được hỗ trợ. Bạn có muốn thử chất lượng khác không?',
    'Lá»—i khá»Ÿi táº¡o player': 'Lỗi khởi tạo player',
    'KhÃ´ng thá»ƒ khá»Ÿi táº¡o láº¡i player.': 'Không thể khởi tạo lại player.',
    'Äang chuyá»ƒn sang cháº¿ Ä‘á»™ giáº£i mÃ£ Ã¢m thanh báº±ng pháº§n má»m...': 'Đang chuyển sang chế độ giải mã âm thanh bằng phần mềm...',
    'ðŸ”‡ Ã‚m thanh Ä‘Ã£ bá»‹ táº¯t': '🔇 Âm thanh đã bị tắt',
    'Äá»‹nh dáº¡ng Ã¢m thanh EAC3 khÃ´ng Ä‘Æ°á»£c há»— trá»£ trÃªn thiáº¿t bá»‹ nÃ y. Video sáº½ phÃ¡t khÃ´ng cÃ³ tiáº¿ng.\\n\\nBáº¡n cÃ³ muá»‘n tiáº¿p tá»¥c xem khÃ´ng?': 'Định dạng âm thanh EAC3 không được hỗ trợ trên thiết bị này. Video sẽ phát không có tiếng.\\n\\nBạn có muốn tiếp tục xem không?',
    'Xem khÃ´ng tiáº¿ng': 'Xem không tiếng',
    'âš ï¸ Lá»—i Ã¢m thanh': '⚠️ Lỗi âm thanh',
    'Äá»‹nh dáº¡ng Ã¢m thanh khÃ´ng Ä‘Æ°á»£c há»— trá»£ trÃªn thiáº¿t bá»‹ nÃ y. Video sáº½ phÃ¡t khÃ´ng cÃ³ tiáº¿ng hoáº·c vá»›i Ã¢m thanh thay tháº¿.\\n\\nBáº¡n cÃ³ muá»‘n tiáº¿p tá»¥c khÃ´ng?': 'Định dạng âm thanh không được hỗ trợ trên thiết bị này. Video sẽ phát không có tiếng hoặc với âm thanh thay thế.\\n\\nBạn có muốn tiếp tục không?',
    'KhÃ´ng thá»ƒ khá»Ÿi táº¡o video renderer. Thá»­ chá»n cháº¥t lÆ°á»£ng khÃ¡c.': 'Không thể khởi tạo video renderer. Thử chọn chất lượng khác.',
    'Äá»‹nh dáº¡ng video khÃ´ng Ä‘Æ°á»£c há»— trá»£ Ä‘áº§y Ä‘á»§ trÃªn thiáº¿t bá»‹ nÃ y. Video cÃ³ thá»ƒ phÃ¡t cháº­m hoáº·c cÃ³ lá»—i hiá»ƒn thá»‹.\\n\\nBáº¡n cÃ³ muá»‘n thá»­ cháº¥t lÆ°á»£ng tháº¥p hÆ¡n khÃ´ng?': 'Định dạng video không được hỗ trợ đầy đủ trên thiết bị này. Video có thể phát chậm hoặc có lỗi hiển thị.\\n\\nBạn có muốn thử chất lượng thấp hơn không?',
    'ðŸŽ¬ Táº­p Ä‘Ã£ káº¿t thÃºc': '🎬 Tập đã kết thúc',
    'Báº¡n cÃ³ muá»‘n tiáº¿p tá»¥c xem táº­p tiáº¿p theo?\\n\\nðŸ“º ': 'Bạn có muốn tiếp tục xem tập tiếp theo?\\n\\n📺 ',
    'Báº¡n cÃ³ muá»‘n tiáº¿p tá»¥c xem táº­p tiáº¿p theo khÃ´ng?': 'Bạn có muốn tiếp tục xem tập tiếp theo không?',
    'âŒ ThoÃ¡t': '❌ Thoát',
    'ðŸ“‹ Chá»n táº­p khÃ¡c': '📋 Chọn tập khác',
    'LÃ¡Â»â€”i tÃƒÂ¬m kiÃ¡ÂºÂ¿m: ': 'Lỗi tìm kiếm: ',
    'LÃ¡Â»â€”i mÃ¡ÂºÂ¡ng: ': 'Lỗi mạng: ',
    'URL video khÃ´ng há»£p lá»‡': 'URL video không hợp lệ',
    'TÃªn Viá»‡t (2024) TÃªn Gá»‘c': 'Tên Việt (2024) Tên Gốc'
}

files_to_fix = [
    'app/src/main/java/com/files/codes/view/PlayerActivity.java',
    'app/src/main/java/com/files/codes/view/fragments/SearchFragment.java'
]

for file_path in files_to_fix:
    if os.path.exists(file_path):
        with open(file_path, 'r', encoding='utf-8') as f:
            content = f.read()
        
        for k, v in replacements.items():
            content = content.replace(k, v)
            
        with open(file_path, 'w', encoding='utf-8') as f:
            f.write(content)
        print('Fixed: ' + file_path)
