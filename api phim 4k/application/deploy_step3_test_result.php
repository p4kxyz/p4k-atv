<?php
/**
 * Script test kết quả sau khi update
 * Kiểm tra parsing có đúng không
 */

// Cấu hình database - THAY ĐỔI THEO SERVER CỦA BẠN
$host = 'localhost';
$username = 'root';  // Thay đổi username
$password = '';      // Thay đổi password  
$database = 'sql_api_phim4k_l';  // Thay đổi tên database

try {
    $pdo = new PDO("mysql:host=$host;dbname=$database;charset=utf8mb4", $username, $password);
    $pdo->setAttribute(PDO::ATTR_ERRMODE, PDO::ERRMODE_EXCEPTION);
    
    echo "<h2>🔍 Kiểm tra kết quả parsing</h2>\n";
    
    // Lấy sample phong phú hơn để kiểm tra
    $stmt = $pdo->query("
        SELECT videos_id, title, title_vietnamese, title_vietnamese_no_accent, title_original 
        FROM videos 
        WHERE title IS NOT NULL 
        ORDER BY RAND()
        LIMIT 50
    ");
    
    $videos = $stmt->fetchAll(PDO::FETCH_ASSOC);
    
    echo "<table border='1' style='border-collapse: collapse; width: 100%;'>";
    echo "<tr style='background: #f0f0f0;'>
            <th>ID</th>
            <th>Original Title</th>
            <th>Vietnamese</th>
            <th>Vietnamese No Accent</th>
            <th>Original</th>
          </tr>";
    
    foreach($videos as $video) {
        echo "<tr>";
        echo "<td>{$video['videos_id']}</td>";
        echo "<td style='max-width: 200px;'>{$video['title']}</td>";
        echo "<td style='color: blue;'>" . ($video['title_vietnamese'] ?: 'NULL') . "</td>";
        echo "<td style='color: green;'>" . ($video['title_vietnamese_no_accent'] ?: 'NULL') . "</td>";
        echo "<td style='color: red;'>" . ($video['title_original'] ?: 'NULL') . "</td>";
        echo "</tr>";
    }
    echo "</table>";
    
    // Thống kê
    $stats = $pdo->query("
        SELECT 
            COUNT(*) as total,
            COUNT(title_vietnamese) as has_vietnamese,
            COUNT(title_vietnamese_no_accent) as has_vietnamese_no_accent,
            COUNT(title_original) as has_original
        FROM videos 
        WHERE title IS NOT NULL
    ")->fetch(PDO::FETCH_ASSOC);
    
    echo "<h3>📊 Thống kê:</h3>";
    echo "<ul>";
    echo "<li><strong>Tổng videos:</strong> {$stats['total']}</li>";
    echo "<li><strong>Có tên Việt:</strong> {$stats['has_vietnamese']} (" . round($stats['has_vietnamese']/$stats['total']*100, 1) . "%)</li>";
    echo "<li><strong>Có tên Việt không dấu:</strong> {$stats['has_vietnamese_no_accent']} (" . round($stats['has_vietnamese_no_accent']/$stats['total']*100, 1) . "%)</li>";
    echo "<li><strong>Có tên gốc:</strong> {$stats['has_original']} (" . round($stats['has_original']/$stats['total']*100, 1) . "%)</li>";
    echo "</ul>";
    
    // Hiển thị progress xử lý
    $progress = $pdo->query("
        SELECT 
            COUNT(*) as total_videos,
            COUNT(CASE WHEN title_vietnamese IS NOT NULL THEN 1 END) as processed_videos,
            COUNT(CASE WHEN title_vietnamese IS NOT NULL THEN 1 END) * 100.0 / COUNT(*) as progress_percent
        FROM videos 
        WHERE title IS NOT NULL
    ")->fetch(PDO::FETCH_ASSOC);
    
    echo "<h3>📊 Tiến trình xử lý:</h3>";
    echo "<div style='background:#e8f5e8; padding:15px; border-radius:8px; margin:10px 0;'>";
    echo "<strong>Đã xử lý:</strong> {$progress['processed_videos']} / {$progress['total_videos']} (" . round($progress['progress_percent'], 1) . "%)<br>";
    
    if($progress['progress_percent'] < 100) {
        $remaining = $progress['total_videos'] - $progress['processed_videos'];
        echo "<strong>🔄 Còn lại:</strong> {$remaining} videos chưa được xử lý<br>";
        echo "<strong>💡 Gợi ý:</strong> Chạy tiếp script update để xử lý hết toàn bộ database";
    } else {
        echo "<strong>✅ Trạng thái:</strong> Đã xử lý hoàn tất toàn bộ database!";
    }
    echo "</div>";

    // Test search
    echo "<h3>🔍 Test tìm kiếm:</h3>";
    
    $test_queries = ['su hoan doi', 'nguoi nhen', 'avengers', 'spirited away', 'fast furious', 'marvel'];
    
    foreach($test_queries as $query) {
        echo "<h4>Tìm kiếm: '{$query}'</h4>";
        
        $sql = "
            SELECT videos_id, title, title_vietnamese, title_vietnamese_no_accent, title_original
            FROM videos 
            WHERE 
                title_vietnamese LIKE :q OR
                title_vietnamese_no_accent LIKE :q OR  
                title_original LIKE :q OR
                title LIKE :q
            LIMIT 5
        ";
        
        $stmt = $pdo->prepare($sql);
        $stmt->execute([':q' => "%{$query}%"]);
        $results = $stmt->fetchAll(PDO::FETCH_ASSOC);
        
        if($results) {
            echo "<ul>";
            foreach($results as $result) {
                echo "<li><strong>{$result['title']}</strong>";
                if($result['title_vietnamese']) echo "<br>→ Vietnamese: {$result['title_vietnamese']}";
                if($result['title_original']) echo "<br>→ Original: {$result['title_original']}";
                echo "</li>";
            }
            echo "</ul>";
        } else {
            echo "<p>Không tìm thấy kết quả</p>";
        }
    }
    
} catch(Exception $e) {
    echo "❌ Lỗi: " . $e->getMessage() . "\n";
}
?>