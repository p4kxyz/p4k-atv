import re

with open('app/src/main/java/com/files/codes/view/PlayerActivity.java', 'r', encoding='utf-8') as f:
    text = f.read()

# showTrackSelectionDialog Subtitles
p_sub = r'        AlertDialog\.Builder builder = new AlertDialog\.Builder\(this\);\s*builder\.setTitle\("Chọn phụ đề \(" \+ totalTracks \+ " có sẵn\)"\);\s*builder\.setItems\(labels\.toArray\(new String\[0\]\), \(dialog, which\) -> \{.*?\n        \}\);\s*builder\.show\(\);'
s_sub = '''        Runnable[] actions = new Runnable[labels.size()];
        for (int i = 0; i < labels.size(); i++) {
            final int which = i;
            actions[i] = () -> {
                int[] pair = selectionPairs.get(which);

                // Store current playback position and state
                long currentPosition = player.getCurrentPosition();
                boolean wasPlaying = player.getPlayWhenReady();

                try {
                    if (pair[0] == -1) {
                        // Turn subtitles off
                        trackSelector.setParameters(trackSelector.buildUponParameters().setRendererDisabled(renderer, true));
                    } else {
                        int group = pair[0];
                        int track = pair[1];
                        // Enable renderer and set selection
                        trackSelector.setParameters(
                                trackSelector.buildUponParameters()
                                        .setRendererDisabled(renderer, false)
                                        .clearSelectionOverrides(renderer)
                                        .setSelectionOverride(renderer, mappedTrackInfo.getTrackGroups(renderer),
                                                new DefaultTrackSelector.SelectionOverride(group, track))
                        );
                    }
                } catch (Exception e) {
                    Log.e("PlayerActivity", "Error setting subtitle track", e);
                }
            };
        }
        
        buildCustomListDialog("Cài đặt phụ đề", labels.toArray(new String[0]), -1, actions, null).show();'''
text = re.sub(p_sub, s_sub, text, flags=re.DOTALL)

# showTrackSelectionDialog Audio
p_audio = r'        AlertDialog\.Builder builder = new AlertDialog\.Builder\(this\);\s*builder\.setTitle\("Chọn âm thanh \(" \+ labels\.size\(\) \+ " có sẵn\)"\);\s*builder\.setItems\(labels\.toArray\(new String\[0\]\), \(dialog, which\) -> \{.*?\n        \}\);\s*builder\.show\(\);'
s_audio = '''        Runnable[] actions = new Runnable[labels.size()];
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
                    DefaultTrackSelector.Parameters.Builder parametersBuilder = trackSelector.buildUponParameters();

                    for (int j = 0; j < mappedTrackInfo.getRendererCount(); j++) {
                        if (mappedTrackInfo.getRendererType(j) == C.TRACK_TYPE_AUDIO) {
                            parametersBuilder.clearSelectionOverrides(j).setRendererDisabled(j, true);
                        }
                    }

                    parametersBuilder.setRendererDisabled(rendererIndex, false)
                            .setSelectionOverride(rendererIndex,
                                    mappedTrackInfo.getTrackGroups(rendererIndex),
                                    new DefaultTrackSelector.SelectionOverride(groupIndex, trackIndex));
                                    
                    trackSelector.setParameters(parametersBuilder);
                } catch (Exception e) {
                    Log.e("PlayerActivity", "Error setting audio track", e);
                }
            };
        }
        
        buildCustomListDialog("Chọn âm thanh", labels.toArray(new String[0]), -1, actions, null).show();'''
text = re.sub(p_audio, s_audio, text, flags=re.DOTALL)

with open('app/src/main/java/com/files/codes/view/PlayerActivity.java', 'w', encoding='utf-8') as f:
    f.write(text)

print("Done tracks patch.")
