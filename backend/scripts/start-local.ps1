param(
    [string]$EnvFile = ".env"
)

$ErrorActionPreference = "Stop"

if (-not (Test-Path $EnvFile)) {
    Write-Error "Env file not found: $EnvFile`nCreate it from .env.example first."
}

Get-Content $EnvFile | ForEach-Object {
    $line = $_.Trim()

    if (-not $line -or $line.StartsWith("#")) {
        return
    }

    $parts = $line -split "=", 2
    if ($parts.Length -ne 2) {
        return
    }

    $key = $parts[0].Trim()
    $value = $parts[1].Trim()
    [System.Environment]::SetEnvironmentVariable($key, $value, "Process")
}

if (-not $env:SPRING_PROFILES_ACTIVE) {
    [System.Environment]::SetEnvironmentVariable("SPRING_PROFILES_ACTIVE", "local", "Process")
}

Write-Host "Loaded environment variables from $EnvFile"
Write-Host "Active Spring profile: $env:SPRING_PROFILES_ACTIVE"
Write-Host "Starting backend with MariaDB configuration..."

& ".\gradlew.bat" bootRun
