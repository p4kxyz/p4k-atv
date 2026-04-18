<?php
/**
 * Script đơn giản để update dữ liệu title
 * Chạy trực tiếp trên web browser hoặc command line
 */

// Cấu hình database - THAY ĐỔI THEO SERVER CỦA BẠN
$host = 'localhost';
$username = 'root';  // Thay đổi username
$password = '';      // Thay đổi password  
$database = 'sql_api_phim4k_l';  // Thay đổi tên database

try {
    // Kết nối database
    $pdo = new PDO("mysql:host=$host;dbname=$database;charset=utf8mb4", $username, $password);
    $pdo->setAttribute(PDO::ATTR_ERRMODE, PDO::ERRMODE_EXCEPTION);
    
    echo "<h2>🚀 Bắt đầu cập nhật title fields</h2>\n";
    
    // Kiểm tra cột đã tồn tại chưa
    $stmt = $pdo->query("SHOW COLUMNS FROM videos LIKE 'title_vietnamese'");
    if($stmt->rowCount() == 0) {
        die("❌ Lỗi: Chưa chạy script tạo cột. Vui lòng chạy deploy_step1_database.sql trước!\n");
    }
    
    // Đếm tổng số videos cần update
    $total_stmt = $pdo->query("SELECT COUNT(*) as total FROM videos WHERE title IS NOT NULL");
    $total_count = $total_stmt->fetch(PDO::FETCH_ASSOC)['total'];
    
    echo "<p>📊 Tổng số video cần update: {$total_count}</p>\n";
    echo "<p>🔄 Xử lý theo batch 500 records/lần...</p>\n";
    
    $batch_size = 500;
    $updated = 0;
    $offset = 0;
    
    while($offset < $total_count) {
        echo "<div style='background:#e3f2fd; padding:10px; margin:5px;'>";
        echo "<strong>📦 Batch " . (floor($offset/$batch_size) + 1) . ":</strong> Processing records " . ($offset + 1) . " - " . min($offset + $batch_size, $total_count) . "<br>";
        
        // Lấy batch hiện tại
        $stmt = $pdo->prepare("SELECT videos_id, title FROM videos WHERE title IS NOT NULL LIMIT :limit OFFSET :offset");
        $stmt->bindValue(':limit', $batch_size, PDO::PARAM_INT);
        $stmt->bindValue(':offset', $offset, PDO::PARAM_INT);
        $stmt->execute();
        $videos = $stmt->fetchAll(PDO::FETCH_ASSOC);
        
        if(empty($videos)) {
            break;
        }
        
        $batch_updated = 0;
        foreach($videos as $video) {
            $title = $video['title'];
            $video_id = $video['videos_id'];
            
            // Parse title thành 3 phần
            $parsed = parseMovieTitle($title);
            
            // Update database
            $sql = "UPDATE videos SET 
                    title_vietnamese = :vietnamese,
                    title_vietnamese_no_accent = :vietnamese_no_accent, 
                    title_original = :original
                    WHERE videos_id = :video_id";
            
            $stmt_update = $pdo->prepare($sql);
            $result = $stmt_update->execute([
                ':vietnamese' => $parsed['vietnamese'] ?: null,
                ':vietnamese_no_accent' => $parsed['vietnamese_no_accent'] ?: null,
                ':original' => $parsed['original'] ?: null,
                ':video_id' => $video_id
            ]);
            
            if($result) {
                $updated++;
                $batch_updated++;
            }
            
            // Debug một số record đầu của batch đầu tiên
            if($updated <= 5) {
                echo "📄 <strong>ID {$video_id}:</strong> {$title}<br>";
                echo "→ Vietnamese: " . ($parsed['vietnamese'] ?: 'NULL') . "<br>";
                echo "→ Vietnamese (no accent): " . ($parsed['vietnamese_no_accent'] ?: 'NULL') . "<br>";
                echo "→ Original: " . ($parsed['original'] ?: 'NULL') . "<br><br>";
            }
        }
        
        $percentage = round(($updated / $total_count) * 100, 1);
        echo "✅ Batch completed: {$batch_updated} records updated | Total: {$updated}/{$total_count} ({$percentage}%)<br>";
        echo "</div>";
        
        $offset += $batch_size;
        flush(); // Force output to browser
        
        // Nghỉ 1 giây giữa các batch để tránh overload
        sleep(1);
    }
    
    echo "<h3>🎉 Hoàn thành! Đã update {$updated}/{$total_count} videos</h3>\n";
    
} catch(Exception $e) {
    echo "❌ Lỗi: " . $e->getMessage() . "\n";
}

// Function loại bỏ dấu tiếng Việt
function removeVietnameseAccents($str) {
    $str = mb_strtolower($str, 'UTF-8');
    
    $from = [
        'á', 'à', 'ả', 'ã', 'ạ', 'ă', 'ắ', 'ằ', 'ẳ', 'ẵ', 'ặ', 'â', 'ấ', 'ầ', 'ẩ', 'ẫ', 'ậ',
        'đ',
        'é', 'è', 'ẻ', 'ẽ', 'ẹ', 'ê', 'ế', 'ề', 'ể', 'ễ', 'ệ',
        'í', 'ì', 'ỉ', 'ĩ', 'ị',
        'ó', 'ò', 'ỏ', 'õ', 'ọ', 'ô', 'ố', 'ồ', 'ổ', 'ỗ', 'ộ', 'ơ', 'ớ', 'ờ', 'ở', 'ỡ', 'ợ',
        'ú', 'ù', 'ủ', 'ũ', 'ụ', 'ư', 'ứ', 'ừ', 'ử', 'ữ', 'ự',
        'ý', 'ỳ', 'ỷ', 'ỹ', 'ỵ'
    ];
    
    $to = [
        'a', 'a', 'a', 'a', 'a', 'a', 'a', 'a', 'a', 'a', 'a', 'a', 'a', 'a', 'a', 'a', 'a',
        'd',
        'e', 'e', 'e', 'e', 'e', 'e', 'e', 'e', 'e', 'e', 'e',
        'i', 'i', 'i', 'i', 'i',
        'o', 'o', 'o', 'o', 'o', 'o', 'o', 'o', 'o', 'o', 'o', 'o', 'o', 'o', 'o', 'o', 'o',
        'u', 'u', 'u', 'u', 'u', 'u', 'u', 'u', 'u', 'u', 'u',
        'y', 'y', 'y', 'y', 'y'
    ];
    
    return str_replace($from, $to, $str);
}

// Function parse title thành 3 phần
function parseMovieTitle($title) {
    $result = [
        'vietnamese' => '',
        'vietnamese_no_accent' => '',
        'original' => ''
    ];
    
    // Pattern 1: "Tên Việt (2023) English Name"
    if (preg_match('/^(.+?)\s*\((\d{4})\)\s*(.+)$/u', $title, $matches)) {
        $vietnamese = trim($matches[1]);
        $year = $matches[2];
        $original = trim($matches[3]);
        
        // Kiểm tra nếu phần sau năm là tiếng Anh (có ký tự Latin)
        if (preg_match('/[a-zA-Z]/', $original)) {
            $result['vietnamese'] = $vietnamese . ' (' . $year . ')';
            $result['original'] = $original;
        } else {
            // Nếu không có tên gốc, chỉ có tên Việt
            $result['vietnamese'] = $title;
        }
    }
    // Pattern 2: "Tên Việt (2023)" - chỉ có tên Việt
    else if (preg_match('/^(.+?)\s*\((\d{4})\)\s*$/u', $title, $matches)) {
        $result['vietnamese'] = $title;
    }
    // Pattern 3: Không có pattern đặc biệt
    else {
        // Kiểm tra nếu title chứa ký tự tiếng Việt
        if (preg_match('/[àáạảãâầấậẩẫăằắặẳẵèéẹẻẽêềếệểễìíịỉĩòóọỏõôồốộổỗơờớợởỡùúụủũưừứựửữỳýỵỷỹđ]/u', $title)) {
            $result['vietnamese'] = $title;
        } else {
            $result['original'] = $title;
        }
    }
    
    // Tạo Vietnamese no accent
    if (!empty($result['vietnamese'])) {
        $result['vietnamese_no_accent'] = removeVietnameseAccents($result['vietnamese']);
    }
    
    return $result;
}
?>