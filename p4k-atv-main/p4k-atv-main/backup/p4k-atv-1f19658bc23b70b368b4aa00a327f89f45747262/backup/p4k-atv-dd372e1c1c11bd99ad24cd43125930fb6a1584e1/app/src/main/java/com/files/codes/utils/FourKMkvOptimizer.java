package com.files.codes.utils;

import android.content.Context;
import com.google.android.exoplayer2.C;
import com.google.android.exoplayer2.DefaultLoadControl;
import com.google.android.exoplayer2.LoadControl;
import com.google.android.exoplayer2.DefaultRenderersFactory;
import com.google.android.exoplayer2.trackselection.DefaultTrackSelector;
import com.google.android.exoplayer2.trackselection.AdaptiveTrackSelection;
import com.google.android.exoplayer2.upstream.DefaultBandwidthMeter;
import com.google.android.exoplayer2.util.MimeTypes;

/**
 * Tối ưu hóa ExoPlayer cho phát MKV 4K mượt mà
 * Optimizes ExoPlayer for smooth 4K MKV playback
 */
public class FourKMkvOptimizer {
    
    /**
     * Tạo LoadControl tối ưu cho 4K MKV
     * Creates optimized LoadControl for 4K MKV
     */
    public static LoadControl createOptimizedLoadControl() {
        return new DefaultLoadControl.Builder()
                // Buffer tối thiểu trước khi phát (5 giây)
                .setBufferDurationsMs(
                        5000,    // minBufferMs - buffer tối thiểu
                        30000,   // maxBufferMs - buffer tối đa 30s cho 4K
                        2500,    // bufferForPlaybackMs - buffer để bắt đầu phát
                        5000     // bufferForPlaybackAfterRebufferMs - buffer sau khi rebuffer
                )
                // Tăng buffer size cho 4K content (64MB)
                .setTargetBufferBytes(64 * 1024 * 1024)
                .setPrioritizeTimeOverSizeThresholds(true)
                .setBackBuffer(20000, true) // 20s back buffer
                .build();
    }
    
    /**
     * Tạo TrackSelector tối ưu cho 4K MKV
     * Creates optimized TrackSelector for 4K MKV
     */
    public static DefaultTrackSelector createOptimizedTrackSelector(Context context) {
        // Adaptive track selection với bandwidth meter
        DefaultBandwidthMeter bandwidthMeter = new DefaultBandwidthMeter.Builder(context).build();
        AdaptiveTrackSelection.Factory trackSelectionFactory = 
                new AdaptiveTrackSelection.Factory();
        
        DefaultTrackSelector trackSelector = new DefaultTrackSelector(context, trackSelectionFactory);
        
        // Cấu hình parameters cho 4K MKV
        DefaultTrackSelector.Parameters.Builder parametersBuilder = 
                new DefaultTrackSelector.Parameters.Builder(context);
        
        // Video settings cho 4K
        parametersBuilder
                .setMaxVideoSize(3840, 2160)              // 4K resolution
                .setMaxVideoBitrate(50000000)             // 50Mbps
                .setPreferredVideoMimeType(MimeTypes.VIDEO_H264) // H.264 preference
                .setExceedRendererCapabilitiesIfNecessary(true)
                .setExceedVideoConstraintsIfNecessary(true);
        
        // Audio settings tối ưu
        parametersBuilder
                .setPreferredAudioLanguage("vi")          // Vietnamese audio
                .setExceedAudioConstraintsIfNecessary(true)
                .setRendererDisabled(C.TRACK_TYPE_AUDIO, false);
        
        // Performance settings
        parametersBuilder
                .setTunnelingEnabled(true)                // Enable tunneling
                .setForceLowestBitrate(false);
        
        trackSelector.setParameters(parametersBuilder.build());
        return trackSelector;
    }
    
    /**
     * Tạo RenderersFactory tối ưu cho 4K MKV
     * Creates optimized RenderersFactory for 4K MKV
     */
    public static DefaultRenderersFactory createOptimizedRenderersFactory(Context context) {
        DefaultRenderersFactory renderersFactory = new DefaultRenderersFactory(context);
        
        // Enable decoder fallback cho tương thích tốt hơn
        renderersFactory.setEnableDecoderFallback(true);
        
        // Extension renderer mode để hỗ trợ thêm codec
        renderersFactory.setExtensionRendererMode(DefaultRenderersFactory.EXTENSION_RENDERER_MODE_PREFER);
        
        return renderersFactory;
    }
    
    /**
     * Kiểm tra device có hỗ trợ 4K không
     * Checks if device supports 4K playback
     */
    public static boolean is4KSupported(Context context) {
        try {
            // Kiểm tra memory available
            Runtime runtime = Runtime.getRuntime();
            long maxMemory = runtime.maxMemory();
            
            // Cần ít nhất 512MB memory cho 4K smooth
            return maxMemory >= (512 * 1024 * 1024);
        } catch (Exception e) {
            return false;
        }
    }
    
    /**
     * Tính toán buffer size tối ưu dựa trên available memory
     * Calculates optimal buffer size based on available memory
     */
    public static int getOptimalBufferSize(Context context) {
        Runtime runtime = Runtime.getRuntime();
        long maxMemory = runtime.maxMemory();
        long usedMemory = runtime.totalMemory() - runtime.freeMemory();
        long freeMemory = maxMemory - usedMemory;
        
        // Sử dụng 25% memory còn lại cho buffer, tối đa 128MB
        int bufferSize = (int) Math.min(freeMemory / 4, 128 * 1024 * 1024);
        
        // Tối thiểu 32MB buffer
        return Math.max(bufferSize, 32 * 1024 * 1024);
    }
}