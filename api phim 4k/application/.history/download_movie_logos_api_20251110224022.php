<?php
/**
 * API Endpoints for TMDB Logo Downloader
 * This file handles all API requests separately from the HTML interface
 */

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