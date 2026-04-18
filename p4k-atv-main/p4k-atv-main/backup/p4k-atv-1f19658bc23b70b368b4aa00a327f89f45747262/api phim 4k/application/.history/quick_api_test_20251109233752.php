<?php
/**
 * Quick API test using curl với endpoint đúng
 */

echo "<h2>🔍 Quick API Test - REST API v130</h2>\n";

// Test multiple endpoints để tìm đúng format
$test_urls = [
    // Movies search
    "https://api.phim4k.lol/rest-api/v130?api_key=bbbb411dea44849&q=duong%20trieu&limit=5",
    "https://api.phim4k.lol/rest-api/v130?api_key=bbbb411dea44849&q=strange&limit=3",
    
    // Có thể cần parameter khác
    "https://api.phim4k.lol/rest-api/v130/movies?api_key=bbbb411dea44849&q=duong%20trieu&limit=5", 
    "https://api.phim4k.lol/rest-api/v130/movies?api_key=bbbb411dea44849&q=strange&limit=3",
    
    // Get all movies để xem cấu trúc
    "https://api.phim4k.lol/rest-api/v130?api_key=bbbb411dea44849&limit=2",
    "https://api.phim4k.lol/rest-api/v130/movies?api_key=bbbb411dea44849&limit=2",
];

foreach($test_urls as $url) {
    echo "<div style='background:#f8f9fa; padding:15px; margin:10px 0; border-radius:8px;'>";
    echo "<strong>Testing:</strong> {$url}<br><br>";

    $ch = curl_init();
    curl_setopt($ch, CURLOPT_URL, $url);
    curl_setopt($ch, CURLOPT_RETURNTRANSFER, true);
    curl_setopt($ch, CURLOPT_TIMEOUT, 30);
    curl_setopt($ch, CURLOPT_FOLLOWLOCATION, true);
    curl_setopt($ch, CURLOPT_SSL_VERIFYPEER, false);
    curl_setopt($ch, CURLOPT_HTTPHEADER, array(
        'Accept: application/json',
        'User-Agent: Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36'
    ));

    $response = curl_exec($ch);
    $http_code = curl_getinfo($ch, CURLINFO_HTTP_CODE);
    $error = curl_error($ch);
    curl_close($ch);

    echo "<strong>HTTP Code:</strong> {$http_code}<br>";

    if($error) {
        echo "<strong>CURL Error:</strong> {$error}<br>";
    }

    if($response) {
        $json = json_decode($response, true);
        
        if($json) {
            echo "<span style='color:green'>✅ JSON Response received</span><br>";
            
            // Kiểm tra structure
            if(isset($json['movies'])) {
                echo "<strong>Movies found:</strong> " . count($json['movies']) . "<br>";
                
                if(count($json['movies']) > 0) {
                    $movie = $json['movies'][0];
                    echo "<strong>Sample movie:</strong> {$movie['title']}<br>";
                    
                    // Kiểm tra cột mới
                    echo "<strong>Columns check:</strong><br>";
                    echo "- title_vietnamese: " . (isset($movie['title_vietnamese']) ? '✅' : '❌') . "<br>";
                    echo "- title_vietnamese_no_accent: " . (isset($movie['title_vietnamese_no_accent']) ? '✅' : '❌') . "<br>";
                    echo "- title_original: " . (isset($movie['title_original']) ? '✅' : '❌') . "<br>";
                }
            } else {
                echo "<strong>Response keys:</strong> " . implode(', ', array_keys($json)) . "<br>";
                echo "<pre>" . json_encode($json, JSON_PRETTY_PRINT | JSON_UNESCAPED_UNICODE) . "</pre>";
            }
        } else {
            echo "<span style='color:orange'>⚠️ Non-JSON response</span><br>";
            echo "<strong>Raw response:</strong><br>";
            echo "<pre>" . htmlspecialchars(substr($response, 0, 1000)) . "</pre>";
        }
    } else {
        echo "<span style='color:red'>❌ No response received</span><br>";
    }
    
    echo "</div>";
}
?>