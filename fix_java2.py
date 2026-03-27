import os, re 
f2 = 'app/src/main/java/com/files/codes/view/MainActivity.java' 
c2 = open(f2, 'r', encoding='utf-8').read() 
open(f2, 'w', encoding='utf-8').write(c2) 
