$ErrorActionPreference = "Stop"

function Convert-ToWslPath {
    param([string] $WindowsPath)

    $resolved = (Resolve-Path -LiteralPath $WindowsPath).Path
    $drive = $resolved.Substring(0, 1).ToLowerInvariant()
    $rest = $resolved.Substring(2).Replace("\", "/")
    return "/mnt/$drive$rest"
}

$projectRoot = $PSScriptRoot
$wslProjectRoot = Convert-ToWslPath $projectRoot
$javaHome = "C:\Program Files\Eclipse Adoptium\jdk-17.0.19.10-hotspot"
$mavenBin = Join-Path (Split-Path -Parent $projectRoot) "tools\apache-maven-3.9.11\bin"

$listener = Get-NetTCPConnection -LocalPort 8080 -State Listen -ErrorAction SilentlyContinue | Select-Object -First 1
if ($listener) {
    $process = Get-CimInstance Win32_Process -Filter "ProcessId=$($listener.OwningProcess)"
    if ($process.CommandLine -match "exchange-proxy") {
        throw "Exchange Proxy tourne deja sur le port 8080 (PID $($listener.OwningProcess)). Lance .\stop.ps1 avant de redemarrer."
    }

    throw "Le port 8080 est deja utilise par le PID $($listener.OwningProcess). Commande: $($process.CommandLine)"
}

$keepAlive = Get-CimInstance Win32_Process | Where-Object {
    $_.Name -eq "wsl.exe" -and $_.CommandLine -like "*sleep infinity*"
}

if (-not $keepAlive) {
    Start-Process -FilePath "C:\Windows\System32\wsl.exe" `
        -ArgumentList @("-d", "Ubuntu", "-u", "root", "--exec", "sleep", "infinity") `
        -WindowStyle Hidden
}

wsl -d Ubuntu -u root -- bash -lc "cd '$wslProjectRoot' && docker compose up -d"

$env:JAVA_HOME = $javaHome
$env:Path = "$javaHome\bin;$mavenBin;$env:Path"

mvn spring-boot:run "-Dspring-boot.run.arguments=--exchange.fetch-interval-ms=10000"
