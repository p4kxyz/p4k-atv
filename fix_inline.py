import re
import sys

def safe_replace(content):
    def do_replace(pattern, replacement, text):
        return re.sub(pattern, lambda m: replacement, text, flags=re.DOTALL)

    r3 = r'''        Runnable[] actions = new Runnable[aspectRatioOptions.length];
        for (int i = 0; i < aspectRatioOptions.length; i++) {
            final int index = i;
            actions[i] = () -> {
                if (index == 0) {
                    exoPlayerView.setResizeMode(com.google.android.exoplayer2.ui.AspectRatioFrameLayout.RESIZE_MODE_FIT);
                    exoPlayerView.setUseArtwork(false);
                    player.setVideoScalingMode(com.google.android.exoplayer2.C.VIDEO_SCALING_MODE_SCALE_TO_FIT);
                } else {
                    exoPlayerView.setResizeMode(aspectRatioModes[index]);
                    if (index == 2) {
                        player.setVideoScalingMode(com.google.android.exoplayer2.C.VIDEO_SCALING_MODE_SCALE_TO_FIT_WITH_CROPPING);
                    } else {
                        player.setVideoScalingMode(com.google.android.exoplayer2.C.VIDEO_SCALING_MODE_SCALE_TO_FIT);
                    }
                }
                new ToastMsg(PlayerActivity.this).toastIconSuccess("Đã chuyển: " + aspectRatioOptions[index]);
            };
        }
        buildCustomListDialog("Chọn tỷ lệ màn hình", aspectRatioOptions, -1, actions, null).show();'''
    content = do_replace(r'        AlertDialog\.Builder builder = new AlertDialog\.Builder\(this\);\s*builder\.setTitle\("Chọn tỷ lệ màn hình"\);\s*builder\.setItems\(aspectRatioOptions, \(dialog, which\) -> \{.*?        \}\);\s*builder\.show\(\);', r3, content)

    r7 = r'''        Runnable[] actions = new Runnable[labels.size()];
        for (int i = 0; i < labels.size(); i++) {
            final int which = i;
            actions[i] = () -> {
                int[] pair = selectionPairs.get(which);
                long currentPosition = player.getCurrentPosition();
                boolean wasPlaying = player.getPlayWhenReady();
                try {
                    if (pair[0] == -1) {
                        trackSelector.setParameters(trackSelector.buildUponParameters().setRendererDisabled(renderer, true));
                    } else {
                        int group = pair[0];
                        int track = pair[1];
                        trackSelector.setParameters(
                                trackSelector.buildUponParameters()
                                        .setRendererDisabled(renderer, false)
                                        .clearSelectionOverrides(renderer)
                                        .setSelectionOverride(renderer, mappedTrackInfo.getTrackGroups(renderer),
                                                new com.google.android.exoplayer2.trackselection.DefaultTrackSelector.SelectionOverride(group, track))
                        );
                    }
                } catch (Exception e) {
                    Log.e("PlayerActivity", "Error setting subtitle track", e);
                }
            };
        }
        buildCustomListDialog("Chọn phụ đề (" + totalTracks + " có sẵn)", labels.toArray(new String[0]), -1, actions, null).show();'''
    content = do_replace(r'        AlertDialog\.Builder builder = new AlertDialog\.Builder\(this\);\s*builder\.setTitle\("Chọn phụ đề \(" \+ totalTracks \+ " có sẵn\)"\);\s*builder\.setItems\(labels\.toArray\(new String\[0\]\), \(dialog, which\) -> \{.*?        \}\);\s*builder\.show\(\);', r7, content)

    r8 = r'''        Runnable[] actions = new Runnable[labels.size()];
        for (int i = 0; i < labels.size(); i++) {
            final int which = i;
            actions[i] = () -> {
                int[] pair = selectionPairs.get(which);
                int rendererIndex = pair[0];
                int groupIndex = pair[1];
                int trackIndex = pair[2];
                long currentPosition = player.getCurrentPosition();
                boolean wasPlaying = player.getPlayWhenReady();
                try {
                    com.google.android.exoplayer2.trackselection.DefaultTrackSelector.Parameters.Builder parametersBuilder = trackSelector.buildUponParameters();
                    for (int j = 0; j < mappedTrackInfo.getRendererCount(); j++) {
                        if (mappedTrackInfo.getRendererType(j) == com.google.android.exoplayer2.C.TRACK_TYPE_AUDIO) {
                            parametersBuilder.clearSelectionOverrides(j).setRendererDisabled(j, true);
                        }
                    }
                    parametersBuilder.setRendererDisabled(rendererIndex, false)
                            .setSelectionOverride(rendererIndex,
                                    mappedTrackInfo.getTrackGroups(rendererIndex),
                                    new com.google.android.exoplayer2.trackselection.DefaultTrackSelector.SelectionOverride(groupIndex, trackIndex));
                    trackSelector.setParameters(parametersBuilder);
                } catch (Exception e) {
                    Log.e("PlayerActivity", "Error setting audio track", e);
                }
            };
        }
        buildCustomListDialog("Chọn âm thanh (" + labels.size() + " có sẵn)", labels.toArray(new String[0]), -1, actions, null).show();'''
    content = do_replace(r'        AlertDialog\.Builder builder = new AlertDialog\.Builder\(this\);\s*builder\.setTitle\("Chọn âm thanh \(" \+ labels\.size\(\) \+ " có sẵn\)"\);\s*builder\.setItems\(labels\.toArray\(new String\[0\]\), \(dialog, which\) -> \{.*?        \}\);\s*builder\.show\(\);', r8, content)

    r9 = r'''                    Runnable[] actions = new Runnable[2];
                    actions[0] = () -> {
                        com.google.android.exoplayer2.trackselection.MappingTrackSelector.MappedTrackInfo info = trackSelector.getCurrentMappedTrackInfo();
                        if (info != null) {
                            for (int i = 0; i < info.getRendererCount(); i++) {
                                if (info.getRendererType(i) == com.google.android.exoplayer2.C.TRACK_TYPE_TEXT) {
                                    trackSelector.setParameters(trackSelector.buildUponParameters().setRendererDisabled(i, false));
                                }
                            }
                        }
                    };
                    actions[1] = () -> {
                        com.google.android.exoplayer2.trackselection.MappingTrackSelector.MappedTrackInfo info = trackSelector.getCurrentMappedTrackInfo();
                        if (info != null) {
                            for (int i = 0; i < info.getRendererCount(); i++) {
                                if (info.getRendererType(i) == com.google.android.exoplayer2.C.TRACK_TYPE_TEXT) {
                                    trackSelector.setParameters(trackSelector.buildUponParameters().setRendererDisabled(i, true));
                                }
                            }
                        }
                    };
                    buildCustomListDialog("Subtitles", new String[]{"On", "Off"}, -1, actions, null).show();'''
    content = do_replace(r'                    AlertDialog\.Builder builder = new AlertDialog\.Builder\(this\);\s*builder\.setTitle\("Subtitles"\);\s*String\[\] options = \{"On", "Off"\};\s*builder\.setItems\(options, \(dialog, which\) -> \{.*?                    \}\);\s*builder\.show\(\);', r9, content)
    
    return content

file_path = 'app/src/main/java/com/files/codes/view/PlayerActivity.java'
with open(file_path, 'r', encoding='utf-8') as f:
    content = f.read()

new_content = safe_replace(content)

with open(file_path, 'w', encoding='utf-8') as f:
    f.write(new_content)

print("Inline dialogs replaced")
