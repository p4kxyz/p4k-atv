# VIP Worker API Documentation

## Base URL
```
https://dmp30.phim4k.lol
```

---

## Xác thực (HMAC-SHA256)

Mỗi request phải kèm 2 query param:

| Param | Mô tả |
|-------|-------|
| `token` | HMAC-SHA256 hex string (64 ký tự) |
| `ts` | Unix timestamp đã mã hóa OTP (8 ký tự hex) |

**Token hết hạn sau 300 giây (5 phút).**

---

## Luồng 2 bước

### BƯỚC 1: Lấy stream URL

```
GET /{filename}?token={token}&ts={ts}
```

**Ví dụ:**
```
GET /KJRY929T61DJ9EX8?token=7a3a89ad...&ts=997b13cf
```

**Response (200 OK):**
```json
{
  "url": "https://dmp30.phim4k.lol/KJRY929T61DJ9EX8?phim=xxx&4k=yyy",
  "expires_in": 21600
}
```

- URL có hiệu lực **6 tiếng** (21600 giây)
- Gắn với IP của client
- Dùng param `phim` + `4k` (mã hóa OTP)
- `phim` chỉ còn **32 ký tự hex** thay vì 64 ký tự

**Lỗi:**
- `403 Forbidden` — token sai hoặc hết hạn
- `400 Bad Request` — thiếu filename

---

### BƯỚC 2: Stream video

```
GET /{filename}?phim={signature}&4k={encrypted_exp}
```

Dùng URL nhận được từ BƯỚC 1 để stream video (hỗ trợ Range/seek).

- `phim` = **32 ký tự hex đầu** của `HMAC-SHA256(secret, "filename:exp_plain:ip")`
- `4k` = expiry đã mã hóa OTP, dài 8 ký tự hex

**Lỗi:**
- `403 Forbidden` — stream URL sai, hết hạn hoặc sai IP

---

## Cách gen token (BƯỚC 1)

> **Lưu ý:** Code dưới đây chỉ gen URL cho BƯỚC 1 (với token+ts).  
> App phải GET URL này để nhận JSON response chứa stream URL thực tế.

### Bước 1 — Tính OTP mask (cố định theo secret)
```
mask = HMAC-SHA256(secret, "otp-ts-mask")[0..3]   // lấy 4 bytes đầu
```

### Bước 2 — Mã hóa timestamp
```
ts_plain  = Unix timestamp hiện tại (số nguyên giây)
ts_bytes  = ts_plain dạng big-endian uint32 (4 bytes)
ts_cipher = ts_bytes XOR mask  →  hex 8 ký tự  (gửi lên URL)
```

### Bước 3 — Tạo token
```
token = HMAC-SHA256(secret, "{filename}:{ts_plain}")  →  hex 64 ký tự
```

---

## Code gen token

### PHP
```php
function genVideoUrl($filename) {
    $secret = '9f3c2a7e6b1d4c8fa2e97d5b0c3a41f8';
    $ts     = time();

    // OTP mask (4 bytes đầu)
    $mask   = substr(hash_hmac('sha256', 'otp-ts-mask', $secret, true), 0, 4);

    // Mã hóa ts
    $tsBytes = pack('N', $ts);
    $encTs   = bin2hex($tsBytes ^ $mask);

    // Token
    $token = hash_hmac('sha256', "{$filename}:{$ts}", $secret);

    return "https://dmp30.phim4k.lol/{$filename}?token={$token}&ts={$encTs}";
}

// Dùng:
echo genVideoUrl('KJRY929T61DJ9EX8');
```

### Node.js / JavaScript
```js
const crypto = require('crypto');

function genVideoUrl(filename) {
  const secret = '9f3c2a7e6b1d4c8fa2e97d5b0c3a41f8';
  const ts     = Math.floor(Date.now() / 1000);

  // OTP mask
  const mask = crypto.createHmac('sha256', secret)
    .update('otp-ts-mask').digest().slice(0, 4);

  // Mã hóa ts
  const tsBytes = Buffer.alloc(4);
  tsBytes.writeUInt32BE(ts);
  const encTs = Buffer.from([
    tsBytes[0] ^ mask[0], tsBytes[1] ^ mask[1],
    tsBytes[2] ^ mask[2], tsBytes[3] ^ mask[3],
  ]).toString('hex');

  // Token
  const token = crypto.createHmac('sha256', secret)
    .update(`${filename}:${ts}`).digest('hex');

  return `https://dmp30.phim4k.lol/${filename}?token=${token}&ts=${encTs}`;
}

// Dùng:
console.log(genVideoUrl('KJRY929T61DJ9EX8'));
```

### Python
```python
import hmac, hashlib, time, struct

def gen_video_url(filename):
    secret = b'9f3c2a7e6b1d4c8fa2e97d5b0c3a41f8'
    ts     = int(time.time())

    # OTP mask
    mask = hmac.new(secret, b'otp-ts-mask', hashlib.sha256).digest()[:4]

    # Mã hóa ts
    ts_bytes = struct.pack('>I', ts)
    enc_ts   = bytes(a ^ b for a, b in zip(ts_bytes, mask)).hex()

    # Token
    token = hmac.new(secret, f'{filename}:{ts}'.encode(), hashlib.sha256).hexdigest()

    return f'https://dmp30.phim4k.lol/{filename}?token={token}&ts={enc_ts}'

# Dùng:
print(gen_video_url('KJRY929T61DJ9EX8'))
```

### Dart / Flutter
```dart
import 'dart:convert';
import 'dart:typed_data';
import 'package:crypto/crypto.dart';

String genVideoUrl(String filename) {
  final secret = utf8.encode('9f3c2a7e6b1d4c8fa2e97d5b0c3a41f8');
  final ts     = DateTime.now().millisecondsSinceEpoch ~/ 1000;

  // OTP mask
  final mask = Hmac(sha256, secret)
      .convert(utf8.encode('otp-ts-mask')).bytes.sublist(0, 4);

  // Mã hóa ts (big-endian uint32)
  final tsBytes = ByteData(4)..setUint32(0, ts, Endian.big);
  final encTs   = List.generate(4, (i) => tsBytes.getUint8(i) ^ mask[i])
      .map((b) => b.toRadixString(16).padLeft(2, '0')).join();

  // Token
  final token = Hmac(sha256, secret)
      .convert(utf8.encode('$filename:$ts')).toString();

  return 'https://dmp30.phim4k.lol/$filename?token=$token&ts=$encTs';
}
```

---

## Cách fetch stream URL (BƯỚC 2)

Sau khi gen URL với token+ts, app phải GET để lấy stream URL:

### cURL
```bash
curl "https://dmp30.phim4k.lol/KJRY929T61DJ9EX8?token=xxx&ts=yyy"
```

**Response:**
```json
{
  "url": "https://dmp30.phim4k.lol/KJRY929T61DJ9EX8?phim=xxx&4k=yyy",
  "expires_in": 21600
}
```

### JavaScript (Node.js / Browser)
```js
const signedUrl = genVideoUrl('KJRY929T61DJ9EX8');
const response = await fetch(signedUrl);
const data = await response.json();
const streamUrl = data.url; // <-- Dùng URL này để play video
```

### PHP
```php
$signedUrl = genVideoUrl('KJRY929T61DJ9EX8');
$response = file_get_contents($signedUrl);
$data = json_decode($response, true);
$streamUrl = $data['url']; // <-- Dùng URL này để play video
```

### Python
```python
import requests
signed_url = gen_video_url('KJRY929T61DJ9EX8')
response = requests.get(signed_url)
data = response.json()
stream_url = data['url']  # <-- Dùng URL này để play video
```

---

## Lưu ý bảo mật

- **Không hardcode secret trong client-side code** (web/app).
- Secret chỉ được dùng ở **server/backend** của bạn.
- Client nhận URL đã có token từ backend, dùng trong vòng 300 giây (5 phút).
- Token bị lộ cũng chỉ dùng được **300 giây (5 phút)** và chỉ cho đúng `filename` đó.

---

## Health check
```
GET /health  →  {"status":"ok","timestamp":...}
GET /ping    →  {"status":"ok","timestamp":...}
```
