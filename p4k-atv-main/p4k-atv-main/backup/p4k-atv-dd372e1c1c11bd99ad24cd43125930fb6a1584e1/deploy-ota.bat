@echo off
REM OXOO TV - Windows OTA Deployment Script
REM Usage: deploy-ota.bat [version_name] [apk_path]

setlocal enabledelayedexpansion

set VERSION_NAME=%1
set APK_PATH=%2

if "%VERSION_NAME%"=="" (
    echo ❌ Usage: deploy-ota.bat [version_name] [apk_path]
    echo    Example: deploy-ota.bat 1.9.8 ".\app\build\outputs\apk\release\app-release.apk"
    exit /b 1
)

if "%APK_PATH%"=="" (
    echo ❌ Usage: deploy-ota.bat [version_name] [apk_path]
    echo    Example: deploy-ota.bat 1.9.8 ".\app\build\outputs\apk\release\app-release.apk"
    exit /b 1
)

echo 🚀 OXOO TV OTA Deployment Script
echo ================================
echo Version: %VERSION_NAME%
echo APK Path: %APK_PATH%
echo.

REM Check if APK exists
if not exist "%APK_PATH%" (
    echo ❌ APK file not found: %APK_PATH%
    exit /b 1
)

REM Get APK size
for %%A in ("%APK_PATH%") do set APK_SIZE=%%~zA
set /a APK_SIZE_MB=%APK_SIZE%/1024/1024

echo 📱 APK Size: !APK_SIZE_MB!MB (%APK_SIZE% bytes)

REM Get version code (manual input for now)
set /p VERSION_CODE="🔢 Enter version code: "

REM Get current date in ISO format
for /f "tokens=1-4 delims=/ " %%a in ('date /t') do (
    set DATE_PART=%%d-%%b-%%c
)
for /f "tokens=1-2 delims=: " %%a in ('time /t') do (
    set TIME_PART=%%a:%%b:00
)
set RELEASE_DATE=%DATE_PART%T%TIME_PART%.000Z

echo.
echo 📋 Deployment Summary:
echo ======================
echo Version Name: %VERSION_NAME%
echo Version Code: %VERSION_CODE%
echo File Size: %APK_SIZE% bytes
echo Release Date: %RELEASE_DATE%
echo.

set /p CONFIRM="🤔 Continue with deployment? (y/N): "
if /i not "%CONFIRM%"=="y" (
    echo ❌ Deployment cancelled
    exit /b 0
)

echo.
echo 📤 Step 1: Manual APK Upload Required
echo =====================================
echo Please upload your APK to one of these services:
echo 1. GitHub Releases: https://github.com/yourusername/oxoo-tv-releases/releases/new
echo 2. Firebase Storage: https://console.firebase.google.com/
echo 3. Your own server/CDN
echo.

set /p DOWNLOAD_URL="📥 Enter the download URL: "

echo.
echo 🔥 Step 2: Updating Firebase Database...

REM Create Firebase update JSON
echo { > update.json
echo   "version_code": %VERSION_CODE%, >> update.json
echo   "version_name": "%VERSION_NAME%", >> update.json
echo   "download_url": "%DOWNLOAD_URL%", >> update.json
echo   "file_size": %APK_SIZE%, >> update.json
echo   "release_notes": "🔥 OXOO TV v%VERSION_NAME%\n\n✅ Các cải tiến trong phiên bản này:\n- Sửa lỗi và cải thiện hiệu suất\n- Tối ưu trải nghiệm người dùng\n- Cập nhật thư viện mới nhất\n\n⚠️ Khuyến nghị cập nhật để có trải nghiệm tốt nhất!", >> update.json
echo   "force_update": false, >> update.json
echo   "min_supported_version": 15, >> update.json
echo   "release_date": "%RELEASE_DATE%" >> update.json
echo } >> update.json

REM Firebase REST API endpoint
set FIREBASE_URL=https://website-19a7d-default-rtdb.asia-southeast1.firebasedatabase.app
set API_ENDPOINT=%FIREBASE_URL%/app_updates/oxoo_tv.json

echo 📡 Firebase endpoint: %API_ENDPOINT%
echo.
echo ⚠️  Manual Firebase Update Required:
echo 1. Go to https://console.firebase.google.com/
echo 2. Select your project: oxoo-tv-ota-system
echo 3. Go to Realtime Database
echo 4. Navigate to app_updates/oxoo_tv
echo 5. Update with the following data:
echo.
type update.json
echo.

REM Alternative: Use curl if available
where curl >nul 2>nul
if %errorlevel%==0 (
    echo 🔧 Attempting automatic update with curl...
    curl -X PUT -H "Content-Type: application/json" -d @update.json "%API_ENDPOINT%"
    if %errorlevel%==0 (
        echo ✅ Firebase updated successfully!
    ) else (
        echo ❌ Automatic update failed. Please update manually.
    )
) else (
    echo ℹ️  curl not found. Please update Firebase manually using the JSON above.
)

REM Cleanup
del update.json

echo.
echo 🎉 OTA Deployment Process Complete!
echo ===================================
echo ✅ APK ready at: %DOWNLOAD_URL%
echo ✅ Firebase data prepared
echo ✅ Version %VERSION_NAME% configuration ready
echo.
echo 📱 Users will receive update notification on next app launch
echo 📊 Monitor updates in Firebase Console
echo.
echo 🚀 Happy deploying!

pause