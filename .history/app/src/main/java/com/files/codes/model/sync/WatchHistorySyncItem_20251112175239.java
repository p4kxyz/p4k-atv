package com.files.codes.model.sync;

import com.google.gson.annotations.SerializedName;
import java.util.List;

/**
 * Model cho dữ liệu lịch sử xem đồng bộ với API json.phim4k.lol
 */
public class WatchHistorySyncItem {
    @SerializedName("position")
    private long position; // Vị trí đã xem (milliseconds)
    
    @SerializedName("duration")
    private long duration; // Tổng thời lượng (milliseconds)
    
    @SerializedName("curUrl")
    private String curUrl; // URL hiện tại đang xem
    
    @SerializedName("curVideo")
    private Video curVideo; // Video hiện tại đang xem
    
    @SerializedName("curSeason")
    private Season curSeason; // Season hiện tại (cho phim bộ)
    
    @SerializedName("curEpisode")
    private Episode curEpisode; // Episode hiện tại (cho phim bộ)
    
    @SerializedName("createdAt")
    private long createdAt; // Timestamp tạo
    
    @SerializedName("videos_id")
    private String videosId; // ID video
    
    @SerializedName("title")
    private String title; // Tiêu đề
    
    @SerializedName("description")
    private String description; // Mô tả
    
    @SerializedName("slug")
    private String slug; // Slug
    
    @SerializedName("release")
    private String release; // Ngày phát hành
    
    @SerializedName("runtime")
    private String runtime; // Thời lượng
    
    @SerializedName("video_quality")
    private String videoQuality; // Chất lượng video
    
    @SerializedName("is_tvseries")
    private String isTvseries; // Có phải phim bộ không
    
    @SerializedName("is_paid")
    private String isPaid; // Có phải trả phí không
    
    @SerializedName("enable_download")
    private String enableDownload; // Cho phép tải không
    
    @SerializedName("thumbnail_url")
    private String thumbnailUrl; // URL thumbnail
    
    @SerializedName("poster_url")
    private String posterUrl; // URL poster
    
    @SerializedName("imdb_rating")
    private String imdbRating; // Điểm IMDB
    
    @SerializedName("genre")
    private List<Genre> genre; // Thể loại
    
    @SerializedName("country")
    private List<Country> country; // Quốc gia
    
    @SerializedName("director")
    private List<Director> director; // Đạo diễn
    
    @SerializedName("writer")
    private List<Writer> writer; // Biên kịch
    
    @SerializedName("cast")
    private List<Cast> cast; // Diễn viên
    
    @SerializedName("season")
    private List<Season> season; // Danh sách season
    
    @SerializedName("videos")
    private List<Video> videos; // Danh sách video (cho movie)
    
    @SerializedName("cast_and_crew")
    private List<Cast> castAndCrew; // Cast and crew
    
    @SerializedName("download_links")
    private List<String> downloadLinks; // Download links
    
    @SerializedName("isMovie")
    private boolean isMovie; // Có phải phim lẻ không
    
    // Episode/File information
    @SerializedName("fileName")
    private String fileName; // Tên file cho phim lẻ
    
    @SerializedName("episodeName") 
    private String episodeName; // Tên tập cho phim bộ
    
    @SerializedName("seasonName")
    private String seasonName; // Tên season
    
    @SerializedName("episodeNumber")
    private int episodeNumber; // Số tập
    
    @SerializedName("seasonNumber")
    private int seasonNumber; // Số season

    // Constructor
    public WatchHistorySyncItem() {
        this.createdAt = System.currentTimeMillis();
    }

    // Getters and Setters
    public long getPosition() {
        return position;
    }

    public void setPosition(long position) {
        this.position = position;
    }

    public long getDuration() {
        return duration;
    }

    public void setDuration(long duration) {
        this.duration = duration;
    }

    public String getCurUrl() {
        return curUrl;
    }

    public void setCurUrl(String curUrl) {
        this.curUrl = curUrl;
    }

    public Video getCurVideo() {
        return curVideo;
    }

    public void setCurVideo(Video curVideo) {
        this.curVideo = curVideo;
    }

    public Season getCurSeason() {
        return curSeason;
    }

    public void setCurSeason(Season curSeason) {
        this.curSeason = curSeason;
    }

    public Episode getCurEpisode() {
        return curEpisode;
    }

    public void setCurEpisode(Episode curEpisode) {
        this.curEpisode = curEpisode;
    }

    public long getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(long createdAt) {
        this.createdAt = createdAt;
    }

    public String getVideosId() {
        return videosId;
    }

    public void setVideosId(String videosId) {
        this.videosId = videosId;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getSlug() {
        return slug;
    }

    public void setSlug(String slug) {
        this.slug = slug;
    }

    public String getRelease() {
        return release;
    }

    public void setRelease(String release) {
        this.release = release;
    }

    public String getRuntime() {
        return runtime;
    }

    public void setRuntime(String runtime) {
        this.runtime = runtime;
    }

    public String getVideoQuality() {
        return videoQuality;
    }

    public void setVideoQuality(String videoQuality) {
        this.videoQuality = videoQuality;
    }

    public String getIsTvseries() {
        return isTvseries;
    }

    public void setIsTvseries(String isTvseries) {
        this.isTvseries = isTvseries;
    }

    public String getIsPaid() {
        return isPaid;
    }

    public void setIsPaid(String isPaid) {
        this.isPaid = isPaid;
    }

    public String getEnableDownload() {
        return enableDownload;
    }

    public void setEnableDownload(String enableDownload) {
        this.enableDownload = enableDownload;
    }

    public String getThumbnailUrl() {
        return thumbnailUrl;
    }

    public void setThumbnailUrl(String thumbnailUrl) {
        this.thumbnailUrl = thumbnailUrl;
    }

    public String getPosterUrl() {
        return posterUrl;
    }

    public void setPosterUrl(String posterUrl) {
        this.posterUrl = posterUrl;
    }

    public String getImdbRating() {
        return imdbRating;
    }

    public void setImdbRating(String imdbRating) {
        this.imdbRating = imdbRating;
    }

    public List<Genre> getGenre() {
        return genre;
    }

    public void setGenre(List<Genre> genre) {
        this.genre = genre;
    }

    public List<Country> getCountry() {
        return country;
    }

    public void setCountry(List<Country> country) {
        this.country = country;
    }

    public List<Director> getDirector() {
        return director;
    }

    public void setDirector(List<Director> director) {
        this.director = director;
    }

    public List<Writer> getWriter() {
        return writer;
    }

    public void setWriter(List<Writer> writer) {
        this.writer = writer;
    }

    public List<Cast> getCast() {
        return cast;
    }

    public void setCast(List<Cast> cast) {
        this.cast = cast;
    }

    public List<Season> getSeason() {
        return season;
    }

    public void setSeason(List<Season> season) {
        this.season = season;
    }

    public boolean isMovie() {
        return isMovie;
    }

    public void setMovie(boolean movie) {
        isMovie = movie;
    }
    
    public void setIsMovie(boolean isMovie) {
        this.isMovie = isMovie;
    }
    
    public List<Video> getVideos() {
        return videos;
    }
    
    public void setVideos(List<Video> videos) {
        this.videos = videos;
    }
    
    public List<Cast> getCastAndCrew() {
        return castAndCrew;
    }
    
    public void setCastAndCrew(List<Cast> castAndCrew) {
        this.castAndCrew = castAndCrew;
    }
    
    public List<String> getDownloadLinks() {
        return downloadLinks;
    }
    
    public void setDownloadLinks(List<String> downloadLinks) {
        this.downloadLinks = downloadLinks;
    }
    
    // Episode/File getters and setters
    public String getFileName() {
        return fileName;
    }
    
    public void setFileName(String fileName) {
        this.fileName = fileName;
    }
    
    public String getEpisodeName() {
        return episodeName;
    }
    
    public void setEpisodeName(String episodeName) {
        this.episodeName = episodeName;
    }
    
    public String getSeasonName() {
        return seasonName;
    }
    
    public void setSeasonName(String seasonName) {
        this.seasonName = seasonName;
    }
    
    public int getEpisodeNumber() {
        return episodeNumber;
    }
    
    public void setEpisodeNumber(int episodeNumber) {
        this.episodeNumber = episodeNumber;
    }
    
    public int getSeasonNumber() {
        return seasonNumber;
    }
    
    public void setSeasonNumber(int seasonNumber) {
        this.seasonNumber = seasonNumber;
    }

    // Nested classes
    public static class Season {
        @SerializedName("seasons_id")
        private String seasonsId;
        
        @SerializedName("seasons_name")
        private String seasonsName;
        
        @SerializedName("episodes")
        private List<Episode> episodes;

        // Getters and Setters
        public String getSeasonsId() {
            return seasonsId;
        }

        public void setSeasonsId(String seasonsId) {
            this.seasonsId = seasonsId;
        }

        public String getSeasonsName() {
            return seasonsName;
        }

        public void setSeasonsName(String seasonsName) {
            this.seasonsName = seasonsName;
        }

        public List<Episode> getEpisodes() {
            return episodes;
        }

        public void setEpisodes(List<Episode> episodes) {
            this.episodes = episodes;
        }
    }
    
    public static class Video {
        @SerializedName("video_file_id")
        private String videoFileId;
        
        @SerializedName("label")
        private String label;
        
        @SerializedName("stream_key")
        private String streamKey;
        
        @SerializedName("file_type")
        private String fileType;
        
        @SerializedName("file_url")
        private String fileUrl;
        
        @SerializedName("subtitle")
        private Object subtitle; // Handle both string array and object

        // Getters and Setters
        public String getVideoFileId() {
            return videoFileId;
        }

        public void setVideoFileId(String videoFileId) {
            this.videoFileId = videoFileId;
        }

        public String getLabel() {
            return label;
        }

        public void setLabel(String label) {
            this.label = label;
        }

        public String getStreamKey() {
            return streamKey;
        }

        public void setStreamKey(String streamKey) {
            this.streamKey = streamKey;
        }

        public String getFileType() {
            return fileType;
        }

        public void setFileType(String fileType) {
            this.fileType = fileType;
        }

        public String getFileUrl() {
            return fileUrl;
        }

        public void setFileUrl(String fileUrl) {
            this.fileUrl = fileUrl;
        }

        public Object getSubtitle() {
            return subtitle;
        }

        public void setSubtitle(Object subtitle) {
            this.subtitle = subtitle;
        }
    }

    public static class Episode {
        @SerializedName("episodes_id")
        private String episodesId;
        
        @SerializedName("episodes_name")
        private String episodesName;
        
        @SerializedName("stream_key")
        private String streamKey;
        
        @SerializedName("file_type")
        private String fileType;
        
        @SerializedName("image_url")
        private String imageUrl;
        
        @SerializedName("file_url")
        private String fileUrl;
        
        @SerializedName("subtitle")
        private Object subtitle; // Change to Object to handle both string array and object

        // Getters and Setters
        public String getEpisodesId() {
            return episodesId;
        }

        public void setEpisodesId(String episodesId) {
            this.episodesId = episodesId;
        }

        public String getEpisodesName() {
            return episodesName;
        }

        public void setEpisodesName(String episodesName) {
            this.episodesName = episodesName;
        }

        public String getStreamKey() {
            return streamKey;
        }

        public void setStreamKey(String streamKey) {
            this.streamKey = streamKey;
        }

        public String getFileType() {
            return fileType;
        }

        public void setFileType(String fileType) {
            this.fileType = fileType;
        }

        public String getImageUrl() {
            return imageUrl;
        }

        public void setImageUrl(String imageUrl) {
            this.imageUrl = imageUrl;
        }

        public String getFileUrl() {
            return fileUrl;
        }

        public void setFileUrl(String fileUrl) {
            this.fileUrl = fileUrl;
        }

        public Object getSubtitle() {
            return subtitle;
        }

        public void setSubtitle(Object subtitle) {
            this.subtitle = subtitle;
        }
    }

    public static class Genre {
        @SerializedName("genre_id")
        private String genreId;
        
        @SerializedName("name")
        private String name;
        
        @SerializedName("url")
        private String url;

        // Getters and Setters
        public String getGenreId() {
            return genreId;
        }

        public void setGenreId(String genreId) {
            this.genreId = genreId;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public String getUrl() {
            return url;
        }

        public void setUrl(String url) {
            this.url = url;
        }
    }

    public static class Country {
        @SerializedName("country_id")
        private String countryId;
        
        @SerializedName("name")
        private String name;
        
        @SerializedName("url")
        private String url;

        // Getters and Setters
        public String getCountryId() {
            return countryId;
        }

        public void setCountryId(String countryId) {
            this.countryId = countryId;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public String getUrl() {
            return url;
        }

        public void setUrl(String url) {
            this.url = url;
        }
    }

    public static class Director {
        @SerializedName("star_id")
        private String starId;
        
        @SerializedName("name")
        private String name;
        
        @SerializedName("url")
        private String url;
        
        @SerializedName("image_url")
        private String imageUrl;

        // Getters and Setters
        public String getStarId() {
            return starId;
        }

        public void setStarId(String starId) {
            this.starId = starId;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public String getUrl() {
            return url;
        }

        public void setUrl(String url) {
            this.url = url;
        }

        public String getImageUrl() {
            return imageUrl;
        }

        public void setImageUrl(String imageUrl) {
            this.imageUrl = imageUrl;
        }
    }

    public static class Writer {
        @SerializedName("star_id")
        private String starId;
        
        @SerializedName("name")
        private String name;
        
        @SerializedName("url")
        private String url;
        
        @SerializedName("image_url")
        private String imageUrl;

        // Getters and Setters
        public String getStarId() {
            return starId;
        }

        public void setStarId(String starId) {
            this.starId = starId;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public String getUrl() {
            return url;
        }

        public void setUrl(String url) {
            this.url = url;
        }

        public String getImageUrl() {
            return imageUrl;
        }

        public void setImageUrl(String imageUrl) {
            this.imageUrl = imageUrl;
        }
    }

    public static class Cast {
        @SerializedName("star_id")
        private String starId;
        
        @SerializedName("name")
        private String name;
        
        @SerializedName("url")
        private String url;
        
        @SerializedName("image_url")
        private String imageUrl;

        // Getters and Setters
        public String getStarId() {
            return starId;
        }

        public void setStarId(String starId) {
            this.starId = starId;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public String getUrl() {
            return url;
        }

        public void setUrl(String url) {
            this.url = url;
        }

        public String getImageUrl() {
            return imageUrl;
        }

        public void setImageUrl(String imageUrl) {
            this.imageUrl = imageUrl;
        }
    }

    /**
     * Inner class để represent một watch history item cho display
     */
    public static class WatchHistoryItem {
        private String videoId;
        private String title;
        private String posterUrl;
        private String thumbnailUrl;
        private String videoUrl; // Thêm field video URL
        private long position;
        private long duration;
        private String description;
        private long createdAt;
        private String videoType;
        private long currentPosition;
        private long totalDuration;
        private long lastWatched;
        private float progressPercentage;
        
        // Metadata fields
        private String releaseDate;
        private String imdbRating;
        private String runtime;
        private String videoQuality;
        private String isTvSeries;
        
        // Episode/File info
        private String fileName; // Tên file cho phim lẻ
        private String episodeName; // Tên tập cho phim bộ
        private String seasonName; // Tên season
        private int episodeNumber; // Số tập
        private int seasonNumber; // Số season

        public WatchHistoryItem() {
        }

        public WatchHistoryItem(String videoId, String title, String posterUrl, long position, long duration, String description, long createdAt, boolean isMovie) {
            this.videoId = videoId;
            this.title = title;
            this.posterUrl = posterUrl;
            this.position = position;
            this.duration = duration;
            this.description = description;
            this.createdAt = createdAt;
            this.isMovie = isMovie;
        }

        public String getVideoId() {
            return videoId;
        }

        public void setVideoId(String videoId) {
            this.videoId = videoId;
        }

        public String getTitle() {
            return title;
        }

        public void setTitle(String title) {
            this.title = title;
        }

        public String getPosterUrl() {
            return posterUrl;
        }

        public void setPosterUrl(String posterUrl) {
            this.posterUrl = posterUrl;
        }

        public String getThumbnailUrl() {
            return thumbnailUrl;
        }

        public void setThumbnailUrl(String thumbnailUrl) {
            this.thumbnailUrl = thumbnailUrl;
        }

        public String getVideoUrl() {
            return videoUrl;
        }

        public void setVideoUrl(String videoUrl) {
            this.videoUrl = videoUrl;
        }

        public long getPosition() {
            return position;
        }

        public void setPosition(long position) {
            this.position = position;
        }

        public long getDuration() {
            return duration;
        }

        public void setDuration(long duration) {
            this.duration = duration;
        }

        public String getDescription() {
            return description;
        }

        public void setDescription(String description) {
            this.description = description;
        }

        public long getCreatedAt() {
            return createdAt;
        }

        public void setCreatedAt(long createdAt) {
            this.createdAt = createdAt;
        }

        public boolean isMovie() {
            return isMovie;
        }

        public void setMovie(boolean movie) {
            isMovie = movie;
        }

        public String getVideoType() {
            return videoType;
        }

        public void setVideoType(String videoType) {
            this.videoType = videoType;
        }

        public long getCurrentPosition() {
            return currentPosition;
        }

        public void setCurrentPosition(long currentPosition) {
            this.currentPosition = currentPosition;
        }

        public long getTotalDuration() {
            return totalDuration;
        }

        public void setTotalDuration(long totalDuration) {
            this.totalDuration = totalDuration;
        }

        public long getLastWatched() {
            return lastWatched;
        }

        public void setLastWatched(long lastWatched) {
            this.lastWatched = lastWatched;
        }

        public float getProgressPercentage() {
            return progressPercentage;
        }

        public void setProgressPercentage(float progressPercentage) {
            this.progressPercentage = progressPercentage;
        }

        /**
         * Calculate progress percentage
         */
        public int getProgressPercentageInt() {
            if (duration > 0) {
                return (int) ((position * 100) / duration);
            }
            return 0;
        }
        
        // Metadata getters and setters
        public String getReleaseDate() {
            return releaseDate;
        }
        
        public void setReleaseDate(String releaseDate) {
            this.releaseDate = releaseDate;
        }
        
        public String getImdbRating() {
            return imdbRating;
        }
        
        public void setImdbRating(String imdbRating) {
            this.imdbRating = imdbRating;
        }
        
        public String getRuntime() {
            return runtime;
        }
        
        public void setRuntime(String runtime) {
            this.runtime = runtime;
        }
        
        public String getVideoQuality() {
            return videoQuality;
        }
        
        public void setVideoQuality(String videoQuality) {
            this.videoQuality = videoQuality;
        }
        
        public String getIsTvSeries() {
            return isTvSeries;
        }
        
        public void setIsTvSeries(String isTvSeries) {
            this.isTvSeries = isTvSeries;
        }
        
        // Episode/File info getters and setters
        public String getFileName() {
            return fileName;
        }
        
        public void setFileName(String fileName) {
            this.fileName = fileName;
        }
        
        public String getEpisodeName() {
            return episodeName;
        }
        
        public void setEpisodeName(String episodeName) {
            this.episodeName = episodeName;
        }
        
        public String getSeasonName() {
            return seasonName;
        }
        
        public void setSeasonName(String seasonName) {
            this.seasonName = seasonName;
        }
        
        public int getEpisodeNumber() {
            return episodeNumber;
        }
        
        public void setEpisodeNumber(int episodeNumber) {
            this.episodeNumber = episodeNumber;
        }
        
        public int getSeasonNumber() {
            return seasonNumber;
        }
        
        public void setSeasonNumber(int seasonNumber) {
            this.seasonNumber = seasonNumber;
        }
        
        // Full metadata fields (JSON strings) - for complete data like mau.json
        private String slug;
        private String enableDownload;
        private String isPaid;
        private String genre; // JSON string
        private String country; // JSON string  
        private String director; // JSON string
        private String writer; // JSON string
        private String cast; // JSON string
        private String curSeason; // JSON string
        private String curEpisode; // JSON string
        private String allSeasons; // JSON string
        private Boolean isMovie;
        
        // Full metadata getters and setters
        public String getSlug() {
            return slug;
        }
        
        public void setSlug(String slug) {
            this.slug = slug;
        }
        
        public String getEnableDownload() {
            return enableDownload;
        }
        
        public void setEnableDownload(String enableDownload) {
            this.enableDownload = enableDownload;
        }
        
        public String getIsPaid() {
            return isPaid;
        }
        
        public void setIsPaid(String isPaid) {
            this.isPaid = isPaid;
        }
        
        public String getGenre() {
            return genre;
        }
        
        public void setGenre(String genre) {
            this.genre = genre;
        }
        
        public String getCountry() {
            return country;
        }
        
        public void setCountry(String country) {
            this.country = country;
        }
        
        public String getDirector() {
            return director;
        }
        
        public void setDirector(String director) {
            this.director = director;
        }
        
        public String getWriter() {
            return writer;
        }
        
        public void setWriter(String writer) {
            this.writer = writer;
        }
        
        public String getCast() {
            return cast;
        }
        
        public void setCast(String cast) {
            this.cast = cast;
        }
        
        public String getCurSeason() {
            return curSeason;
        }
        
        public void setCurSeason(String curSeason) {
            this.curSeason = curSeason;
        }
        
        public String getCurEpisode() {
            return curEpisode;
        }
        
        public void setCurEpisode(String curEpisode) {
            this.curEpisode = curEpisode;
        }
        
        public String getAllSeasons() {
            return allSeasons;
        }
        
        public void setAllSeasons(String allSeasons) {
            this.allSeasons = allSeasons;
        }
        
        public Boolean getIsMovie() {
            return isMovie;
        }
        
        public void setIsMovie(Boolean isMovie) {
            this.isMovie = isMovie;
        }
    }
}