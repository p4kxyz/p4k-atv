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
    echo "<strong>Error:</strong> {$error}<br>";
}

if($response) {
    $json = json_decode($response, true);
    
    if($json) {
        echo "<strong>Response structure:</strong><br>";
        echo "<pre>" . json_encode($json, JSON_PRETTY_PRINT | JSON_UNESCAPED_UNICODE) . "</pre>";
        
        if(isset($json['movies'])) {
            echo "<br><strong>Movies found:</strong> " . count($json['movies']) . "<br>";
        }
    } else {
        echo "<strong>Raw response:</strong><br>";
        echo "<pre>" . htmlspecialchars($response) . "</pre>";
    }
} else {
    echo "<span style='color:red'>No response received</span>";
}

// Test với "strange" để so sánh
echo "<hr><h3>Test với 'strange' để so sánh:</h3>";

$url2 = "https://api.phim4k.lol/api/v1.3.0/movies?api_key=bbbb411dea44849&q=strange&limit=3";

$ch2 = curl_init();
curl_setopt($ch2, CURLOPT_URL, $url2);
curl_setopt($ch2, CURLOPT_RETURNTRANSFER, true);
curl_setopt($ch2, CURLOPT_TIMEOUT, 30);

$response2 = curl_exec($ch2);
$http_code2 = curl_getinfo($ch2, CURLINFO_HTTP_CODE);
curl_close($ch2);

echo "<strong>HTTP Code:</strong> {$http_code2}<br>";

if($response2) {
    $json2 = json_decode($response2, true);
    if($json2 && isset($json2['movies'])) {
        echo "<strong>Movies found:</strong> " . count($json2['movies']) . "<br>";
        foreach($json2['movies'] as $movie) {
            echo "- {$movie['title']}<br>";
        }
    }
}
?>