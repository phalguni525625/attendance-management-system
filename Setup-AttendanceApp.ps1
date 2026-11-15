#requires -Version 5.1
<#
.SYNOPSIS
    One-click setup for the Attendance Management System.
.DESCRIPTION
    Detects/installs prerequisites (Java 17, Maven, MySQL 8, Git), configures a
    dedicated MySQL database and restricted user, initializes the schema without
    destroying existing data, builds the application with Maven, runs tests,
    starts the app, and opens it in the default browser.
    Designed to run safely and repeatedly on a normal Windows user account.
    Administrator privileges are required only to install MySQL server.
#>
$ErrorActionPreference = 'Stop'
$ProgressPreference = 'SilentlyContinue'

# ----------------------------- configuration -----------------------------
$SCRIPT_DIR   = Split-Path -Parent $MyInvocation.MyCommand.Definition
$PROJECT_DIR  = $SCRIPT_DIR
$LOG_FILE     = Join-Path $SCRIPT_DIR "setup.log"
$ENV_FILE     = Join-Path $SCRIPT_DIR ".env"
$ENV_EXAMPLE  = Join-Path $SCRIPT_DIR ".env.example"
$TOOLS_DIR    = Join-Path $env:USERPROFILE "kilo-tools"
$JAVA_DIR     = Join-Path $TOOLS_DIR "corretto17"
$JAVA_EXE     = Join-Path $JAVA_DIR "Amazon Corretto\jdk17.0.21_0\bin\java.exe"
$MAVEN_DIR    = Join-Path $TOOLS_DIR "maven\apache-maven-3.10.1"
$MAVEN_CMD    = Join-Path $MAVEN_DIR "bin\mvn.cmd"
$MYSQL_DIR    = Join-Path $TOOLS_DIR "mysql84"
$MYSQLD_EXE   = Join-Path $MYSQL_DIR "PFiles64\MySQL\MySQL Server 8.4\bin\mysqld.exe"
$MYSQL_CLI    = Join-Path $MYSQL_DIR "PFiles64\MySQL\MySQL Server 8.4\bin\mysql.exe"
$MYSQL_DATA   = Join-Path $MYSQL_DIR "data"
$MYSQL_PORT   = 3306
$APP_PORT     = 8080
$JAR_NAME     = "attendance-management-system-1.0.0.jar"
$JAR_PATH     = Join-Path $PROJECT_DIR $JAR_NAME
$DB_NAME      = "attendance_db"
$DB_USER      = "attendance_app"
$MIN_DISK_GB  = 5

$script:LOG_ENTRIES = @()
$script:EXIT_CODE   = 0
$script:HAS_ADMIN   = $false

# ----------------------------- logging -----------------------------
function Write-Log {
    param([string]$Message, [string]$Level = "INFO")
    $ts = Get-Date -Format "yyyy-MM-dd HH:mm:ss"
    $line = "[$ts] [$Level] $Message"
    $script:LOG_ENTRIES += $line
    if ($Level -eq "ERROR") { Write-Host $line -ForegroundColor Red }
    elseif ($Level -eq "WARN") { Write-Host $line -ForegroundColor Yellow }
    elseif ($Level -eq "OK") { Write-Host $line -ForegroundColor Green }
    else { Write-Host $line }
}

function Show-Banner {
    Write-Host ""
    Write-Host "============================================================" -ForegroundColor Cyan
    Write-Host "  Attendance Management System - One-click Setup" -ForegroundColor Cyan
    Write-Host "============================================================" -ForegroundColor Cyan
    Write-Host ""
}

function Save-Log {
    $script:LOG_ENTRIES | Out-File -FilePath $LOG_FILE -Encoding utf8
}

# ----------------------------- helpers -----------------------------
function Test-IsAdmin {
    return ([Security.Principal.WindowsPrincipal]::new([Security.Principal.WindowsIdentity]::GetCurrent())).IsInRole([Security.Principal.WindowsBuiltInRole]::Administrator)
}

function Test-Internet {
    try {
        $r = Invoke-WebRequest -Uri "https://start.spring.io" -TimeoutSec 10 -UseBasicParsing
        return $r.StatusCode -eq 200
    } catch { return $false }
}

function Get-FreeDiskGB {
    $disk = Get-CimInstance -ClassName Win32_LogicalDisk -Filter "DeviceID='C:'" | Select-Object -First 1
    if (-not $disk) { return 0 }
    return [math]::Round($disk.FreeSpace / 1GB, 2)
}

function Find-Command {
    param([string]$Name)
    $cmd = Get-Command $Name -ErrorAction SilentlyContinue
    if ($cmd) { return $cmd.Source }
    return $null
}

function Run-Cmd {
    param([string]$Command, [string]$Args = "")
    $psi = New-Object System.Diagnostics.ProcessStartInfo
    $psi.FileName = $Command
    $psi.Arguments = $Args
    $psi.RedirectStandardOutput = $true
    $psi.RedirectStandardError = $true
    $psi.UseShellExecute = $false
    $psi.CreateNoWindow = $true
    $proc = [System.Diagnostics.Process]::Start($psi)
    $stdout = $proc.StandardOutput.ReadToEnd()
    $stderr = $proc.StandardError.ReadToEnd()
    $proc.WaitForExit()
    return @{ ExitCode = $proc.ExitCode; StdOut = $stdout; StdErr = $stderr }
}

function Prompt-SecureString {
    param([string]$Prompt)
    Write-Host "$Prompt" -NoNewline
    $sec = Read-Host -AsSecureString
    $ptr = [System.Runtime.InteropServices.Marshal]::SecureStringToBSTR($sec)
    try {
        return [System.Runtime.InteropServices.Marshal]::PtrToStringAuto($ptr)
    } finally {
        [System.Runtime.InteropServices.Marshal]::ZeroFreeBSTR($ptr)
    }
}

function Set-UserEnv {
    param([string]$Name, [string]$Value)
    [Environment]::SetEnvironmentVariable($Name, $Value, [EnvironmentVariableTarget]::User)
    $env:$Name = $Value
}

function Log-PasswordSafe {
    param([string]$Message)
    # Never log passwords; strip anything that looks like a secret
    $safe = $Message -replace '(?i)(password|passwd|pwd|secret|token)\s*[:=]\S*', '$1=***'
    Write-Log $safe
}

# ----------------------------- preflight -----------------------------
function Preflight-Checks {
    Write-Log "Running preflight checks..."
    $script:HAS_ADMIN = Test-IsAdmin
    Write-Log "Windows version: $(Get-CimInstance -Property Win32_OperatingSystem | Select-Object -First 1 | ForEach-Object { $_.Caption })"
    Write-Log "Administrator privileges: $script:HAS_ADMIN"

    $internet = Test-Internet
    if (-not $internet) {
        Write-Log "ERROR: No internet connectivity. Prerequisite installation requires internet." "ERROR"
        $script:EXIT_CODE = 1; return $false
    }
    Write-Log "Internet connectivity: OK"

    $freeGB = Get-FreeDiskGB
    Write-Log "Free disk space (C:): $freeGB GB"
    if ($freeGB -lt $MIN_DISK_GB) {
        Write-Log "ERROR: Insufficient disk space (need at least $MIN_DISK_GB GB)." "ERROR"
        $script:EXIT_CODE = 1; return $false
    }
    return $true
}

# ----------------------------- prerequisites -----------------------------
function Ensure-Java {
    Write-Log "Checking Java..."
    if (Test-Path $JAVA_EXE) {
        Write-Log "Java 17 found at $JAVA_EXE" "OK"
        return $true
    }
    Write-Log "Java 17 not found. Installing Amazon Corretto 17 via winget..."
    $res = Run-Cmd "winget" "install Amazon.Corretto.17.JDK --scope user --accept-package-agreements --accept-source-agreements"
    if ($res.ExitCode -ne 0) {
        Write-Log "Winget install of Java failed. Trying direct MSI download + extract..." "WARN"
        $msiUrl = "https://corretto.aws/downloads/resources/17.0.21.0.11/amazon-corretto-17.0.21.0.11-windows-x64.msi"
        $msiPath = Join-Path $TOOLS_DIR "amazon-corretto-17.msi"
        New-Item -ItemType Directory -Path $TOOLS_DIR -Force | Out-Null
        try {
            Invoke-WebRequest -Uri $msiUrl -OutFile $msiPath -TimeoutSec 300 -UseBasicParsing | Out-Null
            New-Item -ItemType Directory -Path $JAVA_DIR -Force | Out-Null
            $res2 = Run-Cmd "msiexec" "/a `"$msiPath`" /qn TARGETDIR=`"$JAVA_DIR`""
            if ($res2.ExitCode -ne 0) {
                Write-Log "ERROR: Could not install Java automatically. Please install Java 17 (Amazon Corretto or Eclipse Temurin) manually, then re-run setup." "ERROR"
                $script:EXIT_CODE = 1; return $false
            }
        } catch {
            Write-Log "ERROR: Java manual install failed: $($_.Exception.Message)" "ERROR"
            $script:EXIT_CODE = 1; return $false
        }
    }
    if (-not (Test-Path $JAVA_EXE)) {
        Write-Log "ERROR: Java executable not found at expected path after install." "ERROR"
        $script:EXIT_CODE = 1; return $false
    }
    Set-UserEnv "JAVA_HOME" (Split-Path -Parent (Split-Path -Parent $JAVA_EXE))
    $javaVer = Run-Cmd $JAVA_EXE "-version"
    Write-Log "Java version: $($javaVer.StdErr -split "`n" | Select-Object -First 1)" "OK"
    return $true
}

function Ensure-Maven {
    Write-Log "Checking Maven..."
    if (Test-Path $MAVEN_CMD) {
        Write-Log "Maven found at $MAVEN_CMD" "OK"
        return $true
    }
    Write-Log "Maven not found. Downloading Apache Maven 3.10.1..."
    New-Item -ItemType Directory -Path $TOOLS_DIR -Force | Out-Null
    $zipPath = Join-Path $TOOLS_DIR "apache-maven-bin.zip"
    try {
        Invoke-WebRequest -Uri "https://dlcdn.apache.org/maven/maven-3/3.10.1/binaries/apache-maven-3.10.1-bin.zip" -OutFile $zipPath -TimeoutSec 300 -UseBasicParsing | Out-Null
        Expand-Archive -LiteralPath $zipPath -DestinationPath $TOOLS_DIR -Force
    } catch {
        Write-Log "ERROR: Maven download failed: $($_.Exception.Message)" "ERROR"
        $script:EXIT_CODE = 1; return $false
    }
    if (-not (Test-Path $MAVEN_CMD)) {
        Write-Log "ERROR: Maven not found after extraction." "ERROR"
        $script:EXIT_CODE = 1; return $false
    }
    Set-UserEnv "MAVEN_HOME" $MAVEN_DIR
    $mvnVer = Run-Cmd $MAVEN_CMD "-version"
    Write-Log "Maven version: $($mvnVer.StdOut -split "`n" | Select-Object -First 1)" "OK"
    return $true
}

function Ensure-Mysql {
    Write-Log "Checking MySQL..."
    if (Test-Path $MYSQLD_EXE) {
        Write-Log "MySQL binaries found at $MYSQLD_EXE" "OK"
        return $true
    }
    if (-not $script:HAS_ADMIN) {
        Write-Log "MySQL not installed and administrator privileges are required to install MySQL server." "WARN"
        Write-Log "Please install MySQL 8.x manually (e.g. via winget: winget install Oracle.MySQL) as administrator, then re-run setup." "WARN"
        return $false
    }
    Write-Log "Installing MySQL 8.x via winget (requires admin)..."
    $res = Run-Cmd "winget" "install Oracle.MySQL --scope machine --accept-package-agreements --accept-source-agreements"
    if ($res.ExitCode -ne 0) {
        Write-Log "Winget MySQL install failed. You may need to install MySQL manually and re-run setup." "WARN"
        return $false
    }
    # After machine install, common paths:
    $candidates = @(
        "C:\Program Files\MySQL\MySQL Server 8.4\bin\mysqld.exe",
        "C:\Program Files\MySQL\MySQL Server 8.0\bin\mysqld.exe",
        "C:\Program Files\MySQL\MySQL Server 8.1\bin\mysqld.exe"
    )
    foreach ($c in $candidates) { if (Test-Path $c) { $global:MYSQLD_EXE = $c; break } }
    return $true
}

function Ensure-Git {
    $git = Find-Command "git"
    if ($git) { Write-Log "Git found: $git" "OK"; return $true }
    Write-Log "Git not found. Installing Git via winget..."
    $res = Run-Cmd "winget" "install Git.Git --scope user --accept-package-agreements --accept-source-agreements"
    if ($res.ExitCode -ne 0) {
        Write-Log "WARN: Git install failed. Git is required for version control." "WARN"
    }
    return $true
}

# ----------------------------- environment -----------------------------
function Setup-Environment {
    Write-Log "Setting up environment (.env)..."
    if (-not (Test-Path $ENV_FILE)) {
        if (Test-Path $ENV_EXAMPLE) {
            Copy-Item $ENV_EXAMPLE $ENV_FILE
        } else {
            Write-Log "WARNING: .env.example not found; creating minimal .env" "WARN"
            @"
# Attendance Management System environment (ignored by git)
ATTENDANCE_DB_URL=jdbc:mysql://localhost:3306/attendance_db?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC
ATTENDANCE_DB_USER=attendance_app
ATTENDANCE_DB_PASS=change_me
ATTENDANCE_SMTP_HOST=
ATTENDANCE_SMTP_PORT=587
ATTENDANCE_SMTP_USER=
ATTENDANCE_SMTP_PASS=
"@ | Out-File -FilePath $ENV_FILE -Encoding utf8
        }
    }
    # Prompt for DB password securely if placeholder present
    $envContent = Get-Content $ENV_FILE -Raw
    if ($envContent -match 'ATTENDANCE_DB_PASS=change_me' -or $envContent -match 'ATTENDANCE_DB_PASS=$') {
        $dbPass = Prompt-SecureString "Enter a password for the MySQL application user '$DB_USER': "
        $null = ($envContent -replace '(?m)^ATTENDANCE_DB_PASS=.*', "ATTENDANCE_DB_PASS=$dbPass")
        $envContent | Out-File -FilePath $ENV_FILE -Encoding utf8
        Write-Log "Application database password saved to .env (ignored by git)." "OK"
    }
    # Load into current process
    if (Test-Path $ENV_FILE) {
        Get-Content $ENV_FILE | ForEach-Object {
            if ($_ -match '^([^=]+)=(.*)$') { Set-UserEnv $matches[1] $matches[2] }
        }
    }
    return $true
}

# ----------------------------- mysql configure -----------------------------
function Ensure-MysqlRunning {
    # Try connecting; if not running, start mysqld as user process
    $ping = Run-Cmd $MYSQL_CLI "-u root -e \"SELECT 1\""
    if ($ping.ExitCode -eq 0) {
        Write-Log "MySQL is already running." "OK"
        return $true
    }
    Write-Log "MySQL not running. Starting mysqld as a user process..."
    if (-not (Test-Path $MYSQLD_EXE)) {
        Write-Log "ERROR: mysqld not found. Install MySQL first." "ERROR"
        $script:EXIT_CODE = 1; return $false
    }
    if (-not (Test-Path $MYSQL_DATA)) {
        Write-Log "Initializing MySQL data directory..."
        $init = Run-Cmd $MYSQLD_EXE "--initialize-insecure --datadir=`"$MYSQL_DATA`""
        if ($init.ExitCode -ne 0) {
            Write-Log "ERROR: MySQL initialization failed: $($init.StdErr)" "ERROR"
            $script:EXIT_CODE = 1; return $false
        }
        Write-Log "MySQL data directory initialized." "OK"
    }
    $psi = New-Object System.Diagnostics.ProcessStartInfo
    $psi.FileName = $MYSQLD_EXE
    $psi.Arguments = "--datadir=`"$MYSQL_DATA`" --port=$MYSQL_PORT --console"
    $psi.UseShellExecute = $false
    $psi.CreateNoWindow = $true
    $proc = [System.Diagnostics.Process]::Start($psi)
    Start-Sleep -Seconds 5
    for ($i = 0; $i -lt 12; $i++) {
        $ping = Run-Cmd $MYSQL_CLI "-u root -e \"SELECT 1\""
        if ($ping.ExitCode -eq 0) {         Write-Log "MySQL started on port $MYSQL_PORT." "OK"; return $true }
        Start-Sleep -Seconds 2
    }
    Write-Log "ERROR: MySQL did not become ready." "ERROR"
    $script:EXIT_CODE = 1; return $false
}

function Configure-Database {
    Write-Log "Configuring database '$DB_NAME' and user '$DB_USER'..."
    $rootPass = Prompt-SecureString "Enter MySQL root password (leave blank if none): "
    $rootAuth = if ($rootPass) { "-p`"$rootPass`"" } else { "" }
    $appPass  = $env:ATTENDANCE_DB_PASS
    if (-not $appPass) { $appPass = Prompt-SecureString "Enter password for application user '$DB_USER': " }

    $sql = @"
CREATE DATABASE IF NOT EXISTS $DB_NAME CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
CREATE USER IF NOT EXISTS '$DB_USER'@'localhost' IDENTIFIED BY '$appPass';
GRANT SELECT, INSERT, UPDATE, DELETE, CREATE, DROP, REFERENCES, INDEX, ALTER, LOCK TABLES, CREATE TEMPORARY TABLES, EXECUTE, TRIGGER ON $DB_NAME.* TO '$DB_USER'@'localhost';
FLUSH PRIVILEGES;
"@
    $sqlFile = Join-Path $env:TEMP "attendance_db_setup.sql"
    $sql | Out-File -FilePath $sqlFile -Encoding utf8
    $res = Run-Cmd $MYSQL_CLI "-u root $rootAuth < `"$sqlFile`""
    if ($res.ExitCode -ne 0) {
        Write-Log "ERROR: Database configuration failed: $($res.StdErr)" "ERROR"
        $script:EXIT_CODE = 1; return $false
    }
    Write-Log "Database and restricted user configured." "OK"
    return $true
}

function Init-Schema {
    Write-Log "Initializing schema (safe, no data destruction)..."
    $schema = Join-Path $PROJECT_DIR "database\schema.sql"
    if (-not (Test-Path $schema)) {
        Write-Log "ERROR: schema.sql not found." "ERROR"
        $script:EXIT_CODE = 1; return $false
    }
    $appPass = $env:ATTENDANCE_DB_PASS
    $res = Run-Cmd $MYSQL_CLI "-u $DB_USER -p`"$appPass`" $DB_NAME < `"$schema`""
    if ($res.ExitCode -ne 0) {
        Write-Log "ERROR: Schema initialization failed: $($res.StdErr)" "ERROR"
        $script:EXIT_CODE = 1; return $false
    }
    Write-Log "Schema initialized." "OK"
    return $true
}

function Seed-Data {
    Write-Log "Seeding demo data (fictional records only)..."
    $seed = Join-Path $PROJECT_DIR "database\seed.sql"
    if (-not (Test-Path $seed)) { Write-Log "No seed.sql found; skipping." "WARN"; return $true }
    $appPass = $env:ATTENDANCE_DB_PASS
    $res = Run-Cmd $MYSQL_CLI "-u $DB_USER -p`"$appPass`" $DB_NAME < `"$seed`""
    if ($res.ExitCode -ne 0) { Write-Log "WARN: Seed data load failed (non-fatal): $($res.StdErr)" "WARN"; return $true }
    Write-Log "Demo data seeded." "OK"
    return $true
}

# ----------------------------- build / test -----------------------------
function Build-App {
    Write-Log "Building application with Maven..."
    $res = Run-Cmd $MAVEN_CMD "-q -DskipTests clean package"
    if ($res.ExitCode -ne 0) {
        Write-Log "ERROR: Maven build failed." "ERROR"
        Write-Log $res.StdErr "ERROR"
        $script:EXIT_CODE = 1; return $false
    }
    if (-not (Test-Path $JAR_PATH)) {
        Write-Log "ERROR: Built jar not found at $JAR_PATH." "ERROR"
        $script:EXIT_CODE = 1; return $false
    }
    Write-Log "Build complete." "OK"
    return $true
}

function Run-Tests {
    Write-Log "Running automated tests..."
    $res = Run-Cmd $MAVEN_CMD "test"
    if ($res.ExitCode -ne 0) {
        Write-Log "ERROR: Tests failed." "ERROR"
        Write-Log $res.StdErr "ERROR"
        $script:EXIT_CODE = 1; return $false
    }
    Write-Log "Tests passed." "OK"
    return $true
}

# ----------------------------- run app -----------------------------
function Start-AppAndVerify {
    Write-Log "Starting application on port $APP_PORT..."
    if (Test-Path Join-Path $PROJECT_DIR "app.pid") { Remove-Item Join-Path $PROJECT_DIR "app.pid" -Force }
    $psi = New-Object System.Diagnostics.ProcessStartInfo
    $psi.FileName = $env:JAVA_HOME + "\bin\java.exe"
    $psi.Arguments = "-jar `"$JAR_PATH`""
    $psi.WorkingDirectory = $PROJECT_DIR
    $psi.UseShellExecute = $false
    $psi.RedirectStandardOutput = $true
    $psi.RedirectStandardError = $true
    $psi.CreateNoWindow = $true
    $proc = [System.Diagnostics.Process]::Start($psi)
    $outFile = Join-Path $PROJECT_DIR "app.out.log"
    $errFile = Join-Path $PROJECT_DIR "app.err.log"
    $proc.StandardOutput.BaseStream.CopyTo(([System.IO.File]::OpenWrite($outFile)))
    $proc.StandardError.BaseStream.CopyTo(([System.IO.File]::OpenWrite($errFile)))
    Set-Content -Path (Join-Path $PROJECT_DIR "app.pid") -Value $proc.Id

    $ready = $false
    for ($i = 0; $i < 60; $i++) {
        Start-Sleep -Seconds 2
        try {
            $r = Invoke-WebRequest -Uri "http://localhost:$APP_PORT/api/auth/csrf-token" -TimeoutSec 5 -UseBasicParsing
            if ($r.StatusCode -eq 200) { $ready = $true; break }
        } catch { /* not ready yet */ }
    }
    if (-not $ready) {
        Write-Log "ERROR: Application did not become ready on port $APP_PORT." "ERROR"
        Write-Log "Check app.err.log and app.out.log in $PROJECT_DIR" "ERROR"
        $script:EXIT_CODE = 1; return $false
    }
    Write-Log "Application is ready at http://localhost:$APP_PORT" "OK"
    return $true
}

# ----------------------------- main -----------------------------
function Main {
    Show-Banner
    try {
        if (-not (Preflight-Checks)) { Save-Log; exit $script:EXIT_CODE }
        if (-not (Ensure-Java)) { Save-Log; exit $script:EXIT_CODE }
        if (-not (Ensure-Maven)) { Save-Log; exit $script:EXIT_CODE }
        Ensure-Git | Out-Null
        if (-not (Setup-Environment)) { Save-Log; exit $script:EXIT_CODE }

        $mysqlReady = Ensure-MysqlRunning
        if (-not $mysqlReady) {
            # Ensure-MysqlRunning sets exit code on hard failure; if it's just not installed, prompt
            if (Test-Path $MYSQLD_EXE) {
                Write-Log "MySQL binaries present but server not running. Start MySQL and re-run setup." "WARN"
            }
            Save-Log; exit $script:EXIT_CODE
        }
        if (-not (Configure-Database)) { Save-Log; exit $script:EXIT_CODE }
        if (-not (Init-Schema)) { Save-Log; exit $script:EXIT_CODE }
        Seed-Data | Out-Null

        Push-Location $PROJECT_DIR
        if (-not (Build-App)) { Pop-Location; Save-Log; exit $script:EXIT_CODE }
        if (-not (Run-Tests)) { Pop-Location; Save-Log; exit $script:EXIT_CODE }
        Pop-Location

        if (-not (Start-AppAndVerify)) { Save-Log; exit $script:EXIT_CODE }

        Write-Log "Opening application in default browser..." "OK"
        try { Start-Process "http://localhost:$APP_PORT" } catch { Write-Log "Could not auto-open browser; navigate manually to http://localhost:$APP_PORT" "WARN" }

        Write-Host ""
        Write-Host "============================================================" -ForegroundColor Green
        Write-Host "  Setup complete!" -ForegroundColor Green
        Write-Host "  Application URL: http://localhost:$APP_PORT" -ForegroundColor Green
        Write-Host "  Demo credentials: admin / admin123   faculty / faculty123" -ForegroundColor Green
        Write-Host "  To stop: run Stop-AttendanceApp.bat" -ForegroundColor Green
        Write-Host "  Log file: $LOG_FILE" -ForegroundColor Green
        Write-Host "============================================================" -ForegroundColor Green
    } catch {
        Write-Log "FATAL: $($_.Exception.Message)" "ERROR"
        $script:EXIT_CODE = 1
    } finally {
        Save-Log
        exit $script:EXIT_CODE
    }
}

Main
