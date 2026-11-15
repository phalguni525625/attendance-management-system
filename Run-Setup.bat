@echo off
:: One-click setup for the Attendance Management System.
:: Runs the PowerShell setup script.
setlocal
if not exist "%~dp0Setup-AttendanceApp.ps1" (
    echo ERROR: Setup-AttendanceApp.ps1 not found next to this script.
    exit /b 1
)
echo Running Attendance Management System setup...
powershell -ExecutionPolicy Bypass -NoProfile -File "%~dp0Setup-AttendanceApp.ps1" %*
if %errorlevel% neq 0 (
    echo Setup failed with exit code %errorlevel%. See setup.log for details.
    exit /b %errorlevel%)
echo Setup completed successfully.
endlocal
