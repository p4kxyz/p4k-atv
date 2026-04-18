# ✅ CHECKLIST TRIỂN KHAI

## 📋 Danh sách công việc

### Chuẩn bị
- [ ] ✅ Backup database hiện tại
- [ ] ✅ Có quyền truy cập phpMyAdmin 
- [ ] ✅ Có thông tin kết nối database (user/pass)
- [ ] ✅ Upload các file script lên server

### Triển khai Database  
- [ ] ✅ Chạy script `deploy_step1_database.sql` trên phpMyAdmin
- [ ] ✅ Kiểm tra 3 cột mới đã được tạo:
  - `title_vietnamese`
  - `title_vietnamese_no_accent`  
  - `title_original`
- [ ] ✅ Kiểm tra index đã được tạo

### Update dữ liệu
- [ ] ✅ Sửa thông tin database trong `deploy_step2_update_data.php`
- [ ] ✅ Chạy script update data (qua web hoặc command line)
- [ ] ✅ Kiểm tra log không có lỗi
- [ ] ✅ Xem một số record đầu để kiểm tra parsing

### Test kết quả
- [ ] ✅ Chạy `deploy_step3_test_result.php` 
- [ ] ✅ Kiểm tra bảng kết quả parsing
- [ ] ✅ Xem thống kê: % có tên Việt, tên gốc
- [ ] ✅ Test search với các query khác nhau

### Test API (sau khi update code)
- [ ] ✅ Test API: `/api/v130/search?q=su hoan doi&type=movie`
- [ ] ✅ Test API: `/api/v130/search?q=Sự Hoán Đổi&type=movie`  
- [ ] ✅ Test API: `/api/v130/search?q=spider man&type=movie`
- [ ] ✅ Kiểm tra response time không bị chậm

## 🚨 Troubleshooting

### Nếu gặp lỗi "Table doesn't exist":
```sql
SHOW TABLES LIKE 'videos';
```

### Nếu gặp lỗi "Column already exists":
```sql  
SHOW COLUMNS FROM videos;
```

### Nếu script update chạy chậm:
- Chạy từng batch nhỏ (LIMIT 100)
- Kiểm tra server load
- Tạo index trước khi update

### Nếu parsing sai:
- Kiểm tra encoding UTF-8
- Xem log chi tiết trong script update
- Test với một vài title cụ thể

## 📞 Hỗ trợ

Nếu gặp vấn đề:
1. Check log PHP error 
2. Check MySQL error log
3. Backup lại để restore nếu cần
4. Chạy từng bước một, không rush

## 🎯 Kết quả mong đợi

Sau khi hoàn thành:
- Database có 3 cột title mới
- Dữ liệu được parse chính xác
- Search hoạt động với cả tiếng Việt có/không dấu và tên gốc
- API response time không bị ảnh hưởng đáng kể