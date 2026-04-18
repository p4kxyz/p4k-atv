<?php
/**
 * Script update title_search cho dữ liệu có sẵn
 * Chạy file này một lần để cập nhật tất cả video có sẵn
 */

// Load CodeIgniter
require_once('index.php');

class Update_Title_Search extends CI_Controller {
    
    public function __construct() {
        parent::__construct();
        $this->load->database();
        $this->load->helper('custom');
    }
    
    public function run() {
        echo "Bắt đầu update title_search cho videos...\n";
        
        // Thêm cột mới nếu chưa có
        $this->add_columns_if_not_exists();
        
        // Lấy tất cả videos
        $this->db->select('videos_id, title');
        $this->db->where('title IS NOT NULL');
        $videos = $this->db->get('videos')->result_array();
        
        $updated = 0;
        $total = count($videos);
        
        foreach($videos as $video) {
            $title = $video['title'];
            $video_id = $video['videos_id'];
            
            // Tạo title_search (không dấu)
            $title_search = generate_search_title($title);
            
            // Tạo title_vietnamese (tên việt thuần)
            $title_vietnamese = extract_vietnamese_title($title);
            
            // Update database
            $this->db->where('videos_id', $video_id);
            $result = $this->db->update('videos', array(
                'title_search' => $title_search,
                'title_vietnamese' => $title_vietnamese
            ));
            
            if($result) {
                $updated++;
                if($updated % 100 == 0) {
                    echo "Đã update: {$updated}/{$total}\n";
                }
            }
        }
        
        echo "Hoàn thành! Đã update {$updated}/{$total} videos\n";
        
        // Tạo index nếu chưa có
        $this->create_indexes();
        
        echo "Đã tạo index cho tìm kiếm\n";
    }
    
    private function add_columns_if_not_exists() {
        // Check if columns exist
        $fields = $this->db->field_data('videos');
        $has_title_search = false;
        $has_title_vietnamese = false;
        
        foreach($fields as $field) {
            if($field->name == 'title_search') {
                $has_title_search = true;
            }
            if($field->name == 'title_vietnamese') {
                $has_title_vietnamese = true;
            }
        }
        
        if(!$has_title_search) {
            $this->db->query("ALTER TABLE `videos` ADD COLUMN `title_search` varchar(500) DEFAULT NULL COMMENT 'Title không dấu cho tìm kiếm'");
            echo "Đã thêm cột title_search\n";
        }
        
        if(!$has_title_vietnamese) {
            $this->db->query("ALTER TABLE `videos` ADD COLUMN `title_vietnamese` varchar(255) DEFAULT NULL COMMENT 'Tên tiếng Việt thuần'");
            echo "Đã thêm cột title_vietnamese\n";
        }
    }
    
    private function create_indexes() {
        try {
            // Check if index exists
            $indexes = $this->db->query("SHOW INDEX FROM videos WHERE Key_name = 'idx_title_search'")->result();
            if(empty($indexes)) {
                $this->db->query("ALTER TABLE `videos` ADD KEY `idx_title_search` (`title_search`)");
                echo "Đã tạo index cho title_search\n";
            }
            
            $fulltext_indexes = $this->db->query("SHOW INDEX FROM videos WHERE Key_name = 'idx_title_fulltext'")->result();
            if(empty($fulltext_indexes)) {
                $this->db->query("ALTER TABLE `videos` ADD FULLTEXT KEY `idx_title_fulltext` (`title`, `title_search`, `title_vietnamese`)");
                echo "Đã tạo fulltext index\n";
            }
        } catch(Exception $e) {
            echo "Lỗi tạo index: " . $e->getMessage() . "\n";
        }
    }
}

// Chạy script
if(php_sapi_name() === 'cli') {
    $update = new Update_Title_Search();
    $update->run();
} else {
    echo "Script này chỉ chạy được từ command line\n";
    echo "Chạy: php update_title_search_data.php\n";
}
?>