# 🚀 HƯỚNG DẪN DEPLOY VIETNAMESE SEARCH LÊN API SERVER

## 📋 **DANH SÁCH FILES CẦN COPY/CẬP NHẬT:**

### 1. **helpers/custom_helper.php** ✅ ĐÃ CẬP NHẬT
**Thêm 2 functions mới vào cuối file:**
```php
// Vietnamese title parsing function
if (!function_exists('parseMovieTitle')) { ... }

// Vietnamese accent removal function  
if (!function_exists('removeVietnameseAccents')) { ... }
```

### 2. **models/Api_v130_model.php** ✅ ĐÃ CẬP NHẬT
**Cập nhật search method (around line 1800) để sử dụng 3 cột mới:**
- `title_vietnamese` (có dấu)
- `title_vietnamese_no_accent` (không dấu)  
- `title_original` (tên gốc English/Asian)

**Priority search order:**
1. Vietnamese title (exact with accents)
2. Vietnamese title (no accents) 
3. Original title
4. Legacy title field (fallback)

### 3. **models/Api_v100_model.php** ⏳ CẦN CẬP NHẬT
**Tương tự như v130, cập nhật search methods để sử dụng 3 cột mới**

---

## 🗄️ **DATABASE DEPLOYMENT:**

### 4. **deploy_step1_database.sql** 
**Chạy trên phpMyAdmin để tạo 3 cột mới:**
```sql
-- Thêm 3 cột mới vào bảng videos
ALTER TABLE `videos` ADD COLUMN `title_vietnamese` VARCHAR(500) NULL DEFAULT NULL COMMENT 'Tên tiếng Việt có dấu' AFTER `title`;
ALTER TABLE `videos` ADD COLUMN `title_vietnamese_no_accent` VARCHAR(500) NULL DEFAULT NULL COMMENT 'Tên tiếng Việt không dấu' AFTER `title_vietnamese`;  
ALTER TABLE `videos` ADD COLUMN `title_original` VARCHAR(500) NULL DEFAULT NULL COMMENT 'Tên gốc (English/Asian)' AFTER `title_vietnamese_no_accent`;

-- Tạo indexes cho search performance
CREATE INDEX idx_title_vietnamese ON videos(title_vietnamese);
CREATE INDEX idx_title_vietnamese_no_accent ON videos(title_vietnamese_no_accent);
CREATE INDEX idx_title_original ON videos(title_original);
```

### 5. **deploy_step2_update_data_advanced.php**
**Chạy để populate dữ liệu cho 20k+ records:**
- Batch processing 100 records/lần
- Real-time progress bar
- Pause/Resume capability
- Sample display

---

## 🧪 **TEST FILES (Optional):**

### 6. **test_parsing_functions.php**
**File test để kiểm tra parsing logic:**
```php
// Test cases:
'Super 8: Quái Vật Vũ Trụ (2011) Super 8'
'Dịch Vụ Giao Hàng Kiki (1989) 魔女の宅急便'  
'Sự Hoán Đổi Kỳ Diệu (2002) The Hot Chick'
```

### 7. **deploy_step3_test_result.php** 
**File test search results sau khi deploy:**
- Test Vietnamese with accents
- Test Vietnamese without accents  
- Test mixed language titles

---

## ⚡ **THỨ TỰ DEPLOYMENT:**

1. **Backup database** trước khi thay đổi
2. **Copy files** lên server API:
   - `helpers/custom_helper.php` 
   - `models/Api_v130_model.php`
   - `models/Api_v100_model.php` (sau khi update)
3. **Chạy database migration:**
   - `deploy_step1_database.sql`
4. **Populate data:**
   - `deploy_step2_update_data_advanced.php`
5. **Test API:**
   - Test search endpoints
   - Verify Vietnamese search works

---

## 🔍 **EXPECTED RESULTS:**

| **Search Query** | **Before** | **After** |
|-----------------|------------|-----------|
| `quái vật vũ trụ` | ❌ No results | ✅ "Super 8: Quái Vật Vũ Trụ" |
| `quai vat vu tru` | ❌ No results | ✅ "Super 8: Quái Vật Vũ Trụ" |  
| `super 8` | ✅ Works | ✅ Better results |
| `魔女の宅急便` | ❌ No results | ✅ "Dịch Vụ Giao Hàng Kiki" |

---

## ⚠️ **LƯU Ý QUAN TRỌNG:**
- **Database size:** 20k+ records sẽ mất ~30-45 phút để populate
- **Server resources:** Cần đủ RAM và CPU cho batch processing
- **Backup:** Luôn backup trước khi deploy
- **Test:** Test trên staging environment trước production