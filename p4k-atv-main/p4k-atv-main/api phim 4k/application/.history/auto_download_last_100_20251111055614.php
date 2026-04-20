<?php
/**
 * Auto Download Logo cho 100 phim cuối cùng
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
    
    // TMDB API
    'tmdb_api_key' => 'f7cde166455f2044415520dbbcfae843',
    'tmdb_base_url' => 'https://api.themoviedb.org/3',
    'tmdb_image_base_url' => 'https://image.tmdb.org/t/p/w300',
    
    // Paths
    'logo_directory' => '/www/wwwroot/api.phim4k.lol/uploads/logo/',
    
    // Settings
    'limit' => 100,  // Số phim cuối cùng
    'start_from_id' => null,  // Bắt đầu từ ID nào (null = tự động lấy ID cao nhất)
    'delay_between_requests' => 1,  // 1 giây delay
];

// =============================================================================
// HELPER FUNCTIONS
// =============================================================================
function getTMDBLogo($tmdbid, $is_tvseries, $config) {
    if (empty($tmdbid) || $tmdbid === '0') return false;
    
    $content_type = ($is_tvseries == 1) ? 'tv' : 'movie';
    $api_url = "{$config['tmdb_base_url']}/{$content_type}/{$tmdbid}/images?api_key={$config['tmdb_api_key']}";
    
    $ch = curl_init();
    curl_setopt($ch, CURLOPT_URL, $api_url);
    curl_setopt($ch, CURLOPT_RETURNTRANSFER, true);
    curl_setopt($ch, CURLOPT_TIMEOUT, 15);
    curl_setopt($ch, CURLOPT_USERAGENT, 'Auto-Logo-Downloader/1.0');
    curl_setopt($ch, CURLOPT_SSL_VERIFYPEER, false);
    
    $response = curl_exec($ch);
    $http_code = curl_getinfo($ch, CURLINFO_HTTP_CODE);
    curl_close($ch);
    
    if ($http_code !== 200 || !$response) return false;
    
    $data = json_decode($response, true);
    if (!$data || !isset($data['logos']) || empty($data['logos'])) return false;
    
    // Ưu tiên: Tiếng Việt -> Tiếng Anh -> Không có ngôn ngữ -> Bất kỳ
    $priorities = ['vi', 'en', null];
    
    foreach ($priorities as $lang) {
        foreach ($data['logos'] as $logo) {
            $logo_lang = $logo['iso_639_1'] ?? null;
            if ($logo_lang === $lang) {
                return $config['tmdb_image_base_url'] . $logo['file_path'];
            }
        }
    }
    
    // Fallback: logo đầu tiên
    return $config['tmdb_image_base_url'] . $data['logos'][0]['file_path'];
}

function downloadImage($url, $save_path) {
    $ch = curl_init();
    curl_setopt($ch, CURLOPT_URL, $url);
    curl_setopt($ch, CURLOPT_RETURNTRANSFER, true);
    curl_setopt($ch, CURLOPT_FOLLOWLOCATION, true);
    curl_setopt($ch, CURLOPT_TIMEOUT, 30);
    curl_setopt($ch, CURLOPT_USERAGENT, 'Mozilla/5.0 (compatible; Auto-Logo-Downloader)');
    curl_setopt($ch, CURLOPT_SSL_VERIFYPEER, false);
    
    $image_data = curl_exec($ch);
    $http_code = curl_getinfo($ch, CURLINFO_HTTP_CODE);
    curl_close($ch);
    
    if ($http_code !== 200 || !$image_data) return false;
    
    return file_put_contents($save_path, $image_data) !== false;
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
echo "🎬 AUTO DOWNLOAD LOGO - 100 PHIM CUỐI" . PHP_EOL;
echo "========================================" . PHP_EOL;

try {
    // Kết nối database
    logMessage("Đang kết nối database...");
    $pdo = new PDO("mysql:host={$config['host']};dbname={$config['database']};charset=utf8mb4", 
                  $config['username'], $config['password']);
    $pdo->setAttribute(PDO::ATTR_ERRMODE, PDO::ERRMODE_EXCEPTION);
    logMessage("✅ Kết nối database thành công!");

    // Tạo thư mục logo nếu chưa có
    if (!file_exists($config['logo_directory'])) {
        if (mkdir($config['logo_directory'], 0755, true)) {
            logMessage("✅ Tạo thư mục logo: " . $config['logo_directory']);
        } else {
            throw new Exception("❌ Không thể tạo thư mục logo!");
        }
    }

    // Lấy 100 phim cuối cùng có TMDB ID
    logMessage("Đang lấy danh sách 100 phim cuối cùng...");
    $stmt = $pdo->prepare("
        SELECT videos_id, tmdbid, title, is_tvseries, created_time
        FROM videos 
        WHERE (tmdbid IS NOT NULL AND tmdbid != '' AND tmdbid != '0')
        AND publication = 1
        ORDER BY videos_id DESC
        LIMIT ?
    ");
    $stmt->execute([$config['limit']]);
    $movies = $stmt->fetchAll(PDO::FETCH_ASSOC);
    
    $total_movies = count($movies);
    logMessage("📊 Tìm thấy {$total_movies} phim để xử lý");

    // Thống kê
    $stats = [
        'processed' => 0,
        'downloaded' => 0,
        'skipped' => 0,
        'errors' => 0
    ];

    echo PHP_EOL . "🚀 BẮT ĐẦU DOWNLOAD..." . PHP_EOL;
    echo str_repeat("-", 50) . PHP_EOL;

    foreach ($movies as $index => $movie) {
        $stats['processed']++;
        $progress = round(($stats['processed'] / $total_movies) * 100);
        
        $videos_id = $movie['videos_id'];
        $tmdbid = $movie['tmdbid'];
        $title = $movie['title'] ?? 'Unknown Title';
        $is_tvseries = $movie['is_tvseries'] ?? 0;
        $logo_path = $config['logo_directory'] . $videos_id . '.jpg';
        
        $short_title = mb_substr($title, 0, 40) . (mb_strlen($title) > 40 ? '...' : '');
        
        echo sprintf("[%d/%d] [%d%%] #%d: %s", 
            $stats['processed'], $total_movies, $progress, $videos_id, $short_title) . PHP_EOL;

        // Kiểm tra file đã tồn tại
        if (file_exists($logo_path)) {
            logMessage("⏭️  Đã có logo, bỏ qua", 'SKIP');
            $stats['skipped']++;
            continue;
        }

        try {
            // Lấy URL logo từ TMDB
            $logo_url = getTMDBLogo($tmdbid, $is_tvseries, $config);
            
            if ($logo_url) {
                // Tải logo
                if (downloadImage($logo_url, $logo_path)) {
                    logMessage("✅ Tải thành công!", 'SUCCESS');
                    $stats['downloaded']++;
                } else {
                    logMessage("❌ Tải thất bại!", 'ERROR');
                    $stats['errors']++;
                }
            } else {
                logMessage("⚠️  Không tìm thấy logo trên TMDB", 'WARNING');
                $stats['skipped']++;
            }
        } catch (Exception $e) {
            logMessage("❌ Lỗi: " . $e->getMessage(), 'ERROR');
            $stats['errors']++;
        }
        
        // Delay giữa các request
        if ($stats['processed'] < $total_movies) {
            sleep($config['delay_between_requests']);
        }
        
        echo str_repeat("-", 50) . PHP_EOL;
    }

    // Tổng kết
    echo PHP_EOL . "🎉 HOÀN THÀNH!" . PHP_EOL;
    echo "========================================" . PHP_EOL;
    echo "📊 THỐNG KÊ CUỐI CÙNG:" . PHP_EOL;
    echo "• Tổng số phim xử lý: {$stats['processed']}" . PHP_EOL;
    echo "• Logo đã tải: {$stats['downloaded']}" . PHP_EOL;
    echo "• Bỏ qua (đã có): {$stats['skipped']}" . PHP_EOL;
    echo "• Lỗi: {$stats['errors']}" . PHP_EOL;
    echo "• Tỷ lệ thành công: " . round(($stats['downloaded'] / $stats['processed']) * 100) . "%" . PHP_EOL;
    echo "========================================" . PHP_EOL;

} catch (Exception $e) {
    logMessage("💥 LỖI NGHIÊM TRỌNG: " . $e->getMessage(), 'FATAL');
    exit(1);
}

logMessage("Script hoàn thành lúc: " . date('Y-m-d H:i:s'));
?>