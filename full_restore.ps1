$historyDir = "$env:APPDATA\Code\User\History"
$targetTime = [datetime]"2026-03-17 12:05:00"

$entriesFiles = Get-ChildItem -Path $historyDir -Recurse -Filter "entries.json" | 
    Where-Object { (Get-Content $_.FullName | ConvertFrom-Json).resource -match "p4k-atv" }

foreach ($entries in $entriesFiles) {
    $dir = $entries.Directory.FullName
    $resource = (Get-Content $entries.FullName | ConvertFrom-Json).resource
    $relativePath = [uri]::UnescapeDataString($resource.Replace('file:///d%3A/22/p4k-atv/', ''))
    $relativePath = $relativePath.Replace('/', '\')
    $destPath = Join-Path "D:\22\p4k-atv" $relativePath
    
    $latestFile = Get-ChildItem -Path $dir -File | 
        Where-Object { $_.Name -ne "entries.json" -and $_.LastWriteTime -le $targetTime } | 
        Sort-Object LastWriteTime -Descending | 
        Select-Object -First 1

    if ($latestFile) {
        Write-Host "Restoring FULL HISTORY $( $relativePath )"
        $destDir = Split-Path $destPath
        if (-not (Test-Path $destDir)) { New-Item -ItemType Directory -Force -Path $destDir | Out-Null }
        Copy-Item -Path $latestFile.FullName -Destination $destPath -Force
    }
}
