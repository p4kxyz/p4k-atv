-- Script cập nhật cột title_search cho dữ liệu có sẵn
-- Chạy script này sau khi thêm cột mới

-- Thêm cột mới (nếu chưa có)
ALTER TABLE `videos` 
ADD COLUMN `title_search` varchar(500) DEFAULT NULL COMMENT 'Title không dấu cho tìm kiếm' AFTER `year`,
ADD COLUMN `title_vietnamese` varchar(255) DEFAULT NULL COMMENT 'Tên tiếng Việt thuần' AFTER `title_search`;

-- Thêm index cho tìm kiếm
ALTER TABLE `videos` 
ADD KEY `idx_title_search` (`title_search`),
ADD FULLTEXT KEY `idx_title_fulltext` (`title`, `title_search`, `title_vietnamese`);

-- Tạo function để loại bỏ dấu tiếng Việt (MySQL function)
DELIMITER $$
CREATE FUNCTION remove_vietnamese_accents_mysql(input_str TEXT) 
RETURNS TEXT
DETERMINISTIC
READS SQL DATA
BEGIN
    DECLARE result TEXT;
    SET result = LOWER(input_str);
    
    -- Thay thế các ký tự có dấu
    SET result = REPLACE(result, 'á', 'a');
    SET result = REPLACE(result, 'à', 'a');
    SET result = REPLACE(result, 'ả', 'a');
    SET result = REPLACE(result, 'ã', 'a');
    SET result = REPLACE(result, 'ạ', 'a');
    SET result = REPLACE(result, 'ă', 'a');
    SET result = REPLACE(result, 'ắ', 'a');
    SET result = REPLACE(result, 'ằ', 'a');
    SET result = REPLACE(result, 'ẳ', 'a');
    SET result = REPLACE(result, 'ẵ', 'a');
    SET result = REPLACE(result, 'ặ', 'a');
    SET result = REPLACE(result, 'â', 'a');
    SET result = REPLACE(result, 'ấ', 'a');
    SET result = REPLACE(result, 'ầ', 'a');
    SET result = REPLACE(result, 'ẩ', 'a');
    SET result = REPLACE(result, 'ẫ', 'a');
    SET result = REPLACE(result, 'ậ', 'a');
    
    SET result = REPLACE(result, 'đ', 'd');
    
    SET result = REPLACE(result, 'é', 'e');
    SET result = REPLACE(result, 'è', 'e');
    SET result = REPLACE(result, 'ẻ', 'e');
    SET result = REPLACE(result, 'ẽ', 'e');
    SET result = REPLACE(result, 'ẹ', 'e');
    SET result = REPLACE(result, 'ê', 'e');
    SET result = REPLACE(result, 'ế', 'e');
    SET result = REPLACE(result, 'ề', 'e');
    SET result = REPLACE(result, 'ể', 'e');
    SET result = REPLACE(result, 'ễ', 'e');
    SET result = REPLACE(result, 'ệ', 'e');
    
    SET result = REPLACE(result, 'í', 'i');
    SET result = REPLACE(result, 'ì', 'i');
    SET result = REPLACE(result, 'ỉ', 'i');
    SET result = REPLACE(result, 'ĩ', 'i');
    SET result = REPLACE(result, 'ị', 'i');
    
    SET result = REPLACE(result, 'ó', 'o');
    SET result = REPLACE(result, 'ò', 'o');
    SET result = REPLACE(result, 'ỏ', 'o');
    SET result = REPLACE(result, 'õ', 'o');
    SET result = REPLACE(result, 'ọ', 'o');
    SET result = REPLACE(result, 'ô', 'o');
    SET result = REPLACE(result, 'ố', 'o');
    SET result = REPLACE(result, 'ồ', 'o');
    SET result = REPLACE(result, 'ổ', 'o');
    SET result = REPLACE(result, 'ỗ', 'o');
    SET result = REPLACE(result, 'ộ', 'o');
    SET result = REPLACE(result, 'ơ', 'o');
    SET result = REPLACE(result, 'ớ', 'o');
    SET result = REPLACE(result, 'ờ', 'o');
    SET result = REPLACE(result, 'ở', 'o');
    SET result = REPLACE(result, 'ỡ', 'o');
    SET result = REPLACE(result, 'ợ', 'o');
    
    SET result = REPLACE(result, 'ú', 'u');
    SET result = REPLACE(result, 'ù', 'u');
    SET result = REPLACE(result, 'ủ', 'u');
    SET result = REPLACE(result, 'ũ', 'u');
    SET result = REPLACE(result, 'ụ', 'u');
    SET result = REPLACE(result, 'ư', 'u');
    SET result = REPLACE(result, 'ứ', 'u');
    SET result = REPLACE(result, 'ừ', 'u');
    SET result = REPLACE(result, 'ử', 'u');
    SET result = REPLACE(result, 'ữ', 'u');
    SET result = REPLACE(result, 'ự', 'u');
    
    SET result = REPLACE(result, 'ý', 'y');
    SET result = REPLACE(result, 'ỳ', 'y');
    SET result = REPLACE(result, 'ỷ', 'y');
    SET result = REPLACE(result, 'ỹ', 'y');
    SET result = REPLACE(result, 'ỵ', 'y');
    
    RETURN result;
END$$
DELIMITER ;

-- Cập nhật dữ liệu cho các record có sẵn
UPDATE `videos` 
SET 
    `title_search` = remove_vietnamese_accents_mysql(title),
    `title_vietnamese` = CASE 
        WHEN title REGEXP '^[^(]*\\([0-9]{4}\\)' THEN 
            TRIM(SUBSTRING_INDEX(title, '(', 1))
        ELSE 
            title 
    END
WHERE title IS NOT NULL;

-- Tạo trigger để tự động cập nhật title_search khi insert/update
DELIMITER $$
CREATE TRIGGER `videos_title_search_insert` 
BEFORE INSERT ON `videos` 
FOR EACH ROW 
BEGIN
    IF NEW.title IS NOT NULL THEN
        SET NEW.title_search = remove_vietnamese_accents_mysql(NEW.title);
        -- Tách tên tiếng Việt (phần trước dấu ngoặc năm)
        SET NEW.title_vietnamese = CASE 
            WHEN NEW.title REGEXP '^[^(]*\\([0-9]{4}\\)' THEN 
                TRIM(SUBSTRING_INDEX(NEW.title, '(', 1))
            ELSE 
                NEW.title 
        END;
    END IF;
END$$

CREATE TRIGGER `videos_title_search_update` 
BEFORE UPDATE ON `videos` 
FOR EACH ROW 
BEGIN
    IF NEW.title IS NOT NULL AND (NEW.title != OLD.title OR OLD.title_search IS NULL) THEN
        SET NEW.title_search = remove_vietnamese_accents_mysql(NEW.title);
        SET NEW.title_vietnamese = CASE 
            WHEN NEW.title REGEXP '^[^(]*\\([0-9]{4}\\)' THEN 
                TRIM(SUBSTRING_INDEX(NEW.title, '(', 1))
            ELSE 
                NEW.title 
        END;
    END IF;
END$$
DELIMITER ;

-- Test query để kiểm tra
-- SELECT videos_id, title, title_vietnamese, title_search FROM videos LIMIT 10;