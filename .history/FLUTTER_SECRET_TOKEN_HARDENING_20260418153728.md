# Flutter Secret and Play Token Hardening Guide

## 1) Muc tieu
Tai lieu nay giup ban trien khai mo hinh an toan cho API key/secret va play token tren app Flutter, dua tren bai hoc tu app Android hien tai.

Muc tieu chinh:
- Khong de secret ky token nam trong app client.
- Token link play co TTL ngan, chi dung duoc cho user/session hop le.
- Neu bi reverse app, attacker van khong the tu ky token hop le.

## 2) Nguyen tac cot loi
- Rule #1: Secret that can mint access must stay on server.
- Rule #2: Client only gets short-lived token, never root signing secret.
- Rule #3: Native obfuscation, string masking, anti-debug are only extra friction, not real security boundary.

## 3) Why cach cu khong du
Cac cach sau chi "che" bot, khong chong duoc attacker co kinh nghiem:
- Hardcode key trong Dart const/env.
- Chuyen key sang Android JNI/iOS native va goi qua MethodChannel.
- XOR/encrypt string trong binary.
- Proguard/obfuscation cho Java/Kotlin/Swift.

Ly do:
- Attacker co the dump memory, hook function runtime, patch app, replay request.
- Neu app tu ky token, secret ky se bi trich xuat som hay muon.

## 4) Kien truc de xuat (khuyen nghi)
Flow tong:
1. Flutter app login -> nhan access token user.
2. Khi can play, app goi backend endpoint issue-play-token.
3. Backend validate user/session/device/policy.
4. Backend ky token voi server secret (khong tra secret ve app).
5. Backend tra ve signed play URL hoac token ngan han.
6. App dung URL/token do de play.

Token nen rang buoc:
- userId
- deviceId hoac appInstanceId
- streamPath (exact path)
- iat, exp (TTL 30-120s)
- nonce (one-time)
- Optional: ipHash, geo, appBuild

## 5) API contract mau
### Request
POST /v1/play-token

Body JSON:
```json
{
  "contentId": "movie_15460",
  "episodeId": "ep_03",
  "streamPath": "/media/abc/file.m3u8",
  "deviceId": "abc-device",
  "appVersion": "2.4.9",
  "nonce": "uuid-v4"
}
```

### Response
```json
{
  "playUrl": "https://cdn.example.com/media/abc/file.m3u8?token=...",
  "expiresAt": 1760000000,
  "tokenType": "hmac-v1"
}
```

### Error format
```json
{
  "code": "PLAY_TOKEN_DENIED",
  "message": "session_invalid_or_policy_failed"
}
```

## 6) Server-side ky token (pseudo code)
Vi du Node.js style:
```js
const crypto = require('crypto');

function signPlayToken({streamPath, userId, deviceId, nonce, exp}, secret) {
  const payload = `${streamPath}|${userId}|${deviceId}|${nonce}|${exp}`;
  return crypto.createHmac('sha256', secret).update(payload).digest('hex');
}

function buildPlayUrl(baseUrl, args) {
  const {streamPath, token, exp, nonce, userId, deviceId} = args;
  const qs = new URLSearchParams({
    token, exp: String(exp), nonce, uid: userId, did: deviceId
  });
  return `${baseUrl}${streamPath}?${qs.toString()}`;
}
```

Backend verify should enforce:
- exp not expired.
- nonce not reused (store Redis with short TTL).
- payload fields must match current request path/user/device.
- rate limit by user + IP + device.

## 7) Flutter implementation pattern
### Networking layer
- Tao service PlayTokenService chi co 1 nhiem vu: xin playUrl tu backend.
- Tuyet doi khong tu ky token trong Flutter code.

### Sample Dart sketch
```dart
class PlayTokenService {
  final Dio dio;
  PlayTokenService(this.dio);

  Future<String> issuePlayUrl({
    required String contentId,
    required String streamPath,
    required String deviceId,
    String? episodeId,
  }) async {
    final nonce = const Uuid().v4();
    final res = await dio.post('/v1/play-token', data: {
      'contentId': contentId,
      'episodeId': episodeId,
      'streamPath': streamPath,
      'deviceId': deviceId,
      'appVersion': '2.4.9',
      'nonce': nonce,
    });
    return res.data['playUrl'] as String;
  }
}
```

### Player flow
- User bam play -> call issuePlayUrl.
- Neu success -> player.open(playUrl).
- Neu fail -> show non-blocking error + retry logic theo policy.

## 8) Secret management cho Flutter app
- Khong commit secret vao repo.
- Khong de secret trong --dart-define neu secret co quyen ky token.
- Chi de config public (base API domain, feature flags) trong app.

Dung cho build/release:
- CI secret store (GitHub Actions Secrets/GitLab CI variables).
- Runtime config endpoint (public config only).

## 9) Mobile hardening (phu tro, khong thay server security)
- Android: R8 full mode, native symbol stripping, Play Integrity check.
- iOS: strip symbols, disable debug entitlements for release.
- Runtime checks: root/jailbreak/frida indicators.
- TLS pinning (co rollout strategy de tranh lockout).

Luu y:
- Cac bien phap tren giup tang chi phi tan cong, khong thay the server-issued token architecture.

## 10) Rotation va incident response
- Dinh ky rotate signing secret (vi du 30-90 ngay).
- Ho tro multi-key window (kid) de rotate khong downtime.
- Khi nghi lo key:
  1. Revoke key cu.
  2. Rotate sang key moi.
  3. Force token TTL rat ngan tam thoi.
  4. Tang rate-limit va monitor anomaly.

## 11) Checklist trien khai nhanh
- [ ] Tao endpoint /v1/play-token tren backend.
- [ ] Move logic ky token vao backend.
- [ ] Flutter chi xin playUrl, khong ky local.
- [ ] Add nonce replay protection + exp check.
- [ ] Add user/device binding trong payload ky.
- [ ] Add monitoring cho token issue + token verify.
- [ ] Rotate release keystore password ra khoi source code.
- [ ] Xoa hardcoded secret khoi app binary.

## 12) Anti-pattern can tranh
- Token TTL qua dai (nhieu gio/ngay).
- Chi ky theo filename ma khong bind user/device.
- Tra secret tu API cho client de tu ky.
- Tin vao obfuscation nhu lop bao mat chinh.

## 13) Ke hoach migration de khong vo app dang chay
1. Add endpoint moi tren backend, cho chay song song.
2. App version moi uu tien server-issued token.
3. Fallback tam thoi cho version cu (co gioi han, co sunset date).
4. Khi adoption du cao, tat hinh thuc self-sign tren client.

## 14) Test cases toi thieu
- Replay token cu sau khi da dung -> phai fail.
- Doi streamPath voi cung token -> phai fail.
- Doi deviceId voi cung token -> phai fail.
- Token het han -> phai fail.
- User bi disable/subscription het han -> issue token phai fail.
- Spike requests -> rate limit kick in.

## 15) Tom tat 1 dong
Neu can tranh lo key that su, hay xem client la untrusted: secret ky token phai o server, app Flutter chi nhan token ngan han da ky.

## 16) DMP30 recipe (TTL 30s cho link play)
Neu ban dang dung co che token kieu DMP30, co the dung cong thuc don gian nhu sau.

### Muc tieu DMP30
- TTL = 30 giay.
- Token bind vao path + user + device + nonce.
- Token da dung roi thi khong dung lai duoc.

### Query params de goi stream
- exp: unix time, now + 30.
- nonce: random UUID v4.
- uid: user id.
- did: device id.
- sig: HMAC SHA256.

Vi du URL:
```text
https://cdn.example.com/media/abc/file.m3u8?uid=1001&did=tv-01&exp=1760000030&nonce=1f...&sig=ab...
```

### String canonical de ky
```text
METHOD\nPATH\nuid\ndid\nexp\nnonce
```

Vi du:
```text
GET
/media/abc/file.m3u8
1001
tv-01
1760000030
1f5f9c1a-2c3d-4e5f-9a1b-2c3d4e5f6a7b
```

### Sign tren server (Node.js)
```js
const crypto = require('crypto');

function buildCanonical({method, path, uid, did, exp, nonce}) {
  return [method.toUpperCase(), path, uid, did, String(exp), nonce].join('\n');
}

function signDmp30(input, secret) {
  const canonical = buildCanonical(input);
  return crypto.createHmac('sha256', secret).update(canonical).digest('hex');
}

function issueDmp30Url({baseUrl, path, uid, did, secret}) {
  const exp = Math.floor(Date.now() / 1000) + 30;
  const nonce = crypto.randomUUID();
  const sig = signDmp30({method: 'GET', path, uid, did, exp, nonce}, secret);
  const qs = new URLSearchParams({uid, did, exp: String(exp), nonce, sig});
  return `${baseUrl}${path}?${qs.toString()}`;
}
```

### Verify tren edge/backend
Can check dung thu tu nay:
1. Validate format tham so, reject neu thieu.
2. Kiem tra exp >= now va lech gio cho phep <= 5s.
3. Kiem tra nonce chua tung dung (Redis key nonce, TTL 45-60s).
4. Rebuild canonical tu request thuc te (method + path + params da parse).
5. Tinh lai HMAC va compare constant-time.
6. Match uid/did voi session token dang dang nhap.

Neu pass thi stream, neu fail tra 403.

### Flutter can lam gi
- Chi goi API issue token/url.
- Khong tu ky sig trong Flutter.
- Neu URL het han (403), xin URL moi va retry 1 lan.

### Luu y deployment
- NTP clock phai chuan (server + edge).
- Dung cache key gom path + uid + did + exp + nonce, khong cache theo path don le.
- Rotate secret theo kid de doi khoa khong downtime.
