import os, re 
f1 = 'app/src/main/java/com/files/codes/view/fragments/SearchFragment.java' 
c1 = open(f1, 'r', encoding='utf-8').read() 
c1 = c1.replace('searchContent.setQuality(', 'searchContent.setVideoQuality(') 
c1 = c1.replace('searchContent.setImdb(', 'searchContent.setImdbRating(') 
open(f1, 'w', encoding='utf-8').write(c1) 
