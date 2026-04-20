#include <jni.h>
#include <algorithm>
#include <array>
#include <cctype>
#include <chrono>
#include <cstdint>
#include <cstring>
#include <fstream>
#include <string>
#include <vector>

std::string SERVER_URL          = "https://api.phim4k.lol/rest-api/";
std::string API_KEY             = "bbbb411dea44849";

std::string PURCHASE_CODE       = "***********************";

namespace {

constexpr uint8_t kXorKey = 0x5A;
constexpr uint8_t kJniNameKey = 0x37;

const uint8_t HMAC_SECRET_2_XOR[] = {
    111, 63, 98, 62, 107, 56, 110, 60, 99, 57, 104, 59, 108, 63, 109, 105,
    106, 56, 107, 60, 98, 62, 110, 59, 99, 104, 57, 111, 63, 105, 62, 107
};

const uint8_t VTG_CLASS_XOR[] = {
    84, 88, 90, 24, 81, 94, 91, 82, 68, 24, 84, 88, 83, 82, 68, 24, 66, 67,
    94, 91, 68, 24, 97, 94, 83, 82, 88, 99, 88, 92, 82, 89, 112, 82, 89, 82,
    69, 86, 67, 88, 69
};

const uint8_t APP_CLASS_XOR[] = {
    84, 88, 90, 24, 81, 94, 91, 82, 68, 24, 84, 88, 83, 82, 68, 24, 118, 71,
    71, 116, 88, 89, 81, 94, 80
};

const uint8_t APP_M1_XOR[] = {
    80, 82, 67, 118, 71, 94, 100, 82, 69, 65, 82, 69, 98, 69, 91
};

const uint8_t APP_M2_XOR[] = {
    80, 82, 67, 118, 71, 94, 124, 82, 78
};

const uint8_t APP_M3_XOR[] = {
    80, 82, 67, 103, 66, 69, 84, 95, 86, 68, 82, 116, 88, 83, 82
};

const uint8_t APP_SIG_XOR[] = {
    31, 30, 123, 93, 86, 65, 86, 24, 91, 86, 89, 80, 24, 100, 67, 69, 94, 89,
    80, 12
};

const uint32_t SHA256_K[64] = {
        0x428a2f98, 0x71374491, 0xb5c0fbcf, 0xe9b5dba5,
        0x3956c25b, 0x59f111f1, 0x923f82a4, 0xab1c5ed5,
        0xd807aa98, 0x12835b01, 0x243185be, 0x550c7dc3,
        0x72be5d74, 0x80deb1fe, 0x9bdc06a7, 0xc19bf174,
        0xe49b69c1, 0xefbe4786, 0x0fc19dc6, 0x240ca1cc,
        0x2de92c6f, 0x4a7484aa, 0x5cb0a9dc, 0x76f988da,
        0x983e5152, 0xa831c66d, 0xb00327c8, 0xbf597fc7,
        0xc6e00bf3, 0xd5a79147, 0x06ca6351, 0x14292967,
        0x27b70a85, 0x2e1b2138, 0x4d2c6dfc, 0x53380d13,
        0x650a7354, 0x766a0abb, 0x81c2c92e, 0x92722c85,
        0xa2bfe8a1, 0xa81a664b, 0xc24b8b70, 0xc76c51a3,
        0xd192e819, 0xd6990624, 0xf40e3585, 0x106aa070,
        0x19a4c116, 0x1e376c08, 0x2748774c, 0x34b0bcb5,
        0x391c0cb3, 0x4ed8aa4a, 0x5b9cca4f, 0x682e6ff3,
        0x748f82ee, 0x78a5636f, 0x84c87814, 0x8cc70208,
        0x90befffa, 0xa4506ceb, 0xbef9a3f7, 0xc67178f2
};

inline uint32_t rotr(uint32_t value, uint32_t bits) {
    return (value >> bits) | (value << (32 - bits));
}

void secureZero(void *buffer, size_t len) {
    volatile uint8_t *ptr = reinterpret_cast<volatile uint8_t *>(buffer);
    while (len-- > 0) {
        *ptr++ = 0;
    }
}

void secureClearString(std::string &value) {
    if (!value.empty()) {
        secureZero(&value[0], value.size());
        value.clear();
    }
}

std::string decodeMaskedString(const uint8_t *data, size_t len, uint8_t mask) {
    std::string decoded;
    decoded.reserve(len);
    for (size_t i = 0; i < len; ++i) {
        decoded.push_back(static_cast<char>(data[i] ^ mask));
    }
    return decoded;
}

bool isTracerAttached() {
    std::ifstream statusFile("/proc/self/status");
    if (!statusFile.is_open()) {
        return false;
    }

    std::string line;
    while (std::getline(statusFile, line)) {
        if (line.rfind("TracerPid:", 0) == 0) {
            const auto pidText = line.substr(10);
            return std::stoi(pidText) > 0;
        }
    }

    return false;
}

bool mapsContainHookArtifacts() {
    std::ifstream mapsFile("/proc/self/maps");
    if (!mapsFile.is_open()) {
        return false;
    }

    const std::vector<std::string> tokens = {
            "frida", "gadget", "xposed", "substrate", "magisk", "zygisk"
    };

    std::string line;
    while (std::getline(mapsFile, line)) {
        std::transform(line.begin(), line.end(), line.begin(), [](unsigned char ch) {
            return static_cast<char>(std::tolower(ch));
        });

        for (const auto &token : tokens) {
            if (line.find(token) != std::string::npos) {
                return true;
            }
        }
    }

    return false;
}

bool isHostileRuntime() {
    return isTracerAttached() || mapsContainHookArtifacts();
}

std::array<uint8_t, 32> sha256(const uint8_t *data, size_t len) {
    std::vector<uint8_t> padded(data, data + len);
    const uint64_t bitLen = static_cast<uint64_t>(len) * 8ULL;

    padded.push_back(0x80);
    while ((padded.size() % 64) != 56) {
        padded.push_back(0x00);
    }

    for (int shift = 56; shift >= 0; shift -= 8) {
        padded.push_back(static_cast<uint8_t>((bitLen >> shift) & 0xff));
    }

    uint32_t h0 = 0x6a09e667;
    uint32_t h1 = 0xbb67ae85;
    uint32_t h2 = 0x3c6ef372;
    uint32_t h3 = 0xa54ff53a;
    uint32_t h4 = 0x510e527f;
    uint32_t h5 = 0x9b05688c;
    uint32_t h6 = 0x1f83d9ab;
    uint32_t h7 = 0x5be0cd19;

    for (size_t chunk = 0; chunk < padded.size(); chunk += 64) {
        uint32_t w[64] = {0};
        for (int i = 0; i < 16; ++i) {
            const size_t index = chunk + (i * 4);
            w[i] = (static_cast<uint32_t>(padded[index]) << 24) |
                   (static_cast<uint32_t>(padded[index + 1]) << 16) |
                   (static_cast<uint32_t>(padded[index + 2]) << 8) |
                   static_cast<uint32_t>(padded[index + 3]);
        }

        for (int i = 16; i < 64; ++i) {
            const uint32_t s0 = rotr(w[i - 15], 7) ^ rotr(w[i - 15], 18) ^ (w[i - 15] >> 3);
            const uint32_t s1 = rotr(w[i - 2], 17) ^ rotr(w[i - 2], 19) ^ (w[i - 2] >> 10);
            w[i] = w[i - 16] + s0 + w[i - 7] + s1;
        }

        uint32_t a = h0;
        uint32_t b = h1;
        uint32_t c = h2;
        uint32_t d = h3;
        uint32_t e = h4;
        uint32_t f = h5;
        uint32_t g = h6;
        uint32_t h = h7;

        for (int i = 0; i < 64; ++i) {
            const uint32_t s1 = rotr(e, 6) ^ rotr(e, 11) ^ rotr(e, 25);
            const uint32_t ch = (e & f) ^ ((~e) & g);
            const uint32_t temp1 = h + s1 + ch + SHA256_K[i] + w[i];
            const uint32_t s0 = rotr(a, 2) ^ rotr(a, 13) ^ rotr(a, 22);
            const uint32_t maj = (a & b) ^ (a & c) ^ (b & c);
            const uint32_t temp2 = s0 + maj;

            h = g;
            g = f;
            f = e;
            e = d + temp1;
            d = c;
            c = b;
            b = a;
            a = temp1 + temp2;
        }

        h0 += a;
        h1 += b;
        h2 += c;
        h3 += d;
        h4 += e;
        h5 += f;
        h6 += g;
        h7 += h;
    }

    std::array<uint8_t, 32> digest{};
    const uint32_t hashes[8] = {h0, h1, h2, h3, h4, h5, h6, h7};
    for (int i = 0; i < 8; ++i) {
        digest[i * 4] = static_cast<uint8_t>((hashes[i] >> 24) & 0xff);
        digest[i * 4 + 1] = static_cast<uint8_t>((hashes[i] >> 16) & 0xff);
        digest[i * 4 + 2] = static_cast<uint8_t>((hashes[i] >> 8) & 0xff);
        digest[i * 4 + 3] = static_cast<uint8_t>(hashes[i] & 0xff);
    }

    return digest;
}

std::array<uint8_t, 32> hmacSha256(const std::string &key, const std::string &message) {
    std::vector<uint8_t> keyBlock(64, 0x00);
    if (key.size() > 64) {
        const auto hashedKey = sha256(reinterpret_cast<const uint8_t *>(key.data()), key.size());
        std::copy(hashedKey.begin(), hashedKey.end(), keyBlock.begin());
    } else {
        std::copy(key.begin(), key.end(), keyBlock.begin());
    }

    std::vector<uint8_t> innerPad(64);
    std::vector<uint8_t> outerPad(64);
    for (size_t i = 0; i < 64; ++i) {
        innerPad[i] = keyBlock[i] ^ 0x36;
        outerPad[i] = keyBlock[i] ^ 0x5c;
    }

    std::vector<uint8_t> inner(innerPad.begin(), innerPad.end());
    inner.insert(inner.end(), message.begin(), message.end());
    const auto innerHash = sha256(inner.data(), inner.size());

    std::vector<uint8_t> outer(outerPad.begin(), outerPad.end());
    outer.insert(outer.end(), innerHash.begin(), innerHash.end());
    const auto digest = sha256(outer.data(), outer.size());

    secureZero(keyBlock.data(), keyBlock.size());
    secureZero(innerPad.data(), innerPad.size());
    secureZero(outerPad.data(), outerPad.size());
    secureZero(inner.data(), inner.size());
    secureZero(outer.data(), outer.size());

    return digest;
}

std::string bytesToHex(const uint8_t *bytes, size_t len) {
    static const char hex[] = "0123456789abcdef";
    std::string output;
    output.reserve(len * 2);
    for (size_t i = 0; i < len; ++i) {
        output.push_back(hex[(bytes[i] >> 4) & 0x0f]);
        output.push_back(hex[bytes[i] & 0x0f]);
    }
    return output;
}

std::string buildVideoSecret() {
    return decodeMaskedString(SECRET_XOR, sizeof(SECRET_XOR), kXorKey);
}

std::string buildVideoBaseUrl() {
    return decodeMaskedString(BASE_URL_XOR, sizeof(BASE_URL_XOR), kXorKey);
}

void throwJavaException(JNIEnv *env, const char *className, const std::string &message) {
    jclass exceptionClass = env->FindClass(className);
    if (exceptionClass != nullptr) {
        env->ThrowNew(exceptionClass, message.c_str());
    }
}

class ScopedUtfChars {
public:
    ScopedUtfChars(JNIEnv *env, jstring value) : env_(env), value_(value), chars_(nullptr) {
        if (value_ != nullptr) {
            chars_ = env_->GetStringUTFChars(value_, nullptr);
        }
    }

    ~ScopedUtfChars() {
        if (chars_ != nullptr) {
            env_->ReleaseStringUTFChars(value_, chars_);
        }
    }

    const char *get() const {
        return chars_;
    }

private:
    JNIEnv *env_;
    jstring value_;
    const char *chars_;
};

jstring getApiServerUrlImpl(JNIEnv *env, jclass clazz) {
    (void) clazz;
    return env->NewStringUTF(SERVER_URL.c_str());
}

jstring getApiKeyImpl(JNIEnv *env, jclass clazz) {
    (void) clazz;
    return env->NewStringUTF(API_KEY.c_str());
}

jstring getPurchaseCodeImpl(JNIEnv *env, jclass clazz) {
    (void) clazz;
    return env->NewStringUTF(PURCHASE_CODE.c_str());
}

jstring nativeGenVideoUrlImpl(JNIEnv *env, jclass clazz, jstring filename_) {
    (void) clazz;

    if (isHostileRuntime()) {
        throwJavaException(env, "java/lang/SecurityException", "environment not supported");
        return nullptr;
    }

    if (filename_ == nullptr) {
        throwJavaException(env, "java/lang/IllegalArgumentException", "filename is empty");
        return nullptr;
    }

    ScopedUtfChars filenameChars(env, filename_);
    if (filenameChars.get() == nullptr) {
        return nullptr;
    }

    std::string filename(filenameChars.get());
    if (filename.empty()) {
        throwJavaException(env, "java/lang/IllegalArgumentException", "filename is empty");
        return nullptr;
    }

    try {
        std::string secret = buildVideoSecret();
        std::string baseUrl = buildVideoBaseUrl();
        const auto now = std::chrono::system_clock::now();
        const auto ts = static_cast<uint32_t>(
                std::chrono::duration_cast<std::chrono::seconds>(now.time_since_epoch()).count()
        );

        const auto mask = hmacSha256(secret, "otp-ts-mask");
        uint8_t tsBytes[4] = {
                static_cast<uint8_t>((ts >> 24) & 0xff),
                static_cast<uint8_t>((ts >> 16) & 0xff),
                static_cast<uint8_t>((ts >> 8) & 0xff),
                static_cast<uint8_t>(ts & 0xff)
        };

        uint8_t encBytes[4] = {
                static_cast<uint8_t>(tsBytes[0] ^ mask[0]),
                static_cast<uint8_t>(tsBytes[1] ^ mask[1]),
                static_cast<uint8_t>(tsBytes[2] ^ mask[2]),
                static_cast<uint8_t>(tsBytes[3] ^ mask[3])
        };

        std::string encTs = bytesToHex(encBytes, 4);
        std::string message = filename + ":" + std::to_string(ts);
        const auto token = hmacSha256(secret, message);
        const std::string signedUrl = baseUrl + "/" + filename + "?token=" + bytesToHex(token.data(), token.size()) + "&ts=" + encTs;

        secureZero(tsBytes, sizeof(tsBytes));
        secureZero(encBytes, sizeof(encBytes));
        secureClearString(secret);
        secureClearString(baseUrl);
        secureClearString(message);
        secureClearString(encTs);
        secureClearString(filename);

        return env->NewStringUTF(signedUrl.c_str());
    } catch (const std::exception &exception) {
        secureClearString(filename);
        throwJavaException(env, "java/lang/RuntimeException", exception.what());
        return nullptr;
    } catch (...) {
        secureClearString(filename);
        throwJavaException(env, "java/lang/RuntimeException", "native signing failed");
        return nullptr;
    }
}

jstring nativeGetHmacSecret2Impl(JNIEnv *env, jclass clazz) {
    (void) clazz;

    if (isHostileRuntime()) {
        throwJavaException(env, "java/lang/SecurityException", "environment not supported");
        return nullptr;
    }

    std::string secret2 = decodeMaskedString(HMAC_SECRET_2_XOR, sizeof(HMAC_SECRET_2_XOR), kXorKey);
    auto digest = sha256(reinterpret_cast<const uint8_t *>(secret2.data()), secret2.size());
    volatile uint8_t marker = digest[0];
    (void) marker;
    jstring result = env->NewStringUTF(secret2.c_str());
    secureClearString(secret2);
    secureZero(digest.data(), digest.size());
    return result;
}

bool registerVideoTokenGenerator(JNIEnv *env) {
    std::string className = decodeMaskedString(VTG_CLASS_XOR, sizeof(VTG_CLASS_XOR), kJniNameKey);
    std::string methodName = decodeMaskedString(VTG_METHOD_XOR, sizeof(VTG_METHOD_XOR), kJniNameKey);
    std::string methodSig = decodeMaskedString(VTG_SIG_XOR, sizeof(VTG_SIG_XOR), kJniNameKey);

    jclass clazz = env->FindClass(className.c_str());
    if (clazz == nullptr) {
        secureClearString(className);
        secureClearString(methodName);
        secureClearString(methodSig);
        return false;
    }

    const JNINativeMethod methods[] = {
            {
                    const_cast<char *>(methodName.c_str()),
                    const_cast<char *>(methodSig.c_str()),
                    reinterpret_cast<void *>(nativeGenVideoUrlImpl)
            },
            {
            const_cast<char *>("nativeGetHmacSecret2"),
            const_cast<char *>("()Ljava/lang/String;"),
            reinterpret_cast<void *>(nativeGetHmacSecret2Impl)
            }
    };

    const bool ok = env->RegisterNatives(clazz, methods, sizeof(methods) / sizeof(methods[0])) == JNI_OK;
    secureClearString(className);
    secureClearString(methodName);
    secureClearString(methodSig);
    return ok;
}

bool registerAppConfig(JNIEnv *env) {
    std::string className = decodeMaskedString(APP_CLASS_XOR, sizeof(APP_CLASS_XOR), kJniNameKey);
    std::string method1 = decodeMaskedString(APP_M1_XOR, sizeof(APP_M1_XOR), kJniNameKey);
    std::string method2 = decodeMaskedString(APP_M2_XOR, sizeof(APP_M2_XOR), kJniNameKey);
    std::string method3 = decodeMaskedString(APP_M3_XOR, sizeof(APP_M3_XOR), kJniNameKey);
    std::string methodSig = decodeMaskedString(APP_SIG_XOR, sizeof(APP_SIG_XOR), kJniNameKey);

    jclass clazz = env->FindClass(className.c_str());
    if (clazz == nullptr) {
        secureClearString(className);
        secureClearString(method1);
        secureClearString(method2);
        secureClearString(method3);
        secureClearString(methodSig);
        return false;
    }

    const JNINativeMethod methods[] = {
        {
            const_cast<char *>(method1.c_str()),
            const_cast<char *>(methodSig.c_str()),
            reinterpret_cast<void *>(getApiServerUrlImpl)
        },
        {
            const_cast<char *>(method2.c_str()),
            const_cast<char *>(methodSig.c_str()),
            reinterpret_cast<void *>(getApiKeyImpl)
        },
        {
            const_cast<char *>(method3.c_str()),
            const_cast<char *>(methodSig.c_str()),
            reinterpret_cast<void *>(getPurchaseCodeImpl)
        }
    };

    const bool ok = env->RegisterNatives(clazz, methods, sizeof(methods) / sizeof(methods[0])) == JNI_OK;
    secureClearString(className);
    secureClearString(method1);
    secureClearString(method2);
    secureClearString(method3);
    secureClearString(methodSig);
    return ok;
}

}  // namespace

JNIEXPORT jint JNICALL JNI_OnLoad(JavaVM *vm, void *reserved) {
    (void) reserved;

    JNIEnv *env = nullptr;
    if (vm->GetEnv(reinterpret_cast<void **>(&env), JNI_VERSION_1_6) != JNI_OK || env == nullptr) {
        return JNI_ERR;
    }

    if (!registerAppConfig(env) || !registerVideoTokenGenerator(env)) {
        return JNI_ERR;
    }

    return JNI_VERSION_1_6;
}

