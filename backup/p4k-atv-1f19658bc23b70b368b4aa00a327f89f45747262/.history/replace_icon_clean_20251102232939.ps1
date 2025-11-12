# Replace icon script
param([string]$SourceImage = "lgoo.jpg")

Write-Host "Replacing launcher icons..." -ForegroundColor Green

$folders = @(
    "app\src\main\res\mipmap-mdpi",
    "app\src\main\res\mipmap-hdpi", 
    "app\src\main\res\mipmap-xhdpi",
    "app\src\main\res\mipmap-xxhdpi",
    "app\src\main\res\mipmap-xxxhdpi"
)

if (-not (Test-Path $SourceImage)) {
    Write-Host "Source file not found: $SourceImage" -ForegroundColor Red
    exit 1
}

foreach ($folder in $folders) {
    $targetFile = "$folder\ic_launcher.png"
    
    if (Test-Path $targetFile) {
        $backupName = "backup_icons\ic_launcher_$(Split-Path $folder -Leaf).png"
        Copy-Item $targetFile $backupName -Force
        Write-Host "Backed up: $backupName" -ForegroundColor Cyan
    }
    
    Copy-Item $SourceImage $targetFile -Force
    Write-Host "Replaced: $targetFile" -ForegroundColor Green
}

Write-Host "Done! All launcher icons replaced." -ForegroundColor Green