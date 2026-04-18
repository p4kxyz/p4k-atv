<?php
/**
 * TMDB Logo Downloader - ADVANCED VERSION
 * Tải logo phim từ TMDB API với progress bar real-time và khôi phục
 * Ưu tiên logo tiếng Việt, không có thì lấy tiếng Anh
 */

// =============================================================================
// HANDLE API ENDPOINTS FIRST (BEFORE ANY OUTPUT)
// =============================================================================
if (isset($_GET['action'])) {
    // Enable error reporting for debugging API calls
    error_reporting(E_ALL);
    ini_set('display_errors', 0); // Don't display errors in JSON response
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
    
    // Include the API handling code
    include 'download_movie_logos_api.php';
    exit; // Stop execution after API response
}

set_time_limit(0);
ini_set('memory_limit', '512M');

// =============================================================================
// CẤU HÌNH - THAY ĐỔI THEO SERVER
// =============================================================================
$config = [
    // Database
    'host' => 'localhost',
    'username' => 'root',           // THAY ĐỔI
    'password' => '5e6fdc4c3e2dddb9',               // THAY ĐỔI  
    'database' => 'sql_api_phim4k_l',
    
    // TMDB API
    'tmdb_api_key' => 'f7cde166455f2044415520dbbcfae843', // THAY ĐỔI API KEY
    'tmdb_base_url' => 'https://api.themoviedb.org/3',
    'tmdb_image_base_url' => 'https://image.tmdb.org/t/p/w300', // w45, w92, w154, w185, w300, w500, original
    
    // Paths
    'logo_directory' => '/www/wwwroot/api.phim4k.lol/uploads/logo/',
    'progress_file' => 'logo_download_progress.json',
    
    // Settings
    'batch_size' => 5, // Giảm xuống để tránh timeout
    'delay_between_requests' => 2, // seconds
    'max_retries' => 3,
];

?>
<!DOCTYPE html>
<html>
<head>
    <title>🎬 TMDB Logo Downloader</title>
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
    </style>
</head>
<body>
    <div class="container">
        <h1>🎬 TMDB Logo Downloader - Advanced</h1>
        
        <!-- Configuration Status -->
        <div style="background: <?php echo (empty($config['tmdb_api_key']) || $config['tmdb_api_key'] === 'your_tmdb_api_key_here') ? '#f8d7da' : '#d4edda'; ?>; 
                    padding: 10px; margin: 10px 0; border-radius: 5px; font-size: 14px;">
            <strong>📋 Configuration Status:</strong><br>
            • Database: <?php echo $config['database']; ?><br>
            • TMDB API Key: <?php echo (empty($config['tmdb_api_key']) || $config['tmdb_api_key'] === 'your_tmdb_api_key_here') ? '❌ Not configured' : '✅ Configured'; ?><br>
            • Logo Directory: <?php echo $config['logo_directory']; ?><br>
            • Image Size: <?php echo str_replace('https://image.tmdb.org/t/p/', '', $config['tmdb_image_base_url']); ?>
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
        
        <!-- Status -->
        <div id="status" style="margin-top: 20px; padding: 15px; border-radius: 8px; display: none;"></div>
    </div>

    <script>
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
            
            fetch('<?php echo $_SERVER['PHP_SELF']; ?>?action=test')
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
            
            fetch('<?php echo $_SERVER['PHP_SELF']; ?>?action=process_batch&offset=' + currentOffset)
                .then(response => {
                    // Check if response is OK
                    if (!response.ok) {
                        throw new Error(`HTTP ${response.status}: ${response.statusText}`);
                    }
                    // Check if response is JSON
                    const contentType = response.headers.get('content-type');
                    if (!contentType || !contentType.includes('application/json')) {
                        return response.text().then(text => {
                            throw new Error(`Expected JSON but got: ${text.substring(0, 100)}...`);
                        });
                    }
                    return response.json();
                })
                .then(data => {
                    if (data.error) {
                        log('❌ Error: ' + data.error, 'error');
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
                        setTimeout(processNextBatch, 1000); // 1 second delay
                    } else {
                        isRunning = false;
                        log('🎉 Hoàn thành tất cả!', 'success');
                    }
                })
                .catch(error => {
                    log('❌ Network error: ' + error.message, 'error');
                    isRunning = false;
                });
        }

        // Load initial stats
        fetch('<?php echo $_SERVER['PHP_SELF']; ?>?action=get_stats')
            .then(response => {
                if (!response.ok) {
                    throw new Error(`HTTP ${response.status}: ${response.statusText}`);
                }
                const contentType = response.headers.get('content-type');
                if (!contentType || !contentType.includes('application/json')) {
                    return response.text().then(text => {
                        throw new Error(`Expected JSON but got: ${text.substring(0, 200)}...`);
                    });
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

<?php
// =============================================================================
// API ENDPOINTS
// =============================================================================

if (isset($_GET['action'])) {
    // Clear any previous output and set JSON header
    ob_end_clean();
    ob_start();
    header('Content-Type: application/json');
    header('Cache-Control: no-cache');
    
    try {
        // Validate config only for process_batch action
        if ($_GET['action'] === 'process_batch' && (empty($config['tmdb_api_key']) || $config['tmdb_api_key'] === 'your_tmdb_api_key_here')) {
            http_response_code(400);
            echo json_encode(['error' => 'TMDB API key not configured. Please set your API key in the config.']);
            exit;
        }
        
        // Test database connection
        try {
            $pdo = new PDO("mysql:host={$config['host']};dbname={$config['database']};charset=utf8mb4", 
                          $config['username'], $config['password']);
            $pdo->setAttribute(PDO::ATTR_ERRMODE, PDO::ERRMODE_EXCEPTION);
        } catch (PDOException $e) {
            http_response_code(500);
            echo json_encode(['error' => 'Database connection failed: ' . $e->getMessage()]);
            exit;
        }
        
        switch ($_GET['action']) {
            case 'test':
                // Test TMDB API
                $tmdb_test = false;
                if (!empty($config['tmdb_api_key']) && $config['tmdb_api_key'] !== 'your_tmdb_api_key_here') {
                    $test_url = $config['tmdb_base_url'] . '/configuration?api_key=' . $config['tmdb_api_key'];
                    $ch = curl_init();
                    curl_setopt($ch, CURLOPT_URL, $test_url);
                    curl_setopt($ch, CURLOPT_RETURNTRANSFER, true);
                    curl_setopt($ch, CURLOPT_TIMEOUT, 5);
                    $response = curl_exec($ch);
                    $http_code = curl_getinfo($ch, CURLINFO_HTTP_CODE);
                    curl_close($ch);
                    $tmdb_test = ($http_code === 200);
                }
                
                echo json_encode([
                    'status' => 'ok',
                    'database' => $config['database'],
                    'api_key_set' => !empty($config['tmdb_api_key']) && $config['tmdb_api_key'] !== 'your_tmdb_api_key_here',
                    'api_key_works' => $tmdb_test,
                    'logo_directory' => $config['logo_directory'],
                    'directory_exists' => file_exists($config['logo_directory']),
                    'directory_writable' => is_writable(dirname($config['logo_directory'])),
                    'php_version' => PHP_VERSION,
                    'curl_enabled' => function_exists('curl_init')
                ]);
                break;
                
            case 'get_stats':
                getStats($pdo);
                break;
                
            case 'process_batch':
                processBatch($pdo, $config);
                break;
                
            case 'debug_simple':
                // Simple debug endpoint
                $stmt = $pdo->query("SELECT COUNT(*) as total FROM videos WHERE publication = 1 LIMIT 1");
                $result = $stmt->fetch();
                echo json_encode([
                    'status' => 'debug_ok',
                    'total_videos' => $result['total'] ?? 0,
                    'timestamp' => date('Y-m-d H:i:s')
                ]);
                break;
                
            default:
                http_response_code(400);
                echo json_encode(['error' => 'Unknown action: ' . $_GET['action']]);
        }
    } catch (Exception $e) {
        http_response_code(500);
        echo json_encode([
            'error' => 'Server error: ' . $e->getMessage(),
            'file' => basename($e->getFile()),
            'line' => $e->getLine()
        ]);
    }
    exit;
}

function getStats($pdo) {
    try {
        $stmt = $pdo->query("
            SELECT COUNT(*) as total 
            FROM videos 
            WHERE (tmdbid IS NOT NULL AND tmdbid != '' AND tmdbid != '0')
            AND publication = 1
        ");
        
        $result = $stmt->fetch();
        $total = (int)($result['total'] ?? 0);
        
        echo json_encode([
            'total' => $total,
            'status' => 'success'
        ]);
    } catch (Exception $e) {
        http_response_code(500);
        echo json_encode([
            'error' => 'Database query failed: ' . $e->getMessage()
        ]);
    }
}

function processBatch($pdo, $config) {
    try {
        $offset = (int)($_GET['offset'] ?? 0);
        $batch_size = $config['batch_size'];
        
        // Create logo directory if not exists
        if (!file_exists($config['logo_directory'])) {
            if (!mkdir($config['logo_directory'], 0755, true)) {
                throw new Exception("Cannot create logo directory: " . $config['logo_directory']);
            }
        }
        
        // Verify directory is writable
        if (!is_writable($config['logo_directory'])) {
            throw new Exception("Logo directory is not writable: " . $config['logo_directory']);
        }
    
    // Get total count
    $count_stmt = $pdo->query("
        SELECT COUNT(*) as total 
        FROM videos 
        WHERE (tmdbid IS NOT NULL AND tmdbid != '' AND tmdbid != '0')
        AND publication = 1
    ");
    $total = (int)$count_stmt->fetch()['total'];
    
    // Get batch
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
    
    $results = [];
    $downloaded = 0;
    $skipped = 0;
    $errors = 0;
    
    foreach ($movies as $movie) {
        $videos_id = $movie['videos_id'];
        $tmdbid = $movie['tmdbid'];
        $title = $movie['title'];
        $is_tvseries = $movie['is_tvseries'];
        
        $logo_path = $config['logo_directory'] . $videos_id . '.jpg';
        
        // Skip if file exists
        if (file_exists($logo_path)) {
            $results[] = [
                'message' => "⏭️ Skip #{$videos_id}: {$title} (logo đã có)",
                'type' => 'info'
            ];
            $skipped++;
            continue;
        }
        
        try {
            // Get logo from TMDB
            $logo_url = getTMDBLogo($tmdbid, $is_tvseries, $config);
            
            if ($logo_url) {
                if (downloadImage($logo_url, $logo_path)) {
                    $results[] = [
                        'message' => "✅ Downloaded #{$videos_id}: {$title}",
                        'type' => 'success'
                    ];
                    $downloaded++;
                } else {
                    $results[] = [
                        'message' => "❌ Download failed #{$videos_id}: {$title}",
                        'type' => 'error'
                    ];
                    $errors++;
                }
            } else {
                $results[] = [
                    'message' => "⚠️ No logo #{$videos_id}: {$title}",
                    'type' => 'warning'
                ];
                $skipped++;
            }
        } catch (Exception $e) {
            $results[] = [
                'message' => "❌ Error #{$videos_id}: {$title} - " . $e->getMessage(),
                'type' => 'error'
            ];
            $errors++;
        }
        
        // Small delay
        usleep($config['delay_between_requests'] * 1000000); // Convert to microseconds
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
        http_response_code(500);
        echo json_encode([
            'error' => 'Batch processing failed: ' . $e->getMessage(),
            'file' => basename($e->getFile()),
            'line' => $e->getLine(),
            'offset' => $offset ?? 0
        ]);
    }
}

function getTMDBLogo($tmdbid, $is_tvseries, $config) {
    if (empty($tmdbid) || $tmdbid === '0') return false;
    
    $content_type = ($is_tvseries == 1) ? 'tv' : 'movie';
    $api_url = "{$config['tmdb_base_url']}/{$content_type}/{$tmdbid}/images?api_key={$config['tmdb_api_key']}";
    
    // Use cURL instead of file_get_contents for better error handling
    $ch = curl_init();
    curl_setopt($ch, CURLOPT_URL, $api_url);
    curl_setopt($ch, CURLOPT_RETURNTRANSFER, true);
    curl_setopt($ch, CURLOPT_TIMEOUT, 15);
    curl_setopt($ch, CURLOPT_CONNECTTIMEOUT, 10);
    curl_setopt($ch, CURLOPT_USERAGENT, 'Mozilla/5.0 (compatible; TMDB-Logo-Downloader/1.0)');
    curl_setopt($ch, CURLOPT_FOLLOWLOCATION, true);
    curl_setopt($ch, CURLOPT_SSL_VERIFYPEER, false);
    
    $response = curl_exec($ch);
    $http_code = curl_getinfo($ch, CURLINFO_HTTP_CODE);
    $curl_error = curl_error($ch);
    curl_close($ch);
    
    if ($curl_error) {
        throw new Exception("cURL error for TMDB ID {$tmdbid}: {$curl_error}");
    }
    
    if ($http_code !== 200 || !$response) {
        if ($http_code === 404) return false; // Not found is OK
        throw new Exception("TMDB API error for ID {$tmdbid}: HTTP {$http_code}");
    }
    
    $data = json_decode($response, true);
    if (!$data || !isset($data['logos']) || empty($data['logos'])) return false;
    
    $logos = $data['logos'];
    
    // Priority: Vietnamese -> English -> No language -> Any
    $priorities = ['vi', 'en', null];
    
    foreach ($priorities as $lang) {
        foreach ($logos as $logo) {
            $logo_lang = $logo['iso_639_1'] ?? null;
            if ($logo_lang === $lang) {
                return $config['tmdb_image_base_url'] . $logo['file_path'];
            }
        }
    }
    
    // Fallback: first logo
    return !empty($logos) ? $config['tmdb_image_base_url'] . $logos[0]['file_path'] : false;
}

function downloadImage($url, $save_path) {
    $ch = curl_init();
    curl_setopt($ch, CURLOPT_URL, $url);
    curl_setopt($ch, CURLOPT_RETURNTRANSFER, true);
    curl_setopt($ch, CURLOPT_FOLLOWLOCATION, true);
    curl_setopt($ch, CURLOPT_TIMEOUT, 30);
    curl_setopt($ch, CURLOPT_CONNECTTIMEOUT, 10);
    curl_setopt($ch, CURLOPT_USERAGENT, 'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36');
    curl_setopt($ch, CURLOPT_SSL_VERIFYPEER, false);
    
    $image_data = curl_exec($ch);
    $http_code = curl_getinfo($ch, CURLINFO_HTTP_CODE);
    $curl_error = curl_error($ch);
    curl_close($ch);
    
    if ($curl_error) {
        throw new Exception("Image download cURL error: {$curl_error}");
    }
    
    if ($http_code !== 200 || !$image_data) {
        throw new Exception("Image download failed: HTTP {$http_code}");
    }
    
    $result = file_put_contents($save_path, $image_data);
    if ($result === false) {
        throw new Exception("Failed to save image to: {$save_path}");
    }
    
    return true;
}
?>