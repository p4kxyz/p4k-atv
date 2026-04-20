-- Script cập nhật cột title_search cho dữ liệu có sẵn
-- Chạy script này sau khi thêm cột mới

-- Thêm cột mới (nếu chưa có)
ALTER TABLE `videos` 
ADD COLUMN `title_vietnamese` varchar(300) DEFAULT NULL COMMENT 'Tên tiếng Việt có dấu' AFTER `year`,
ADD COLUMN `title_vietnamese_no_accent` varchar(300) DEFAULT NULL COMMENT 'Tên tiếng Việt không dấu' AFTER `title_vietnamese`,
ADD COLUMN `title_original` varchar(300) DEFAULT NULL COMMENT 'Tên gốc (English/Original)' AFTER `title_vietnamese_no_accent`;

-- Thêm index cho tìm kiếm
ALTER TABLE `videos` 
ADD KEY `idx_title_vietnamese_no_accent` (`title_vietnamese_no_accent`),
ADD KEY `idx_title_original` (`title_original`),
ADD FULLTEXT KEY `idx_title_search_fulltext` (`title`, `title_vietnamese`, `title_vietnamese_no_accent`, `title_original`);

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

-- Function tách title thành Vietnamese và Original
DELIMITER $$
CREATE FUNCTION parse_title_vietnamese(input_title TEXT) 
RETURNS TEXT
DETERMINISTIC
READS SQL DATA
BEGIN
    DECLARE vietnamese_title TEXT DEFAULT '';
    
    -- Pattern: "Tên Việt (2023) English Name" -> "Tên Việt (2023)"
    IF input_title REGEXP '^.+\\([0-9]{4}\\).+$' THEN
        -- Có cả tên Việt và tên gốc
        SET vietnamese_title = TRIM(SUBSTRING_INDEX(SUBSTRING_INDEX(input_title, ')', 1), '(', 1));
        -- Thêm lại năm
        SET vietnamese_title = CONCAT(vietnamese_title, ' (', 
            SUBSTRING(input_title, LOCATE('(', input_title) + 1, 4), ')');
    ELSE
        -- Chỉ có tên Việt hoặc không có pattern
        IF input_title REGEXP '[àáạảãâầấậẩẫăằắặẳẵèéẹẻẽêềếệểễìíịỉĩòóọỏõôồốộổỗơờớợởỡùúụủũưừứựửữỳýỵỷỹđ]' THEN
            SET vietnamese_title = input_title;
        END IF;
    END IF;
    
    RETURN vietnamese_title;
END$$

CREATE FUNCTION parse_title_original(input_title TEXT) 
RETURNS TEXT
DETERMINISTIC
READS SQL DATA
BEGIN
    DECLARE original_title TEXT DEFAULT '';
    
    -- Pattern: "Tên Việt (2023) English Name" -> "English Name"
    IF input_title REGEXP '^.+\\([0-9]{4}\\)\\s*(.+)$' THEN
        SET original_title = TRIM(SUBSTRING(input_title, LOCATE(')', input_title) + 1));
        -- Kiểm tra nếu có ký tự Latin
        IF original_title REGEXP '[a-zA-Z]' THEN
            -- Là tên gốc
            SET original_title = original_title;
        ELSE
            SET original_title = '';
        END IF;
    ELSE
        -- Không có pattern đặc biệt, kiểm tra nếu toàn bộ title là tiếng Anh
        IF input_title NOT REGEXP '[àáạảãâầấậẩẫăằắặẳẵèéẹẻẽêềếệểễìíịỉĩòóọỏõôồốộổỗơờớợởỡùúụủũưừứựửữỳýỵỷỹđ]' THEN
            SET original_title = input_title;
        END IF;
    END IF;
    
    RETURN original_title;
END$$
DELIMITER ;

-- Cập nhật dữ liệu cho các record có sẵn
UPDATE `videos` 
SET 
    `title_vietnamese` = parse_title_vietnamese(title),
    `title_vietnamese_no_accent` = CASE 
        WHEN parse_title_vietnamese(title) != '' THEN 
            remove_vietnamese_accents_mysql(parse_title_vietnamese(title))
        ELSE NULL 
    END,
    `title_original` = CASE 
        WHEN parse_title_original(title) != '' THEN 
            parse_title_original(title)
        ELSE NULL 
    END
WHERE title IS NOT NULL;

-- Tạo trigger để tự động cập nhật title fields khi insert/update
DELIMITER $$
CREATE TRIGGER `videos_title_fields_insert` 
BEFORE INSERT ON `videos` 
FOR EACH ROW 
BEGIN
    IF NEW.title IS NOT NULL THEN
        -- Tách title thành 3 phần
        SET NEW.title_vietnamese = parse_title_vietnamese(NEW.title);
        SET NEW.title_vietnamese_no_accent = CASE 
            WHEN NEW.title_vietnamese != '' AND NEW.title_vietnamese IS NOT NULL THEN 
                remove_vietnamese_accents_mysql(NEW.title_vietnamese)
            ELSE NULL 
        END;
        SET NEW.title_original = parse_title_original(NEW.title);
        
        -- Set NULL nếu rỗng
        IF NEW.title_vietnamese = '' THEN SET NEW.title_vietnamese = NULL; END IF;
        IF NEW.title_original = '' THEN SET NEW.title_original = NULL; END IF;
    END IF;
END$$

CREATE TRIGGER `videos_title_fields_update` 
BEFORE UPDATE ON `videos` 
FOR EACH ROW 
BEGIN
    IF NEW.title IS NOT NULL AND (NEW.title != OLD.title OR OLD.title_vietnamese IS NULL) THEN
        -- Tách title thành 3 phần
        SET NEW.title_vietnamese = parse_title_vietnamese(NEW.title);
        SET NEW.title_vietnamese_no_accent = CASE 
            WHEN NEW.title_vietnamese != '' AND NEW.title_vietnamese IS NOT NULL THEN 
                remove_vietnamese_accents_mysql(NEW.title_vietnamese)
            ELSE NULL 
        END;
        SET NEW.title_original = parse_title_original(NEW.title);
        
        -- Set NULL nếu rỗng
        IF NEW.title_vietnamese = '' THEN SET NEW.title_vietnamese = NULL; END IF;
        IF NEW.title_original = '' THEN SET NEW.title_original = NULL; END IF;
    END IF;
END$$
DELIMITER ;

-- Test query để kiểm tra
-- SELECT videos_id, title, title_vietnamese, title_vietnamese_no_accent, title_original FROM videos LIMIT 10;