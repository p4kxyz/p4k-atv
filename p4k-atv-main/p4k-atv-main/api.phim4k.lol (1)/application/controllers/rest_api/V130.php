<?php
if (!defined('BASEPATH'))
    exit('No direct script access allowed');


/**
 * OXOO - Android Live TV & Movie Portal App
 * ---------------------- OXOO --------------------
 * ------- Android Live TV & Movie Portal App --------
 * - Live tv channel & movie management system -
 *
 * @package     OXOO - Android Live TV & Movie Portal App
 * @author      Abdul Mannan/SpaGreen Creative
 * @copyright   Copyright (c) 2014 - 2019 SpaGreen,
 * @license     http://codecanyon.net/wiki/support/legal-terms/licensing-terms/ 
 * @link        http://www.spagreen.net
 * @link        support@spagreen.net
 *
 **/
require(APPPATH.'/libraries/RestController.php');
use chriskacerguis\RestServer\RestController;
class V130 extends RestController{ 
    
	function __construct(){
		parent::__construct();
        $this->load->model('common_model');
        $this->load->model('api_v130_model');
        $this->load->helper('custom');
		$this->load->database();
	
   		/*cache controling*/
		$this->output->set_header('Last-Modified: ' . gmdate("D, d M Y H:i:s") . ' GMT');('Cache-Control: no-store, no-cache, must-revalidate, post-check=0, pre-check=0');
        $this->output->set_header('Pragma: no-cache');
        $this->output->set_header("Expires: Mon, 26 Jul 1997 05:00:00 GMT");
	}
    
    // index function
    public function index() {
        echo "Method is not defined.";
    }

    //test api function
    public function test_get() {
        $response['status']     = 'success';
        $response['message']    = 'Rest API is working...';            
        $this->response($response,200);
    }

    // get slider function
    public function slider_get() {
        $this->load->driver('cache', array('adapter' => 'redis', 'backup' => 'file'));
        if ( ! $response = $this->cache->get('slider_v130')) {
            $response['slider_type']        =   $this->db->get_where('config' , array('title'=>'slider_type'))->row()->value;
            if($response['slider_type'] == 'image'):
                if($this->db->get_where('slider',array('publication'=>'1'))->num_rows()==0)
                    $response['slider_type']        = "disable";
                $response['data']           =   $this->common_model->all_published_slider();
            elseif($response['slider_type'] == 'movie'):
                $response['data']           =   $this->api_v130_model->get_movies_for_slider();
            endif;
            $this->cache->save('slider_v130', $response, 3600);
        }
        $this->response($response,200);
    }

    public function home_content_get() {
        // Load Driver. Fallback to file if redis/memcached is not available
        $this->load->driver('cache', array('adapter' => 'redis', 'backup' => 'file'));
        
        // Try to get cached response (cache for 60 minutes)
        if ( ! $response = $this->cache->get('home_content_data_api_v130')) {
            $response = $this->api_v130_model->get_home_content();
            // Save to cache
            $this->cache->save('home_content_data_api_v130', $response, 3600);
        }
        $this->response($response, 200);
    }

    public function home_content_for_android_get() {
        // Load Driver. Ưu tiên Redis, fallback về File nếu Redis lỗi
        $this->load->driver('cache', array('adapter' => 'redis', 'backup' => 'file'));
        
        // OPTIMIZATION: Cache Raw JSON output to avoid PHP Array serialization/json_encode overhead
        // This significantly reduces CPU usage for large payloads
        $cache_key = 'home_content_android_v130_json'; 
        
        if ($cached_json = $this->cache->get($cache_key)) {
            // HIT: Output raw JSON directly
            $this->output->set_content_type('application/json')->set_output($cached_json);
            return;
        }

        // MISS: Build Data
        // slider
        $response['slider']                 =   $this->api_v130_model->get_slider();
        // popular star
        $response['popular_stars']           =   $this->api_v130_model->get_popular_stars();

        // all country
        $response['all_country']             =   $this->api_v130_model->get_all_country();

        // all country
        $response['all_genre']              =   $this->api_v130_model->get_all_genre();

        // featured tv channnel
        $response['featured_tv_channel']     =   $this->api_v130_model->get_featured_tv_channel();
        // latest movie
        $response['latest_movies']           =   $this->api_v130_model->get_latest_movies();
        // latest tvseries
        $response['latest_tvseries']         =   $this->api_v130_model->get_latest_tvseries();
        // features genre and movie
        $response['features_genre_and_movie']=   $this->api_v130_model->get_features_genre_and_movie(); 

        // Encode to JSON
        $json_output = json_encode($response);

        // Save Raw JSON to cache
        $this->cache->save($cache_key, $json_output, 3600);
        
        // Output
        $this->output->set_content_type('application/json')->set_output($json_output);
    }

    /***** 
    movie section strat here
    *****/

    // get latest movies function
    public function latest_movies_get() {
        $limit       = $this->input->get('limit');
        $genre_id    = $this->input->get('genre_id');
        $country_id  = $this->input->get('country_id');
        $range_from  = $this->input->get('range_from');
        $range_to    = $this->input->get('range_to');
        $page        = $this->input->get('page');
        $this->load->driver('cache', array('adapter' => 'redis', 'backup' => 'file'));
        $cache_key = 'latest_movies_' . md5($limit . $genre_id . $country_id . $range_from . $range_to . $page);
        if ( ! $response = $this->cache->get($cache_key)) {
            $response = $this->api_v130_model->get_latest_movies($limit, $genre_id, $country_id, $range_from, $range_to, $page);
            $this->cache->save($cache_key, $response, 3600);
        }
        $this->response($response, 200);
    }

    // get movies function
    public function movies_get() {
        $page              =   $this->input->get('page');
        $this->load->driver('cache', array('adapter' => 'redis', 'backup' => 'file'));
        $cache_key = 'movies_page_v130_' . $page;
        if ( ! $response = $this->cache->get($cache_key)) {
            $response          =   $this->api_v130_model->get_movies($page);
            $this->cache->save($cache_key, $response, 3600);
        }
        $this->response($response,200);
    }
    // get features genre  function
    public function features_genre_and_movie_get() {      
        $this->load->driver('cache', array('adapter' => 'redis', 'backup' => 'file'));
        if ( ! $response = $this->cache->get('features_genre_and_movie_v130')) {
            $response               =   $this->api_v130_model->get_features_genre_and_movie();            
            $this->cache->save('features_genre_and_movie_v130', $response, 3600);
        }
        $this->response($response,200);
    }

    // get movies  by genre ID function
    public function content_by_genre_id_get() {
        $id                 =   $this->input->get('id');
        $page               =   $this->input->get('page');
        $this->load->driver('cache', array('adapter' => 'redis', 'backup' => 'file'));
        $cache_key = 'genre_v130_' . $id . '_page_' . $page;
        
        if ( ! $response = $this->cache->get($cache_key)) {
            if(!empty($id) && $id !='' && $id !=NULL && is_numeric($id)):
                // Removed verification query here to save CPU
                // Any invalid ID will likely return empty result from get_content_by_genre_id anyway
                $response               =   $this->api_v130_model->get_content_by_genre_id($id,$page);
            else:
                $response['status']     = 'error';
                $response['message']    = 'Genre ID must not be null or empty.';
            endif;
            $this->cache->save($cache_key, $response, 3600); 
        }        
        $this->response($response,200);
    }

    // get movies  by country ID function
    public function content_by_country_id_get() {      
        $id                 =   $this->input->get('id');
        $this->load->driver('cache', array('adapter' => 'redis', 'backup' => 'file'));
        $page                   =   $this->input->get('page');
        $cache_key = 'country_v130_' . $id . '_page_' . $page;

        if ( ! $response = $this->cache->get($cache_key)) {
            if(!empty($id) && $id !='' && $id !=NULL && is_numeric($id)):
                 // Removed verification query here to save CPU
                $response               =   $this->api_v130_model->content_by_country_id($id,$page);
            else:
                $response['status']     = 'error';
                $response['message']    = 'Genre ID must not be null or empty.';
            endif;
            $this->cache->save($cache_key, $response, 3600);
        }
        $this->response($response,200);
    }

    // get movies  by country ID function
    public function content_by_star_id_get() {
        $id                 =   $this->input->get('id');
        $this->load->driver('cache', array('adapter' => 'redis', 'backup' => 'file'));
        $page                   =   $this->input->get('page');
        $cache_key = 'star_v130_' . $id . '_page_' . $page;

        if ( ! $response = $this->cache->get($cache_key)) {
            if(!empty($id) && $id !='' && $id !=NULL && is_numeric($id)):
                // Removed verification query here to save CPU
                $response               =   $this->api_v130_model->content_by_star_id($id,$page);
            else:
                $response['status']     = 'error';
                $response['message']    = 'Genre ID must not be null or empty.';
            endif;
            $this->cache->save($cache_key, $response, 3600);
        }
        $this->response($response,200);
    }

    /***** 
    movie section end here
    *****/


    /***** 
    tv-series section start here
    *****/


    // get latest movies function
    public function latest_tvseries_get() {
        $limit       = $this->input->get('limit');
        $genre_id    = $this->input->get('genre_id');
        $country_id  = $this->input->get('country_id');
        $range_from  = $this->input->get('range_from');
        $range_to    = $this->input->get('range_to');
        $page        = $this->input->get('page');
        $this->load->driver('cache', array('adapter' => 'redis', 'backup' => 'file'));
        $cache_key = 'latest_tvseries_v130_' . md5($limit . $genre_id . $country_id . $range_from . $range_to . $page);
        if ( ! $response = $this->cache->get($cache_key)) {
            $response = $this->api_v130_model->get_latest_tvseries($limit, $genre_id, $country_id, $range_from, $range_to, $page);
            $this->cache->save($cache_key, $response, 3600);
        }
        $this->response($response, 200);
    }

    // get movies function
    public function tvseries_get() {      
        $page              =   $this->input->get('page');
        $this->load->driver('cache', array('adapter' => 'redis', 'backup' => 'file'));
        $cache_key = 'tvseries_page_v130_' . $page;
        if ( ! $response = $this->cache->get($cache_key)) {
            $response           =   $this->api_v130_model->get_tvseries($page);
            $this->cache->save($cache_key, $response, 3600);
        }
        $this->response($response,200);
    }

    /***** 
    tvseries section end here
    *****/


    // get all country function
    public function all_country_get() {
        $this->load->driver('cache', array('adapter' => 'redis', 'backup' => 'file'));
        if ( ! $response = $this->cache->get('all_country_v130')) {
            $response               =   $this->api_v130_model->get_all_country();
            $this->cache->save('all_country_v130', $response, 86400); // 24 hours
        }
        $this->response($response,200);
    }


    // get all genre  function
    public function all_genre_get() {
        $this->load->driver('cache', array('adapter' => 'redis', 'backup' => 'file'));
        if ( ! $response = $this->cache->get('all_genre_v130')) {
            $response               =   $this->api_v130_model->get_all_genre();
            $this->cache->save('all_genre_v130', $response, 86400); // 24 hours
        }
        $this->response($response,200);
    }

    // get featured tv channel function
    public function featured_tv_channel_get() {
        $page              =   $this->input->get('page');
        $this->load->driver('cache', array('adapter' => 'redis', 'backup' => 'file'));
        $cache_key = 'featured_tv_channel_v130_' . $page;
        if ( ! $response = $this->cache->get($cache_key)) {
            $response           =   $this->api_v130_model->get_featured_tv_channel($page);
            $this->cache->save($cache_key, $response, 3600);
        }
        $this->response($response,200);
    }

    // get featured tv channel function
    public function all_tv_channel_get() {
        $page              =   $this->input->get('page');
        $this->load->driver('cache', array('adapter' => 'redis', 'backup' => 'file'));
        $cache_key = 'all_tv_channel_v130_' . $page;
        if ( ! $response = $this->cache->get($cache_key)) {
            $response           =   $this->api_v130_model->get_all_tv_channel($page);
            $this->cache->save($cache_key, $response, 3600);
        }
        $this->response($response,200);
    }

    // get featured tv channel function
    public function tv_channel_get() {
        $limit              =   $this->input->get('limit');
        $this->load->driver('cache', array('adapter' => 'redis', 'backup' => 'file'));
        $cache_key = 'tv_channel_limit_' . $limit;
        if ( ! $response = $this->cache->get($cache_key)) {
            $response           =   $this->api_v130_model->get_tv_channel($limit);
            $this->cache->save($cache_key, $response, 3600);
        }
        $this->response($response,200);
    }

    // get featured tv channel function
    public function all_tv_channel_by_category_get() {
        $limit              =   $this->input->get('limit');
        $this->load->driver('cache', array('adapter' => 'redis', 'backup' => 'file'));
        $cache_key = 'all_tv_channel_by_cat_limit_' . $limit;
        if ( ! $response = $this->cache->get($cache_key)) {
            $response           =   $this->api_v130_model->get_all_tv_channel_by_category($limit);
            $this->cache->save($cache_key, $response, 3600);
        }
        $this->response($response,200);
    }

    // get movies  by country ID function
    public function tv_channel_by_category_id_get() {
        $id                 =   $this->input->get('id');
        $this->load->driver('cache', array('adapter' => 'redis', 'backup' => 'file'));
        if(!empty($id) && $id !='' && $id !=NULL && is_numeric($id)):
            $validity           = $this->api_v130_model->verify_live_tv_category_id($id);
            if($validity):
                $page                   =   $this->input->get('page');
                $cache_key = 'tv_channel_by_cat_id_' . $id . '_page_' . $page;
                if ( ! $response = $this->cache->get($cache_key)):
                    $response               =   $this->api_v130_model->get_tv_channel_by_category_id($id,$page);
                    $this->cache->save($cache_key, $response, 3600);
                endif;
            else:
                $response['status']     = 'error';
                $response['message']    = 'TV Channel category ID not found.';
            endif;
        else:
            $response['status']     = 'error';
            $response['message']    = 'TV Channel category must not be null or empty.';
        endif;
        $this->response($response,200);
    }


    // get single movie,tvseries & live tv details
    public function single_details_get() {
        $type                       =   $this->input->get('type');
        $id                         =   $this->input->get('id');
        $this->load->driver('cache', array('adapter' => 'redis', 'backup' => 'file'));
        
        // OPTIMIZATION: Use Raw JSON caching to avoid array overhead
        // Verify type
        if($type=='movie' || $type=='tvseries' || $type=='tv'):
            // verify id
            if(!empty($id) && $id !='' && $id !=NULL && is_numeric($id)):
                if($type=='movie'):
                    $cache_key = 'single_movie_v130_' . $id . '_json';
                    if ($cached_json = $this->cache->get($cache_key)) {
                        $this->output->set_content_type('application/json')->set_output($cached_json);
                        return;
                    }
                    
                    $verify           = $this->api_v130_model->verify_movie_tvseries_id($id);
                    if($verify):
                        $this->common_model->watch_count_by_slug($id);
                        $response               =   $this->api_v130_model->get_single_movie_details_by_id($id);
                        $json_response = json_encode($response);
                        $this->cache->save($cache_key, $json_response, 3600);
                        $this->output->set_content_type('application/json')->set_output($json_response);
                        return;
                    else:
                        $response['status']     = 'error';
                        $response['message']    = 'Movie ID not found.';
                    endif;

                elseif($type=='tvseries'):
                    $cache_key = 'single_tvseries_v130_' . $id . '_json';
                    if ($cached_json = $this->cache->get($cache_key)) {
                        $this->output->set_content_type('application/json')->set_output($cached_json);
                        return;
                    }

                    $verify           = $this->api_v130_model->verify_movie_tvseries_id($id);
                    if($verify):
                        $this->common_model->watch_count_by_slug($id);
                        $response               =   $this->api_v130_model->get_single_tvseries_details_by_id($id);
                        $json_response = json_encode($response);
                        $this->cache->save($cache_key, $json_response, 3600);
                        $this->output->set_content_type('application/json')->set_output($json_response);
                        return;
                    else:
                        $response['status']     = 'error';
                        $response['message']    = 'Movie ID not found.';
                    endif;

                elseif($type=='tv'):
                    $cache_key = 'single_tv_v130_' . $id . '_json';
                    if ($cached_json = $this->cache->get($cache_key)) {
                        $this->output->set_content_type('application/json')->set_output($cached_json);
                        return;
                    }

                    $verify           = $this->api_v130_model->verify_tv_id($id);
                    if($verify):
                        $this->common_model->watch_count_by_slug($id);
                        $response               =   $this->api_v130_model->get_single_tv_details_by_id($id);
                        $json_response = json_encode($response);
                        $this->cache->save($cache_key, $json_response, 3600);
                        $this->output->set_content_type('application/json')->set_output($json_response);
                        return;
                    else:
                        $response['status']     = 'error';
                        $response['message']    = 'TV ID not found.';
                    endif;
                endif;
            else:
                $response['status']     = 'error';
                $response['message']    = 'ID must be valid.';
            endif;
        else:
            $response['status']     = 'error';
            $response['message']    = 'Type must be satisfied.';
        endif;
        $this->response($response,200);   
    }




    // login function
    public function login_post() {
        $email                      =   trim($this->input->post('email'));
        $password                   =   md5(trim($this->input->post('password')));
        if (filter_var($email, FILTER_VALIDATE_EMAIL) && $password !='' && $password !=NULL):            
            $login_status               = $this->api_v130_model->validate_user( $email ,$password);        
            if ($login_status):
                $credential    =   array(  'email' => $email , 'password' => $password,'status'=>'1');
                $this->db->where($credential);
                $this->db->update('user', array('last_login' => date('Y-m-d H:i:s')));
                $user_info              = $this->api_v130_model->get_user_info( $email ,$password);
                $response['status']     = 'success';
                $response['user_id']    = $user_info->user_id;
                $response['name']       = $user_info->name;
                $response['email']      = $user_info->email;
                $response['phone']      = $user_info->phone;
                $response['password_available']     = TRUE;
                $response['image_url']  = $this->common_model->get_image_url('user',$user_info->user_id);
                $response['gender']     = "Unknown";
                if($user_info->gender =='1'):
                    $response['gender']      = "Male";
                elseif($user_info->gender =='0'):
                    $response['gender']      = "Female";
                endif;
                $response['join_date']  = $user_info->join_date;
                $response['last_login'] = $user_info->last_login;
            else:
                $response['status']     = 'error';
                $response['data']       = 'Emai & username not match.Please try again.';
            endif;
        else:
            $response['status']     = 'error';
            $response['data']       = 'Please enter valid email & password.';
        endif;
        $this->response($response,200);
    }


    // signup function
    public function signup_post() {
        $name                       =   trim($this->input->post('name'));
        $email                      =   trim($this->input->post('email'));
        $password                   =   trim($this->input->post('password'));
        //var_dump($password);
        if (filter_var($email, FILTER_VALIDATE_EMAIL) && $password !='' && $password !=NULL && strlen($password) > 3):
            $md5_password               = md5($password);         
            $signup_ability             = $this->api_v130_model->check_signup_ability_by_email( $email);       
            if ($signup_ability):
                $this->api_v130_model->create_user($name, $email ,$md5_password);
                $this->load->model('email_model');
                $this->email_model->account_opening_email($email, $password);
                $user_info              = $this->api_v130_model->get_user_info( $email ,$md5_password);                        
                $response['status']     = 'success';
                $response['user_id']    = $user_info->user_id;
                $response['name']       = $user_info->name;
                $response['email']      = $user_info->email;
                $response['phone']      = $user_info->phone;
                $response['password_available']     = TRUE;
                $response['image_url']  = $this->common_model->get_image_url('user',$user_info->user_id);
                $response['gender']     = "Unknown";
                if($user_info->gender =='1'):
                    $response['gender']      = "Male";
                elseif($user_info->gender =='0'):
                    $response['gender']      = "Female";
                endif;
                $response['join_date']  = $user_info->join_date;
                $response['last_login'] = $user_info->last_login;
            else:
                $response['status']     = 'error';
                $response['data']       = 'Email already exist.';
            endif;
        else:
            $response['status']     = 'error';
            $response['data']       = 'Please enter valid email & password.';
        endif;
        $this->response($response,200);
    }

    // get_user_details_by_user_id function
    public function user_details_by_user_id_get() {
        $user_id                      =   trim($this->input->get('id'));
        $this->load->driver('cache', array('adapter' => 'redis', 'backup' => 'file'));
        
        if (is_numeric($user_id) && $user_id !='' && $user_id !=NULL):
            $cache_key = 'user_details_v130_' . $user_id;
            
            if ( ! $response = $this->cache->get($cache_key)):
                $is_valid_user_id               = $this->api_v130_model->validate_user_by_id( $user_id);        
                if ($is_valid_user_id):
                    $user_info              = $this->api_v130_model->get_user_info_by_user_id($user_id);
                    $response['status']     = 'success';
                    $response['user_id']    = $user_info->user_id;
                    $response['name']       = $user_info->name;
                    $response['email']      = $user_info->email;
                    $response['phone']      = $user_info->phone;
                    $response['password_available']     = $this->api_v130_model->check_password_set_status($user_id);
                    $response['image_url']  = $this->common_model->get_image_url('user',$user_id);
                    $response['gender']     = "Unknown";
                    $response['is_authorized']     = "0";
                    if($user_info->user_id =='1')
                        $response['is_authorized']     = "1";
                    if($user_info->gender =='1'):
                        $response['gender']      = "Male";
                    elseif($user_info->gender =='0'):
                        $response['gender']      = "Female";
                    endif;
                    $response['join_date']  = $user_info->join_date;
                    $response['last_login'] = $user_info->last_login;
                else:
                    $response['status']     = 'error';
                    $response['data']       = 'User ID not found.';
                endif;
                // Cache for 5 minutes
                $this->cache->save($cache_key, $response, 300);
            endif;
        else:
            $response['status']     = 'error';
            $response['data']       = 'Please enter valid user ID.';
        endif;
        $this->response($response,200);
    }


    // get_user_details_by_email function
    public function user_details_by_email_get() {                
        $email                      =   trim($this->input->get('email'));
        if (filter_var($email, FILTER_VALIDATE_EMAIL) && $email !='' && $email !=NULL):            
            $is_valid_email               = $this->api_v130_model->validate_user_by_email( $email);        
            if ($is_valid_email):
                $user_info              = $this->api_v130_model->get_user_info_by_email($email);
                $response['status']     = 'success';
                $response['user_id']    = $user_info->user_id;
                $response['name']       = $user_info->name;
                $response['email']      = $user_info->email;
                $response['image_url']  = $this->common_model->get_image_url('user',$user_info->user_id);
                $response['gender']     = "Unknown";
                if($user_info->gender =='1'):
                    $response['gender']      = "Male";
                elseif($user_info->gender =='0'):
                    $response['gender']      = "Female";
                endif;
                $response['join_date']  = $user_info->join_date;
                $response['last_login'] = $user_info->last_login;
            else:
                $response['status']     = 'error';
                $response['data']       = 'Email not found.';
            endif;
        else:
            $response['status']     = 'error';
            $response['data']       = 'Please enter valid email.';
        endif;            
        $this->response($response,200);
    }

    // update profile function
    public function update_profile_post() {
        $this->form_validation->set_rules('id', 'User ID', 'trim|required|numeric');
        $this->form_validation->set_rules('name', 'Name', 'trim|min_length[2]');        
        $this->form_validation->set_rules('email', 'Email', 'trim|valid_email|min_length[4]');        
        $this->form_validation->set_rules('current_password', 'Current Password', 'trim|required|min_length[4]');
        $this->form_validation->set_rules('password', 'New Password', 'trim|min_length[4]');
        $this->form_validation->set_rules('phone', 'Phone', 'trim');
        if ($this->form_validation->run() == FALSE):
            $response['status']     = 'error';
            $response['data']       = 'Invalid input.Please type all field carefully.';
        else:
            $user_id                    =   $this->input->post('id');
            $name                       =   $this->input->post('name');
            $email                      =   $this->input->post('email');
            $current_password           =   md5($this->input->post('current_password'));
            $password                   =   $this->input->post('password');
            $gender                     =   $this->input->post('gender');      
            $phone                      =   $this->input->post('phone');      
            $is_valid_user_id           = $this->api_v130_model->validate_user_by_id_password($user_id,$current_password);        
            if ($is_valid_user_id):
                $data['email']              =   $email;
                if(!empty($name) && $name !='' && $name !=NULL):
                    $data['name']           =   $name;
                endif;
                if(!empty($password) && $password !='' && $password !=NULL):
                    $data['password']           =   md5($password);
                endif;
                if(!empty($gender) && $gender !='' && $gender !=NULL):
                    if($gender=='Male'):
                        $data['gender']           =   '1';
                    elseif($gender=='Female'):
                        $data['gender']           =   '0';
                    endif;
                endif;

                if(!empty($phone) && $phone !='' && $phone !=NULL):
                    $data['phone']           =   $phone;
                endif;

                $this->api_v130_model->update_profile($user_id,$data);
                if(!empty($_FILES['photo']))
                    move_uploaded_file($_FILES['photo']['tmp_name'], 'uploads/user_image/' .$user_id.'.jpg');
                $response['status']     = 'success';
                $response['data']       = 'Profile updated successfully.';
            else:
                $response['status']     = 'error';
                $response['data']       = 'Authentication fail.';
            endif;
        endif;         
        $this->response($response,200);
    }

    // set user password
    public function set_password_post() {
        $this->form_validation->set_rules('user_id', 'User ID', 'trim|required|numeric');
        $this->form_validation->set_rules('firebase_auth_uid', 'Name', 'trim|required|min_length[4]');
        $this->form_validation->set_rules('password', 'Password', 'trim|required|min_length[4]');
        if ($this->form_validation->run() == FALSE):
            $response['status']     = 'error';
            $response['data']       = 'User ID or Password or Firebase Authentication UID absent.';
        else:
            $user_id                    =   $this->input->post('user_id');
            $firebase_auth_uid          =   $this->input->post('firebase_auth_uid');
            $password                   =   $this->input->post('password');

            $is_valid_user              = $this->api_v130_model->validate_user_by_id_firebase_auth_uid($user_id,$firebase_auth_uid);        
            if ($is_valid_user):
                $data['password']           =   md5($password);
                $data['is_password_set']    =   1;
                if($this->api_v130_model->check_password_set_status($user_id) !== TRUE):
                    $this->api_v130_model->update_profile($user_id,$data);
                    $response['status']     = 'success';
                    $response['data']       = 'Password set successfully.';
                else:
                    $response['status']     = 'error';
                    $response['data']       = 'Hahahaha!!!You are not allowed to set password for this user.';
                endif;
            else:
                $response['status']     = 'error';
                $response['data']       = 'Invalid User details.';
            endif;
        endif;         
        $this->response($response,200);
    }

    // deactivate account function
    public function deactivate_account_post() {        
        $user_id                    =   trim($this->input->post('id'));
        $password                   =   md5(trim($this->input->post('password')));
        $reason                     =   trim($this->input->post('reason'));
        if ($password !='' && $password !=NULL && $reason !='' && $reason !=NULL):          
            $user_exist               = $this->api_v130_model->validate_user_by_id_password($user_id ,$password);        
            if ($user_exist):
                $credential    =   array('user_id' => $user_id , 'password' => $password );
                $this->db->where($credential);
                $this->db->update('user', array('status' => '0','deactivate_reason'=>$reason));
                $response['status']     = 'success';
                $response['data']       = 'Account successfully deactivated.';
            else:
                $response['status']     = 'error';
                $response['data']       = 'Please send valid user ID & password.';
            endif;
        else:
            $response['status']     = 'error';
            $response['data']       = 'Please enter user ID & password.';
        endif;            
        $this->response($response,200);
    }



    // get search function
    public function search_get() {
        $q                  =   $this->input->get('q');
        $page               =   $this->input->get('page');
        $type               =   $this->input->get('type');
        $genre_id           =   $this->input->get('genre_id');
        $country_id         =   $this->input->get('country_id');
        $tv_category_id     =   $this->input->get('type');
        $range_to           =   $this->input->get('range_to');
        $range_from         =   $this->input->get('range_from');

        $this->load->driver('cache', array('adapter' => 'redis', 'backup' => 'file'));
        $cache_key = 'search_v130_' . md5($q . $page . $type . $genre_id . $country_id . $range_to . $range_from);
        
        if ( ! $response = $this->cache->get($cache_key)) {
            $response['movie']              =   array();
            $response['tvseries']           =   array();
            $response['tv_channels']        =   array();

            if(strpos($type, "movie")!==false):
                // Get movies by title
                $movies_by_title = $this->api_v130_model->get_movie_search_result($q,$page,$genre_id,$country_id,$range_to,$range_from);
                // Get movies by actor name
                $movies_by_actor = array();
                if(!empty($q) && $q !='' && $q !=NULL):
                    $movies_by_actor = $this->api_v130_model->search_by_actor_name_by_type($q,$page,'0',$genre_id,$country_id,$range_to,$range_from);
                endif;
                // Merge and remove duplicates
                $response['movie'] = $this->merge_unique_videos($movies_by_title, $movies_by_actor);
            endif;

            if(strpos($type, "series")!==false):
                // Get series by title
                $series_by_title = $this->api_v130_model->get_tvseries_search_result($q,$page,$genre_id,$country_id,$range_to,$range_from);
                // Get series by actor name
                $series_by_actor = array();
                if(!empty($q) && $q !='' && $q !=NULL):
                    $series_by_actor = $this->api_v130_model->search_by_actor_name_by_type($q,$page,'1',$genre_id,$country_id,$range_to,$range_from);
                endif;
                // Merge and remove duplicates
                $response['tvseries'] = $this->merge_unique_videos($series_by_title, $series_by_actor);
            endif;

            if(strpos($type, "live")!==false):
                $response['tv_channels']              =   $this->api_v130_model->get_tv_channel_search_result($q,$page,$tv_category_id);
            endif;
            $this->cache->save($cache_key, $response, 300); 
        }
        
        $this->response($response,200);
    }

    // Helper function to merge videos and remove duplicates
    private function merge_unique_videos($array1, $array2) {
        $merged = array_merge($array1, $array2);
        $unique = array();
        $ids = array();
        
        foreach($merged as $video):
            if(!in_array($video['videos_id'], $ids)):
                $unique[] = $video;
                $ids[] = $video['videos_id'];
            endif;
        endforeach;
        
        return $unique;
    }

    // search by actor/star name function
    public function search_by_actor_get() {
        $q                  =   $this->input->get('q');
        $this->load->driver('cache', array('adapter' => 'redis', 'backup' => 'file'));
        if(!empty($q) && $q !='' && $q !=NULL):
            $page               =   $this->input->get('page');
            $cache_key = 'search_by_actor_v130_' . md5($q . $page);
            if ( ! $response = $this->cache->get($cache_key)):
                $response           =   $this->api_v130_model->search_by_actor_name($q, $page);
                $this->cache->save($cache_key, $response, 300);
            endif;
        else:
            $response['status']     = 'error';
            $response['message']    = 'Search query (q) is required.';
        endif;
        $this->response($response,200);
    }

    // get app config function
    public function config_get() { 
        $this->load->driver('cache', array('adapter' => 'redis', 'backup' => 'file'));
        if ( ! $response = $this->cache->get('config_v130')) {
            $response['app_config']                         =   $this->api_v130_model->get_app_config();
            $response['ads_config_new']                     =   $this->api_v130_model->get_ads_config_new();
            $response['ads_config']                         =   $this->api_v130_model->get_ads_config();
            $response['payment_config']                     =   $this->api_v130_model->get_payment_config();
            $response['apk_version_info']                   =   $this->api_v130_model->get_apk_version_info();
            $response['genre']                              =   $this->api_v130_model->get_all_genre();
            $response['country']                            =   $this->api_v130_model->get_all_country();
            $response['tv_category']                        =   $this->api_v130_model->get_all_tv_channel_category();
            $this->cache->save('config_v130', $response, 3600);
        }
        $this->response($response,200);
    }




    // get favorite function
    public function favorite_get() {        
        $user_id                  =   $this->input->get('user_id');
        $this->load->driver('cache', array('adapter' => 'redis', 'backup' => 'file'));
        if(!empty($user_id) && $user_id !='' && $user_id !=NULL && is_numeric($user_id)):
            $page               =   $this->input->get('page');
            $cache_key = 'favorite_v130_' . $user_id . '_' . $page;
            if ( ! $response = $this->cache->get($cache_key)):
                $response           =   $this->api_v130_model->get_favorite($user_id,$page);
                $this->cache->save($cache_key, $response, 300);
            endif;
        else:
            $response['status']     = 'error';
            $response['message']    = 'Invalid user id.';
        endif;            
        $this->response($response,200);
    }

    // get add_favorite function
    public function add_favorite_get() {        
        $user_id                  =   $this->input->get('user_id');
        if(!empty($user_id) && $user_id !='' && $user_id !=NULL && is_numeric($user_id)):
            $is_valid_user_id         = $this->api_v130_model->validate_user_by_id( $user_id);        
            if ($is_valid_user_id):                        
                $videos_id              =   $this->input->get('videos_id');
                if(!empty($videos_id) && $videos_id !='' && $videos_id !=NULL && is_numeric($videos_id)):
                    $verify                 = $this->api_v130_model->verify_movie_tvseries_id($videos_id);
                    if($verify):
                        $if_exist = $this->api_v130_model->verify_favorite_list($user_id,$videos_id);
                        if(!$if_exist):
                            $this->api_v130_model->add_favorite($user_id,$videos_id);
                            // Clear favorite caches for this user
                            $this->load->driver('cache', array('adapter' => 'redis', 'backup' => 'file'));
                            $this->cache->delete('verify_fav_' . $user_id . '_' . $videos_id);
                            for ($p = 1; $p <= 20; $p++) {
                                $this->cache->delete('favorite_v130_' . $user_id . '_' . $p);
                            }
                            $this->cache->delete('favorite_v130_' . $user_id . '_');
                            $response['status']     = 'success';
                            $response['message']    = 'Added successfully.';
                        else:
                            $response['status']     = 'error';
                            $response['message']    = 'Already exist in your favorite.';
                        endif;
                    else:
                        $response['status']     = 'error';
                        $response['message']    = 'Movie ID not found.';
                    endif;
                else:
                    $response['status']     = 'error';
                    $response['message']    = 'Invalid videos ID.';
                endif;
            else:
                $response['status']     = 'error';
                $response['message']    = 'User ID not found.';
            endif;
        else:
            $response['status']     = 'error';
            $response['message']    = 'Invalid user id.';
        endif;            
        $this->response($response,200);
    }


    // get add_favorite function
    public function verify_favorite_list_get() {        
        $user_id                  =   $this->input->get('user_id');
        $this->load->driver('cache', array('adapter' => 'redis', 'backup' => 'file'));
        if(!empty($user_id) && $user_id !='' && $user_id !=NULL && is_numeric($user_id)):
            $videos_id              =   $this->input->get('videos_id');
            $cache_key = 'verify_fav_' . $user_id . '_' . $videos_id;
            
            if ( ! $response = $this->cache->get($cache_key)):
                $is_valid_user_id         = $this->api_v130_model->validate_user_by_id( $user_id);        
                if ($is_valid_user_id):                        
                    $if_exist = $this->api_v130_model->verify_favorite_list($user_id,$videos_id);
                    if($if_exist):
                        $response['status']     = 'success';
                        $response['message']    = 'Found in favorite.';
                    else:
                        $response['status']     = 'error';
                        $response['message']    = 'Not found in favorite.';
                    endif;
                else:
                    $response['status']     = 'error';
                    $response['message']    = 'User ID not found.';
                endif;
                // Cache for very short time (2-3 mins) because users might add/remove often
                $this->cache->save($cache_key, $response, 180);
            endif;
        else:
            $response['status']     = 'error';
            $response['message']    = 'Invalid user id.';
        endif;            
        $this->response($response,200);
    }

    // get remove_favorite function
    public function remove_favorite_get() {        
        $user_id                  =   $this->input->get('user_id');
        if(!empty($user_id) && $user_id !='' && $user_id !=NULL && is_numeric($user_id)):
            $is_valid_user_id         = $this->api_v130_model->validate_user_by_id( $user_id);        
            if ($is_valid_user_id):                        
                $videos_id              =   $this->input->get('videos_id');
                if(!empty($videos_id) && $videos_id !='' && $videos_id !=NULL && is_numeric($videos_id)):
                    $verify                 = $this->api_v130_model->verify_favorite_list($user_id,$videos_id);
                    if($verify):
                        $this->api_v130_model->remove_favorite($user_id,$videos_id);
                        // Clear favorite caches for this user
                        $this->load->driver('cache', array('adapter' => 'redis', 'backup' => 'file'));
                        $this->cache->delete('verify_fav_' . $user_id . '_' . $videos_id);
                        for ($p = 1; $p <= 20; $p++) {
                            $this->cache->delete('favorite_v130_' . $user_id . '_' . $p);
                        }
                        $this->cache->delete('favorite_v130_' . $user_id . '_');
                        $response['status']     = 'success';
                        $response['message']    = 'Removed successfully.';
                    else:
                        $response['status']     = 'error';
                        $response['message']    = 'Movie ID not found to your favorite list.';
                    endif;
                else:
                    $response['status']     = 'error';
                    $response['message']    = 'Invalid videos ID.';
                endif;
            else:
                $response['status']     = 'error';
                $response['message']    = 'User ID not found.';
            endif;
        else:
            $response['status']     = 'error';
            $response['message']    = 'Invalid user id.';
        endif;            
        $this->response($response,200);
    }


    // password reset function
    public function password_reset_post() {        
        $email                      =   trim($this->input->post('email'));
        //var_dump($password);
        if (filter_var($email, FILTER_VALIDATE_EMAIL)):         
            $user_exist             = $this->common_model->check_email($email);
            if($user_exist):
                $new_password           = $this->common_model->generate_random_string();                
                $this->load->model('email_model');
                if($this->email_model->android_password_reset_email($email,$new_password)):
                    $this->api_v130_model->reset_password($email,$new_password);
                    $response['status']     = 'success';
                    $response['message']    = 'Check you email.We have sent your password throught email.';
                else:
                    $response['status']     = 'error';
                    $response['message']    = 'Unable to reset password. Your server might not be configured to send mail.Please contact with system administrator';
                endif;
                
            else:
                $response['status']     = 'error';
                $response['data']       = 'Email not registered.';
            endif;
        else:
            $response['status']     = 'error';
            $response['message']    = 'Please enter valid email.';
        endif;            
        $this->response($response,200);
    }

    // get single movie,tvseries & live tv details
    public function all_comments_get() {
        $type                       =   $this->input->get('type');
        $id                         =   $this->input->get('id');
        $this->load->driver('cache', array('adapter' => 'redis', 'backup' => 'file'));
        // verify id
        if(!empty($id) && $id !='' && $id !=NULL && is_numeric($id)):
            $verify           = $this->api_v130_model->verify_movie_tvseries_id($id);
            if($verify):
                $cache_key = 'all_comments_v130_' . $id;
                if ( ! $response = $this->cache->get($cache_key)):
                    $response               =   $this->api_v130_model->get_all_comments($id);
                    $this->cache->save($cache_key, $response, 300);
                endif;
            else:
                $response['status']     = 'error';
                $response['message']    = 'Movie ID not found.';
            endif;
        else:
            $response['status']     = 'error';
            $response['message']    = 'ID must be valid.';
        endif;
        $this->response($response,200);
    }

    // add_comments function
    public function comments_post() {        
        $user_id                  =   $this->input->post('user_id');
        if(!empty($user_id) && $user_id !='' && $user_id !=NULL && is_numeric($user_id)):
            $is_valid_user_id         = $this->api_v130_model->validate_user_by_id( $user_id);        
            if ($is_valid_user_id):                        
                $videos_id              =   $this->input->post('videos_id');
                if(!empty($videos_id) && $videos_id !='' && $videos_id !=NULL && is_numeric($videos_id)):
                    //var_dump($videos_id);
                    $verify                 = $this->api_v130_model->verify_movie_tvseries_id($videos_id);
                    if($verify):
                        $comment              =   trim($this->input->post('comment'));
                        if(!empty($comment) && $comment !='' && $comment !=NULL):
                            $comments_id                = $this->api_v130_model->add_comments($user_id,$videos_id,$comment);
                            $response['status']         = 'success';
                            $response['message']        = 'Comment Added successfully.';
                            $response['comments_id']    = $comments_id;
                        else:
                            $response['status']     = 'error';
                            $response['message']    = 'Comments empty.';
                        endif;
                    else:
                        $response['status']     = 'error';
                        $response['message']    = 'Movie ID not found.';
                    endif;
                else:
                    $response['status']     = 'error';
                    $response['message']    = 'Invalid videos ID.';
                endif;
            else:
                $response['status']     = 'error';
                $response['message']    = 'User ID not found.';
            endif;
        else:
            $response['status']     = 'error';
            $response['message']    = 'Invalid user id.';
        endif;            
        $this->response($response,200);
    }

    // add_comments function
    public function add_replay_post(){
        $user_id                  =   $this->input->post('user_id');
        if(!empty($user_id) && $user_id !='' && $user_id !=NULL && is_numeric($user_id)):
            $is_valid_user_id         = $this->api_v130_model->validate_user_by_id( $user_id);        
            if ($is_valid_user_id):                        
                $comments_id              =   $this->input->post('comments_id');
                if(!empty($comments_id) && $comments_id !='' && $comments_id !=NULL && is_numeric($comments_id)):
                    //var_dump($videos_id);
                    $verify                 = $this->api_v130_model->verify_comments_id($comments_id);
                    if($verify):
                        $comment              =   trim($this->input->post('comment'));
                        if(!empty($comment) && $comment !='' && $comment !=NULL):
                            $replay_id                = $this->api_v130_model->add_replay($user_id,$comments_id,$comment);
                            $response['status']         = 'success';
                            $response['message']        = 'Replay Added successfully.';
                            $response['replay_id']      = $replay_id;
                        else:
                            $response['status']     = 'error';
                            $response['message']    = 'Comments empty.';
                        endif;
                    else:
                        $response['status']     = 'error';
                        $response['message']    = 'Comments ID not found.';
                    endif;
                else:
                    $response['status']     = 'error';
                    $response['message']    = 'Invalid videos ID.';
                endif;
            else:
                $response['status']     = 'error';
                $response['message']    = 'User ID not found.';
            endif;
        else:
            $response['status']     = 'error';
            $response['message']    = 'Invalid user id.';
        endif;            
        $this->response($response,200);
    }

    // get single all replay
    public function all_replay_get() {
        $id                         =   $this->input->get('id');
        $this->load->driver('cache', array('adapter' => 'redis', 'backup' => 'file'));
        if(!empty($id) && $id !='' && $id !=NULL && is_numeric($id)):
            $verify           = $this->api_v130_model->verify_comments_id($id);
            if($verify):
                $cache_key = 'replay_v130_' . $id;
                if ( ! $response = $this->cache->get($cache_key)):
                    $response               =   $this->api_v130_model->get_replay_by_comments_id($id);
                    $this->cache->save($cache_key, $response, 300);
                endif;
            else:
                $response['status']     = 'error';
                $response['message']    = 'Movie ID not found.';
            endif;
        else:
            $response['status']     = 'error';
            $response['message']    = 'ID must be valid.';
        endif;
        $this->response($response,200);
    }

    // get preroll ads function
    public function preroll_ads_details_get() {        
        $this->load->driver('cache', array('adapter' => 'redis', 'backup' => 'file'));
        if ( ! $response = $this->cache->get('preroll_ads_v130')) {
            $response['status']       =   $this->db->get_where('config' , array('title'=>'preroll_ads_enable'))->row()->value;
            $response['video_url']    =   $this->db->get_where('config' , array('title'=>'preroll_ads_video'))->row()->value;
            $this->cache->save('preroll_ads_v130', $response, 3600);
        }
        $this->response($response,200);
    }
    // get admob ads function
    public function admob_ads_details_get() {        
        $this->load->driver('cache', array('adapter' => 'redis', 'backup' => 'file'));
        if ( ! $response = $this->cache->get('admob_ads_v130')) {      
            $response['status']                     =   $this->db->get_where('config' , array('title'=>'admob_ads_enable'))->row()->value;
            $response['admob_app_id']               =   $this->db->get_where('config' , array('title'=>'admob_app_id'))->row()->value;
            $response['admob_banner_ads_id']        =   $this->db->get_where('config' , array('title'=>'admob_banner_ads_id'))->row()->value; 
            $response['admob_interstitial_ads_id']  =   $this->db->get_where('config' , array('title'=>'admob_interstitial_ads_id'))->row()->value;           
            $this->cache->save('admob_ads_v130', $response, 3600);
        }
        $this->response($response,200);
    }


    // get admob ads function
    public function ads_get() {        
        $this->load->driver('cache', array('adapter' => 'redis', 'backup' => 'file'));
        if ( ! $response = $this->cache->get('ads_get_v130')) {        
            $response['ima_preroll']=   $this->api_v130_model->get_preroll_ads_details();
            $response['admob']      =   $this->api_v130_model->get_admob_ads_details();            
            $this->cache->save('ads_get_v130', $response, 3600);
        }
        $this->response($response,200);
    }

    // get admob ads function
    public function user_info_by_code_post() {        
        $code                    =   trim($this->input->post('code'));
        if (is_numeric($code) && $code !='' && $code !=NULL):            
            $is_valid_code               = $this->api_v130_model->validate_tv_connection_code($code);       
            if ($is_valid_code):
                $user_id                    =   $this->api_v130_model->get_user_id_by_tv_connection_code($code);
                if (is_numeric($user_id) && $user_id !='' && $user_id !=NULL):            
                    $is_valid_user_id               = $this->api_v130_model->validate_user_by_id( $user_id);        
                    if ($is_valid_user_id):                                
                        $response['status']     = 'success';
                        $response['user_info']=   $this->api_v130_model->get_user_info_by_user_id($user_id);
                    else:
                        $response['status']     = 'error';
                        $response['message']    = 'There is a problem with user.Please contact with system administrator.';
                    endif;
                else:
                    $response['status']     = 'error';
                    $response['message']       = 'Please enter valid user ID.';
                endif;                                
            else:
                $response['status']         = 'error';
                $response['message']        = 'Code invalid or expired.Please regenerate code from your phone.';
            endif;
        else:
            $response['status']         = 'error';
            $response['message']        = 'Please enter valid Code.';
        endif;
        $this->response($response,200);
    }

    // get admob ads function
    public function tv_connection_code_get() {        
        $user_id                    =   trim($this->input->get('id'));
        $this->load->driver('cache', array('adapter' => 'redis', 'backup' => 'file'));
        
        if (is_numeric($user_id) && $user_id !='' && $user_id !=NULL):
            // Codes expire or change fast? Not sure. Let's cache for 3 minutes.
            $cache_key = 'tv_code_' . $user_id;
            if ( ! $response = $this->cache->get($cache_key)):
                $is_valid_user_id               = $this->api_v130_model->validate_user_by_id( $user_id);        
                if ($is_valid_user_id):
                    $response['status']         = 'success';
                    $response['code']           =    $this->api_v130_model->get_tv_connection_code($user_id);
                else:
                    $response['status']     = 'error';
                    $response['message']       = 'User ID not found.';
                endif;
                $this->cache->save($cache_key, $response, 180);
            endif;
        else:
        $response['status']     = 'error';
        $response['message']       = 'Please enter valid user ID.';
        endif;            
        $this->response($response,200);
    }

    // Netflix-style reverse flow — Step 1: TV calls this to get a pending code (no login needed)
    public function generate_tv_code_get() {
        $code = $this->api_v130_model->generate_tv_code();
        $response['status'] = 'success';
        $response['code']   = $code;
        $response['expires_in'] = 300; // 5 minutes in seconds
        $this->response($response, 200);
    }

    // Netflix-style reverse flow — Step 2: Phone calls this to confirm the code shown on TV
    public function confirm_tv_code_post() {
        $code    = trim($this->input->post('code'));
        $user_id = trim($this->input->post('user_id'));

        if (!is_numeric($code) || $code === '' || !is_numeric($user_id) || (int)$user_id <= 0) {
            $this->response(array('status' => 'error', 'message' => 'Invalid code or user_id.'), 200);
            return;
        }

        $is_valid_user = $this->api_v130_model->validate_user_by_id($user_id);
        if (!$is_valid_user) {
            $this->response(array('status' => 'error', 'message' => 'User not found.'), 200);
            return;
        }

        $confirmed = $this->api_v130_model->confirm_tv_code($code, (int)$user_id);
        if ($confirmed) {
            $response['status']     = 'success';
            $response['message']    = 'TV login confirmed.';
        } else {
            $response['status']     = 'error';
            $response['message']    = 'Code invalid, expired, or already used.';
        }
        $this->response($response, 200);
    }

    // Netflix-style reverse flow — Step 3: TV polls this until phone confirms
    public function check_tv_code_get() {
        $code = trim($this->input->get('code'));

        if (!is_numeric($code) || $code === '') {
            $this->response(array('status' => 'error', 'message' => 'Invalid code.'), 200);
            return;
        }

        $result = $this->api_v130_model->check_tv_code_status($code);

        if ($result['status'] === 'confirmed') {
            $user_id = $result['user_id'];
            $is_valid_user = $this->api_v130_model->validate_user_by_id($user_id);
            if ($is_valid_user) {
                $user_info = (array) $this->api_v130_model->get_user_info_by_user_id($user_id);
                unset($user_info['password']);
                $response['status']    = 'confirmed';
                $response['user_info'] = $user_info;
            } else {
                $response['status']  = 'error';
                $response['message'] = 'User account issue. Contact administrator.';
            }
        } elseif ($result['status'] === 'pending') {
            $response['status']  = 'pending';
            $response['message'] = 'Waiting for phone confirmation.';
        } else {
            $response['status']  = 'expired';
            $response['message'] = 'Code expired. Please generate a new one.';
        }
        $this->response($response, 200);
    }

   // get check_validated_subscription_plan
    public function check_user_subscription_status_get() {        
        $user_id                =   $this->input->get('user_id');
        $this->load->driver('cache', array('adapter' => 'redis', 'backup' => 'file'));
        if(!empty($user_id) && $user_id !='' && $user_id !=NULL):
            // User subscription status needs to be real-time or short cache
            $cache_key = 'user_sub_status_' . $user_id;
            if ( ! $response = $this->cache->get($cache_key)):
                $response['status']             =   $this->api_v130_model->check_user_subscription_status($user_id);
                $package_data                   =   $this->api_v130_model->get_user_subscription_package_title_and_expired_date($user_id);
                $response['package_title']      =   $package_data['title'];
                $response['expire_date']        =   $package_data['expire_date'];
                // Short cache time (e.g. 5 minutes) to save DB but keep reasonable freshness
                $this->cache->save($cache_key, $response, 300);
            endif;
        endif;            
        $this->response($response,200);
    }

    // get check_validated_subscription_plan
    public function subscription_history_get() {       
        $user_id                =   $this->input->get('user_id');
        $this->load->driver('cache', array('adapter' => 'redis', 'backup' => 'file'));
        
        if(!empty($user_id) && $user_id !='' && $user_id !=NULL):
            $cache_key = 'subscription_history_' . $user_id;
            if ( ! $response = $this->cache->get($cache_key)):
                $response['active_subscription']    =   $this->api_v130_model->get_active_subscription($user_id);
                $response['inactive_subscription']  =   $this->api_v130_model->get_inactive_subscription($user_id);
                $this->cache->save($cache_key, $response, 300);
            endif;
        else:
            $response['status']     = 'error';
            $response['message']    = 'You must provide user ID.';
        endif;            
        $this->response($response,200);
    }

    // get check_validated_subscription_plan
    public function payment_config_get() {        
        $this->load->driver('cache', array('adapter' => 'redis', 'backup' => 'file'));
        if ( ! $response = $this->cache->get('payment_config_v130')) {        
            $response['config']    =   $this->api_v130_model->get_payment_config();            
            $this->cache->save('payment_config_v130', $response, 3600);
        }
        $this->response($response,200);
    }

    // get check_validated_subscription_plan
    public function store_payment_info_post() {
        $data['plan_id']                    =   $this->input->post('plan_id');
        $data['user_id']                    =   $this->input->post('user_id');
        $data['paid_amount']                =   (int)$this->input->post('paid_amount')/100;
        $data['payment_timestamp']          =   strtotime(date("Y-m-d H:i:s"));
        $data['timestamp_from']             =   strtotime(date("Y-m-d H:i:s"));
        $day                                =   $this->db->get_where('plan', array('plan_id'=>$data['plan_id']))->row()->day;
        $day                                =   '+'.$day.' days';
        $data['timestamp_to']               =   strtotime($day, $data['timestamp_from']);
        $data['payment_method']             =   $this->input->post('payment_method');
        $data['payment_info']               =   $this->input->post('payment_info');
        $data['status']                     =   1;
        $this->db->insert('subscription' , $data);
        $response['status']         = 'success';
        $response['message']        = 'Stored successfully.';            
        $this->response($response,200);
    }

    // get check_validated_subscription_plan
    public function all_package_get() {        
        $this->load->driver('cache', array('adapter' => 'redis', 'backup' => 'file'));
        if ( ! $response = $this->cache->get('all_package_v130')) {        
            $response['package']    =   $this->db->get_where('plan',array('status'=>'1'))->result_array();            
            $this->cache->save('all_package_v130', $response, 3600);
        }
        $this->response($response,200);
    }

     // get cancel subscription
    public function cancel_subscription_get() {        
        $user_id                =   $this->input->get('user_id');
        if(!empty($user_id) && $user_id !='' && $user_id !=NULL):
            $subscription_id                =   $this->input->get('subscription_id');
            if(!empty($subscription_id) && $subscription_id !='' && $subscription_id !=NULL):
                if($this->api_v130_model->process_cancel_subscription($user_id,$subscription_id)):
                    $response['status']     = 'success';
                    $response['message']    = 'Subscription Cancelled.';
                else:
                    $response['status']     = 'error';
                    $response['message']    = 'An error occurs.';
                endif;

            else:
                $response['status']     = 'error';
                $response['message']    = 'You must provide subscription ID.';
            endif;
        else:
            $response['status']     = 'error';
            $response['message']    = 'You must provide user ID.';
        endif;            
        $this->response($response,200);
    }


    // login function
    // firebase auth
    public function firebase_auth_post() {        
        $uid                      =   trim($this->input->post('uid'));
        $email                    = $this->input->post('email'); 
        $phone                    = $this->input->post('phone'); 
        if($uid !='' && $uid !=NULL):         
            $fire_base_auth_id    = $this->api_v130_model->user_exist_by_uid($uid);
            $user_exist_by_email  = $this->api_v130_model->user_exist_by_email($email);        
            $user_exist_by_phone  = $this->api_v130_model->user_exist_by_phone($phone);
            //var_dump($fire_base_auth_id,$user_exist_by_email,$user_exist_by_phone);       
            if($fire_base_auth_id):
                $user_info              = $this->api_v130_model->get_user_info_by_uid($uid);
                if($user_info->status == '1'):
                    $this->api_v130_model->update_last_login_info_by_user_id($user_info->user_id);
                    $response['status']     = 'success';
                    $response['user_id']    = $user_info->user_id;
                    $response['name']       = $user_info->name;
                    $response['email']      = $user_info->email;
                    $response['phone']      = $user_info->phone;
                    $response['password_available']     = $this->api_v130_model->check_password_set_status($user_info->user_id);
                    $response['image_url']  = $this->common_model->get_image_url('user',$user_info->user_id);
                    $response['gender']     = "Unknown";
                    if($user_info->gender =='1'):
                        $response['gender']      = "Male";
                    elseif($user_info->gender =='0'):
                        $response['gender']      = "Female";
                    endif;
                    $response['join_date']  = $user_info->join_date;
                    $response['last_login'] = $user_info->last_login;
                else:
                    $response['status']     = 'error';
                    $response['message']    = 'Account may be block or disabled..';
                endif;
            elseif($user_exist_by_email):
                $user_info              = $this->api_v130_model->get_user_info_by_email($email);
                if($user_info->status == '1'):
                    $this->api_v130_model->update_last_login_info_by_user_id($user_info->user_id);
                    $response['status']     = 'success';
                    $response['user_id']    = $user_info->user_id;
                    $response['name']       = $user_info->name;
                    $response['email']      = $user_info->email;
                    $response['phone']      = $user_info->phone;
                    $response['password_available']     = $this->api_v130_model->check_password_set_status($user_info->user_id);
                    $response['image_url']  = $this->common_model->get_image_url('user',$user_info->user_id);
                    $response['gender']     = "Unknown";
                    if($user_info->gender =='1'):
                        $response['gender']      = "Male";
                    elseif($user_info->gender =='0'):
                        $response['gender']      = "Female";
                    endif;
                    $response['join_date']  = $user_info->join_date;
                    $response['last_login'] = $user_info->last_login;
                else:
                    $response['status']     = 'error';
                    $response['message']    = 'Account may be block or disabled..';
                endif;
            elseif($user_exist_by_phone):
                $user_info              = $this->api_v130_model->get_user_info_by_phone($phone);
                if($user_info->status == '1'):
                    $this->api_v130_model->update_last_login_info_by_user_id($user_info->user_id);
                    $response['status']     = 'success';
                    $response['user_id']    = $user_info->user_id;
                    $response['name']       = $user_info->name;
                    $response['email']      = $user_info->email;
                    $response['phone']      = $user_info->phone;
                    $response['password_available']     = $this->api_v130_model->check_password_set_status($user_info->user_id);
                    $response['image_url']  = $this->common_model->get_image_url('user',$user_info->user_id);
                    $response['gender']     = "Unknown";
                    if($user_info->gender =='1'):
                        $response['gender']      = "Male";
                    elseif($user_info->gender =='0'):
                        $response['gender']      = "Female";
                    endif;
                    $response['join_date']  = $user_info->join_date;
                    $response['last_login'] = $user_info->last_login;
                else:
                    $response['status']     = 'error';
                    $response['message']    = 'Account may be block or disabled..';
                endif;
            else:
                $name = $this->input->post('name');
                if($name =='' || $name == NULL):
                    $name = 'No name set';
                endif;                
                if($email =='' || $email == NULL):
                    $email = uniqid()."@mail.com";
                endif;

                $phone = $this->input->post('phone');
                if($phone =='' || $phone == NULL):
                    $phone = '00000000000';
                endif;

                $gender = strtolower($this->input->post('gender'));
                if($gender =='' || $gender == NULL):
                    $gender = '1';
                elseif($gender == 'male'):
                    $gender = '1';
                elseif($gender =='female'):
                    $gender = '0';
                endif;

                $firebase_data['name']               = $name;
                $firebase_data['username']           = uniqid();
                $firebase_data['email']              = $email;
                $firebase_data['phone']              = $phone;
                $firebase_data['gender']             = $gender;
                $firebase_data['password']           = md5(uniqid());
                $firebase_data['firebase_auth_uid']  = $uid;
                $firebase_data['role']               = 'subscriber';
                $firebase_data['join_date']          = date('Y-m-d H:i:s');
                $firebase_data['last_login']         = date('Y-m-d H:i:s');
                $this->api_v130_model->create_user_by_firebase_auth_uid($firebase_data);
                $user_info              = $this->api_v130_model->get_user_info_by_uid($uid);
                $image_source           =   $this->input->post('image_url');
                if($image_source !='' && $image_source !=NULL):
                    $save_to                =   'uploads/user_image/' .$user_info->user_id.'.jpg';          
                    $this->common_model->grab_image($image_source,$save_to);
                endif;                     
                $response['status']     = 'success';
                $response['user_id']    = $user_info->user_id;
                $response['name']       = $user_info->name;
                $response['email']      = $user_info->email;
                $response['phone']      = $user_info->phone;
                $response['password_available']     = $this->api_v130_model->check_password_set_status($user_info->user_id);
                $response['image_url']  = $this->common_model->get_image_url('user',$user_info->user_id);
                $response['gender']     = "Unknown";
                if($user_info->gender =='1'):
                    $response['gender']      = "Male";
                elseif($user_info->gender =='0'):
                    $response['gender']      = "Female";
                endif;
                $response['join_date']  = $user_info->join_date;
                $response['last_login'] = $user_info->last_login;
            endif;
        else: 
            $response['status']     = 'error';
            $response['message']    = 'Firebase UID is required.';
        endif;            
        $this->response($response,200);
    }

     // request function
    public function request_post() {
        $this->form_validation->set_rules('name', 'Name', 'trim|required');
        $this->form_validation->set_rules('movie_name', 'Movie Name', 'trim|required|min_length[2]');
        $this->form_validation->set_rules('email', 'Email', 'trim|valid_email|min_length[4]');
        $this->form_validation->set_rules('message', 'Message', 'trim');
        if ($this->form_validation->run() == FALSE):
            $response['status']     = 'error';
            $response['data']       = json_encode(strip_tags(validation_errors()));
        else:
            $data['name']           =   trim($this->input->post('name'));
            $data['email']          =   trim($this->input->post('email'));
            $data['movie_name']     =   trim($this->input->post('movie_name'));
            $data['message']        =   trim($this->input->post('message'));
            $this->db->insert('request',$data);
            $response['status']     = 'success';
            $response['data']       = 'Request Sent successfully';
        endif;
        $this->response($response,200);
    }

    public function report_post() {
        $i              =    1;
        $data['issue']  =   '';

        if($this->input->post('video') !="" && $this->input->post('video') !=NULL){
            $data['issue']      .= $i.'- Video ' .$this->input->post('video').'<br>';
            $i++;
        }
        if($this->input->post('audio') !="" && $this->input->post('audio') !=NULL){
            $data['issue']      .= $i.'- Audio ' .$this->input->post('audio').'<br>';
            $i++;
        }
        if($this->input->post('subtitle') !="" && $this->input->post('subtitle') !=NULL){
            $data['issue']   .= $i.'- Subtitle ' .$this->input->post('subtitle').'<br>';
            $i++;
        }

        if($this->input->post('message') !="" && $this->input->post('message') !=NULL){
            $data['message']    = $this->input->post('message');
        }
        $this->form_validation->set_rules('type', 'Type', 'trim|required');
        $this->form_validation->set_rules('id', 'ID', 'trim|required');
        if ($this->form_validation->run() == FALSE):
            $response['status']     = 'error';
            $response['data']       = json_encode(strip_tags(validation_errors()));
        else:
            $data['type']           =   $this->input->post('type');
            $data['id']             =   $this->input->post('id');
            $this->db->insert('report',$data);            
            $response['status']     = 'success';
            $response['data']       = 'Report Sent successfully';
            $this->response($response,200);
        endif;
        $this->response($response,200);
    }
    function verify_app_store_in_app_post($receipt="", $is_sandbox =TRUE) 
    {
        $receipt        = $this->input->post("receipt");
        $is_sandbox     = $this->input->post("is_sandbox");
        //$sandbox should be TRUE if you want to test against itunes sandbox servers
        if ($is_sandbox)
            $verify_host = "ssl://sandbox.itunes.apple.com";
        else
            $verify_host = "ssl://buy.itunes.apple.com";
        
        $json='{"receipt-data" : "'.$receipt.'" }';
        //opening socket to itunes
        $fp = fsockopen ($verify_host, 443, $errno, $errstr, 30);
        if (!$fp) 
        {
            // HTTP ERROR
            $return_data = false;
        } 
        else
        { 
            //iTune's request url is /verifyReceipt     
            $header = "POST /verifyReceipt HTTP/1.0\r\n";
            $header .= "Content-Type: application/x-www-form-urlencoded\r\n";
            $header .= "Content-Length: " . strlen($json) . "\r\n\r\n";
            fputs ($fp, $header . $json);
            $res = '';
            while (!feof($fp)) 
            {
                $step_res = fgets ($fp, 1024);
                $res = $res . $step_res;
            }
            fclose ($fp);
            //taking the JSON response
            $json_source = substr($res, stripos($res, "\r\n\r\n{") + 4);
            //decoding
            $app_store_response_map = json_decode($json_source);
            $app_store_response_status = $app_store_response_map->{'status'};
            if ($app_store_response_status == 0)//eithr OK or expired and needs to synch
            {
                //here are some fields from the json, btw.
                $json_receipt = $app_store_response_map->{'receipt'};
                $transaction_id = $json_receipt->{'transaction_id'};
                $original_transaction_id = $json_receipt->{'original_transaction_id'};
                $json_latest_receipt = $app_store_response_map->{'latest_receipt_info'};
                $return_data = true;
            }
            else
            {
                $return_data = false;
            }
        }
        if($return_data): http_response_code(200); else: http_response_code(404); endif;
    }

    function verify_market_in_app_post($signed_data="", $signature="", $public_key_base64="") 
    {
        $signed_data                = $this->input->post("signed_data");
        $signature                  = $this->input->post("signature");
        $public_key_base64          = $this->input->post("public_key_base64");
        $key =  "-----BEGIN PUBLIC KEY-----\n".
            chunk_split($public_key_base64, 64,"\n").
            '-----END PUBLIC KEY-----';   
        //using PHP to create an RSA key
        $key = openssl_get_publickey($key);
        //$signature should be in binary format, but it comes as BASE64. 
        //So, I'll convert it.
        $signature = base64_decode($signature);   
        //using PHP's native support to verify the signature
        $result = openssl_verify(
                $signed_data,
                $signature,
                $key,
                OPENSSL_ALGO_SHA1);
        if (0 === $result) 
        {
            $return_data =  false;
        }
        else if (1 !== $result)
        {
            $return_data =  false;
        }
        else 
        {
            $return_data =  true;
        }
        if($return_data): http_response_code(200); else: http_response_code(404); endif;
    } 

}
    
