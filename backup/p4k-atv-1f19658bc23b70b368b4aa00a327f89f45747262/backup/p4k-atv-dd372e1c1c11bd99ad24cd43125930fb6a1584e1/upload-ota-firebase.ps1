# Firebase OTA Upload Script for Phim4K v1.9.17
# Upload JSON data to Firebase Realtime Database

Write-Host "🔥 Firebase OTA Upload - Phim4K v1.9.17" -ForegroundColor Cyan
Write-Host "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━" -ForegroundColor Cyan
Write-Host ""

# Firebase configuration
$FIREBASE_URL = "https://website-19a7d-default-rtdb.asia-southeast1.firebasedatabase.app"
$FIREBASE_PATH = "app_updates/phim4k"
$FULL_URL = "$FIREBASE_URL/$FIREBASE_PATH.json"

# Read JSON file
$jsonFile = "firebase-ota-phim4k-v1.9.17.json"

if (-Not (Test-Path $jsonFile)) {
    Write-Host "❌ Error: File $jsonFile not found!" -ForegroundColor Red
    exit 1
}

Write-Host "📄 Reading JSON file: $jsonFile" -ForegroundColor Yellow
$jsonContent = Get-Content $jsonFile -Raw

Write-Host "🌐 Firebase URL: $FIREBASE_URL" -ForegroundColor Green
Write-Host "📍 Path: $FIREBASE_PATH" -ForegroundColor Green
Write-Host "🔗 Full URL: $FULL_URL" -ForegroundColor Green
Write-Host ""

# Confirm before upload
Write-Host "⚠️  This will upload OTA data to Firebase!" -ForegroundColor Yellow
$confirm = Read-Host "Do you want to continue? (y/n)"

if ($confirm -ne "y") {
    Write-Host "❌ Upload cancelled." -ForegroundColor Red
    exit 0
}

Write-Host ""
Write-Host "⬆️  Uploading to Firebase..." -ForegroundColor Cyan

try {
    $response = Invoke-RestMethod -Uri $FULL_URL -Method Put -Body $jsonContent -ContentType "application/json"
    
    Write-Host ""
    Write-Host "✅ Upload successful!" -ForegroundColor Green
    Write-Host ""
    Write-Host "📊 Response from Firebase:" -ForegroundColor Cyan
    Write-Host ($response | ConvertTo-Json -Depth 10) -ForegroundColor White
    Write-Host ""
    
    # Verify upload
    Write-Host "🔍 Verifying upload..." -ForegroundColor Yellow
    $verify = Invoke-RestMethod -Uri $FULL_URL -Method Get
    
    if ($verify.version_code -eq 23) {
        Write-Host "✅ Verification successful!" -ForegroundColor Green
        Write-Host "   Version: $($verify.version_name) (Code: $($verify.version_code))" -ForegroundColor White
        Write-Host "   App: $($verify.app_name)" -ForegroundColor White
        Write-Host "   Download URL: $($verify.download_url)" -ForegroundColor White
    } else {
        Write-Host "⚠️  Warning: Verification data mismatch!" -ForegroundColor Yellow
    }
    
} catch {
    Write-Host ""
    Write-Host "❌ Upload failed!" -ForegroundColor Red
    Write-Host "Error: $($_.Exception.Message)" -ForegroundColor Red
    exit 1
}

Write-Host ""
Write-Host "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━" -ForegroundColor Cyan
Write-Host "🎉 OTA Update is now live for all users!" -ForegroundColor Green
Write-Host "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━" -ForegroundColor Cyan
