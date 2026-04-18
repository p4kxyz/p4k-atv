# Replace all icon variants including adaptive icons
param([string]$SourceImage = "lgoo.jpg")

Write-Host "Replacing ALL launcher icons including adaptive..." -ForegroundColor Green

$folders = @(
    "app\src\main\res\mipmap-mdpi",
    "app\src\main\res\mipmap-hdpi", 
    "app\src\main\res\mipmap-xhdpi",
    "app\src\main\res\mipmap-xxhdpi",
    "app\src\main\res\mipmap-xxxhdpi"
)

$iconFiles = @(
    "ic_launcher.png",
    "ic_launcher_adaptive_fore.png",
    "ic_launcher_adaptive_back.png",
    "ic_launcher_round.png",
    "ic_launcher_round_adaptive_fore.png",
    "ic_launcher_round_adaptive_back.png"
)

if (-not (Test-Path $SourceImage)) {
    Write-Host "Source file not found: $SourceImage" -ForegroundColor Red
    exit 1
}

foreach ($folder in $folders) {
    foreach ($iconFile in $iconFiles) {
        $targetFile = "$folder\$iconFile"
        
        if (Test-Path $targetFile) {
            $backupName = "backup_icons\${iconFile}_$(Split-Path $folder -Leaf).png"
            Copy-Item $targetFile $backupName -Force
            Write-Host "Backed up: $backupName" -ForegroundColor Cyan
            
            Copy-Item $SourceImage $targetFile -Force
            Write-Host "Replaced: $targetFile" -ForegroundColor Green
        }
    }
}

Write-Host "Done! All launcher icon variants replaced." -ForegroundColor Green