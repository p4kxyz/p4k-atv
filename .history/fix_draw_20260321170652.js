const fs = require('fs');

const shape = `<?xml version="1.0" encoding="utf-8"?>
<shape xmlns:android="http://schemas.android.com/apk/res/android" android:shape="rectangle">
    <solid android:color="#222222" />
    <corners android:radius="4dp" />
</shape>`;

const vector = `<?xml version="1.0" encoding="utf-8"?>
<vector xmlns:android="http://schemas.android.com/apk/res/android"
    android:width="24dp" android:height="24dp"
    android:viewportWidth="24.0" android:viewportHeight="24.0">
    <path android:fillColor="#FFFFFF" android:pathData="M11.99,2C6.47,2 2,6.48 2,12s4.47,10 9.99,10C17.52,22 22,17.52 22,12S17.52,2 11.99,2zM12,20c-4.42,0 -8,-3.58 -8,-8s3.58,-8 8,-8 8,3.58 8,8 -3.58,8 -8,8z M12.5,7H11v6l5.25,3.15 0.75,-1.23 -4.5,-2.67z"/>
</vector>`;

const shapes = ['bg_bottom_shadow','bg_tag_dark_trans','bg_tag_yellow','bg_tag_orange','bg_tag_blue','bg_tag_green','bg_tag_gray'];

shapes.forEach(n => {
    fs.writeFileSync('app/src/main/res/drawable/' + n + '.xml', shape, 'utf8');
});
fs.writeFileSync('app/src/main/res/drawable/ic_clock.xml', vector, 'utf8');
console.log('Done fixing xml encoding!');
