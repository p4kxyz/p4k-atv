<?php
/**
 * TMDB Logo Downloader
 * Tải logo phim từ TMDB API dựa vào tmdbid trong database
 * Ưu tiên logo tiếng Việt, không có thì lấy tiếng Anh
 */

set_time_limit(0); // Không giới hạn thời gian chạy
ini_set('memory_limit', '512M'); // Tăng memory limit

// =============================================================================
// CẤU HÌNH DATABASE - THAY ĐỔI THEO SERVER
// =============================================================================
$host = 'localhost';
$username = 'root';  // Thay đổi username
$password = '';      // Thay đổi password  
$database = 'sql_api_phim4k_l';  // Thay đổi tên database

// =============================================================================
// CẤU HÌNH TMDB API
// =============================================================================
$tmdb_api_key = 'your_tmdb_api_key_here'; // THAY ĐỔI API KEY TMDB
$tmdb_base_url = 'https://api.themoviedb.org/3';
$tmdb_image_base_url = 'https://image.tmdb.org/t/p/w500'; // Size logo

// =============================================================================
// CẤU HÌNH ĐƯỜNG DẪN
// =============================================================================
$logo_directory = '/www/wwwroot/api.phim4k.lol/uploads/logo/';
$batch_size = 50; // Xử lý 50 phim mỗi batch
$delay_between_requests = 1; // Delay 1 giây giữa các request để tránh rate limit

// =============================================================================
// KHỞI TẠO
// =============================================================================
echo "<h1>🎬 TMDB Logo Downloader</h1>\n";
echo "<p><strong>Bắt đầu:</strong> " . date('Y-m-d H:i:s') . "</p>\n";

// Tạo thư mục logo nếu chưa có
if (!file_exists($logo_directory)) {
    mkdir($logo_directory, 0755, true);
    echo "<p>✅ Tạo thư mục: {$logo_directory}</p>\n";
}

try {
    // Kết nối database
    $pdo = new PDO("mysql:host=$host;dbname=$database;charset=utf8mb4", $username, $password);
    $pdo->setAttribute(PDO::ATTR_ERRMODE, PDO::ERRMODE_EXCEPTION);
    echo "<p>✅ Kết nối database thành công</p>\n";
    
    // Đếm tổng số phim cần xử lý
    $count_stmt = $pdo->query("
        SELECT COUNT(*) as total 
        FROM videos 
        WHERE (tmdbid IS NOT NULL AND tmdbid != '' AND tmdbid != '0')
        AND publication = 1
    ");
    $total_movies = $count_stmt->fetch()['total'];
    echo "<p><strong>📊 Tổng số phim cần xử lý:</strong> {$total_movies}</p>\n";
    
    // Thống kê
    $processed = 0;
    $downloaded = 0;
    $skipped = 0;
    $errors = 0;
    
    // Xử lý theo batch
    $offset = 0;
    
    while ($offset < $total_movies) {
        echo "<div style='background:#f8f9fa; padding:10px; margin:10px 0; border-radius:5px;'>";
        echo "<h3>📦 Batch " . (floor($offset / $batch_size) + 1) . " (Records {$offset}-" . min($offset + $batch_size, $total_movies) . ")</h3>\n";
        
        // Lấy batch hiện tại
        $stmt = $pdo->prepare("
            SELECT videos_id, tmdbid, title, is_tvseries
            FROM videos 
            WHERE (tmdbid IS NOT NULL AND tmdbid != '' AND tmdbid != '0')
            AND publication = 1
            ORDER BY videos_id ASC
            LIMIT ? OFFSET ?
        ");
        $stmt->execute([$batch_size, $offset]);
        $movies = $stmt->fetchAll(PDO::FETCH_ASSOC);
        
        foreach ($movies as $movie) {
            $processed++;
            $videos_id = $movie['videos_id'];
            $tmdbid = $movie['tmdbid'];
            $title = $movie['title'];
            $is_tvseries = $movie['is_tvseries'];
            
            // Đường dẫn file logo
            $logo_path = $logo_directory . $videos_id . '.jpg';
            
            // Skip nếu file đã tồn tại
            if (file_exists($logo_path)) {
                echo "<span style='color:#6c757d'>⏭️ Skip #{$videos_id} (đã có logo)</span><br>";
                $skipped++;
                continue;
            }
            
            echo "<strong>🎬 #{$videos_id}:</strong> {$title} (TMDB: {$tmdbid}) ";
            
            // Gọi TMDB API để lấy logo
            $logo_url = getTMDBLogo($tmdbid, $is_tvseries, $tmdb_api_key, $tmdb_base_url, $tmdb_image_base_url);
            
            if ($logo_url) {
                // Tải và lưu logo
                if (downloadImage($logo_url, $logo_path)) {
                    echo "<span style='color:green'>✅ Downloaded</span><br>";
                    $downloaded++;
                } else {
                    echo "<span style='color:red'>❌ Download failed</span><br>";
                    $errors++;
                }
            } else {
                echo "<span style='color:orange'>⚠️ No logo found</span><br>";
                $skipped++;
            }
            
            // Delay để tránh rate limit
            sleep($delay_between_requests);
        }
        
        $offset += $batch_size;
        
        // Hiển thị tiến trình
        $percent = round(($processed / $total_movies) * 100, 2);
        echo "<p><strong>📈 Tiến trình:</strong> {$processed}/{$total_movies} ({$percent}%) | Downloaded: {$downloaded} | Skipped: {$skipped} | Errors: {$errors}</p>";
        echo "</div>\n";
        
        // Flush output để hiển thị real-time
        if (ob_get_level()) ob_flush();
        flush();
    }
    
    echo "<div style='background:#d4edda; padding:15px; margin:20px 0; border-radius:8px; border-left:5px solid #28a745;'>";
    echo "<h2>🎉 HOÀN THÀNH!</h2>";
    echo "<p><strong>Thời gian kết thúc:</strong> " . date('Y-m-d H:i:s') . "</p>";
    echo "<p><strong>Tổng xử lý:</strong> {$processed} phim</p>";
    echo "<p><strong>✅ Downloaded:</strong> {$downloaded} logos</p>";
    echo "<p><strong>⏭️ Skipped:</strong> {$skipped} (đã có hoặc không tìm thấy)</p>";
    echo "<p><strong>❌ Errors:</strong> {$errors}</p>";
    echo "</div>";
    
} catch (Exception $e) {
    echo "<div style='background:#f8d7da; padding:15px; border-radius:8px; color:#721c24;'>";
    echo "<h3>❌ LỖI:</h3>";
    echo "<p>" . $e->getMessage() . "</p>";
    echo "</div>";
}

// =============================================================================
// FUNCTIONS
// =============================================================================

/**
 * Lấy logo từ TMDB API
 * Ưu tiên tiếng Việt, không có thì lấy tiếng Anh
 */
function getTMDBLogo($tmdbid, $is_tvseries, $api_key, $base_url, $image_base_url) {
    // Xác định loại content (movie hoặc tv)
    $content_type = ($is_tvseries == 1) ? 'tv' : 'movie';
    
    // URL API để lấy images
    $api_url = "{$base_url}/{$content_type}/{$tmdbid}/images?api_key={$api_key}";
    
    // Gọi API
    $response = @file_get_contents($api_url);
    if (!$response) {
        return false;
    }
    
    $data = json_decode($response, true);
    if (!isset($data['logos']) || empty($data['logos'])) {
        return false;
    }
    
    $logos = $data['logos'];
    
    // Tìm logo tiếng Việt trước
    foreach ($logos as $logo) {
        if (isset($logo['iso_639_1']) && $logo['iso_639_1'] === 'vi') {
            return $image_base_url . $logo['file_path'];
        }
    }
    
    // Không có tiếng Việt, tìm tiếng Anh
    foreach ($logos as $logo) {
        if (isset($logo['iso_639_1']) && $logo['iso_639_1'] === 'en') {
            return $image_base_url . $logo['file_path'];
        }
    }
    
    // Không có tiếng Việt và tiếng Anh, lấy logo đầu tiên (null language)
    foreach ($logos as $logo) {
        if (!isset($logo['iso_639_1']) || $logo['iso_639_1'] === null) {
            return $image_base_url . $logo['file_path'];
        }
    }
    
    // Cuối cùng, lấy logo đầu tiên bất kỳ
    if (!empty($logos)) {
        return $image_base_url . $logos[0]['file_path'];
    }
    
    return false;
}

/**
 * Tải và lưu hình ảnh
 */
function downloadImage($url, $save_path) {
    $ch = curl_init();
    curl_setopt($ch, CURLOPT_URL, $url);
    curl_setopt($ch, CURLOPT_RETURNTRANSFER, true);
    curl_setopt($ch, CURLOPT_FOLLOWLOCATION, true);
    curl_setopt($ch, CURLOPT_TIMEOUT, 30);
    curl_setopt($ch, CURLOPT_USERAGENT, 'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36');
    
    $image_data = curl_exec($ch);
    $http_code = curl_getinfo($ch, CURLINFO_HTTP_CODE);
    curl_close($ch);
    
    if ($http_code === 200 && $image_data) {
        return file_put_contents($save_path, $image_data) !== false;
    }
    
    return false;
}
?>