import re

with open('app/src/main/java/com/files/codes/view/PlayerActivity.java', 'r', encoding='utf-8') as f:
    text = f.read()

# Subtitles on/off
p_sub2 = r'                    AlertDialog\.Builder builder = new AlertDialog\.Builder\(this\);\s*builder\.setTitle\("Subtitles"\);\s*String\[\] options = \{"On", "Off"\};\s*builder\.setItems\(options, \(dialog, which\) -> \{.*?\n                    \}\);\s*builder\.show\(\);'
s_sub2 = '''                    Runnable[] actions = new Runnable[2];
                    actions[0] = () -> {
                        MappingTrackSelector.MappedTrackInfo info = trackSelector.getCurrentMappedTrackInfo();
                        if (info != null) {
                            for (int i = 0; i < info.getRendererCount(); i++) {
                                if (info.getRendererType(i) == C.TRACK_TYPE_TEXT) {
                                    trackSelector.setParameters(trackSelector.buildUponParameters().setRendererDisabled(i, false));
                                }
                            }
                        }
                    };
                    actions[1] = () -> {
                        MappingTrackSelector.MappedTrackInfo info = trackSelector.getCurrentMappedTrackInfo();
                        if (info != null) {
                            for (int i = 0; i < info.getRendererCount(); i++) {
                                if (info.getRendererType(i) == C.TRACK_TYPE_TEXT) {
                                    trackSelector.setParameters(trackSelector.buildUponParameters().setRendererDisabled(i, true));
                                }
                            }
                        }
                    };
                    buildCustomListDialog("Subtitles", new String[]{"On", "Off"}, -1, actions, null).show();'''
text = re.sub(p_sub2, s_sub2, text, flags=re.DOTALL)

# Default showErrorDialog which doesn't take 3 parameters
p_err = r'    private void showErrorDialog\(String title, String message\) \{\s*runOnUiThread\(new Runnable\(\) \{\s*@Override\s*public void run\(\) \{\s*new android\.app\.AlertDialog\.Builder\(PlayerActivity\.this\)\s*\.setTitle\(title\)\s*\.setMessage\(message\)\s*\.setPositiveButton\("OK", null\)\s*\.show\(\);\s*\}\s*\}\);\s*\}'
s_err = '''    private void showErrorDialog(String title, String message) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                buildCustomAlertDialog(
                    title, message,
                    "OK", null,
                    null, null, null, null
                ).show();
            }
        });
    }'''
text = re.sub(p_err, s_err, text, flags=re.DOTALL)

with open('app/src/main/java/com/files/codes/view/PlayerActivity.java', 'w', encoding='utf-8') as f:
    f.write(text)
print("Done final patches.")
