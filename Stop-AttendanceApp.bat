@echo off
:: Stop only the application process launched by Run-AttendanceApp.bat.
:: Does NOT terminate unrelated Java processes or stop MySQL.
setlocal
set "SCRIPT_DIR=%~dp0"
set "PIDFILE=%SCRIPT_DIR%app.pid"

if exist "%PIDFILE%" (
    set /p PID=<%PIDFILE%
    if defined PID (
        echo Stopping Attendance Management System (PID %PID%)...
        taskkill /PID %PID% /T /F >nul 2>&1
        if %errorlevel% equ 0 (
            echo Stopped.
        ) else (
            echo Process %PID% not running or already stopped.
        )
    )
    del "%PIDFILE%" >nul 2>&1
) else (
    echo No app.pid found. Attempting to locate the Attendance app by jar...
    for /f "tokens=2" %%a in ('tasklist /v /fi "imagename eq java.exe" 2^>nul ^| findstr /i "attendance-management"') do (
        echo Found PID %%a
        taskkill /PID %%a /T /F >nul 2>&1
        set "FOUND=1"
    )
    if not defined FOUND echo No Attendance app process found.
)

if exist "%SCRIPT_DIR%app.startcode" del "%SCRIPT_DIR%app.startcode" >nul 2>&1
echo Done.
endlocal
