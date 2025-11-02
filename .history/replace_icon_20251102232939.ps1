# Script tự động thay thế icon launcher
# Cần cài đặt ImageMagick hoặc sử dụng .NET System.Drawing

param(
    [Parameter(Mandatory=$true)]
    [string]$SourceImage
)

# Kiểm tra file nguồn
if (-not (Test-Path $SourceImage)) {
    Write-Error "Không tìm thấy file: $SourceImage"
    exit 1
}

# Định nghĩa các kích thước và thư mục đích
$IconSizes = @{
    "mipmap-mdpi" = 48
    "mipmap-hdpi" = 72
    "mipmap-xhdpi" = 96
    "mipmap-xxhdpi" = 144
    "mipmap-xxxhdpi" = 192
}

Write-Host "Bắt đầu thay thế icon launcher..." -ForegroundColor Green

foreach ($folder in $IconSizes.Keys) {
    $size = $IconSizes[$folder]
    $targetPath = "app\src\main\res\$folder\ic_launcher.png"
    
    Write-Host "Đang xử lý $folder ($size x $size px)..." -ForegroundColor Yellow
    
    try {
        # Sử dụng .NET System.Drawing để resize (cần .NET Framework)
        Add-Type -AssemblyName System.Drawing
        
        # Load image gốc
        $sourceImg = [System.Drawing.Image]::FromFile((Resolve-Path $SourceImage))
        
        # Tạo bitmap mới với kích thước đích
        $resized = New-Object System.Drawing.Bitmap($size, $size)
        $graphics = [System.Drawing.Graphics]::FromImage($resized)
        
        # Cài đặt chất lượng resize
        $graphics.InterpolationMode = [System.Drawing.Drawing2D.InterpolationMode]::HighQualityBicubic
        $graphics.SmoothingMode = [System.Drawing.Drawing2D.SmoothingMode]::HighQuality
        $graphics.PixelOffsetMode = [System.Drawing.Drawing2D.PixelOffsetMode]::HighQuality
        $graphics.CompositingQuality = [System.Drawing.Drawing2D.CompositingQuality]::HighQuality
        
        # Resize image
        $graphics.DrawImage($sourceImg, 0, 0, $size, $size)
        
        # Backup file cũ nếu tồn tại
        if (Test-Path $targetPath) {
            $backupPath = "backup_icons\ic_launcher_$folder.png"
            Copy-Item $targetPath $backupPath -Force
            Write-Host "  Đã backup: $backupPath" -ForegroundColor Cyan
        }
        
        # Lưu file mới
        $resized.Save($targetPath, [System.Drawing.Imaging.ImageFormat]::Png)
        
        # Cleanup
        $graphics.Dispose()
        $resized.Dispose()
        $sourceImg.Dispose()
        
        Write-Host "  ✓ Hoàn thành: $targetPath" -ForegroundColor Green
        
    } catch {
        Write-Error "Lỗi khi xử lý $folder : $($_.Exception.Message)"
    }
}

Write-Host "`nHoàn thành thay thế icon launcher!" -ForegroundColor Green
Write-Host "Các file backup được lưu trong thư mục backup_icons\" -ForegroundColor Cyan