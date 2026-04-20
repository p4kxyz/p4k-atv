# Hướng Dẫn Sử Dụng ADB Search Command

## Cách sử dụng ADB để mở app và tìm kiếm

### 1. Command cơ bản (ĐÃ TEST THÀNH CÔNG):
```bash
adb shell am start -n com.oxootv.spagreen/com.files.codes.view.SearchActivity --es query "Avatar"
```

### 2. Các biến thể command có thể sử dụng:
```bash
# Sử dụng tham số "query" 
adb shell am start -n com.oxootv.spagreen/com.files.codes.view.SearchActivity --es query "Avengers"

# Sử dụng tham số "search_query" (backup)
adb shell am start -n com.oxootv.spagreen/com.files.codes.view.SearchActivity --es search_query "Spider-Man"

# Mở SearchActivity không có query (sẽ hiển thị màn hình search trống)
adb shell am start -n com.oxootv.spagreen/com.files.codes.view.SearchActivity
```

### 3. Ví dụ tìm kiếm các bộ phim phổ biến (ĐÃ TEST):
```bash
# Tìm Avatar (✅ Đã test - tìm thấy 10 kết quả)
adb shell am start -n com.oxootv.spagreen/com.files.codes.view.SearchActivity --es query "Avatar"

# Tìm Na Tra (✅ Đã test - tìm thấy 9 kết quả)
adb shell am start -n com.oxootv.spagreen/com.files.codes.view.SearchActivity --es query "Na Tra"

# Tìm phim tiếng Việt (✅ Đã test)
adb shell am start -n com.oxootv.spagreen/com.files.codes.view.SearchActivity --es query "trò chơi con mực"

# Tìm Doraemon (✅ Đã test)
adb shell am start -n com.oxootv.spagreen/com.files.codes.view.SearchActivity --es query "Doraemon"

# Tìm phim Marvel
adb shell am start -n com.oxootv.spagreen/com.files.codes.view.SearchActivity --es query "Iron Man"
```

### 4. Lưu ý khi sử dụng:
- Package name: `com.oxootv.spagreen` 
- Activity class: `com.files.codes.view.SearchActivity`
- Phải sử dụng full class name: `com.oxootv.spagreen/com.files.codes.view.SearchActivity`
- Từ khóa tìm kiếm nên đặt trong dấu ngoặc kép để tránh lỗi với khoảng trắng
- App sẽ tự động hiển thị kết quả tìm kiếm sau khi nhận query
- Nếu app chưa mở, command sẽ tự động khởi động app và chuyển đến màn hình tìm kiếm

### 5. Debug và kiểm tra logs:
```bash
# Xem logs để debug
adb logcat | grep "SearchActivity\|SearchFragment"

# Xem logs chỉ của app
adb logcat | grep "com.oxootv.spagreen"
```

### 6. Cách test trên Android TV:
1. Kết nối ADB với Android TV Box/Stick
2. Chạy command từ máy tính
3. App sẽ tự động mở và hiển thị kết quả tìm kiếm

## Cấu trúc kỹ thuật đã implement:

### AndroidManifest.xml:
- Thêm `android:exported="true"` cho SearchActivity
- Thêm intent-filter để có thể nhận external intents

### SearchActivity.java:
- Xử lý Intent trong onCreate() và onNewIntent()
- Hỗ trợ nhiều loại query parameter: "query", "search_query"
- Log chi tiết để debug

### SearchFragment.java:
- Method setSearchQuery() để nhận query từ Activity
- Tự động trigger search khi nhận query

Chúc bạn sử dụng thành công! 🎯