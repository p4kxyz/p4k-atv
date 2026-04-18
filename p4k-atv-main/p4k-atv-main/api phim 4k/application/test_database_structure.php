<?php
/**
 * Test database structure để kiểm tra có cột tmdb_id không
 */

$api_key = "bbbb411dea44849";
$base_url = "https://api.phim4k.lol";

echo "<h2>🔍 Test Database Structure - Kiểm tra cột tmdb_id</h2>\n";

function testAPI($url) {
    $ch = curl_init();
    curl_setopt($ch, CURLOPT_URL, $url);
    curl_setopt($ch, CURLOPT_RETURNTRANSFER, true);
    curl_setopt($ch, CURLOPT_TIMEOUT, 30);
    
    $headers = [
        'Accept: application/json',
        'User-Agent: Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36'
    ];
    
    // Thêm API-KEY vào header
    $headers[] = 'API-KEY: bbbb411dea44849';
    
    curl_setopt($ch, CURLOPT_HTTPHEADER, $headers);
    
    $response = curl_exec($ch);
    $http_code = curl_getinfo($ch, CURLINFO_HTTP_CODE);
    curl_close($ch);
    
    return [
        'http_code' => $http_code,
        'response' => $response,
        'json' => json_decode($response, true)
    ];
}

// Test latest_movies để xem cấu trúc movie record
echo "<h3>📋 Test latest_movies để xem cấu trúc record:</h3>";
$url = "{$base_url}/rest-api/v130/latest_movies?api_key={$api_key}&limit=1";
$result = testAPI($url);

if($result['http_code'] == 200 && $result['json']) {
    if(isset($result['json'][0])) {
        $movie = $result['json'][0];
        echo "<div style='background:#e8f5e8; padding:15px; margin:10px 0; border-radius:8px;'>";
        echo "<strong>✅ Cấu trúc Movie Record:</strong><br>";
        echo "<pre>";
        foreach($movie as $key => $value) {
            echo "$key: " . (is_string($value) ? substr($value, 0, 100) : $value) . "\n";
        }
        echo "</pre>";
        
        if(isset($movie['tmdb_id'])) {
            echo "<span style='color:green; font-weight:bold'>✅ CÓ cột tmdb_id: " . $movie['tmdb_id'] . "</span><br>";
        } else {
            echo "<span style='color:red; font-weight:bold'>❌ KHÔNG có cột tmdb_id</span><br>";
        }
        echo "</div>";
    }
} else {
    echo "<span style='color:red'>❌ Lỗi khi test latest_movies</span><br>";
}

// Test single movie details để xem có chi tiết hơn không
echo "<h3>🔍 Test single movie details:</h3>";
$url = "{$base_url}/rest-api/v130/single_details?api_key={$api_key}&type=movie&id=1";
$result = testAPI($url);

if($result['http_code'] == 200 && $result['json']) {
    echo "<div style='background:#fff3cd; padding:15px; margin:10px 0; border-radius:8px;'>";
    echo "<strong>📋 Cấu trúc Single Movie Details:</strong><br>";
    echo "<pre>";
    foreach($result['json'] as $key => $value) {
        if(is_array($value)) {
            echo "$key: [array with " . count($value) . " items]\n";
        } else {
            echo "$key: " . (is_string($value) ? substr($value, 0, 100) : $value) . "\n";
        }
    }
    echo "</pre>";
    
    if(isset($result['json']['tmdb_id'])) {
        echo "<span style='color:green; font-weight:bold'>✅ CÓ cột tmdb_id: " . $result['json']['tmdb_id'] . "</span><br>";
    } else {
        echo "<span style='color:red; font-weight:bold'>❌ KHÔNG có cột tmdb_id trong single details</span><br>";
    }
    echo "</div>";
} else {
    echo "<span style='color:red'>❌ Lỗi khi test single movie details</span><br>";
}

echo "<h3>🎯 Kết luận:</h3>";
echo "<p>Từ kết quả trên, chúng ta có thể xác định xem database có cột <code>tmdb_id</code> hay không.</p>";
?>