#requires -Version 5.1
<#
.SYNOPSIS
    Starts the already-built Attendance Management System.
.DESCRIPTION
    Loads .env, finds the built jar and Java, starts the application in the
    background and writes the PID to app.pid. Does not rebuild or reset anything.
#>
$ErrorActionPreference = 'Stop'
$SCRIPT_DIR = Split-Path -Parent $MyInvocation.MyCommand.Definition
$JAR_CANDIDATES = @(
    (Join-Path $SCRIPT_DIR 'attendance-management-system-1.0.0.jar'),
    (Join-Path $SCRIPT_DIR 'target\attendance-management-system-1.0.0.jar')
)
$JAR = $JAR_CANDIDATES | Where-Object { Test-Path -LiteralPath $_ } | Select-Object -First 1
if (-not $JAR) {
    Write-Host "ERROR: Built application not found. Run Run-Setup.bat first." -ForegroundColor Red
    exit 1
}

# Load .env into the current process (simple KEY=VALUE parser).
$envFile = Join-Path $SCRIPT_DIR '.env'
if (Test-Path -LiteralPath $envFile) {
    Get-Content -LiteralPath $envFile | ForEach-Object {
        if ($_ -match '^\s*([^#=]+)=(.*)$') {
            Set-Item -Path "Env:$($matches[1].Trim())" -Value $matches[2] -ErrorAction SilentlyContinue
        }
    }
}

if (-not $env:ATTENDANCE_DB_PASS) {
    Write-Host 'ERROR: ATTENDANCE_DB_PASS is not set. Run Run-Setup.bat or set it in .env.' -ForegroundColor Red
    exit 1
}

$javaExe = $null
if ($env:JAVA_HOME) {
    $candidate = Join-Path $env:JAVA_HOME 'bin\java.exe'
    if (Test-Path -LiteralPath $candidate) { $javaExe = $candidate }
}
if (-not $javaExe) {
    $cmd = Get-Command java -ErrorAction SilentlyContinue
    if ($cmd) { $javaExe = $cmd.Source }
}
if (-not $javaExe) {
    Write-Host 'ERROR: Java not found. Run Run-Setup.bat first.' -ForegroundColor Red
    exit 1
}

$outFile = Join-Path $SCRIPT_DIR 'app.out.log'
$errFile = Join-Path $SCRIPT_DIR 'app.err.log'
$pidFile = Join-Path $SCRIPT_DIR 'app.pid'

Write-Host "Starting Attendance Management System..."
Write-Host "  Jar:  $JAR"
Write-Host "  Logs: $outFile / $errFile"

$proc = Start-Process -FilePath $javaExe -ArgumentList @('-jar', "`"$JAR`"") -WorkingDirectory $SCRIPT_DIR `
    -RedirectStandardOutput $outFile -RedirectStandardError $errFile -WindowStyle Hidden -PassThru
$proc.Id | Out-File -FilePath $pidFile -Encoding ascii

Write-Host "Started (PID $($proc.Id)). Open http://localhost:8080 in a few seconds."
Write-Host 'To stop: run Stop-AttendanceApp.bat'