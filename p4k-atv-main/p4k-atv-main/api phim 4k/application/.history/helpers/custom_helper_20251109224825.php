<?php  if ( ! defined('BASEPATH')) exit('No direct script access allowed');

// translation
if ( ! function_exists('trans')):
	function trans($phrase){
        return ucwords(str_replace('_', ' ', $phrase));
  //       $translate_phrase = $phrase;
	 //    $ci =& get_instance();
		// $ci->load->helper('language');
		// $active_language = $ci->language_model->get_active_language();
		// $ci->lang->load('site_lang',$active_language);
		// $ci->config->set_item('language', $active_language);
  //       if($ci->lang->line($phrase) == FALSE):
  //           //$ci->language_model->create_phrase($active_language,$phrase);            
  //       else:
  //           $translate_phrase = $ci->lang->line($phrase,FALSE);
  //       endif;
	 //    return $translate_phrase;
    }
endif;

// configuration helper
if (! function_exists('ovoo_config')):
	function ovoo_config($title)
    {
    	$ci =& get_instance();
        return $ci->common_model->get_config($title);
    }
endif;

if (! function_exists('is_demo')):
    function is_demo()
    {
        $ci =& get_instance();
        return ($ci->config->item('is_demo') === true ) ? true : false;
    }
endif;


// theme helper
if (! function_exists('active_theme')):
	function active_theme()
    {
    	$ci =& get_instance();
        return $ci->common_model->get_active_theme();
    }
endif;

//generate slug
if (!function_exists('str_slug')) {
    function str_slug($str)
    {
        return url_title($str, "-", true);
    }
}

// Remove Vietnamese accents for search
if (!function_exists('remove_vietnamese_accents')) {
    function remove_vietnamese_accents($str) {
        // Convert to lowercase first
        $str = mb_strtolower($str, 'UTF-8');
        
        // Replace Vietnamese characters
        $from = array(
            'á', 'à', 'ả', 'ã', 'ạ', 'ă', 'ắ', 'ằ', 'ẳ', 'ẵ', 'ặ', 'â', 'ấ', 'ầ', 'ẩ', 'ẫ', 'ậ',
            'đ',
            'é', 'è', 'ẻ', 'ẽ', 'ẹ', 'ê', 'ế', 'ề', 'ể', 'ễ', 'ệ',
            'í', 'ì', 'ỉ', 'ĩ', 'ị',
            'ó', 'ò', 'ỏ', 'õ', 'ọ', 'ô', 'ố', 'ồ', 'ổ', 'ỗ', 'ộ', 'ơ', 'ớ', 'ờ', 'ở', 'ỡ', 'ợ',
            'ú', 'ù', 'ủ', 'ũ', 'ụ', 'ư', 'ứ', 'ừ', 'ử', 'ữ', 'ự',
            'ý', 'ỳ', 'ỷ', 'ỹ', 'ỵ'
        );
        
        $to = array(
            'a', 'a', 'a', 'a', 'a', 'a', 'a', 'a', 'a', 'a', 'a', 'a', 'a', 'a', 'a', 'a', 'a',
            'd',
            'e', 'e', 'e', 'e', 'e', 'e', 'e', 'e', 'e', 'e', 'e',
            'i', 'i', 'i', 'i', 'i',
            'o', 'o', 'o', 'o', 'o', 'o', 'o', 'o', 'o', 'o', 'o', 'o', 'o', 'o', 'o', 'o', 'o',
            'u', 'u', 'u', 'u', 'u', 'u', 'u', 'u', 'u', 'u', 'u',
            'y', 'y', 'y', 'y', 'y'
        );
        
        return str_replace($from, $to, $str);
    }
}

// Normalize name by replacing hyphens with spaces
if (!function_exists('normalize_name')) {
    function normalize_name($str) {
        // Replace hyphens with spaces
        $str = str_replace('-', ' ', $str);
        // Remove extra spaces
        $str = preg_replace('/\s+/', ' ', trim($str));
        return $str;
    }
}

// Split search query into words for fuzzy search
if (!function_exists('get_search_keywords')) {
    function get_search_keywords($query) {
        // Replace hyphens with spaces first (for names like Lee-Min-Ho)
        $query = str_replace('-', ' ', $query);
        
        // Remove extra spaces
        $query = preg_replace('/\s+/', ' ', trim($query));
        
        // Remove special characters but keep Vietnamese characters
        $query = preg_replace('/[^\p{L}\p{N}\s]/u', ' ', $query);
        
        // Split into words
        $keywords = explode(' ', $query);
        
        // Remove words shorter than 2 characters and filter empty
        $keywords = array_filter($keywords, function($word) {
            return mb_strlen($word, 'UTF-8') >= 2;
        });
        
        return array_values($keywords); // Re-index array
    }
}

// Extract Vietnamese name from full title (before year in parentheses)
if (!function_exists('extract_vietnamese_title')) {
    function extract_vietnamese_title($title) {
        // Pattern: "Tên Việt (2023) English Name" -> "Tên Việt"
        if (preg_match('/^([^(]+)\s*\(\d{4}\)/', $title, $matches)) {
            return trim($matches[1]);
        }
        return $title;
    }
}

// Generate search-friendly title
if (!function_exists('generate_search_title')) {
    function generate_search_title($title) {
        $searchTitle = remove_vietnamese_accents($title);
        // Also remove punctuation for better search
        $searchTitle = preg_replace('/[^\p{L}\p{N}\s]/u', ' ', $searchTitle);
        // Remove extra spaces
        $searchTitle = preg_replace('/\s+/', ' ', trim($searchTitle));
        return $searchTitle;
    }
}

// Enhanced fuzzy search with intelligent word matching
if (!function_exists('get_intelligent_search_variations')) {
    function get_intelligent_search_variations($query) {
        $variations = array();
        
        // Original query
        $variations[] = $query;
        
        // Normalized query (no accents)
        $normalized = remove_vietnamese_accents($query);
        if($normalized !== $query) {
            $variations[] = $normalized;
        }
        
        // Common Vietnamese word variations (specific mappings)
        $word_map = array(
            'cho' => 'chó',
            'san' => 'săn', 
            'cong' => 'công',
            'ly' => 'lý',
            'nhen' => 'nhện',
            'nguoi' => 'người',
            'duoc' => 'được',
            'tho' => 'thổ',
            'nhi' => 'nhị',
            'tam' => 'tam',
            'tu' => 'tử',
            'ngu' => 'ngũ',
            'luc' => 'lục',
            'that' => 'thất',
            'bat' => 'bát',
            'cuu' => 'cửu',
            'thap' => 'thập'
        );
        
        // Apply word mapping to normalized query
        $mapped_query = $normalized;
        foreach($word_map as $from => $to) {
            // Word boundary replacement to avoid partial matches
            $mapped_query = preg_replace('/\b' . preg_quote($from, '/') . '\b/u', $to, $mapped_query);
        }
        
        if($mapped_query !== $normalized && $mapped_query !== $query) {
            $variations[] = $mapped_query;
        }
        
        return array_unique($variations);
    }
}