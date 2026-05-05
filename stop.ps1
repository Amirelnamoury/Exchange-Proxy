$ErrorActionPreference = "Continue"

function Convert-ToWslPath {
    param([string] $WindowsPath)

    $resolved = (Resolve-Path -LiteralPath $WindowsPath).Path
    $drive = $resolved.Substring(0, 1).ToLowerInvariant()
    $rest = $resolved.Substring(2).Replace("\", "/")
    return "/mnt/$drive$rest"
}

$projectRoot = $PSScriptRoot
$wslProjectRoot = Convert-ToWslPath $projectRoot

$listeners = Get-NetTCPConnection -LocalPort 8080 -State Listen -ErrorAction SilentlyContinue
foreach ($listener in $listeners) {
    $process = Get-CimInstance Win32_Process -Filter "ProcessId=$($listener.OwningProcess)"
    if ($process.CommandLine -match "exchange-proxy") {
        Stop-Process -Id $listener.OwningProcess -Force
        Write-Host "Application Spring Boot arretee (PID $($listener.OwningProcess))."
    } else {
        Write-Host "Port 8080 utilise par un autre process, non arrete: PID $($listener.OwningProcess)"
    }
}

wsl -d Ubuntu -u root -- bash -lc "cd '$wslProjectRoot' && docker compose down"

$keepAliveProcesses = Get-CimInstance Win32_Process | Where-Object {
    $_.Name -eq "wsl.exe" -and $_.CommandLine -like "*sleep infinity*"
}

foreach ($process in $keepAliveProcesses) {
    Stop-Process -Id $process.ProcessId -Force
    Write-Host "Garde-vie WSL arrete (PID $($process.ProcessId))."
}
