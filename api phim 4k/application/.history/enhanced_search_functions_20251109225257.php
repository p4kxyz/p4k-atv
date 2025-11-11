<?php
/**
 * Enhanced search functions for API v130
 * Thêm vào Api_v130_model.php
 */

// Thêm function này vào class Api_v130_model

public function enhanced_movie_search($q='', $page='', $genre_id='', $country_id='', $range_to='', $range_from='') {
    $q = trim($q);
    $page = trim($page);
    $genre_id = trim($genre_id);
    $country_id = trim($country_id);
    $range_to = trim($range_to);
    $range_from = trim($range_from);

    $response = array();
    
    // Pagination
    if(!empty($page) && $page !='' && $page !=NULL && is_numeric($page)):
        $offset = ((int)$page * $this->default_limit) - $this->default_limit;
        $this->db->limit($this->default_limit, $offset);
    else:
        $this->db->limit($this->default_limit);
    endif;

    if(!empty($q) && $q !='' && $q !=NULL):
        $keywords = get_search_keywords($q);
        
        if(!empty($keywords)):
            $this->db->group_start();
            
            // Priority 1: Exact match in original title
            $this->db->like('title', $q, 'both');
            
            // Priority 2: Exact match in Vietnamese title (có dấu)
            $this->db->or_like('title_vietnamese', $q, 'both');
            
            // Priority 3: Exact match in Vietnamese no accent
            $q_normalized = remove_vietnamese_accents($q);
            if($q !== $q_normalized):
                $this->db->or_like('title_vietnamese_no_accent', $q_normalized, 'both');
            endif;
            
            // Priority 4: Exact match in original English name
            $this->db->or_like('title_original', $q, 'both');
            
            $this->db->group_end();
            
            // Priority 5: Fuzzy search with all keywords (AND logic)
            if(count($keywords) > 1):
                foreach($keywords as $keyword):
                    $this->db->group_start();
                        // Search in all title fields
                        $this->db->like('title', $keyword, 'both');
                        $this->db->or_like('title_vietnamese', $keyword, 'both');
                        $this->db->or_like('title_original', $keyword, 'both');
                        
                        $keyword_normalized = remove_vietnamese_accents($keyword);
                        if($keyword !== $keyword_normalized):
                            $this->db->or_like('title_vietnamese_no_accent', $keyword_normalized, 'both');
                        endif;
                    $this->db->group_end();
                endforeach;
            elseif(strlen($keywords[0]) >= 2):
                // Single keyword search
                $this->db->group_start();
                    $this->db->like('title', $keywords[0], 'both');
                    $this->db->or_like('title_vietnamese', $keywords[0], 'both');
                    $this->db->or_like('title_original', $keywords[0], 'both');
                    
                    $keyword_normalized = remove_vietnamese_accents($keywords[0]);
                    if($keywords[0] !== $keyword_normalized):
                        $this->db->or_like('title_vietnamese_no_accent', $keyword_normalized, 'both');
                    endif;
                $this->db->group_end();
            endif;
        endif;
    endif;

    // Apply filters
    if(!empty($genre_id) && $genre_id !='' && $genre_id !=NULL && is_numeric($genre_id)):
        $this->db->where("find_in_set(".$genre_id.",genre) >", 0);
    endif;

    if(!empty($country_id) && $country_id !='' && $country_id !=NULL && is_numeric($country_id)):
        $this->db->where("find_in_set(".$country_id.",country) >", 0);
    endif;

    if(!empty($range_from) && $range_from !='' && $range_from !=NULL && is_numeric($range_from) && strlen($range_from) == 4 && !empty($range_to) && $range_to !='' && $range_to !=NULL && is_numeric($range_to) && strlen($range_to) == 4):
        $release_from = date("Y-m-d", strtotime($range_from.'-01-01'));
        $this->db->where("release >=", $release_from);

        $release_to = date("Y-m-d", strtotime($range_to.'-12-31'));
        $this->db->where("release <=", $release_to);
    endif;

    $this->db->where('publication', '1');
    $this->db->where('is_tvseries !=', '1');
    
    // Order by relevance (exact match first)
    if(!empty($q)):
        $this->db->order_by("CASE 
            WHEN title LIKE '%".$this->db->escape_like_str($q)."%' THEN 1
            WHEN title_vietnamese LIKE '%".$this->db->escape_like_str($q)."%' THEN 2  
            WHEN title_search LIKE '%".$this->db->escape_like_str($q_normalized ?? $q)."%' THEN 3
            ELSE 4 END");
    endif;
    
    $this->db->order_by("total_view", "DESC");
    
    $latest_movies = $this->db->get('videos')->result_array();
    
    // Format response
    $i = 0;
    foreach ($latest_movies as $video):
        $response[$i]['videos_id'] = $video['videos_id'];
        $response[$i]['title'] = $video['title'];
        $response[$i]['description'] = strip_tags($video['description']);
        $response[$i]['slug'] = $video['slug'];
        $response[$i]['release'] = '2000';
        if($response[$i]['release'] !='' && $response[$i]['release'] !=NULL)
            $response[$i]['release'] = date("Y", strtotime($video['release']));
        $response[$i]['runtime'] = $video['runtime'];
        $response[$i]['is_tvseries'] = $video['is_tvseries'];
        $response[$i]['video_quality'] = $video['video_quality'];
        $response[$i]['imdb_rating'] = isset($video['imdb_rating']) ? $video['imdb_rating'] : null;
        $response[$i]['thumbnail_url'] = $this->common_model->get_video_thumb_url($video['videos_id']);
        $response[$i]['poster_url'] = $this->common_model->get_video_poster_url($video['videos_id']);
        $i++;
    endforeach;
    
    return $response;
}

// Function tự động update title_search khi thêm/sửa video
public function update_video_search_fields($videos_id, $title = null) {
    if(empty($title)) {
        $video = $this->db->get_where('videos', array('videos_id' => $videos_id))->row_array();
        $title = $video['title'] ?? '';
    }
    
    if(!empty($title)) {
        $title_search = generate_search_title($title);
        $title_vietnamese = extract_vietnamese_title($title);
        
        $this->db->where('videos_id', $videos_id);
        return $this->db->update('videos', array(
            'title_search' => $title_search,
            'title_vietnamese' => $title_vietnamese
        ));
    }
    
    return false;
}
?>