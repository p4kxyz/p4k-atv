import re
file_path = 'app/src/main/java/com/files/codes/view/PlayerActivity.java'
with open(file_path, 'r', encoding='utf-8') as f: content = f.read()
import os
replacements = []
old_track_sub = re.search(r'AlertDialog\.Builder builder = new AlertDialog\.Builder\(this\);\s*builder\.setTitle\("Ch?n ph? d? .*?builder\.setItems\(.*?\}\);.*?builder\.setNeutralButton\("H?y", \(dialog, which\) -> dialog\.dismiss\(\)\);\s*AlertDialog dialog = builder\.create\(\);\s*dialog\.show\(\);', content, re.DOTALL)
if old_track_sub:
    sub_replacement = '''buildCustomListDialog(
            "Ch?n ph? d? (" + totalTracks + " có s?n)", 
            labels.toArray(new String[0]), 
            -1,
            actions, 
            null
        ).show();'''
    # We need to construct actions array...
    # Not that easy to replace with regex because actions run inside setItems listener.
