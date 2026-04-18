<?php
/**
 * Simple Debug Script to Isolate HTTP 500 Issue
 */

// Enable all error reporting
error_reporting(E_ALL);
ini_set('display_errors', 1);
ini_set('log_errors', 1);

// Configuration
$config = [
    'host' => 'localhost',
    'username' => 'root',
    'password' => '5e6fdc4c3e2dddb9',
    'database' => 'sql_api_phim4k_l',
    'tmdb_api_key' => 'f7cde166455f2044415520dbbcfae843',
];

// Set JSON header
header('Content-Type: application/json');
header('Cache-Control: no-cache');

try {
    echo "Step 1: Starting debug...\n";
    
    // Test database connection
    echo "Step 2: Testing database...\n";
    $pdo = new PDO("mysql:host={$config['host']};dbname={$config['database']};charset=utf8mb4", 
                  $config['username'], $config['password']);
    $pdo->setAttribute(PDO::ATTR_ERRMODE, PDO::ERRMODE_EXCEPTION);
    echo "Step 3: Database connected OK\n";
    
    // Test simple query
    echo "Step 4: Testing simple query...\n";
    $stmt = $pdo->query("SELECT COUNT(*) as total FROM videos WHERE publication = 1 LIMIT 1");
    $result = $stmt->fetch();
    $total = $result['total'] ?? 0;
    echo "Step 5: Found {$total} published videos\n";
    
    // Test tmdbid query
    echo "Step 6: Testing tmdbid query...\n";
    $stmt = $pdo->query("
        SELECT COUNT(*) as total 
        FROM videos 
        WHERE (tmdbid IS NOT NULL AND tmdbid != '' AND tmdbid != '0')
        AND publication = 1
        LIMIT 1
    ");
    $result = $stmt->fetch();
    $tmdb_total = $result['total'] ?? 0;
    echo "Step 7: Found {$tmdb_total} movies with TMDB ID\n";
    
    // Test getting first 2 movies
    echo "Step 8: Getting first 2 movies...\n";
    $stmt = $pdo->prepare("
        SELECT videos_id, tmdbid, title, is_tvseries
        FROM videos 
        WHERE (tmdbid IS NOT NULL AND tmdbid != '' AND tmdbid != '0')
        AND publication = 1
        ORDER BY videos_id ASC
        LIMIT 2
    ");
    $stmt->execute();
    $movies = $stmt->fetchAll(PDO::FETCH_ASSOC);
    echo "Step 9: Retrieved " . count($movies) . " movies\n";
    
    foreach ($movies as $i => $movie) {
        echo "Movie " . ($i+1) . ": ID={$movie['videos_id']}, TMDB={$movie['tmdbid']}, Title=" . substr($movie['title'], 0, 30) . "\n";
    }
    
    echo "Step 10: All tests completed successfully!\n";
    
    // Return JSON response
    echo json_encode([
        'status' => 'success',
        'total_videos' => $total,
        'tmdb_videos' => $tmdb_total,
        'sample_movies' => $movies,
        'message' => 'Debug completed successfully'
    ]);
    
} catch (Exception $e) {
    http_response_code(500);
    echo json_encode([
        'error' => 'Debug failed: ' . $e->getMessage(),
        'file' => basename($e->getFile()),
        'line' => $e->getLine(),
        'trace' => $e->getTraceAsString()
    ]);
}
?>