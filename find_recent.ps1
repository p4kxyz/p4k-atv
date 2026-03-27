$startTime = [DateTimeOffset]::new([datetime]'2026-03-10T00:00:00Z').ToUnixTimeMilliseconds()
Get-ChildItem $env:APPDATA\Code\User\History -Recurse -Filter 'entries.json' -ErrorAction SilentlyContinue | ForEach-Object {
    $content = Get-Content $_.FullName -Raw -ErrorAction SilentlyContinue
    if ($content) {
        $json = $content | ConvertFrom-Json -ErrorAction SilentlyContinue
        if ($json.resource -like '*p4k-atv*') {
            $recent = $json.entries | Where-Object { $.timestamp -ge $startTime }
            if ($recent) { Write-Output $json.resource }
        }
    }
} | Select-Object -Unique
