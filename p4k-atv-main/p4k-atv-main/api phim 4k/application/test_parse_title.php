<?php
/**
 * Test script để kiểm tra parse title function
 */

require_once('helpers/custom_helper.php');

// Test cases
$test_titles = array(
    'Sự Hoán Đổi Kỳ Diệu (2002) The Hot Chick',
    'Người Nhện: Không Còn Nhà (2021) Spider-Man: No Way Home',
    'Avengers: Endgame (2019)',
    'Cô Gái Đan Mạch (2015) The Danish Girl',
    'Parasite (2019)',
    'Your Name (2016)',
    'Spirited Away',
    'Thế Giới Cổ Tích (2023)',
    'Fast & Furious Presents: Hobbs & Shaw (2019)'
);

echo "=== TEST PARSE MOVIE TITLE ===\n\n";

foreach($test_titles as $title) {
    echo "Original: {$title}\n";
    $parsed = parse_movie_title($title);
    echo "  Vietnamese: " . ($parsed['vietnamese'] ?: 'NULL') . "\n";
    echo "  Vietnamese (no accent): " . ($parsed['vietnamese_no_accent'] ?: 'NULL') . "\n";
    echo "  Original: " . ($parsed['original'] ?: 'NULL') . "\n";
    echo "---\n";
}

echo "\n=== TEST SEARCH SCENARIOS ===\n\n";

// Test search scenarios
$search_queries = array(
    'su hoan doi',
    'hoan doi ky dieu', 
    'Sự Hoán Đổi',
    'the hot chick',
    'nguoi nhen',
    'spider man',
    'avengers',
    'parasite',
    'spirited away'
);

foreach($search_queries as $query) {
    echo "Search: '{$query}'\n";
    $normalized = remove_vietnamese_accents($query);
    echo "  Normalized: '{$normalized}'\n";
    
    // Tìm trong test titles
    $found = array();
    foreach($test_titles as $title) {
        $parsed = parse_movie_title($title);
        
        // Check Vietnamese
        if(!empty($parsed['vietnamese']) && 
           (stripos($parsed['vietnamese'], $query) !== false || 
            stripos($parsed['vietnamese_no_accent'], $normalized) !== false)) {
            $found[] = $title . " (Vietnamese match)";
        }
        
        // Check Original
        if(!empty($parsed['original']) && stripos($parsed['original'], $query) !== false) {
            $found[] = $title . " (Original match)";
        }
    }
    
    if(!empty($found)) {
        echo "  Found: \n";
        foreach($found as $match) {
            echo "    - {$match}\n";
        }
    } else {
        echo "  Found: None\n";
    }
    echo "---\n";
}
?>