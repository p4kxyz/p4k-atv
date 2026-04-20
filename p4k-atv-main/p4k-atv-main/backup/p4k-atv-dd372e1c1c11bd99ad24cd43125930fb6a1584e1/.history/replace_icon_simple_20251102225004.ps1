# Script đơn giản để thay thế icon bằng cách sao chép một file
param(
    [string]$SourceImage = "4k_icon.png"
)

Write-Host "Đang thay thế icon launcher..." -ForegroundColor Green

# Danh sách các thư mục cần thay thế
$folders = @(
    "app\src\main\res\mipmap-mdpi",
    "app\src\main\res\mipmap-hdpi", 
    "app\src\main\res\mipmap-xhdpi",
    "app\src\main\res\mipmap-xxhdpi",
    "app\src\main\res\mipmap-xxxhdpi"
)

# Kiểm tra xem file nguồn có tồn tại không
if (-not (Test-Path $SourceImage)) {
    Write-Host "Không tìm thấy file $SourceImage" -ForegroundColor Red
    Write-Host "Vui lòng đặt file icon 4K vào workspace với tên '4k_icon.png'" -ForegroundColor Yellow
    exit 1
}

foreach ($folder in $folders) {
    $targetFile = "$folder\ic_launcher.png"
    
    if (Test-Path $targetFile) {
        # Backup file cũ
        $backupName = "backup_icons\ic_launcher_$(Split-Path $folder -Leaf).png"
        Copy-Item $targetFile $backupName -Force
        Write-Host "Đã backup: $backupName" -ForegroundColor Cyan
    }
    
    # Copy file mới
    Copy-Item $SourceImage $targetFile -Force
    Write-Host "✓ Đã thay thế: $targetFile" -ForegroundColor Green
}

Write-Host "`nHoàn thành! Tất cả icon launcher đã được thay thế." -ForegroundColor Green
Write-Host "Lưu ý: Bạn có thể cần resize lại các file để phù hợp với kích thước của từng thư mục." -ForegroundColor Yellow