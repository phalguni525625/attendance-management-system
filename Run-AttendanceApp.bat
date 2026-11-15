@echo off
:: Start the already-built Attendance Management Application without
:: reinstalling dependencies or resetting the database.
:: Expects attendance-management-system-1.0.0.jar and a .env file in this folder.
setlocal
set "SCRIPT_DIR=%~dp0"
set "JAR=%SCRIPT_DIR%attendance-management-system-1.0.0.jar"
if not exist "%JAR%" (
    echo ERROR: Built application not found: %JAR%
    echo Run Run-Setup.bat first to build the application.
    exit /b 1
)

:: Load .env if present (simple KEY=VALUE parser, no quotes handling beyond trimming)
if exist "%SCRIPT_DIR%.env" (
    for /f "usebackq tokens=1,* delims=" %%a in ("%SCRIPT_DIR%.env") do (
        set "%%a=%%b"
    )
)

:: Require database password
if "%ATTENDANCE_DB_PASS%"=="" (
    echo ERROR: ATTENDANCE_DB_PASS is not set. Set it in .env or your environment.
    exit /b 1
)

echo Starting Attendance Management System...
echo Logs: %SCRIPT_DIR%app.out.log / %SCRIPT_DIR%app.err.log
start "" /b "%JAVA_HOME%\bin\java.exe" -jar "%JAR%" > "%SCRIPT_DIR%app.out.log" 2> "%SCRIPT_DIR%app.err.log"
echo %ERRORLEVEL% > "%SCRIPT_DIR%app.startcode"
:: Capture PID of the started java process is unreliable via start; save via a small check.
for /f "tokens=2" %%a in ('tasklist /fi "imagename eq java.exe"') do set "PID=%%a"
if defined PID echo %PID% > "%SCRIPT_DIR%app.pid"

echo.
echo Application is starting at http://localhost:8080
echo Wait a few seconds, then open http://localhost:8080 in your browser.
echo To stop: run Stop-AttendanceApp.bat
endlocal
