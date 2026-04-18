# Tài liệu triển khai HMAC key động cho Java

## 1) Mục tiêu

Triển khai phía Java tương thích hoàn toàn với logic Worker hiện tại:

- Hỗ trợ song song 2 secret trong giai đoạn chuyển đổi.
- HMAC_SECRET là secret cũ (ưu tiên ký nếu có).
- HMAC_SECRET_2 là secret mới, sinh key động theo ngày bằng AES-GCM deterministic.
- Verify chấp nhận nhiều key ứng viên để không đứt phiên.

## 2) Biến cấu hình cần có

- HMAC_SECRET: chuỗi secret cũ.
- HMAC_SECRET_2: chuỗi secret mới dùng để sinh key động theo ngày.
- TOKEN_TTL: mặc định 300 giây.
- STREAM_SIG_HEX_LEN: mặc định 32 ký tự hex đầu.

Khuyến nghị lấy từ biến môi trường hoặc vault.

## 3) Thuật toán key động mới (phải khớp 1:1)

### 3.1 Định dạng ngày

- Dùng UTC.
- Định dạng yyyyMMdd. Ví dụ: 20260418.

### 3.2 Sinh derived secret từ HMAC_SECRET_2

Input:

- baseSecret = HMAC_SECRET_2
- dateStr = yyyyMMdd theo UTC

Các bước:

1. keyRaw = SHA-256(baseSecret bytes UTF-8)
2. ivSeed = SHA-256("iv:" + baseSecret)
3. iv = 12 byte đầu của ivSeed
4. plaintext = baseSecret + ":" + dateStr (UTF-8)
5. encrypted = AES-GCM encrypt với keyRaw, iv, plaintext
6. encHex = hex thường (lowercase) của encrypted (bao gồm tag GCM do Java trả về trong doFinal)
7. derivedSecret = baseSecret + ":" + encHex

Lưu ý quan trọng:

- Dùng AES/GCM/NoPadding.
- IV cố định theo secret để ra kết quả deterministic, đúng với Worker.
- Đây là cơ chế derive key tạm thời cho rotate key, không dùng để mã hóa dữ liệu nghiệp vụ.

## 4) Chọn key để ký và verify

### 4.1 Signing secret

- Nếu HMAC_SECRET có giá trị: dùng HMAC_SECRET để ký.
- Nếu HMAC_SECRET rỗng và HMAC_SECRET_2 có giá trị: dùng derivedSecret của hôm nay (UTC).

### 4.2 Candidate secrets cho verify

Tập key verify:

- HMAC_SECRET (nếu có)
- derivedSecret(today)
- derivedSecret(yesterday)
- derivedSecret(tomorrow)

Sau đó unique theo giá trị.

## 5) Logic token auth (token + ts)

Với mỗi secret ứng viên:

1. Giải mã ts:
- maskTs = 4 byte đầu của HMAC-SHA256(secret, "otp-ts-mask")
- encTs là hex 8 ký tự
- timestamp = encTs XOR maskTs

2. Kiểm tra TTL:
- abs(nowEpochSec - timestamp) <= TOKEN_TTL

3. Verify token:
- message = filename + ":" + timestamp
- expected raw bytes = HMAC-SHA256(secret, message)
- token nhận vào là hex của full digest (64 hex)
- so sánh constant-time trên bytes

Nếu một secret verify thành công thì chấp nhận.

## 6) Logic stream URL (phim + 4k)

### 6.1 Generate stream URL

1. expPlain = nowEpochSec + 21600
2. expMask = 4 byte đầu của HMAC-SHA256(signingSecret, "otp-exp-mask")
3. encExp (tham số 4k) = expPlain XOR expMask, encode hex 8 ký tự
4. messagePhim = filename + ":" + expPlain + ":" + asn
5. phimFull = hex(HMAC-SHA256(signingSecret, messagePhim))
6. phim = 32 ký tự hex đầu của phimFull

### 6.2 Verify stream URL

Lặp qua candidate secrets:

1. Giải mã expPlain từ 4k bằng expMask của secret đó.
2. Kiểm tra now <= expPlain.
3. Tính lại phim và so sánh constant-time với giá trị gửi lên.
4. Secret nào khớp thì pass.

## 7) Java reference implementation

```java
import javax.crypto.Cipher;
import javax.crypto.Mac;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;

public final class DynamicHmacCompat {
    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("yyyyMMdd");

    public static String formatDateUtc(int offsetDays) {
        return LocalDate.now(ZoneOffset.UTC).plusDays(offsetDays).format(DATE_FMT);
    }

    public static byte[] sha256(byte[] input) throws Exception {
        MessageDigest md = MessageDigest.getInstance("SHA-256");
        return md.digest(input);
    }

    public static byte[] hmacSha256(String secret, String message) throws Exception {
        Mac mac = Mac.getInstance("HmacSHA256");
        mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
        return mac.doFinal(message.getBytes(StandardCharsets.UTF_8));
    }

    public static String toHex(byte[] bytes) {
        StringBuilder sb = new StringBuilder(bytes.length * 2);
        for (byte b : bytes) sb.append(String.format("%02x", b));
        return sb.toString();
    }

    public static byte[] fromHex(String hex) {
        if (hex == null || (hex.length() & 1) == 1) throw new IllegalArgumentException("invalid hex");
        int n = hex.length();
        byte[] out = new byte[n / 2];
        for (int i = 0; i < n; i += 2) {
            out[i / 2] = (byte) Integer.parseInt(hex.substring(i, i + 2), 16);
        }
        return out;
    }

    public static String deriveDailySecretV2(String baseSecret, String dateStr) throws Exception {
        byte[] keyRaw = sha256(baseSecret.getBytes(StandardCharsets.UTF_8));
        byte[] ivSeed = sha256(("iv:" + baseSecret).getBytes(StandardCharsets.UTF_8));
        byte[] iv = new byte[12];
        System.arraycopy(ivSeed, 0, iv, 0, 12);

        Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
        SecretKeySpec keySpec = new SecretKeySpec(keyRaw, "AES");
        GCMParameterSpec gcm = new GCMParameterSpec(128, iv);
        cipher.init(Cipher.ENCRYPT_MODE, keySpec, gcm);

        byte[] plaintext = (baseSecret + ":" + dateStr).getBytes(StandardCharsets.UTF_8);
        byte[] encrypted = cipher.doFinal(plaintext);
        return baseSecret + ":" + toHex(encrypted);
    }

    public static List<String> candidateSecrets(String hmacSecret, String hmacSecret2) throws Exception {
        LinkedHashSet<String> set = new LinkedHashSet<>();
        if (hmacSecret != null && !hmacSecret.isEmpty()) set.add(hmacSecret);
        if (hmacSecret2 != null && !hmacSecret2.isEmpty()) {
            set.add(deriveDailySecretV2(hmacSecret2, formatDateUtc(0)));
            set.add(deriveDailySecretV2(hmacSecret2, formatDateUtc(-1)));
            set.add(deriveDailySecretV2(hmacSecret2, formatDateUtc(1)));
        }
        return new ArrayList<>(set);
    }

    public static String signingSecret(String hmacSecret, String hmacSecret2) throws Exception {
        if (hmacSecret != null && !hmacSecret.isEmpty()) return hmacSecret;
        if (hmacSecret2 != null && !hmacSecret2.isEmpty()) return deriveDailySecretV2(hmacSecret2, formatDateUtc(0));
        return "";
    }

    public static byte[] otpMask(String secret, String purpose) throws Exception {
        byte[] h = hmacSha256(secret, purpose);
        byte[] out = new byte[4];
        System.arraycopy(h, 0, out, 0, 4);
        return out;
    }

    public static String xorIntToHex8(long value, byte[] mask4) {
        byte[] v = ByteBuffer.allocate(4).putInt((int) value).array();
        for (int i = 0; i < 4; i++) v[i] = (byte) (v[i] ^ mask4[i]);
        return toHex(v);
    }

    public static long xorHex8ToInt(String hex8, byte[] mask4) {
        if (hex8 == null || hex8.length() != 8) throw new IllegalArgumentException("invalid hex8");
        byte[] b = fromHex(hex8);
        for (int i = 0; i < 4; i++) b[i] = (byte) (b[i] ^ mask4[i]);
        return ByteBuffer.wrap(b).getInt() & 0xffffffffL;
    }

    public static boolean constantTimeEquals(byte[] a, byte[] b) {
        if (a == null || b == null || a.length != b.length) return false;
        int diff = 0;
        for (int i = 0; i < a.length; i++) diff |= a[i] ^ b[i];
        return diff == 0;
    }
}
```

## 8) Mapping nhanh Java và Worker

- otp ts mask: HMAC(secret, otp-ts-mask) lấy 4 byte đầu.
- otp exp mask: HMAC(secret, otp-exp-mask) lấy 4 byte đầu.
- token message: filename:timestamp.
- stream message: filename:expPlain:asn.
- phim: lấy 32 hex đầu của digest.

## 9) Checklist rollout an toàn

1. Triển khai Java verify theo candidate secrets trước.
2. Triển khai Java sign vẫn ưu tiên HMAC_SECRET, giống Worker.
3. Bật HMAC_SECRET_2 ở cả 2 phía.
4. Kiểm thử chéo:
- Java tạo token, Worker verify.
- Worker tạo stream URL, Java verify.
- Java tạo stream URL, Worker verify.
5. Sau thời gian ổn định mới bỏ HMAC_SECRET cũ.

## 10) Test vector gợi ý

Nên tạo test cố định với:

- filename mẫu
- asn mẫu
- epoch mẫu
- HMAC_SECRET_2 mẫu
- date UTC cố định

Sau đó assert:

- deriveDailySecretV2 cho ra đúng chuỗi.
- enc ts, dec ts round-trip.
- enc exp, dec exp round-trip.
- token verify pass với today hoặc day lân cận.
- phim verify pass và fail khi đổi 1 ký tự.
