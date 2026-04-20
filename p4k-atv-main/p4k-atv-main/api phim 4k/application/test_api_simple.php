<?php
/**
 * Test API đơn giản để debug
 */

error_reporting(E_ALL);
ini_set('display_errors', 0);
ini_set('log_errors', 1);

// Clean output buffer
if (ob_get_level()) {
    ob_end_clean();
}

// Set headers
header('Content-Type: application/json');
header('Cache-Control: no-cache');

try {
    $action = $_GET['action'] ?? 'unknown';
    
    if ($action === 'test') {
        echo json_encode([
            'status' => 'ok',
            'message' => 'API test thành công!',
            'timestamp' => date('Y-m-d H:i:s'),
            'php_version' => PHP_VERSION
        ]);
    } elseif ($action === 'get_stats') {
        echo json_encode([
            'total' => 14555,
            'status' => 'success',
            'message' => 'Fake stats để test'
        ]);
    } else {
        echo json_encode([
            'error' => 'Unknown action: ' . $action,
            'available_actions' => ['test', 'get_stats']
        ]);
    }
} catch (Exception $e) {
    echo json_encode([
        'error' => 'Server error: ' . $e->getMessage(),
        'file' => basename($e->getFile()),
        'line' => $e->getLine()
    ]);
}
?>