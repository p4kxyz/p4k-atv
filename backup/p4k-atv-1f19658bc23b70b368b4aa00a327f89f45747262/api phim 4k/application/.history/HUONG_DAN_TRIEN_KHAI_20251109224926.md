# HƯỚNG DẪN CẢI THIỆN TÌM KIẾM TIẾNG VIỆT

## 📝 Tổng quan
Giải pháp này thêm 2 cột mới vào bảng `videos`:
- `title_search`: Title không dấu tiếng Việt để tìm kiếm
- `title_vietnamese`: Tên tiếng Việt thuần (trước dấu ngoặc năm)

## 🔧 Các bước triển khai

### Bước 1: Backup database
```bash
mysqldump -u username -p database_name > backup_$(date +%Y%m%d).sql
```

### Bước 2: Chạy script SQL update
```bash
mysql -u username -p database_name < update_title_search.sql
```

### Bước 3: Update dữ liệu có sẵn  
```bash
cd /path/to/application
php update_title_search_data.php
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
title_search: NULL
title_vietnamese: NULL
```

### Sau khi update:
```
title: "Sự Hoán Đổi Kỳ Diệu (2002) The Hot Chick"  
title_search: "su hoan doi ky dieu 2002 the hot chick"
title_vietnamese: "Sự Hoán Đổi Kỳ Diệu"
```

## 🔍 Cải thiện tìm kiếm

### API Test
```
GET /api/v130/search?q=su hoan doi&type=movie
GET /api/v130/search?q=hoan doi ky dieu&type=movie  
GET /api/v130/search?q=Sự Hoán Đổi&type=movie
```

### Kết quả mong đợi:
- ✅ "su hoan doi" → Tìm thấy "Sự Hoán Đổi Kỳ Diệu"
- ✅ "hoan doi" → Tìm thấy "Sự Hoán Đổi Kỳ Diệu"  
- ✅ "ky dieu" → Tìm thấy "Sự Hoán Đổi Kỳ Diệu"

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