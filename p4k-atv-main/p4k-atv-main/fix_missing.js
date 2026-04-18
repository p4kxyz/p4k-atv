const fs = require('fs');
const path = require('path');
const appData = process.env.APPDATA;
const historyDir = path.join(appData, 'Code/User/History');

let filesToRestore = [
    'app/src/main/java/com/files/codes/utils/sync/WatchHistorySyncManager.java',
    'app/src/main/java/com/files/codes/model/SearchContent.java',
    'app/src/main/java/com/files/codes/view/fragments/FilterCardPresenter.java',
    'app/src/main/java/com/files/codes/view/fragments/SpacerItem.java',
    'app/src/main/java/com/files/codes/view/fragments/SpacerPresenter.java',
    'app/src/main/res/layout/item_vertical_card.xml',
    'app/src/main/res/drawable/bg_tag_yellow.xml',
    'app/src/main/res/drawable/bg_tag_blue.xml',
    'app/src/main/res/drawable/bg_tag_green.xml',
    'app/src/main/res/drawable/bg_tag_gray.xml',
    'app/src/main/java/com/files/codes/view/MainActivity.java'
];

function checkDir(dirPath) {
    let entries;
    try {
        entries = fs.readdirSync(dirPath);
    } catch(e) { return; }
    
    for (let entry of entries) {
        let fullPath = path.join(dirPath, entry);
        let stat = fs.statSync(fullPath);
        if (stat.isDirectory()) {
            let jsonPath = path.join(fullPath, 'entries.json');
            if (fs.existsSync(jsonPath)) {
                let data = JSON.parse(fs.readFileSync(jsonPath, 'utf8'));
                let resPath = data.resource;
                
                for (let tgt of filesToRestore) {
                    let tgtUnix = tgt.replace(/\\\\/g, '/');
                    let resUnix = decodeURIComponent(resPath).replace(/\\\\/g, '/');
                    if (resUnix.endsWith(tgtUnix)) {
                        let bestEntry = null;
                        for (let e of data.entries) {
                            if (!bestEntry || e.timestamp > bestEntry.timestamp) {
                                bestEntry = e;
                            }
                        }
                        if (bestEntry) {
                            let srcPath = path.join(fullPath, bestEntry.id);
                            let targetPath = path.join('D:/22/p4k-atv', tgt);
                            fs.mkdirSync(path.dirname(targetPath), {recursive: true});
                            fs.copyFileSync(srcPath, targetPath);
                            console.log('Restored: ' + tgt + ' from ' + bestEntry.timestamp);
                        }
                    }
                }
            }
        }
    }
}
checkDir(historyDir);
