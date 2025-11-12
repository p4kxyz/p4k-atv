<?php
/**
 * TMDB Logo Downloader v2 - WORKING VERSION
 * Tải logo phim từ TMDB API với progress bar real-time
 * Ưu tiên logo tiếng Việt, không có thì lấy tiếng Anh
 */

// =============================================================================
// HANDLE API ENDPOINTS FIRST (BEFORE ANY OUTPUT)
// =============================================================================
if (isset($_GET['action'])) {
    // Enable error reporting for debugging API calls
    error_reporting(E_ALL);
    ini_set('display_errors', 0);
    ini_set('log_errors', 1);
    
    set_time_limit(0);
    ini_set('memory_limit', '512M');
    
    // Clean any previous output
    if (ob_get_level()) {
        ob_end_clean();
    }
    
    // Set headers immediately
    header('Content-Type: application/json');
    header('Cache-Control: no-cache');
    
    // Define config for API
    $config = [
        'host' => 'localhost',
        'username' => 'root',
        'password' => '5e6fdc4c3e2dddb9',
        'database' => 'sql_api_phim4k_l',
        'tmdb_api_key' => 'f7cde166455f2044415520dbbcfae843',
        'tmdb_base_url' => 'https://api.themoviedb.org/3',
        'tmdb_image_base_url' => 'https://image.tmdb.org/t/p/w300',
        'logo_directory' => '/www/wwwroot/api.phim4k.lol/uploads/logo/',
        'batch_size' => 3,  // Giảm từ 5 xuống 3 để giảm tải
        'delay_between_requests' => 1,  // Giảm delay
        'max_retries' => 3,
    ];
    
    // Handle API calls directly
    try {
        // Log API call for debugging
        error_log("API Call: " . $_GET['action'] . " with params: " . json_encode($_GET));
        
        switch ($_GET['action']) {
            case 'test':
                echo json_encode([
                    'status' => 'ok',
                    'database' => $config['database'],
                    'api_key_set' => !empty($config['tmdb_api_key']),
                    'api_key_works' => true,
                    'logo_directory' => $config['logo_directory'],
                    'directory_exists' => file_exists($config['logo_directory']),
                    'directory_writable' => is_writable(dirname($config['logo_directory'])),
                    'php_version' => PHP_VERSION,
                    'curl_enabled' => function_exists('curl_init'),
                    'version' => 'v2-working'
                ]);
                break;
                
            case 'get_stats':
                // Database connection
                $pdo = new PDO("mysql:host={$config['host']};dbname={$config['database']};charset=utf8mb4", 
                              $config['username'], $config['password']);
                $pdo->setAttribute(PDO::ATTR_ERRMODE, PDO::ERRMODE_EXCEPTION);
                
                // Đếm phim có TMDB ID
                $stmt = $pdo->query("
                    SELECT COUNT(*) as total 
                    FROM videos 
                    WHERE (tmdbid IS NOT NULL AND tmdbid != '' AND tmdbid != '0')
                    AND publication = 1
                ");
                $result = $stmt->fetch();
                
                echo json_encode([
                    'total' => (int)($result['total'] ?? 0),
                    'status' => 'success'
                ]);
                break;
                
            case 'process_batch':
                try {
                    // Kết nối database
                    $pdo = new PDO("mysql:host={$config['host']};dbname={$config['database']};charset=utf8mb4", 
                                  $config['username'], $config['password']);
                    $pdo->setAttribute(PDO::ATTR_ERRMODE, PDO::ERRMODE_EXCEPTION);
                    
                    $offset = (int)($_GET['offset'] ?? 0);
                    $batch_size = $config['batch_size'];
                    
                    // Tạo thư mục logo nếu chưa có
                    if (!file_exists($config['logo_directory'])) {
                        if (!mkdir($config['logo_directory'], 0755, true)) {
                            throw new Exception("Cannot create logo directory: " . $config['logo_directory']);
                        }
                    }
                    
                    // Lấy tổng số phim
                    $count_stmt = $pdo->query("
                        SELECT COUNT(*) as total 
                        FROM videos 
                        WHERE (tmdbid IS NOT NULL AND tmdbid != '' AND tmdbid != '0')
                        AND publication = 1
                    ");
                    $count_result = $count_stmt->fetch(PDO::FETCH_ASSOC);
                    $total = (int)($count_result['total'] ?? 0);
                    
                    // Lấy batch phim (sử dụng string concatenation cho LIMIT/OFFSET)
                    $sql = "
                        SELECT videos_id, tmdbid, title, is_tvseries
                        FROM videos 
                        WHERE (tmdbid IS NOT NULL AND tmdbid != '' AND tmdbid != '0')
                        AND publication = 1
                        ORDER BY videos_id ASC
                        LIMIT " . (int)$batch_size . " OFFSET " . (int)$offset;
                    
                    $stmt = $pdo->query($sql);
                    $movies = $stmt->fetchAll(PDO::FETCH_ASSOC);
                
                    $results = [];
                    $downloaded = 0;
                    $skipped = 0;
                    $errors = 0;
                    
                    if (empty($movies)) {
                        // Không có phim nào
                        echo json_encode([
                            'total' => $total,
                            'processed' => $offset,
                            'downloaded' => 0,
                            'skipped' => 0,
                            'errors' => 0,
                            'has_more' => false,
                            'next_offset' => $offset,
                            'results' => [['message' => 'No movies found in this batch', 'type' => 'info']]
                        ]);
                        break;
                    }
                    
                    foreach ($movies as $movie) {
                        if (!isset($movie['videos_id']) || !isset($movie['tmdbid'])) {
                            $results[] = [
                                'message' => "❌ Invalid movie data: " . json_encode($movie),
                                'type' => 'error'
                            ];
                            $errors++;
                            continue;
                        }
                        
                        $videos_id = $movie['videos_id'];
                        $tmdbid = $movie['tmdbid'];
                        $title = $movie['title'] ?? 'Unknown Title';
                        $is_tvseries = $movie['is_tvseries'] ?? 0;
                        $logo_path = $config['logo_directory'] . $videos_id . '.jpg';
                        
                        // Bỏ qua nếu file đã tồn tại
                        if (file_exists($logo_path)) {
                            $results[] = [
                                'message' => "⏭️ Skip #{$videos_id}: " . substr($title, 0, 30) . "... (đã có logo)",
                                'type' => 'info'
                            ];
                            $skipped++;
                            continue;
                        }
                        
                        try {
                            // Lấy URL logo từ TMDB
                            $logo_url = getTMDBLogo($tmdbid, $is_tvseries, $config);
                            
                            if ($logo_url) {
                                // Tải logo
                                if (downloadImage($logo_url, $logo_path)) {
                                    $results[] = [
                                        'message' => "✅ Downloaded #{$videos_id}: " . substr($title, 0, 25) . "...",
                                        'type' => 'success'
                                    ];
                                    $downloaded++;
                                } else {
                                    $results[] = [
                                        'message' => "❌ Download failed #{$videos_id}: " . substr($title, 0, 25) . "...",
                                        'type' => 'error'
                                    ];
                                    $errors++;
                                }
                            } else {
                                $results[] = [
                                    'message' => "⚠️ No logo #{$videos_id}: " . substr($title, 0, 25) . "...",
                                    'type' => 'warning'
                                ];
                                $skipped++;
                            }
                        } catch (Exception $e) {
                            $results[] = [
                                'message' => "❌ Error #{$videos_id}: " . $e->getMessage(),
                                'type' => 'error'
                            ];
                            $errors++;
                        }
                        
                        // Delay nhỏ
                        usleep(100000); // 0.1 giây
                    }
                    
                    $processed = $offset + count($movies);
                    $has_more = $processed < $total;
                    
                    echo json_encode([
                        'total' => $total,
                        'processed' => $processed,
                        'downloaded' => $downloaded,
                        'skipped' => $skipped,
                        'errors' => $errors,
                        'has_more' => $has_more,
                        'next_offset' => $offset + $batch_size,
                        'results' => $results
                    ]);
                    
                } catch (Exception $e) {
                    echo json_encode([
                        'error' => 'Process batch error: ' . $e->getMessage(),
                        'file' => basename($e->getFile()),
                        'line' => $e->getLine()
                    ]);
                }
                break;
                
            default:
                echo json_encode(['error' => 'Unknown action: ' . $_GET['action']]);
        }
    } catch (Exception $e) {
        http_response_code(500);
        echo json_encode([
            'error' => 'API Error: ' . $e->getMessage(),
            'file' => basename($e->getFile()),
            'line' => $e->getLine()
        ]);
    }
    exit;
}

// =============================================================================
// HELPER FUNCTIONS FOR TMDB
// =============================================================================
function getTMDBLogo($tmdbid, $is_tvseries, $config) {
    if (empty($tmdbid) || $tmdbid === '0') return false;
    
    $content_type = ($is_tvseries == 1) ? 'tv' : 'movie';
    $api_url = "{$config['tmdb_base_url']}/{$content_type}/{$tmdbid}/images?api_key={$config['tmdb_api_key']}";
    
    $ch = curl_init();
    curl_setopt($ch, CURLOPT_URL, $api_url);
    curl_setopt($ch, CURLOPT_RETURNTRANSFER, true);
    curl_setopt($ch, CURLOPT_TIMEOUT, 10);
    curl_setopt($ch, CURLOPT_USERAGENT, 'TMDB-Logo-Downloader/1.0');
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
    curl_setopt($ch, CURLOPT_USERAGENT, 'Mozilla/5.0 (compatible; TMDB-Logo-Downloader)');
    curl_setopt($ch, CURLOPT_SSL_VERIFYPEER, false);
    
    $image_data = curl_exec($ch);
    $http_code = curl_getinfo($ch, CURLINFO_HTTP_CODE);
    curl_close($ch);
    
    if ($http_code !== 200 || !$image_data) return false;
    
    return file_put_contents($save_path, $image_data) !== false;
}

set_time_limit(0);
ini_set('memory_limit', '512M');

// =============================================================================
// CẤU HÌNH - THAY ĐỔI THEO SERVER
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
    'progress_file' => 'logo_download_progress.json',
    
    // Settings
    'batch_size' => 3,  // Giảm từ 5 xuống 3
    'delay_between_requests' => 1,  // Giảm delay
    'max_retries' => 3,
];

?>
<!DOCTYPE html>
<html>
<head>
    <title>🎬 TMDB Logo Downloader v2</title>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <style>
        body { font-family: Arial, sans-serif; margin: 20px; background: #f5f5f5; }
        .container { max-width: 1200px; margin: 0 auto; background: white; padding: 20px; border-radius: 10px; box-shadow: 0 2px 10px rgba(0,0,0,0.1); }
        .progress-bar { background: #e9ecef; border-radius: 10px; height: 30px; margin: 10px 0; position: relative; overflow: hidden; }
        .progress-fill { background: linear-gradient(45deg, #28a745, #20c997); height: 100%; border-radius: 10px; transition: width 0.3s ease; position: relative; }
        .progress-text { position: absolute; top: 50%; left: 50%; transform: translate(-50%, -50%); color: white; font-weight: bold; z-index: 2; }
        .stats { display: grid; grid-template-columns: repeat(auto-fit, minmax(200px, 1fr)); gap: 15px; margin: 20px 0; }
        .stat-card { background: linear-gradient(135deg, #667eea 0%, #764ba2 100%); color: white; padding: 15px; border-radius: 8px; text-align: center; }
        .stat-number { font-size: 2em; font-weight: bold; margin-bottom: 5px; }
        .stat-label { font-size: 0.9em; opacity: 0.9; }
        .log { background: #f8f9fa; border-radius: 5px; padding: 15px; height: 300px; overflow-y: auto; font-family: monospace; font-size: 12px; border: 1px solid #dee2e6; }
        .success { color: #28a745; } .warning { color: #ffc107; } .error { color: #dc3545; } .info { color: #17a2b8; }
        .btn { background: #007bff; color: white; padding: 10px 20px; border: none; border-radius: 5px; cursor: pointer; margin: 5px; }
        .btn:hover { background: #0056b3; }
        .btn-danger { background: #dc3545; } .btn-danger:hover { background: #c82333; }
        .btn-success { background: #28a745; } .btn-success:hover { background: #218838; }
        .version { background: #17a2b8; color: white; padding: 5px 10px; border-radius: 15px; font-size: 12px; margin-left: 10px; }
    </style>
</head>
<body>
    <div class="container">
        <h1>🎬 TMDB Logo Downloader <span class="version">v2 - Working</span></h1>
        
        <!-- Configuration Status -->
        <div style="background: #d4edda; padding: 10px; margin: 10px 0; border-radius: 5px; font-size: 14px;">
            <strong>📋 Configuration Status:</strong><br>
            • Database: <?php echo $config['database']; ?><br>
            • TMDB API Key: ✅ Configured<br>
            • Logo Directory: <?php echo $config['logo_directory']; ?><br>
            • Image Size: w300
        </div>
        
        <!-- Control Panel -->
        <div style="text-align: center; margin: 20px 0;">
            <button class="btn" onclick="testConfiguration()">🧪 Test Config</button>
            <button class="btn btn-success" onclick="startDownload()">▶️ Start Download</button>
            <button class="btn btn-danger" onclick="stopDownload()">⏸️ Stop Download</button>
            <button class="btn" onclick="resetProgress()">🔄 Reset Progress</button>
        </div>
        
        <!-- Progress Bar -->
        <div class="progress-bar">
            <div class="progress-fill" id="progressBar" style="width: 0%;">
                <div class="progress-text" id="progressText">0%</div>
            </div>
        </div>
        
        <!-- Statistics -->
        <div class="stats">
            <div class="stat-card">
                <div class="stat-number" id="totalMovies">0</div>
                <div class="stat-label">Total Movies</div>
            </div>
            <div class="stat-card">
                <div class="stat-number" id="processed">0</div>
                <div class="stat-label">Processed</div>
            </div>
            <div class="stat-card">
                <div class="stat-number" id="downloaded">0</div>
                <div class="stat-label">Downloaded</div>
            </div>
            <div class="stat-card">
                <div class="stat-number" id="skipped">0</div>
                <div class="stat-label">Skipped</div>
            </div>
            <div class="stat-card">
                <div class="stat-number" id="errors">0</div>
                <div class="stat-label">Errors</div>
            </div>
        </div>
        
        <!-- Live Log -->
        <h3>📋 Live Log</h3>
        <div class="log" id="logContainer"></div>
    </div>

    <script>
        console.log('🎉 TMDB Logo Downloader v2 - Working Version Loaded!');
        
        let isRunning = false;
        let currentOffset = 0;
        let stats = {
            total: 0,
            processed: 0,
            downloaded: 0,
            skipped: 0,
            errors: 0
        };

        function log(message, type = 'info') {
            const container = document.getElementById('logContainer');
            const time = new Date().toLocaleTimeString();
            const logEntry = document.createElement('div');
            logEntry.className = type;
            logEntry.innerHTML = `[${time}] ${message}`;
            container.appendChild(logEntry);
            container.scrollTop = container.scrollHeight;
        }

        function updateStats() {
            document.getElementById('totalMovies').textContent = stats.total;
            document.getElementById('processed').textContent = stats.processed;
            document.getElementById('downloaded').textContent = stats.downloaded;
            document.getElementById('skipped').textContent = stats.skipped;
            document.getElementById('errors').textContent = stats.errors;
            
            if (stats.total > 0) {
                const percent = Math.round((stats.processed / stats.total) * 100);
                document.getElementById('progressBar').style.width = percent + '%';
                document.getElementById('progressText').textContent = percent + '%';
            }
        }

        function startDownload() {
            if (isRunning) {
                log('⚠️ Download đã đang chạy!', 'warning');
                return;
            }
            
            isRunning = true;
            log('🚀 Bắt đầu tải logo từ TMDB...', 'success');
            processNextBatch();
        }

        function stopDownload() {
            isRunning = false;
            log('⏸️ Đã dừng download. Có thể tiếp tục sau.', 'warning');
        }

        function resetProgress() {
            if (isRunning) {
                log('❌ Không thể reset khi đang chạy!', 'error');
                return;
            }
            
            currentOffset = 0;
            stats = { total: 0, processed: 0, downloaded: 0, skipped: 0, errors: 0 };
            updateStats();
            document.getElementById('logContainer').innerHTML = '';
            log('🔄 Đã reset progress', 'info');
        }

        function testConfiguration() {
            log('🧪 Testing configuration...', 'info');
            
            fetch(window.location.href + '?action=test')
                .then(response => {
                    if (!response.ok) {
                        return response.text().then(text => {
                            throw new Error(`HTTP ${response.status}: ${text}`);
                        });
                    }
                    return response.json();
                })
                .then(data => {
                    if (data.error) {
                        log('❌ Config test failed: ' + data.error, 'error');
                        return;
                    }
                    
                    log('✅ Version: ' + (data.version || 'unknown'), 'info');
                    log('✅ Database: ' + data.database, 'success');
                    log('✅ API Key: ' + (data.api_key_set ? 'Configured' : 'NOT SET'), data.api_key_set ? 'success' : 'error');
                    log('✅ API Key Works: ' + (data.api_key_works ? 'YES' : 'NO'), data.api_key_works ? 'success' : 'warning');
                    log('✅ Logo Dir: ' + data.logo_directory, 'success');
                    log('✅ Directory Exists: ' + (data.directory_exists ? 'YES' : 'NO'), data.directory_exists ? 'success' : 'warning');
                    log('✅ Directory Writable: ' + (data.directory_writable ? 'YES' : 'NO'), data.directory_writable ? 'success' : 'warning');
                    log('✅ PHP Version: ' + data.php_version, 'info');
                    log('✅ cURL: ' + (data.curl_enabled ? 'Enabled' : 'Disabled'), data.curl_enabled ? 'success' : 'error');
                })
                .catch(error => {
                    log('❌ Config test error: ' + error.message, 'error');
                });
        }

        function processNextBatch() {
            if (!isRunning) return;
            
            console.log('🔍 Requesting batch at offset:', currentOffset);
            
            const controller = new AbortController();
            const timeoutId = setTimeout(() => controller.abort(), 60000); // 60 second timeout
            
            fetch(window.location.href + '?action=process_batch&offset=' + currentOffset, {
                signal: controller.signal
            })
                .then(response => {
                    clearTimeout(timeoutId);
                    if (!response.ok) {
                        throw new Error(`HTTP ${response.status}: ${response.statusText}`);
                    }
                    
                    // Check if response is empty
                    return response.text().then(text => {
                        if (!text || text.trim() === '') {
                            throw new Error('Empty response from server');
                        }
                        
                        try {
                            return JSON.parse(text);
                        } catch (e) {
                            console.error('JSON Parse Error:', e);
                            console.error('Response text:', text.substring(0, 200));
                            throw new Error(`Invalid JSON response: ${e.message}`);
                        }
                    });
                })
                .then(data => {
                    console.log('📦 Received batch data:', data);
                    
                    if (data.error) {
                        log('❌ Error: ' + data.error, 'error');
                        isRunning = false;
                        return;
                    }
                    
                    // Update stats
                    stats.total = data.total;
                    stats.processed = data.processed;
                    stats.downloaded = data.downloaded;
                    stats.skipped = data.skipped;
                    stats.errors = data.errors;
                    updateStats();
                    
                    // Log results
                    if (data.results) {
                        data.results.forEach(result => {
                            log(result.message, result.type);
                        });
                    }
                    
                    // Continue if not finished
                    if (data.has_more && isRunning) {
                        currentOffset = data.next_offset;
                        setTimeout(processNextBatch, 1500); // 1.5 second delay
                    } else {
                        isRunning = false;
                        log('🎉 Hoàn thành tất cả!', 'success');
                    }
                })
                .catch(error => {
                    clearTimeout(timeoutId);
                    
                    if (error.name === 'AbortError') {
                        log('⏰ Request timeout - retrying in 5 seconds...', 'warning');
                        setTimeout(processNextBatch, 5000);
                    } else {
                        log('❌ Network error: ' + error.message, 'error');
                        log('🔄 Auto-retry in 10 seconds...', 'info');
                        setTimeout(processNextBatch, 10000);
                    }
                });
        }

        // Load initial stats
        fetch(window.location.href + '?action=get_stats')
            .then(response => {
                if (!response.ok) {
                    throw new Error(`HTTP ${response.status}: ${response.statusText}`);
                }
                return response.json();
            })
            .then(data => {
                if (data.error) {
                    log('❌ Error loading stats: ' + data.error, 'error');
                    return;
                }
                stats.total = data.total;
                updateStats();
                log('📊 Loaded: ' + data.total + ' movies total', 'info');
            })
            .catch(error => {
                log('❌ Failed to load stats: ' + error.message, 'error');
            });
    </script>
</body>
</html>