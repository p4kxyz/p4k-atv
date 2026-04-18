<?php
/**
 * Script xử lý toàn bộ database với Ajax progress bar
 * Hiển thị progress real-time
 */

// Cấu hình database - THAY ĐỔI THEO SERVER CỦA BẠN
$host = 'localhost';
$username = 'root';  // Thay đổi username
$password = '';      // Thay đổi password  
$database = 'sql_api_phim4k_l';  // Thay đổi tên database

// Xử lý AJAX request
if(isset($_GET['action']) && $_GET['action'] == 'process') {
    header('Content-Type: application/json');
    
    try {
        $pdo = new PDO("mysql:host=$host;dbname=$database;charset=utf8mb4", $username, $password);
        $pdo->setAttribute(PDO::ATTR_ERRMODE, PDO::ERRMODE_EXCEPTION);
        
        $offset = intval($_GET['offset'] ?? 0);
        $batch_size = 100; // Smaller batch for real-time progress
        
        // Lấy batch hiện tại
        $stmt = $pdo->prepare("SELECT videos_id, title FROM videos WHERE title IS NOT NULL LIMIT :limit OFFSET :offset");
        $stmt->bindValue(':limit', $batch_size, PDO::PARAM_INT);
        $stmt->bindValue(':offset', $offset, PDO::PARAM_INT);
        $stmt->execute();
        $videos = $stmt->fetchAll(PDO::FETCH_ASSOC);
        
        $processed = 0;
        $samples = [];
        
        foreach($videos as $video) {
            $title = $video['title'];
            $video_id = $video['videos_id'];
            
            $parsed = parseMovieTitle($title);
            
            $sql = "UPDATE videos SET 
                    title_vietnamese = :vietnamese,
                    title_vietnamese_no_accent = :vietnamese_no_accent, 
                    title_original = :original
                    WHERE videos_id = :video_id";
            
            $stmt_update = $pdo->prepare($sql);
            $result = $stmt_update->execute([
                ':vietnamese' => $parsed['vietnamese'] ?: null,
                ':vietnamese_no_accent' => $parsed['vietnamese_no_accent'] ?: null,
                ':original' => $parsed['original'] ?: null,
                ':video_id' => $video_id
            ]);
            
            if($result) {
                $processed++;
                
                // Lưu một vài sample để hiển thị
                if(count($samples) < 3) {
                    $samples[] = [
                        'id' => $video_id,
                        'original' => $title,
                        'vietnamese' => $parsed['vietnamese'],
                        'vietnamese_no_accent' => $parsed['vietnamese_no_accent'],
                        'original_parsed' => $parsed['original']
                    ];
                }
            }
        }
        
        echo json_encode([
            'success' => true,
            'processed' => $processed,
            'samples' => $samples,
            'has_more' => count($videos) == $batch_size
        ]);
        
    } catch(Exception $e) {
        echo json_encode(['success' => false, 'error' => $e->getMessage()]);
    }
    exit;
}

// Lấy tổng số records
if(isset($_GET['action']) && $_GET['action'] == 'count') {
    header('Content-Type: application/json');
    
    try {
        $pdo = new PDO("mysql:host=$host;dbname=$database;charset=utf8mb4", $username, $password);
        $total_stmt = $pdo->query("SELECT COUNT(*) as total FROM videos WHERE title IS NOT NULL");
        $total = $total_stmt->fetch(PDO::FETCH_ASSOC)['total'];
        
        echo json_encode(['success' => true, 'total' => $total]);
    } catch(Exception $e) {
        echo json_encode(['success' => false, 'error' => $e->getMessage()]);
    }
    exit;
}

// Function loại bỏ dấu tiếng Việt và các ký tự đặc biệt
function removeVietnameseAccents($str) {
    $str = mb_strtolower($str, 'UTF-8');
    
    $from = [
        'á', 'à', 'ả', 'ã', 'ạ', 'ă', 'ắ', 'ằ', 'ẳ', 'ẵ', 'ặ', 'â', 'ấ', 'ầ', 'ẩ', 'ẫ', 'ậ',
        'đ',
        'é', 'è', 'ẻ', 'ẽ', 'ẹ', 'ê', 'ế', 'ề', 'ể', 'ễ', 'ệ',
        'í', 'ì', 'ỉ', 'ĩ', 'ị',
        'ó', 'ò', 'ỏ', 'õ', 'ọ', 'ô', 'ố', 'ồ', 'ổ', 'ỗ', 'ộ', 'ơ', 'ớ', 'ờ', 'ở', 'ỡ', 'ợ',
        'ú', 'ù', 'ủ', 'ũ', 'ụ', 'ư', 'ứ', 'ừ', 'ử', 'ữ', 'ự',
        'ý', 'ỳ', 'ỷ', 'ỹ', 'ỵ'
    ];
    
    $to = [
        'a', 'a', 'a', 'a', 'a', 'a', 'a', 'a', 'a', 'a', 'a', 'a', 'a', 'a', 'a', 'a', 'a',
        'd',
        'e', 'e', 'e', 'e', 'e', 'e', 'e', 'e', 'e', 'e', 'e',
        'i', 'i', 'i', 'i', 'i',
        'o', 'o', 'o', 'o', 'o', 'o', 'o', 'o', 'o', 'o', 'o', 'o', 'o', 'o', 'o', 'o', 'o',
        'u', 'u', 'u', 'u', 'u', 'u', 'u', 'u', 'u', 'u', 'u',
        'y', 'y', 'y', 'y', 'y'
    ];
    
    // Bỏ dấu tiếng Việt
    $str = str_replace($from, $to, $str);
    
    // Bỏ các ký tự đặc biệt như :, -, ., ! ? etc
    $str = preg_replace('/[:\-\.!?\'"",;]/', ' ', $str);
    
    // Bỏ nhiều space thành 1 space và trim
    $str = preg_replace('/\s+/', ' ', $str);
    $str = trim($str);
    
    return $str;
}

function parseMovieTitle($title) {
    $result = [
        'vietnamese' => '',
        'vietnamese_no_accent' => '',
        'original' => ''
    ];
    
    // Pattern 1: "Tên Việt (2023) English Name + Asian characters"
    if (preg_match('/^(.+?)\s*\((\d{4})\)\s*(.+)$/u', $title, $matches)) {
        $vietnamese = trim($matches[1]);
        $year = $matches[2];
        $after_year = trim($matches[3]);
        
        // Kiểm tra nếu có ký tự Latin (English) hoặc Asian (Hàn/Nhật/Trung)
        if (preg_match('/[a-zA-Z\x{4e00}-\x{9fff}\x{3040}-\x{309f}\x{30a0}-\x{30ff}\x{ac00}-\x{d7af}]/u', $after_year)) {
            $result['vietnamese'] = $vietnamese . ' (' . $year . ')';
            $result['original'] = $after_year;
        } else {
            // Nếu không có tên gốc rõ ràng, toàn bộ là tên Việt
            $result['vietnamese'] = $title;
        }
    }
    // Pattern 2: "Tên Việt (2023)" - chỉ có tên Việt với năm
    else if (preg_match('/^(.+?)\s*\((\d{4})\)\s*$/u', $title, $matches)) {
        $result['vietnamese'] = $title;
    }
    // Pattern 3: Không có pattern năm
    else {
        // Kiểm tra nếu title chứa ký tự tiếng Việt
        if (preg_match('/[àáạảãâầấậẩẫăằắặẳẵèéẹẻẽêềếệểễìíịỉĩòóọỏõôồốộổỗơờớợởỡùúụủũưừứựửữỳýỵỷỹđ]/u', $title)) {
            $result['vietnamese'] = $title;
        } 
        // Nếu có Asian characters nhưng không có tiếng Việt
        else if (preg_match('/[\x{4e00}-\x{9fff}\x{3040}-\x{309f}\x{30a0}-\x{30ff}\x{ac00}-\x{d7af}]/u', $title)) {
            $result['original'] = $title;
        }
        // Nếu chỉ có Latin characters
        else if (preg_match('/[a-zA-Z]/', $title)) {
            $result['original'] = $title;
        }
        // Fallback: đặt vào original
        else {
            $result['original'] = $title;
        }
    }
    
    // Tạo Vietnamese no accent (chỉ khi có vietnamese)
    if (!empty($result['vietnamese'])) {
        $result['vietnamese_no_accent'] = removeVietnameseAccents($result['vietnamese']);
    }
    
    return $result;
}
?>
<!DOCTYPE html>
<html>
<head>
    <meta charset="UTF-8">
    <title>🚀 Update Title Fields - Progress</title>
    <style>
        body { font-family: Arial, sans-serif; max-width: 1200px; margin: 20px auto; padding: 20px; }
        .progress-container { background: #f5f5f5; padding: 20px; border-radius: 10px; margin: 20px 0; }
        .progress-bar { width: 100%; height: 30px; background: #ddd; border-radius: 15px; overflow: hidden; }
        .progress-fill { height: 100%; background: linear-gradient(90deg, #4CAF50, #45a049); transition: width 0.3s; }
        .sample-box { background: #e3f2fd; padding: 15px; margin: 10px 0; border-radius: 8px; border-left: 4px solid #2196F3; }
        .btn { background: #4CAF50; color: white; padding: 15px 30px; border: none; border-radius: 5px; cursor: pointer; font-size: 16px; }
        .btn:hover { background: #45a049; }
        .btn:disabled { background: #ccc; cursor: not-allowed; }
        .status { font-size: 18px; font-weight: bold; margin: 10px 0; }
        .error { color: #f44336; }
        .success { color: #4CAF50; }
    </style>
</head>
<body>
    <h1>🚀 Cập nhật Title Fields - Toàn bộ Database</h1>
    
    <div class="progress-container">
        <div class="status" id="status">Sẵn sàng để bắt đầu...</div>
        <div class="progress-bar">
            <div class="progress-fill" id="progressFill" style="width: 0%"></div>
        </div>
        <div id="progressText">0 / 0 (0%)</div>
    </div>
    
    <button class="btn" id="startBtn" onclick="startProcessing()">Bắt đầu xử lý</button>
    <button class="btn" id="pauseBtn" onclick="pauseProcessing()" style="display:none; background:#ff9800;">Tạm dừng</button>
    
    <div id="samplesContainer"></div>
    <div id="finalResults"></div>

    <script>
        let processing = false;
        let totalRecords = 0;
        let processedRecords = 0;
        let currentOffset = 0;
        
        async function getTotalCount() {
            const response = await fetch('?action=count');
            const data = await response.json();
            if(data.success) {
                totalRecords = data.total;
                document.getElementById('progressText').textContent = `0 / ${totalRecords} (0%)`;
                document.getElementById('status').textContent = `Phát hiện ${totalRecords} videos cần xử lý`;
            } else {
                document.getElementById('status').innerHTML = `<span class="error">Lỗi: ${data.error}</span>`;
            }
        }
        
        async function startProcessing() {
            if(totalRecords === 0) {
                await getTotalCount();
            }
            
            processing = true;
            document.getElementById('startBtn').style.display = 'none';
            document.getElementById('pauseBtn').style.display = 'inline-block';
            document.getElementById('status').textContent = 'Đang xử lý...';
            
            processNextBatch();
        }
        
        function pauseProcessing() {
            processing = false;
            document.getElementById('startBtn').style.display = 'inline-block';
            document.getElementById('startBtn').textContent = 'Tiếp tục xử lý';
            document.getElementById('pauseBtn').style.display = 'none';
            document.getElementById('status').textContent = 'Đã tạm dừng';
        }
        
        async function processNextBatch() {
            if(!processing) return;
            
            try {
                const response = await fetch(`?action=process&offset=${currentOffset}`);
                const data = await response.json();
                
                if(data.success) {
                    processedRecords += data.processed;
                    currentOffset += data.processed;
                    
                    const percentage = Math.round((processedRecords / totalRecords) * 100);
                    document.getElementById('progressFill').style.width = percentage + '%';
                    document.getElementById('progressText').textContent = `${processedRecords} / ${totalRecords} (${percentage}%)`;
                    
                    // Hiển thị samples
                    if(data.samples && data.samples.length > 0) {
                        displaySamples(data.samples);
                    }
                    
                    if(data.has_more && processing) {
                        // Tiếp tục batch tiếp theo sau 500ms
                        setTimeout(processNextBatch, 500);
                    } else {
                        // Hoàn thành
                        processing = false;
                        document.getElementById('status').innerHTML = '<span class="success">🎉 Hoàn thành!</span>';
                        document.getElementById('pauseBtn').style.display = 'none';
                        showFinalResults();
                    }
                } else {
                    processing = false;
                    document.getElementById('status').innerHTML = `<span class="error">Lỗi: ${data.error}</span>`;
                    document.getElementById('pauseBtn').style.display = 'none';
                }
            } catch(error) {
                processing = false;
                document.getElementById('status').innerHTML = `<span class="error">Lỗi network: ${error.message}</span>`;
                document.getElementById('pauseBtn').style.display = 'none';
            }
        }
        
        function displaySamples(samples) {
            const container = document.getElementById('samplesContainer');
            samples.forEach(sample => {
                const div = document.createElement('div');
                div.className = 'sample-box';
                div.innerHTML = `
                    <strong>ID ${sample.id}:</strong> ${sample.original}<br>
                    → Vietnamese: ${sample.vietnamese || 'NULL'}<br>
                    → Vietnamese (no accent): ${sample.vietnamese_no_accent || 'NULL'}<br>
                    → Original: ${sample.original_parsed || 'NULL'}
                `;
                container.appendChild(div);
            });
        }
        
        function showFinalResults() {
            document.getElementById('finalResults').innerHTML = `
                <div class="sample-box" style="border-left-color: #4CAF50;">
                    <h3>✅ Kết quả cuối cùng:</h3>
                    <p><strong>Tổng số records đã xử lý:</strong> ${processedRecords}</p>
                    <p><strong>Thời gian:</strong> ${new Date().toLocaleString()}</p>
                    <p>Bây giờ bạn có thể chạy file <strong>deploy_step3_test_result.php</strong> để kiểm tra kết quả!</p>
                </div>
            `;
        }
        
        // Load total count khi trang load
        window.onload = getTotalCount;
    </script>
</body>
</html>