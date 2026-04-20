<?php
/**
 * Auto Update Vietnamese Title Fields cho 20 videos cuối cùng
 * Chạy tự động không cần giao diện web
 */

set_time_limit(0);
ini_set('memory_limit', '512M');
error_reporting(E_ALL);
ini_set('display_errors', 1);

// =============================================================================
// CẤU HÌNH
// =============================================================================
$config = [
    // Database
    'host' => 'localhost',
    'username' => 'root',
    'password' => '5e6fdc4c3e2dddb9',
    'database' => 'sql_api_phim4k_l',
    
    // Settings
    'limit' => 20,  // Số video cuối cùng
];

// =============================================================================
// HELPER FUNCTIONS
// =============================================================================
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
    
    // Bỏ dấu tiếng Việt
    $str = str_replace($from, $to, $str);
    
    // Bỏ các ký tự đặc biệt
    $str = preg_replace('/[:\-\.!?\'"",;]/', ' ', $str);
    
    // Bỏ nhiều space thành 1 space và trim
    $str = preg_replace('/\s+/', ' ', $str);
    $str = trim($str);
    
    return $str;
}

function parseMovieTitle($title) {
    $result = [
        'vietnamese' => '',
        'vietnamese_no_accent' => '',
        'original' => ''
    ];
    
    // Pattern 1: "Tên Việt (2023) English Name + Asian characters"
    if (preg_match('/^(.+?)\s*\((\d{4})\)\s*(.+)$/u', $title, $matches)) {
        $vietnamese = trim($matches[1]);
        $year = $matches[2];
        $after_year = trim($matches[3]);
        
        // Kiểm tra nếu có ký tự Latin (English) hoặc Asian (Hàn/Nhật/Trung)
        if (preg_match('/[a-zA-Z\x{4e00}-\x{9fff}\x{3040}-\x{309f}\x{30a0}-\x{30ff}\x{ac00}-\x{d7af}]/u', $after_year)) {
            $result['vietnamese'] = $vietnamese . ' (' . $year . ')';
            $result['original'] = $after_year;
        } else {
            // Nếu không có tên gốc rõ ràng, toàn bộ là tên Việt
            $result['vietnamese'] = $title;
        }
    }
    // Pattern 2: "Tên Việt (2023)" - chỉ có tên Việt với năm
    else if (preg_match('/^(.+?)\s*\((\d{4})\)\s*$/u', $title, $matches)) {
        $result['vietnamese'] = $title;
    }
    // Pattern 3: Không có pattern năm
    else {
        // Kiểm tra nếu title chứa ký tự tiếng Việt
        if (preg_match('/[àáạảãâầấậẩẫăằắặẳẵèéẹẻẽêềếệểễìíịỉĩòóọỏõôồốộổỗơờớợởỡùúụủũưừứựửữỳýỵỷỹđ]/u', $title)) {
            $result['vietnamese'] = $title;
        } 
        // Nếu có Asian characters nhưng không có tiếng Việt
        else if (preg_match('/[\x{4e00}-\x{9fff}\x{3040}-\x{309f}\x{30a0}-\x{30ff}\x{ac00}-\x{d7af}]/u', $title)) {
            $result['original'] = $title;
        }
        // Nếu chỉ có Latin characters
        else if (preg_match('/[a-zA-Z]/', $title)) {
            $result['original'] = $title;
        }
        // Fallback: đặt vào original
        else {
            $result['original'] = $title;
        }
    }
    
    // Tạo Vietnamese no accent (chỉ khi có vietnamese)
    if (!empty($result['vietnamese'])) {
        $result['vietnamese_no_accent'] = removeVietnameseAccents($result['vietnamese']);
    }
    
    return $result;
}

function logMessage($message, $type = 'INFO') {
    $timestamp = date('Y-m-d H:i:s');
    echo "[$timestamp] [$type] $message" . PHP_EOL;
    flush();
}

// =============================================================================
// MAIN SCRIPT
// =============================================================================
echo "========================================" . PHP_EOL;
echo "📝 AUTO UPDATE VIETNAMESE FIELDS - 20 VIDEO CUỐI" . PHP_EOL;
echo "========================================" . PHP_EOL;

try {
    // Kết nối database
    logMessage("Đang kết nối database...");
    $pdo = new PDO("mysql:host={$config['host']};dbname={$config['database']};charset=utf8mb4", 
                  $config['username'], $config['password']);
    $pdo->setAttribute(PDO::ATTR_ERRMODE, PDO::ERRMODE_EXCEPTION);
    logMessage("✅ Kết nối database thành công!");

    // Lấy 20 video cuối cùng
    logMessage("Đang lấy {$config['limit']} video cuối cùng...");
    $sql = "
        SELECT videos_id, title
        FROM videos 
        WHERE title IS NOT NULL 
        ORDER BY videos_id DESC
        LIMIT " . (int)$config['limit'];
    
    $stmt = $pdo->query($sql);
    $videos = $stmt->fetchAll(PDO::FETCH_ASSOC);
    
    $total_videos = count($videos);
    logMessage("📊 Tìm thấy {$total_videos} video để xử lý");

    if ($total_videos == 0) {
        logMessage("⚠️ Không có video nào để xử lý!", 'WARNING');
        exit(0);
    }

    // Thống kê
    $stats = [
        'processed' => 0,
        'updated' => 0,
        'errors' => 0
    ];

    echo PHP_EOL . "🚀 BẮT ĐẦU XỬ LÝ..." . PHP_EOL;
    echo str_repeat("-", 80) . PHP_EOL;

    foreach ($videos as $index => $video) {
        $stats['processed']++;
        $progress = round(($stats['processed'] / $total_videos) * 100);
        
        $videos_id = $video['videos_id'];
        $title = $video['title'];
        
        echo sprintf("[%d/%d] [%d%%] #%d: %s", 
            $stats['processed'], $total_videos, $progress, $videos_id, 
            mb_substr($title, 0, 50) . (mb_strlen($title) > 50 ? '...' : '')) . PHP_EOL;

        try {
            // Parse title
            $parsed = parseMovieTitle($title);
            
            // Update database
            $update_sql = "UPDATE videos SET 
                          title_vietnamese = :vietnamese,
                          title_vietnamese_no_accent = :vietnamese_no_accent, 
                          title_original = :original
                          WHERE videos_id = :video_id";
            
            $stmt_update = $pdo->prepare($update_sql);
            $result = $stmt_update->execute([
                ':vietnamese' => $parsed['vietnamese'] ?: null,
                ':vietnamese_no_accent' => $parsed['vietnamese_no_accent'] ?: null,
                ':original' => $parsed['original'] ?: null,
                ':video_id' => $videos_id
            ]);
            
            if ($result) {
                $stats['updated']++;
                logMessage("✅ Cập nhật thành công!", 'SUCCESS');
                
                // Hiển thị kết quả parse
                if ($parsed['vietnamese']) {
                    logMessage("   → Vietnamese: " . $parsed['vietnamese'], 'INFO');
                }
                if ($parsed['vietnamese_no_accent']) {
                    logMessage("   → Vietnamese (no accent): " . $parsed['vietnamese_no_accent'], 'INFO');
                }
                if ($parsed['original']) {
                    logMessage("   → Original: " . $parsed['original'], 'INFO');
                }
            } else {
                logMessage("❌ Lỗi cập nhật database!", 'ERROR');
                $stats['errors']++;
            }
        } catch (Exception $e) {
            logMessage("❌ Lỗi: " . $e->getMessage(), 'ERROR');
            $stats['errors']++;
        }
        
        echo str_repeat("-", 80) . PHP_EOL;
    }

    // Tổng kết
    echo PHP_EOL . "🎉 HOÀN THÀNH!" . PHP_EOL;
    echo "========================================" . PHP_EOL;
    echo "📊 THỐNG KÊ CUỐI CÙNG:" . PHP_EOL;
    echo "• Tổng số video xử lý: {$stats['processed']}" . PHP_EOL;
    echo "• Cập nhật thành công: {$stats['updated']}" . PHP_EOL;
    echo "• Lỗi: {$stats['errors']}" . PHP_EOL;
    echo "• Tỷ lệ thành công: " . round(($stats['updated'] / $stats['processed']) * 100) . "%" . PHP_EOL;
    echo "========================================" . PHP_EOL;

    // Hiển thị kết quả mẫu
    echo PHP_EOL . "📋 KẾT QUẢ MẪU:" . PHP_EOL;
    $sample_sql = "
        SELECT videos_id, title, title_vietnamese, title_vietnamese_no_accent, title_original
        FROM videos 
        WHERE videos_id IN (" . implode(',', array_column($videos, 'videos_id')) . ")
        ORDER BY videos_id DESC
        LIMIT 3";
    
    $sample_stmt = $pdo->query($sample_sql);
    $samples = $sample_stmt->fetchAll(PDO::FETCH_ASSOC);
    
    foreach ($samples as $sample) {
        echo str_repeat("-", 80) . PHP_EOL;
        echo "ID: {$sample['videos_id']}" . PHP_EOL;
        echo "Original Title: {$sample['title']}" . PHP_EOL;
        echo "Vietnamese: " . ($sample['title_vietnamese'] ?: 'NULL') . PHP_EOL;
        echo "Vietnamese (no accent): " . ($sample['title_vietnamese_no_accent'] ?: 'NULL') . PHP_EOL;
        echo "Original: " . ($sample['title_original'] ?: 'NULL') . PHP_EOL;
    }

} catch (Exception $e) {
    logMessage("💥 LỖI NGHIÊM TRỌNG: " . $e->getMessage(), 'FATAL');
    exit(1);
}

logMessage("Script hoàn thành lúc: " . date('Y-m-d H:i:s'));
?>