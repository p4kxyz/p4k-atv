-- =====================================================
-- SCRIPT TRIỂN KHAI CẢI THIỆN TÌM KIẾM TIẾNG VIỆT
-- Chạy từng bước trên phpMyAdmin
-- =====================================================

-- BƯỚC 1: Thêm các cột mới
ALTER TABLE `videos` 
ADD COLUMN `title_vietnamese` varchar(300) DEFAULT NULL COMMENT 'Tên tiếng Việt có dấu',
ADD COLUMN `title_vietnamese_no_accent` varchar(300) DEFAULT NULL COMMENT 'Tên tiếng Việt không dấu',  
ADD COLUMN `title_original` varchar(300) DEFAULT NULL COMMENT 'Tên gốc (English/Original)';

-- BƯỚC 2: Tạo index cho tìm kiếm
ALTER TABLE `videos` 
ADD KEY `idx_title_vietnamese_no_accent` (`title_vietnamese_no_accent`),
ADD KEY `idx_title_original` (`title_original`);

-- BƯỚC 3: Tạo fulltext index (nếu MySQL hỗ trợ)
-- ALTER TABLE `videos` 
-- ADD FULLTEXT KEY `idx_title_search_fulltext` (`title`, `title_vietnamese`, `title_vietnamese_no_accent`, `title_original`);

-- BƯỚC 4: Kiểm tra cấu trúc bảng
-- SHOW COLUMNS FROM videos;