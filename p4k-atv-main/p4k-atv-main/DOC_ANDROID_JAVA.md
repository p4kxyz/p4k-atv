# VIP Worker – Hướng dẫn tích hợp Android (Java)

## Thông tin
| | |
|---|---|
| **Worker URL** | `https://dmp30.phim4k.lol` |
| **Secret** | `9f3c2a7e6b1d4c8fa2e97d5b0c3a41f8` |
| **Token TTL** | 300 giây (5 phút) |

---

## Luồng hoạt động

```
App Android
    │
    ├─ BƯỚC 1: Gen token + ts từ filename
    │
    ├─ BƯỚC 2: GET https://dmp30.phim4k.lol/KJRY929T61DJ9EX8?token=xxx&ts=yyy
    │          → Trả về JSON: {"url": "https://...?phim=xxx&4k=yyy", "expires_in": 21600}
    │
    └─ BƯỚC 3: Đưa URL từ JSON vào ExoPlayer để stream video
               (URL hiệu lực 6 tiếng, gắn với IP, `phim` dài 32 ký tự hex)
```

---

## Thêm dependency (build.gradle)
```gradle
implementation 'androidx.media3:media3-exoplayer:1.3.1'
implementation 'androidx.media3:media3-ui:1.3.1'
```

---

## VideoTokenGenerator.java

Tạo file `VideoTokenGenerator.java` trong project:

```java
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;

public class VideoTokenGenerator {

    private static final String SECRET = "9f3c2a7e6b1d4c8fa2e97d5b0c3a41f8";
    private static final String BASE_URL = "https://dmp30.phim4k.lol";

    /**
     * Tạo signed URL cho video
     * @param filename tên file (không cần đuôi .mp4)
     * @return URL đầy đủ kèm token và ts
     */
    public static String genVideoUrl(String filename) throws Exception {
        long ts = System.currentTimeMillis() / 1000L;

        // Bước 1: OTP mask = 4 bytes đầu của HMAC-SHA256(secret, "otp-ts-mask")
        byte[] maskFull = hmacSha256(
            SECRET.getBytes(StandardCharsets.UTF_8),
            "otp-ts-mask".getBytes(StandardCharsets.UTF_8)
        );

        // Bước 2: Mã hóa ts bằng XOR (big-endian uint32)
        ByteBuffer tsBuffer = ByteBuffer.allocate(4);
        tsBuffer.putInt((int) ts);
        byte[] tsBytes = tsBuffer.array();

        byte[] encBytes = new byte[4];
        for (int i = 0; i < 4; i++) {
            encBytes[i] = (byte) (tsBytes[i] ^ maskFull[i]);
        }
        String encTs = bytesToHex(encBytes); // 8 ký tự hex

        // Bước 3: HMAC token
        String message = filename + ":" + ts;
        byte[] tokenBytes = hmacSha256(
            SECRET.getBytes(StandardCharsets.UTF_8),
            message.getBytes(StandardCharsets.UTF_8)
        );
        String token = bytesToHex(tokenBytes); // 64 ký tự hex

        return BASE_URL + "/" + filename + "?token=" + token + "&ts=" + encTs;
    }

    private static byte[] hmacSha256(byte[] key, byte[] data) throws Exception {
        Mac mac = Mac.getInstance("HmacSHA256");
        mac.init(new SecretKeySpec(key, "HmacSHA256"));
        return mac.doFinal(data);
    }

    private static String bytesToHex(byte[] bytes) {
        StringBuilder sb = new StringBuilder();
        for (byte b : bytes) {
            sb.append(String.format("%02x", b));
        }
        return sb.toString();
    }
}
```

---

## VideoActivity.java – Fetch stream URL và play

```java
import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.media3.common.MediaItem;
import androidx.media3.exoplayer.ExoPlayer;
import androidx.media3.ui.PlayerView;

import org.json.JSONObject;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

public class VideoActivity extends AppCompatActivity {

    private ExoPlayer player;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_video);

        String videoId = getIntent().getStringExtra("video_id"); // "KJRY929T61DJ9EX8"

        // Fetch stream URL trên background thread
        new Thread(() -> {
            try {
                // BƯớc 1: Gen signed URL với token+ts
                String signedUrl = VideoTokenGenerator.genVideoUrl(videoId);

                // BƯớc 2: GET để lấy JSON response
                URL url = new URL(signedUrl);
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("GET");
                conn.setConnectTimeout(5000);
                conn.setReadTimeout(5000);

                BufferedReader reader = new BufferedReader(
                    new InputStreamReader(conn.getInputStream()));
                StringBuilder response = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) {
                    response.append(line);
                }
                reader.close();

                // BƯớc 3: Parse JSON để lấy stream URL
                JSONObject json = new JSONObject(response.toString());
                String streamUrl = json.getString("url");

                // BƯớc 4: Play video
                runOnUiThread(() -> playVideo(streamUrl));
            } catch (Exception e) {
                e.printStackTrace();
            }
        }).start();
    }

    private void playVideo(String url) {
        PlayerView playerView = findViewById(R.id.player_view);

        player = new ExoPlayer.Builder(this).build();
        playerView.setPlayer(player);

        player.setMediaItem(MediaItem.fromUri(url));
        player.prepare();
        player.play();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (player != null) player.release();
    }
}
```

### Layout (activity_video.xml)
```xml
<androidx.media3.ui.PlayerView
    android:id="@+id/player_view"
    android:layout_width="match_parent"
    android:layout_height="match_parent" />
```

### Gọi Activity
```java
Intent intent = new Intent(this, VideoActivity.class);
intent.putExtra("video_id", "KJRY929T61DJ9EX8");
startActivity(intent);
```

---

## Lưu ý quan trọng

- Mỗi lần play phải **gen token mới** — token chỉ sống 300 giây (5 phút).
- Không cache signed URL (URL có token+ts).
- Stream URL (có phim+4k) **hiệu lực 6 tiếng**, có thể cache trong vòng 6h.
- Param `phim` trong stream URL chỉ dài **32 ký tự hex**.
- `genVideoUrl()` và HTTP request phải chạy trên **background thread**.
