<?php
/**
 * Test API endpoints để tìm hiểu cấu trúc
 */

$api_key = "bbbb411dea44849";
$base_url = "https://api.phim4k.lol";

echo "<h2>🔍 Test API Endpoints</h2>\n";

function testAPI($url) {
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
    curl_close($ch);
    
    return [
        'http_code' => $http_code,
        'response' => $response,
        'json' => json_decode($response, true)
    ];
}

// Test các endpoints đúng với REST API
$endpoints = [
    // REST API v130 - Movies 
    "/rest-api/v130?api_key={$api_key}&limit=2",
    "/rest-api/v130?api_key={$api_key}&q=strange&limit=2", 
    "/rest-api/v130?api_key={$api_key}&q=duong%20trieu&limit=2",
    "/rest-api/v130?api_key={$api_key}&q=Đường%20Triều&limit=2",
    
    // REST API v100
    "/rest-api/v100?api_key={$api_key}&limit=2",
    "/rest-api/v100?api_key={$api_key}&q=strange&limit=2",
    "/rest-api/v100?api_key={$api_key}&q=duong%20trieu&limit=2",
    
    // Có thể cần thêm /movies
    "/rest-api/v130/movies?api_key={$api_key}&limit=2",
    "/rest-api/v130/movies?api_key={$api_key}&q=strange&limit=2",
    
    // TV Series
    "/rest-api/v130/tvseries?api_key={$api_key}&limit=2",
];

foreach($endpoints as $endpoint) {
    $full_url = $base_url . $endpoint;
    echo "<div style='background:#f8f9fa; padding:15px; margin:10px 0; border-radius:8px; border-left:4px solid #007bff;'>";
    echo "<strong>Testing:</strong> {$endpoint}<br>";
    
    $result = testAPI($full_url);
    
    echo "<strong>HTTP Code:</strong> {$result['http_code']}<br>";
    
    if($result['http_code'] == 200) {
        echo "<span style='color:green'>✅ SUCCESS</span><br>";
        
        if($result['json']) {
            // Kiểm tra structure của response
            if(isset($result['json']['movies'])) {
                $movies = $result['json']['movies'];
                echo "<strong>Movies found:</strong> " . count($movies) . "<br>";
                
                if(count($movies) > 0) {
                    $movie = $movies[0];
                    echo "<strong>Sample movie:</strong><br>";
                    echo "- ID: " . ($movie['videos_id'] ?? 'N/A') . "<br>";
                    echo "- Title: " . ($movie['title'] ?? 'N/A') . "<br>";
                    echo "- Slug: " . ($movie['slug'] ?? 'N/A') . "<br>";
                    
                    // Kiểm tra nếu có các cột mới
                    if(isset($movie['title_vietnamese'])) {
                        echo "- Vietnamese: " . $movie['title_vietnamese'] . "<br>";
                    } else {
                        echo "- Vietnamese: <span style='color:red'>NOT FOUND</span><br>";
                    }
                    
                    if(isset($movie['title_vietnamese_no_accent'])) {
                        echo "- Vietnamese (no accent): " . $movie['title_vietnamese_no_accent'] . "<br>";
                    } else {
                        echo "- Vietnamese (no accent): <span style='color:red'>NOT FOUND</span><br>";
                    }
                    
                    if(isset($movie['title_original'])) {
                        echo "- Original: " . $movie['title_original'] . "<br>";
                    } else {
                        echo "- Original: <span style='color:red'>NOT FOUND</span><br>";
                    }
                }
            } else {
                echo "<strong>Response structure:</strong><br>";
                echo "<pre>" . json_encode($result['json'], JSON_PRETTY_PRINT | JSON_UNESCAPED_UNICODE) . "</pre>";
            }
        } else {
            echo "<strong>Raw response:</strong><br>";
            echo "<pre>" . htmlspecialchars(substr($result['response'], 0, 500)) . "</pre>";
        }
    } else {
        echo "<span style='color:red'>❌ FAILED</span><br>";
        if(!empty($result['response'])) {
            echo "<strong>Error:</strong> " . htmlspecialchars(substr($result['response'], 0, 200)) . "<br>";
        }
    }
    
    echo "</div>";
}

// Test specific movie tìm "Đường Triều"
echo "<h3>🎯 Test tìm phim có chữ 'Đường Triều':</h3>";

$search_terms = [
    'Đường Triều',
    'duong trieu', 
    'Strange Tales',
    'Tang Dynasty'
];

foreach($search_terms as $term) {
    $encoded_term = urlencode($term);
    $url = "{$base_url}/rest-api/v130?api_key={$api_key}&q={$encoded_term}&limit=5";
    
    echo "<div style='background:#e3f2fd; padding:10px; margin:5px 0; border-radius:5px;'>";
    echo "<strong>Search term:</strong> '{$term}' <br>";
    echo "<strong>URL:</strong> {$url}<br>";
    
    $result = testAPI($url);
    echo "<strong>HTTP Code:</strong> {$result['http_code']}<br>";
    
    if($result['http_code'] == 200) {
        if($result['json'] && isset($result['json']['movies'])) {
            $count = count($result['json']['movies']);
            echo "<strong>Results:</strong> {$count} movies<br>";
            
            if($count > 0) {
                foreach($result['json']['movies'] as $movie) {
                    echo "- {$movie['title']}<br>";
                }
            }
        } else if($result['json']) {
            echo "<strong>JSON Response:</strong><br>";
            echo "<pre>" . json_encode($result['json'], JSON_PRETTY_PRINT | JSON_UNESCAPED_UNICODE) . "</pre>";
        } else {
            echo "<strong>Raw Response:</strong><br>";
            echo "<pre>" . htmlspecialchars(substr($result['response'], 0, 300)) . "</pre>";
        }
    } else {
        echo "<span style='color:red'>HTTP Error: {$result['http_code']}</span><br>";
    }
    echo "</div>";
}
?>