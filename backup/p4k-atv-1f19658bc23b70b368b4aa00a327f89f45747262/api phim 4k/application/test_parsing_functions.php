<?php
/**
 * Test script để kiểm tra parsing functions
 */

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

// Function parse title thành 3 phần
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

echo "<h2>🧪 Test Parsing Functions</h2>\n";

// Test cases
$testCases = [
    'Super 8: Quái Vật Vũ Trụ (2011) Super 8',
    'Dịch Vụ Giao Hàng Kiki (1989) 魔女の宅急便',
    'Sự Hoán Đổi Kỳ Diệu (2002) The Hot Chick',
    'Avatar: The Way of Water (2022)',
    'Top Gun: Maverick (2022) Top Gun: Maverick',
    'Spirited Away',
    '魔女の宅急便',
    'The Batman (2022)',
];

foreach($testCases as $title) {
    echo "<div style='background:#f0f0f0; padding:15px; margin:10px 0; border-radius:8px;'>";
    echo "<strong>Input:</strong> {$title}<br>";
    
    $parsed = parseMovieTitle($title);
    
    echo "<strong>Vietnamese:</strong> " . ($parsed['vietnamese'] ?: '<span style="color:red">NULL</span>') . "<br>";
    echo "<strong>Vietnamese (no accent):</strong> " . ($parsed['vietnamese_no_accent'] ?: '<span style="color:red">NULL</span>') . "<br>";
    echo "<strong>Original:</strong> " . ($parsed['original'] ?: '<span style="color:red">NULL</span>') . "<br>";
    echo "</div>";
}

// Test removeVietnameseAccents
echo "<h3>🔤 Test Remove Accents:</h3>";
$accentTests = [
    'Super 8: Quái Vật Vũ Trụ (2011)',
    'Sự Hoán Đổi Kỳ Diệu: Phần 2',
    'Tôi Yêu Việt Nam!'
];

foreach($accentTests as $test) {
    echo "<div style='background:#e8f5e8; padding:10px; margin:5px 0; border-radius:5px;'>";
    echo "<strong>Input:</strong> {$test}<br>";
    echo "<strong>Output:</strong> " . removeVietnameseAccents($test) . "<br>";
    echo "</div>";
}
?>