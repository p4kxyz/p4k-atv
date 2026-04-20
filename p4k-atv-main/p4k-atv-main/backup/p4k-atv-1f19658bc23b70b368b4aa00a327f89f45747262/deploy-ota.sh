#!/bin/bash

# OXOO TV - Automated OTA Deployment Script
# Usage: ./deploy-ota.sh [version_name] [apk_path]

VERSION_NAME=$1
APK_PATH=$2

if [ -z "$VERSION_NAME" ] || [ -z "$APK_PATH" ]; then
    echo "❌ Usage: ./deploy-ota.sh [version_name] [apk_path]"
    echo "   Example: ./deploy-ota.sh 1.9.8 ./app/build/outputs/apk/release/app-release.apk"
    exit 1
fi

echo "🚀 OXOO TV OTA Deployment Script"
echo "================================"
echo "Version: $VERSION_NAME"
echo "APK Path: $APK_PATH"
echo ""

# Check if APK exists
if [ ! -f "$APK_PATH" ]; then
    echo "❌ APK file not found: $APK_PATH"
    exit 1
fi

# Get APK size
APK_SIZE=$(stat -f%z "$APK_PATH" 2>/dev/null || stat -c%s "$APK_PATH" 2>/dev/null)
APK_SIZE_MB=$(( APK_SIZE / 1024 / 1024 ))

echo "📱 APK Size: ${APK_SIZE_MB}MB (${APK_SIZE} bytes)"

# Extract version code from APK (requires aapt)
if command -v aapt >/dev/null 2>&1; then
    VERSION_CODE=$(aapt dump badging "$APK_PATH" | grep versionCode | sed 's/.*versionCode=.//' | sed 's/ .*//')
    echo "🔢 Version Code: $VERSION_CODE"
else
    echo "⚠️  Cannot extract version code (aapt not found)"
    read -p "Enter version code manually: " VERSION_CODE
fi

# Get current date in ISO format
RELEASE_DATE=$(date -u +"%Y-%m-%dT%H:%M:%S.%3NZ")

echo ""
echo "📋 Deployment Summary:"
echo "======================"
echo "Version Name: $VERSION_NAME"
echo "Version Code: $VERSION_CODE" 
echo "File Size: $APK_SIZE bytes"
echo "Release Date: $RELEASE_DATE"
echo ""

read -p "🤔 Continue with deployment? (y/N): " CONFIRM
if [ "$CONFIRM" != "y" ] && [ "$CONFIRM" != "Y" ]; then
    echo "❌ Deployment cancelled"
    exit 0
fi

echo ""
echo "📤 Step 1: Uploading APK to GitHub Releases..."

# Upload to GitHub Releases (requires gh CLI)
if command -v gh >/dev/null 2>&1; then
    REPO_NAME="oxoo-tv-releases"  # Change this to your repo
    TAG_NAME="v$VERSION_NAME"
    
    # Create release
    gh release create "$TAG_NAME" "$APK_PATH" \
        --title "OXOO TV v$VERSION_NAME" \
        --notes "🔥 OXOO TV v$VERSION_NAME

✅ Các cải tiến trong phiên bản này:
- Sửa lỗi và cải thiện hiệu suất
- Tối ưu trải nghiệm người dùng
- Cập nhật thư viện mới nhất

⚠️ Khuyến nghị cập nhật để có trải nghiệm tốt nhất!" \
        --repo "$REPO_NAME"
    
    if [ $? -eq 0 ]; then
        DOWNLOAD_URL="https://github.com/yourusername/$REPO_NAME/releases/download/$TAG_NAME/$(basename "$APK_PATH")"
        echo "✅ APK uploaded successfully!"
        echo "📥 Download URL: $DOWNLOAD_URL"
    else
        echo "❌ Failed to upload APK to GitHub"
        exit 1
    fi
else
    echo "⚠️  GitHub CLI not found. Please upload APK manually and enter download URL:"
    read -p "Download URL: " DOWNLOAD_URL
fi

echo ""
echo "🔥 Step 2: Updating Firebase Database..."

# Create Firebase update JSON
cat > /tmp/ota_update.json << EOF
{
  "version_code": $VERSION_CODE,
  "version_name": "$VERSION_NAME",
  "download_url": "$DOWNLOAD_URL",
  "file_size": $APK_SIZE,
  "release_notes": "🔥 OXOO TV v$VERSION_NAME\n\n✅ Các cải tiến trong phiên bản này:\n- Sửa lỗi và cải thiện hiệu suất\n- Tối ưu trải nghiệm người dùng\n- Cập nhật thư viện mới nhất\n\n⚠️ Khuyến nghị cập nhật để có trải nghiệm tốt nhất!",
  "force_update": false,
  "min_supported_version": 15,
  "release_date": "$RELEASE_DATE"
}
EOF

# Update Firebase using curl (requires Firebase REST API)
FIREBASE_URL="https://website-19a7d-default-rtdb.asia-southeast1.firebasedatabase.app"
API_ENDPOINT="$FIREBASE_URL/app_updates/oxoo_tv.json"

echo "📡 Updating Firebase at: $API_ENDPOINT"

RESPONSE=$(curl -s -X PUT \
    -H "Content-Type: application/json" \
    -d @/tmp/ota_update.json \
    "$API_ENDPOINT")

if echo "$RESPONSE" | grep -q "version_code"; then
    echo "✅ Firebase updated successfully!"
else
    echo "❌ Failed to update Firebase:"
    echo "$RESPONSE"
    exit 1
fi

# Cleanup
rm -f /tmp/ota_update.json

echo ""
echo "🎉 OTA Deployment Complete!"
echo "=========================="
echo "✅ APK uploaded: $DOWNLOAD_URL"
echo "✅ Firebase updated"
echo "✅ Version $VERSION_NAME is now live"
echo ""
echo "📱 Users will receive update notification on next app launch"
echo "📊 Monitor updates in Firebase Console"
echo ""
echo "🚀 Happy deploying!"