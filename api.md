# Flutter Decode Guide (AES-GCM from Worker)

Tai lieu nay huong dan trien khai giai ma du lieu tra ve tu Worker trong app Flutter.

## 1. Dau vao API tu Worker

Worker cua ban tra ve JSON dang:

```json
{
  "alg": "AES-GCM-256",
  "encoding": "base64url",
  "iv": "...",
  "ciphertext": "..."
}
```

Noi dung goc sau khi giai ma se co dang:

```json
{
  "data": {
    "native_banner": "...",
    "smartlink": "...",
    "pops": "...",
    "api": "...",
    "tokens": "..."
  },
  "ts": "2026-04-08T...Z"
}
```

## 2. Cai package Flutter

Cap nhat `pubspec.yaml`:

```yaml
dependencies:
  flutter:
    sdk: flutter
  http: ^1.2.2
  cryptography: ^2.7.0
```

Sau do chay:

```bash
flutter pub get
```

## 3. Tao file helper giai ma

Tao file `lib/services/ads_crypto_service.dart`:

```dart
import 'dart:convert';
import 'dart:typed_data';
import 'package:cryptography/cryptography.dart';

class AdsCryptoService {
  static Uint8List base64UrlToBytes(String input) {
    final normalized = input.replaceAll('-', '+').replaceAll('_', '/');
    final padLen = (4 - normalized.length % 4) % 4;
    final padded = normalized + ('=' * padLen);
    return Uint8List.fromList(base64.decode(padded));
  }

  static Future<SecretKey> deriveAesKeyFromSecret(String secret) async {
    final secretBytes = utf8.encode(secret);
    final hash = await Sha256().hash(secretBytes);
    return SecretKey(hash.bytes);
  }

  static Future<Map<String, dynamic>> decryptPayload({
    required Map<String, dynamic> encryptedJson,
    required String secret,
  }) async {
    if (encryptedJson['alg'] != 'AES-GCM-256') {
      throw Exception('Unsupported alg: ${encryptedJson['alg']}');
    }

    final iv = base64UrlToBytes(encryptedJson['iv'] as String);
    final combined = base64UrlToBytes(encryptedJson['ciphertext'] as String);

    if (combined.length < 16) {
      throw Exception('Ciphertext invalid: too short');
    }

    // WebCrypto AES-GCM output = cipherText + 16-byte auth tag.
    final cipherText = combined.sublist(0, combined.length - 16);
    final tagBytes = combined.sublist(combined.length - 16);

    final box = SecretBox(
      cipherText,
      nonce: iv,
      mac: Mac(tagBytes),
    );

    final key = await deriveAesKeyFromSecret(secret);
    final clearBytes = await AesGcm.with256bits().decrypt(box, secretKey: key);
    final clearText = utf8.decode(clearBytes);

    return jsonDecode(clearText) as Map<String, dynamic>;
  }
}
```

## 4. Tao API service goi Worker + decode

Tao file `lib/services/ads_api_service.dart`:

```dart
import 'dart:convert';
import 'package:http/http.dart' as http;
import 'ads_crypto_service.dart';

class AdsApiService {
  AdsApiService({
    required this.baseUrl,
    required this.secret,
  });

  final String baseUrl;
  final String secret;

  Future<Map<String, dynamic>> getAllAds() async {
    final uri = Uri.parse('$baseUrl/api/ads');
    final res = await http.get(uri);

    if (res.statusCode != 200) {
      throw Exception('API error: ${res.statusCode} - ${res.body}');
    }

    final encrypted = jsonDecode(res.body) as Map<String, dynamic>;
    final plain = await AdsCryptoService.decryptPayload(
      encryptedJson: encrypted,
      secret: secret,
    );

    return (plain['data'] as Map).cast<String, dynamic>();
  }

  Future<Map<String, dynamic>> getOneAd(String type) async {
    final uri = Uri.parse('$baseUrl/api/ads/$type');
    final res = await http.get(uri);

    if (res.statusCode != 200) {
      throw Exception('API error: ${res.statusCode} - ${res.body}');
    }

    final encrypted = jsonDecode(res.body) as Map<String, dynamic>;
    final plain = await AdsCryptoService.decryptPayload(
      encryptedJson: encrypted,
      secret: secret,
    );

    return (plain['data'] as Map).cast<String, dynamic>();
  }
}
```

## 5. Cach dung trong UI

Vi du trong `initState` hoac bloc/provider:

```dart
final adsService = AdsApiService(
  baseUrl: 'https://your-worker-domain.workers.dev',
  secret: '9f3c2a7e6b1d4c8fa2e97d5b0c3a41f8',
);

final ads = await adsService.getAllAds();
print('native_banner: ${ads['native_banner']}');
```

Lay 1 loai:

```dart
final one = await adsService.getOneAd('smartlink');
print('type: ${one['type']}');
print('url: ${one['url']}');
```

## 6. Cac loi thuong gap

- Loi `OperationError` khi decrypt:
  - Sai secret.
  - Sai iv/ciphertext.
  - Du lieu bi sua doi tren duong truyen.

- Loi parse JSON:
  - Payload giai ma khong dung dinh dang mong doi.

- API tra ve `Missing SECRET in environment`:
  - Worker chua set secret. Chay: `wrangler secret put SECRET`.

## 7. Ghi chu bao mat

- Neu secret nam trong Flutter app, nguoi dung van co the reverse app de lay secret.
- Mo hinh nay phu hop cho obfuscation/chong doc nhanh payload.
- Neu can bao mat manh, nen de app goi backend rieng va backend moi la noi decrypt.

## 8. Checklist deploy

1. Worker da deploy ban moi nhat.
2. Worker da set `SECRET` bang `wrangler secret put SECRET`.
3. Flutter da `pub get` thanh cong.
4. `baseUrl` dung domain Worker.
5. Secret trong app trung voi secret Worker.
6. Test `getAllAds()` va `getOneAd()` tren build release.
