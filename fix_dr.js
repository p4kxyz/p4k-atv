const fs=require('fs'); 
let s='<?xml version=\" "1.0\ encoding=\utf-8\?><shape xmlns:android=\http://schemas.android.com/apk/res/android\ android:shape=\rectangle\><solid android:color=\#222222\ /><corners android:radius=\4dp\ /></shape>'; 
let names=['bg_bottom_shadow','bg_tag_dark_trans','bg_tag_yellow','bg_tag_orange','bg_tag_blue','bg_tag_green','bg_tag_gray']; 
names.forEach(n=>{fs.writeFileSync('app/src/main/res/drawable/'+n+'.xml', s, 'utf8')}); 
console.log('Fixed'); 
