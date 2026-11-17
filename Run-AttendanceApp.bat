@echo off
setlocal
set SCRIPT_DIR=%~dp0
powershell -NoProfile -ExecutionPolicy Bypass -File "%SCRIPT_DIR%Run-AttendanceApp.ps1"
if errorlevel 1 (
    echo.
    echo Failed to start the application. See app.err.log for details.
    pause
)
endlocal