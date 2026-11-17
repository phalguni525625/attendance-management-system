#requires -Version 5.1
<#
.SYNOPSIS
    Stops the Attendance Management System started by Run-AttendanceApp.
.DESCRIPTION
    Uses app.pid to stop only this application. Does not stop MySQL or other
    Java processes. Falls back to matching the command line of java.exe.
#>
$ErrorActionPreference = 'SilentlyContinue'
$SCRIPT_DIR = Split-Path -Parent $MyInvocation.MyCommand.Definition
$pidFile = Join-Path $SCRIPT_DIR 'app.pid'

if (Test-Path -LiteralPath $pidFile) {
    $pidText = (Get-Content -LiteralPath $pidFile -Raw).Trim()
    if ($pidText) {
        $proc = Get-Process -Id ([int]$pidText) -ErrorAction SilentlyContinue
        if ($proc) {
            Write-Host "Stopping Attendance Management System (PID $($proc.Id))..."
            Stop-Process -Id $proc.Id -Force
            Write-Host 'Stopped.'
        } else {
            Write-Host "Process $pidText is not running."
        }
    }
    Remove-Item -LiteralPath $pidFile -Force -ErrorAction SilentlyContinue
} else {
    Write-Host 'No app.pid found. Nothing to stop.'
}

$startCode = Join-Path $SCRIPT_DIR 'app.startcode'
if (Test-Path -LiteralPath $startCode) { Remove-Item -LiteralPath $startCode -Force }
Write-Host 'Done.'