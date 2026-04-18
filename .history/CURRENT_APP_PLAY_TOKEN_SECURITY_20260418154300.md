# Current App Play Token Security Guide

## 1. Pham vi tai lieu
Tai lieu nay mo ta cach bao mat token link play trong app hien tai (Android TV), dua tren implementation dang co trong:
- app/src/main/java/com/files/codes/utils/VideoTokenGenerator.java
- app/src/main/jni/api_config.cpp
- app/src/main/java/com/files/codes/AppConfig.java
- app/proguard-rules.pro

Muc tieu:
- Giam nguy co lo token link play trong app hien tai.
- Khong pha vo flow playback hien co.
- Co checklist ro rang de hardening theo muc uu tien.

## 2. Hien trang bao mat cua app
### 2.1 Luong hien tai
1. App goi VideoTokenGenerator.resolveStreamUrlAsync(...).
2. Neu URL thuoc cdn.phim4k.lol, app goi nativeGenVideoUrl(filename).
3. Native code tao signed URL voi token + ts.
4. App goi endpoint signed URL de lay stream URL thuc te.

### 2.2 Diem da lam dung
- Logic ky token da dua vao native (khong ky bang Java thuong).
- Co check anti-debug/anti-hook co ban trong native (TracerPid, maps scan).
- Co xoa memory cho mot so bien nhay cam (secureZero, secureClearString).

### 2.3 Diem con lo
- Plaintext van ton tai trong native source (SERVER_URL, API_KEY).
- App van la ben tu ky token (client-side signer), nen ve ban chat van co the bi reverse va clone logic ky.
- Proguard keep rules qua rong cho nhieu package, lam reverse de doc hon.
- Neu backend khong enforce nonce/TTL/chat replay, token co the bi dung lai.

## 3. Mo hinh de doa (thuc te)
Attacker co the:
1. Reverse APK/AAB de tim native flow.
2. Hook JNI runtime de lay input/output ky token.
3. Replay token neu TTL dai hoac backend khong chong replay.
4. Patch app bo qua check anti-debug.

Ket luan quan trong:
- Client hardening chi tang chi phi tan cong.
- Khong co cach "giau tuyet doi" secret neu app tu ky token.

## 4. Hardening trong app hien tai (khong doi architecture server)
## 4.1 Muc P0 (lam ngay)
1. Bo plaintext secret trong native source.
- Khong de SERVER_URL/API_KEY dang chuoi ro.
- Dung split + mask + runtime decode.

2. Khong expose secret qua Java API.
- Chi expose ham tao signed URL.
- Khong them getSecret/getSigningKey.

3. Rut ngan TTL token (DMP30).
- exp <= 30s.
- Truong hop bat buoc, toi da 60s.

4. Khong log token/link da ky.
- Xoa/chan toan bo log co chua token, sig, ts.

## 4.2 Muc P1
1. Tang rang buoc payload ky.
- Ky theo method + path + uid + did + exp + nonce.
- Khong ky theo filename don le.

2. Tang runtime checks.
- Kiem tra root/emulator/frida theo policy.
- Fail-safe: khong crash vong lap, tra loi tu choi co kiem soat.

3. Sieu hoa obfuscation Java.
- Giam keep rule qua rong trong proguard-rules.pro.
- Chi keep toi thieu class bat buoc cho reflection/retrofit/gson.

## 4.3 Muc P2
1. Dung split secret theo nhieu segment compile-time.
2. Co key rotation co version (kid) trong native logic.
3. Pin cert cho endpoint cap stream/token (neu van hanh cho phep).

## 5. Hardening o backend/cdn (bat buoc de token an toan hon)
Ngay ca khi chua doi architecture, backend van phai enforce:
1. TTL check nghiem (exp).
2. Nonce one-time (Redis TTL 45-60s).
3. Constant-time compare cho signature.
4. Bind uid/did voi session dang nhap.
5. Rate limit theo uid + did + ip.
6. Reject token neu path khong trung payload da ky.

Neu backend khong enforce cac rule tren, hardening phia app se khong du.

## 6. DMP30 payload recommendation cho app hien tai
Canonical string de ky:

METHOD\nPATH\nuid\ndid\nexp\nnonce

Vi du:
GET
/media/abc/file.m3u8
1001
tv-box-01
1760000030
1f5f9c1a-2c3d-4e5f-9a1b-2c3d4e5f6a7b

Query params de gui:
- uid
- did
- exp
- nonce
- sig

## 7. Checklist audit nhanh truoc release
- [ ] Khong con plaintext secret trong app/src/main/jni/api_config.cpp
- [ ] Khong co log token/link ky trong Java va native
- [ ] TTL token <= 30s
- [ ] Payload ky da bind uid + did + path + nonce
- [ ] Backend da chong replay bang nonce one-time
- [ ] Backend verify exp va compare signature constant-time
- [ ] Proguard rules da thu gon keep scope
- [ ] Da test replay, tamper path, token het han, thay doi deviceId

## 8. Test cases can co
1. Replay cung token lan 2 -> phai 403.
2. Sua path giu nguyen sig -> phai 403.
3. Sua did/uid giu nguyen sig -> phai 403.
4. Delay qua 30s -> phai 403.
5. Brute request issue token -> rate limit kich hoat.

## 9. Gioi han cua mo hinh hien tai
Do app hien tai tu ky token, khong the dat muc "khong lo secret" tuyet doi.

Moc nang cap khuyen nghi:
- Chuyen sang server-issued token (app chi xin token/link da ky),
- Khi do secret ky token roi khoi client hoan toan.

## 10. Tom tat 1 dong
Trong app hien tai, ban co the harden rat nhieu de giam lo token, nhung boundary bao mat that su chi dat duoc khi secret ky token o server, khong o app.
