# HƯỚNG DẪN CẢI THIỆN TÌM KIẾM TIẾNG VIỆT

## 📝 Tổng quan
Giải pháp này thêm 3 cột mới vào bảng `videos` để tách rõ ràng:
- `title_vietnamese`: Tên tiếng Việt có dấu (ví dụ: "Sự Hoán Đổi Kỳ Diệu (2002)")
- `title_vietnamese_no_accent`: Tên tiếng Việt không dấu (ví dụ: "su hoan doi ky dieu (2002)")  
- `title_original`: Tên gốc tiếng Anh (ví dụ: "The Hot Chick")

## 🔧 Các bước triển khai

### Bước 1: Backup database (BẮT BUỘC)
**Trên phpMyAdmin:**
1. Vào phpMyAdmin → Chọn database `sql_api_phim4k_l`
2. Click tab "Export" 
3. Chọn "Quick" hoặc "Custom" 
4. Format: SQL
5. Click "Go" để download backup file

**Hoặc qua command line:**
```bash
mysqldump -u username -p sql_api_phim4k_l > backup_$(date +%Y%m%d).sql
```

### Bước 2: Chạy script SQL tạo cột
**Trên phpMyAdmin:**
1. Vào tab "SQL"
2. Copy toàn bộ nội dung file `deploy_step1_database.sql`
3. Paste vào và click "Go"
4. Kiểm tra message "Query executed successfully"

### Bước 3: Update dữ liệu có sẵn
**Cách 1: Qua web browser**
1. Sửa thông tin database trong file `deploy_step2_update_data.php`
2. Upload file lên server 
3. Truy cập: `http://yoursite.com/deploy_step2_update_data.php`
4. Chờ script chạy xong

**Cách 2: Command line**
```bash
cd /path/to/application  
php deploy_step2_update_data.php
```

### Bước 4: Test tìm kiếm
```sql
-- Test query
SELECT videos_id, title, title_vietnamese, title_search 
FROM videos 
WHERE title_search LIKE '%su hoan doi%' 
   OR title_vietnamese LIKE '%Sự Hoán Đổi%'
LIMIT 10;
```

## 📊 Ví dụ dữ liệu

### Trước khi update:
```
title: "Sự Hoán Đổi Kỳ Diệu (2002) The Hot Chick"
title_vietnamese: NULL
title_vietnamese_no_accent: NULL
title_original: NULL
```

### Sau khi update:
```
title: "Sự Hoán Đổi Kỳ Diệu (2002) The Hot Chick"  
title_vietnamese: "Sự Hoán Đổi Kỳ Diệu (2002)"
title_vietnamese_no_accent: "su hoan doi ky dieu (2002)"
title_original: "The Hot Chick"
```

## 🔍 Cải thiện tìm kiếm

### API Test
```
GET /api/v130/search?q=su hoan doi&type=movie           # Tìm không dấu
GET /api/v130/search?q=Sự Hoán Đổi&type=movie         # Tìm có dấu  
GET /api/v130/search?q=the hot chick&type=movie        # Tìm tên gốc
GET /api/v130/search?q=spider man&type=movie           # Tìm tên gốc
```

### Kết quả mong đợi:
- ✅ "su hoan doi" → Tìm thấy "Sự Hoán Đổi Kỳ Diệu (2002)"
- ✅ "Sự Hoán Đổi" → Tìm thấy "Sự Hoán Đổi Kỳ Diệu (2002)"  
- ✅ "the hot chick" → Tìm thấy "Sự Hoán Đổi Kỳ Diệu (2002)"
- ✅ "spider man" → Tìm thấy "Người Nhện: Không Còn Nhà (2021)"

## ⚡ Tối ưu performance

### Index được tạo:
```sql
KEY `idx_title_search` (`title_search`)
FULLTEXT KEY `idx_title_fulltext` (`title`, `title_search`, `title_vietnamese`)
```

### Trigger tự động update:
- Khi INSERT video mới → tự động tạo title_search
- Khi UPDATE title → tự động cập nhật title_search

## 🔄 Tích hợp với code hiện tại

### Model changes (Api_v130_model.php):
```php
// Thay thế function get_movie_search_result() 
// bằng enhanced_movie_search() từ file enhanced_search_functions.php
```

### Tự động update khi thêm video:
```php
// Sau khi insert/update video
$this->api_v130_model->update_video_search_fields($video_id, $title);
```

## 📈 Kết quả mong đợi

### Trước:
- Tìm "su hoan doi" → Không có kết quả
- Tìm "hoan doi" → Không có kết quả

### Sau:  
- Tìm "su hoan doi" → ✅ Tìm thấy "Sự Hoán Đổi Kỳ Diệu"
- Tìm "hoan doi" → ✅ Tìm thấy "Sự Hoán Đổi Kỳ Diệu"
- Tìm "ky dieu" → ✅ Tìm thấy "Sự Hoán Đổi Kỳ Diệu" 
- Tìm "Sự Hoán" → ✅ Tìm thấy "Sự Hoán Đổi Kỳ Diệu"

## 🚨 Lưu ý quan trọng

1. **Backup trước khi chạy**: Luôn backup database trước
2. **Test trên staging**: Test đầy đủ trước khi deploy production
3. **Monitor performance**: Theo dõi performance sau khi deploy
4. **Update existing data**: Chạy script update cho dữ liệu có sẵn

## 🔧 Troubleshooting

### Lỗi "Column already exists":
```sql
-- Check column exists
SHOW COLUMNS FROM videos LIKE 'title_search';
```

### Performance slow:
```sql
-- Check indexes
SHOW INDEX FROM videos;
-- Analyze table
ANALYZE TABLE videos;
```

### Search không hoạt động:
```php
// Check helper loaded
$this->load->helper('custom');

// Test function
echo remove_vietnamese_accents('Sự Hoán Đổi Kỳ Diệu');
// Output: su hoan doi ky dieu
```