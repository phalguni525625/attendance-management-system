#requires -Version 5.1
<#
.SYNOPSIS
    One-click installer/launcher for the Attendance Management System.

.DESCRIPTION
    Works on a fresh Windows 10/11 machine. Detects already-installed tools and
    only downloads what is missing:
      - Amazon Corretto 17 (Java 17)
      - Apache Maven 3.10.1
      - MySQL 8.4 (server + client)
      - Git (optional, for version control)

    It then creates the attendance_db / attendance_db_test databases, initializes
    the schema and demo data without destroying existing data, builds the app,
    runs the automated tests, starts the Spring Boot application and opens it in
    the default browser.

    Python is NOT required by this project (it is a Java/Spring Boot app).

.NOTES
    Run from a normal user account. Administrator rights are only needed if
    winget has to install MySQL as a machine-wide service; otherwise the script
    falls back to a portable MySQL data directory under %LOCALAPPDATA%.
#>

$ErrorActionPreference = 'Stop'
$ProgressPreference = 'SilentlyContinue'

# ----------------------------- configuration -----------------------------
$SCRIPT_DIR  = Split-Path -Parent $MyInvocation.MyCommand.Definition
$PROJECT_DIR = $SCRIPT_DIR
$LOG_FILE    = Join-Path $SCRIPT_DIR 'setup.log'
$ENV_FILE    = Join-Path $SCRIPT_DIR '.env'
$ENV_EXAMPLE = Join-Path $SCRIPT_DIR '.env.example'
$TOOLS_DIR   = Join-Path $env:LOCALAPPDATA 'AttendanceApp\prereqs'
$APP_DATA    = Join-Path $env:LOCALAPPDATA 'AttendanceApp'
$JAR_NAME    = 'attendance-management-system-1.0.0.jar'
$JAR_PATH    = Join-Path $PROJECT_DIR $JAR_NAME
$DB_NAME     = 'attendance_db'
$TEST_DB     = 'attendance_db_test'
$DB_USER     = 'attendance_app'
$MYSQL_PORT  = 3306
$APP_PORT    = 8080
$MIN_DISK_GB = 4

$script:LOG_ENTRIES = @()
$script:EXIT_CODE   = 0
$script:JAVA_EXE    = $null
$script:MAVEN_CMD   = $null
$script:MYSQLD_EXE  = $null
$script:MYSQL_CLI   = $null
$script:MYSQL_PID   = $null

# ----------------------------- logging -----------------------------
function Write-Log {
    param([string]$Message, [string]$Level = 'INFO')
    $ts = Get-Date -Format 'yyyy-MM-dd HH:mm:ss'
    $line = "[$ts] [$Level] $Message"
    $script:LOG_ENTRIES += $line
    # Append immediately so setup.log is always current and the final write can never appear to hang.
    try { Add-Content -LiteralPath $LOG_FILE -Value $line -Encoding utf8 } catch { }
    switch ($Level) {
        'ERROR' { Write-Host $line -ForegroundColor Red }
        'WARN'  { Write-Host $line -ForegroundColor Yellow }
        'OK'    { Write-Host $line -ForegroundColor Green }
        default { Write-Host $line }
    }
}

function Save-Log {
    # Log lines are appended by Write-Log as they happen. This function is kept
    # as a final hook so the log file exists even if no message was written.
    if (-not (Test-Path -LiteralPath $LOG_FILE)) {
        try { New-Item -ItemType File -Path $LOG_FILE -Force | Out-Null } catch { }
    }
}

function Show-Banner {
    Write-Host ''
    Write-Host '============================================================' -ForegroundColor Cyan
    Write-Host '  Attendance Management System - One-click Setup' -ForegroundColor Cyan
    Write-Host '============================================================' -ForegroundColor Cyan
    Write-Host ''
}

# ----------------------------- helpers -----------------------------
function Test-Command {
    param([string]$Name)
    return [bool](Get-Command $Name -ErrorAction SilentlyContinue)
}

function Add-ToUserPath {
    param([string]$Directory)
    if (-not $Directory) { return }
    $userPath = [Environment]::GetEnvironmentVariable('Path', 'User')
    if ($null -eq $userPath) { $userPath = '' }
    $parts = $userPath.Split(';') | Where-Object { $_ -and $_.TrimEnd('\') -ieq $Directory.TrimEnd('\') }
    if (-not $parts) {
        $newPath = if ([string]::IsNullOrWhiteSpace($userPath)) { $Directory } else { $userPath.TrimEnd(';') + ';' + $Directory }
        [Environment]::SetEnvironmentVariable('Path', $newPath, 'User')
    }
    if ($env:Path -notlike "*$Directory*") { $env:Path = $env:Path + ';' + $Directory }
}

function Set-UserEnv {
    param([string]$Name, [string]$Value)
    [Environment]::SetEnvironmentVariable($Name, $Value, 'User')
    Set-Item -Path "Env:$Name" -Value $Value -ErrorAction SilentlyContinue
}

function Prompt-SecureString {
    param([string]$Prompt)
    Write-Host "$Prompt" -NoNewline
    $sec = Read-Host -AsSecureString
    $ptr = [System.Runtime.InteropServices.Marshal]::SecureStringToBSTR($sec)
    try { return [System.Runtime.InteropServices.Marshal]::PtrToStringAuto($ptr) }
    finally { [System.Runtime.InteropServices.Marshal]::ZeroFreeBSTR($ptr) }
}

function New-RandomPassword {
    param([int]$Length = 20)
    $bytes = New-Object byte[] $Length
    [System.Security.Cryptography.RandomNumberGenerator]::Create().GetBytes($bytes)
    $chars = 'abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789'
    -join ($bytes | ForEach-Object { $chars[ $_ % $chars.Length ] })
}

function Get-FreeDiskGB {
    try {
        $disk = Get-CimInstance Win32_LogicalDisk -Filter "DeviceID='$($env:SystemDrive)'" | Select-Object -First 1
        if ($disk) { return [math]::Round($disk.FreeSpace / 1GB, 2) }
    } catch { }
    return 0
}

function Test-Internet {
    try {
        $r = Invoke-WebRequest -Uri 'https://www.mysql.com' -TimeoutSec 12 -UseBasicParsing
        return $r.StatusCode -eq 200
    } catch {
        try { return (Invoke-WebRequest -Uri 'https://repo.maven.apache.org' -TimeoutSec 12 -UseBasicParsing).StatusCode -eq 200 }
        catch { return $false }
    }
}

function Find-FileInTree {
    param([string]$Root, [string]$Name, [int]$Depth = 5)
    if (-not (Test-Path -LiteralPath $Root)) { return $null }
    try {
        $found = Get-ChildItem -LiteralPath $Root -Filter $Name -Recurse -Depth $Depth -File -ErrorAction SilentlyContinue |
            Select-Object -First 1
        if ($found) { return $found.FullName }
    } catch { }
    return $null
}

function Invoke-Native {
    param([ScriptBlock]$Action)
    $old = $ErrorActionPreference
    $ErrorActionPreference = 'Continue'
    try {
        $output = & $Action
        return [pscustomobject]@{ Output = $output; ExitCode = $LASTEXITCODE }
    } finally {
        $ErrorActionPreference = $old
    }
}

# ----------------------------- detection -----------------------------
function Find-Java {
    if ($env:JAVA_HOME) {
        $candidate = Join-Path $env:JAVA_HOME 'bin\java.exe'
        if (Test-Path -LiteralPath $candidate) { return $candidate }
    }
    $cmd = Get-Command java -ErrorAction SilentlyContinue
    if ($cmd -and (Test-Path -LiteralPath $cmd.Source)) { return $cmd.Source }

    $roots = @(
        'C:\Program Files\Amazon Corretto',
        'C:\Program Files\Eclipse Adoptium',
        'C:\Program Files\Java',
        'C:\Program Files\Microsoft',
        'C:\Program Files\BellSoft',
        (Join-Path $env:LOCALAPPDATA 'Programs\Eclipse Adoptium'),
        (Join-Path $TOOLS_DIR 'corretto17'),
        (Join-Path $env:USERPROFILE 'kilo-tools\corretto17')
    )
    foreach ($root in $roots) {
        $found = Find-FileInTree -Root $root -Name 'java.exe' -Depth 5
        if ($found) { return $found }
    }
    return $null
}

function Find-Maven {
    if ($env:MAVEN_HOME) {
        $candidate = Join-Path $env:MAVEN_HOME 'bin\mvn.cmd'
        if (Test-Path -LiteralPath $candidate) { return $candidate }
    }
    $cmd = Get-Command mvn -ErrorAction SilentlyContinue
    if ($cmd -and (Test-Path -LiteralPath $cmd.Source)) { return $cmd.Source }

    $roots = @('C:\Program Files\apache-maven', 'C:\apache-maven', (Join-Path $TOOLS_DIR 'maven'), (Join-Path $env:USERPROFILE 'kilo-tools\maven'))
    foreach ($root in $roots) {
        $found = Find-FileInTree -Root $root -Name 'mvn.cmd' -Depth 4
        if ($found) { return $found }
    }
    return $null
}

function Find-Mysql {
    $cmd = Get-Command mysqld -ErrorAction SilentlyContinue
    if ($cmd -and (Test-Path -LiteralPath $cmd.Source)) {
        $bin = Split-Path -Parent $cmd.Source
        return @{ Daemon = $cmd.Source; Client = (Join-Path $bin 'mysql.exe') }
    }
    $cmd = Get-Command mysql -ErrorAction SilentlyContinue
    if ($cmd -and (Test-Path -LiteralPath $cmd.Source)) {
        $bin = Split-Path -Parent $cmd.Source
        $daemon = Join-Path $bin 'mysqld.exe'
        if (Test-Path -LiteralPath $daemon) { return @{ Daemon = $daemon; Client = $cmd.Source } }
    }

    $roots = @('C:\Program Files\MySQL', 'C:\Program Files (x86)\MySQL', (Join-Path $TOOLS_DIR 'mysql84'), (Join-Path $env:USERPROFILE 'kilo-tools\mysql84'))
    foreach ($root in $roots) {
        $daemon = Find-FileInTree -Root $root -Name 'mysqld.exe' -Depth 6
        if ($daemon) {
            $bin = Split-Path -Parent $daemon
            return @{ Daemon = $daemon; Client = (Join-Path $bin 'mysql.exe') }
        }
    }
    return $null
}

# ----------------------------- installers -----------------------------
function Install-Java {
    Write-Log 'Java 17 not found. Installing Amazon Corretto 17...'
    New-Item -ItemType Directory -Path $TOOLS_DIR -Force | Out-Null

    if (Test-Command 'winget') {
        Write-Log 'Trying winget install (user scope)...'
        $res = Start-Process -FilePath 'winget' -ArgumentList @(
            'install', 'Amazon.Corretto.17.JDK', '--scope', 'user',
            '--accept-package-agreements', '--accept-source-agreements', '--silent'
        ) -Wait -PassThru -NoNewWindow
        if ($res.ExitCode -eq 0) {
            $script:JAVA_EXE = Find-Java
            if ($script:JAVA_EXE) { Write-Log "Java installed at $script:JAVA_EXE" 'OK'; return $true }
        }
        Write-Log 'winget Java install did not produce a usable java.exe. Trying portable MSI extract...' 'WARN'
    }

    try {
        $msiUrl = 'https://corretto.aws/downloads/resources/17.0.21.0.11/amazon-corretto-17.0.21.0.11-windows-x64.msi'
        $msi    = Join-Path $TOOLS_DIR 'amazon-corretto-17.msi'
        Write-Log "Downloading $msiUrl"
        Invoke-WebRequest -Uri $msiUrl -OutFile $msi -TimeoutSec 600 -UseBasicParsing
        $target = Join-Path $TOOLS_DIR 'corretto17'
        New-Item -ItemType Directory -Path $target -Force | Out-Null
        Write-Log 'Extracting Java (administrative install, no admin rights required)...'
        $res = Start-Process -FilePath 'msiexec.exe' -ArgumentList @('/a', "`"$msi`"", '/qn', "TARGETDIR=`"$target`"") -Wait -PassThru -NoNewWindow
        if ($res.ExitCode -ne 0) { throw "msiexec extract failed with exit code $($res.ExitCode)" }
    } catch {
        Write-Log "Java installation failed: $($_.Exception.Message)" 'ERROR'
        return $false
    }

    $script:JAVA_EXE = Find-Java
    if (-not $script:JAVA_EXE) {
        Write-Log 'Java still not found after installation.' 'ERROR'
        return $false
    }
    Write-Log "Java installed at $script:JAVA_EXE" 'OK'
    return $true
}

function Install-Maven {
    Write-Log 'Maven not found. Downloading Apache Maven 3.10.1...'
    New-Item -ItemType Directory -Path $TOOLS_DIR -Force | Out-Null
    try {
        $zipUrl = 'https://dlcdn.apache.org/maven/maven-3/3.10.1/binaries/apache-maven-3.10.1-bin.zip'
        $zip    = Join-Path $TOOLS_DIR 'apache-maven-3.10.1-bin.zip'
        Write-Log "Downloading $zipUrl"
        Invoke-WebRequest -Uri $zipUrl -OutFile $zip -TimeoutSec 600 -UseBasicParsing
        Write-Log 'Extracting Maven...'
        Expand-Archive -LiteralPath $zip -DestinationPath (Join-Path $TOOLS_DIR 'maven') -Force
    } catch {
        Write-Log "Maven installation failed: $($_.Exception.Message)" 'ERROR'
        return $false
    }
    $script:MAVEN_CMD = Find-Maven
    if (-not $script:MAVEN_CMD) {
        Write-Log 'Maven still not found after extraction.' 'ERROR'
        return $false
    }
    Write-Log "Maven installed at $script:MAVEN_CMD" 'OK'
    return $true
}

function Install-Mysql {
    Write-Log 'MySQL not found. Installing MySQL 8.4...'
    New-Item -ItemType Directory -Path $TOOLS_DIR -Force | Out-Null

    if (Test-Command 'winget') {
        Write-Log 'Trying winget install (machine scope, may require admin)...'
        $res = Start-Process -FilePath 'winget' -ArgumentList @(
            'install', 'Oracle.MySQL', '--scope', 'machine',
            '--accept-package-agreements', '--accept-source-agreements', '--silent'
        ) -Wait -PassThru -NoNewWindow
        if ($res.ExitCode -eq 0) {
            $found = Find-Mysql
            if ($found) {
                $script:MYSQLD_EXE = $found.Daemon
                $script:MYSQL_CLI  = $found.Client
                Write-Log "MySQL installed at $($found.Daemon)" 'OK'
                return $true
            }
        }
        Write-Log 'winget MySQL install did not produce usable binaries. Trying portable MSI extract...' 'WARN'
    }

    try {
        $msiUrl = 'https://dev.mysql.com/get/Downloads/MySQL-8.4/mysql-8.4.9-winx64.msi'
        $msi    = Join-Path $TOOLS_DIR 'mysql-8.4.9-winx64.msi'
        Write-Log "Downloading $msiUrl"
        Invoke-WebRequest -Uri $msiUrl -OutFile $msi -TimeoutSec 900 -UseBasicParsing
        $target = Join-Path $TOOLS_DIR 'mysql84'
        New-Item -ItemType Directory -Path $target -Force | Out-Null
        Write-Log 'Extracting MySQL (administrative install, no admin rights required)...'
        $res = Start-Process -FilePath 'msiexec.exe' -ArgumentList @('/a', "`"$msi`"", '/qn', "TARGETDIR=`"$target`"") -Wait -PassThru -NoNewWindow
        if ($res.ExitCode -ne 0) { throw "msiexec extract failed with exit code $($res.ExitCode)" }
    } catch {
        Write-Log "MySQL installation failed: $($_.Exception.Message)" 'ERROR'
        return $false
    }

    $found = Find-Mysql
    if (-not $found) {
        Write-Log 'MySQL still not found after extraction.' 'ERROR'
        return $false
    }
    $script:MYSQLD_EXE = $found.Daemon
    $script:MYSQL_CLI  = $found.Client
    Write-Log "MySQL installed at $($found.Daemon)" 'OK'
    return $true
}

function Install-Git {
    if (Test-Command 'git') { Write-Log "Git found: $((Get-Command git).Source)" 'OK'; return $true }
    if (-not (Test-Command 'winget')) { Write-Log 'Git not found and winget is unavailable. Skipping Git.' 'WARN'; return $false }
    Write-Log 'Installing Git via winget...'
    $res = Start-Process -FilePath 'winget' -ArgumentList @(
        'install', 'Git.Git', '--scope', 'user',
        '--accept-package-agreements', '--accept-source-agreements', '--silent'
    ) -Wait -PassThru -NoNewWindow
    if ($res.ExitCode -eq 0 -and (Test-Command 'git')) { Write-Log 'Git installed.' 'OK'; return $true }
    Write-Log 'Git install failed or git is still not on PATH. Git is optional for running the app.' 'WARN'
    return $false
}

# ----------------------------- MySQL lifecycle -----------------------------
function Test-MysqlReady {
    param([string]$RootPassword = '')
    if (-not $script:MYSQL_CLI) { return $false }
    $cmdArgs = @('-u', 'root')
    if ($RootPassword) { $cmdArgs += "--password=$RootPassword" }
    $cmdArgs += @('-e', 'SELECT 1')
    try {
        $r = Invoke-Native { & $script:MYSQL_CLI @cmdArgs 2>&1 }
        return $r.ExitCode -eq 0
    } catch { return $false }
}

function Test-PortOpen {
    param([int]$Port)
    try {
        return [bool](Get-NetTCPConnection -LocalPort $Port -State Listen -ErrorAction SilentlyContinue)
    } catch {
        try { return (Test-NetConnection -ComputerName localhost -Port $Port -InformationLevel Quiet) } catch { return $false }
    }
}

function Start-MysqlIfNeeded {
    if (Test-MysqlReady) { Write-Log 'MySQL is already running.' 'OK'; return $true }

    if (Test-PortOpen -Port $MYSQL_PORT) {
        Write-Log "Port $MYSQL_PORT is already in use; assuming MySQL is running (root password may be required)." 'WARN'
        return $true
    }

    # Try an installed Windows service first.
    $service = Get-Service -ErrorAction SilentlyContinue | Where-Object { $_.Name -like 'MySQL*' -or $_.DisplayName -like '*MySQL*' } | Select-Object -First 1
    if ($service) {
        Write-Log "Starting MySQL service '$($service.Name)'..."
        try {
            if ($service.Status -ne 'Running') { Start-Service -Name $service.Name -ErrorAction Stop }
            Start-Sleep -Seconds 3
            if (Test-MysqlReady) { Write-Log 'MySQL service is ready.' 'OK'; return $true }
            if (Test-PortOpen -Port $MYSQL_PORT) { Write-Log 'MySQL service is listening; root password may be required.' 'WARN'; return $true }
        } catch {
            Write-Log "Could not start MySQL service: $($_.Exception.Message)" 'WARN'
        }
    }

    if (-not $script:MYSQLD_EXE) {
        Write-Log 'mysqld.exe was not found, cannot start MySQL.' 'ERROR'
        return $false
    }

    $dataDir = Join-Path $APP_DATA 'mysql-data'
    if (-not (Test-Path -LiteralPath $dataDir)) {
        Write-Log "Initializing MySQL data directory at $dataDir ..."
        New-Item -ItemType Directory -Path $APP_DATA -Force | Out-Null
        $init = Start-Process -FilePath $script:MYSQLD_EXE -ArgumentList @("--initialize-insecure", "--datadir=`"$dataDir`"") -Wait -PassThru -NoNewWindow
        if ($init.ExitCode -ne 0) {
            Write-Log "MySQL initialization failed with exit code $($init.ExitCode)." 'ERROR'
            return $false
        }
        Write-Log 'MySQL data directory initialized (root has no password).' 'OK'
    }

    $pidFile = Join-Path $APP_DATA 'mysql.pid'
    if (Test-Path -LiteralPath $pidFile) {
        $oldPid = Get-Content -LiteralPath $pidFile -ErrorAction SilentlyContinue
        if ($oldPid) {
            $proc = Get-Process -Id $oldPid -ErrorAction SilentlyContinue
            if ($proc) { Write-Log "MySQL already running with PID $oldPid." 'OK'; return $true }
        }
    }

    Write-Log "Starting mysqld on port $MYSQL_PORT..."
    $proc = Start-Process -FilePath $script:MYSQLD_EXE -ArgumentList @(
        "--datadir=`"$dataDir`"", "--port=$MYSQL_PORT", '--console'
    ) -WindowStyle Hidden -PassThru
    $script:MYSQL_PID = $proc.Id
    $proc.Id | Out-File -FilePath $pidFile -Encoding ascii

    for ($i = 0; $i -lt 30; $i++) {
        Start-Sleep -Seconds 2
        if (Test-MysqlReady) { Write-Log "MySQL is ready (PID $($proc.Id))." 'OK'; return $true }
    }
    Write-Log 'MySQL did not become ready within 60 seconds.' 'ERROR'
    return $false
}

function Get-RootPassword {
    if (Test-MysqlReady) { return '' }
    Write-Log 'MySQL is running but root requires a password.' 'WARN'
    $secure = Prompt-SecureString 'Enter MySQL root password: '
    $plain = [System.Runtime.InteropServices.Marshal]::PtrToStringAuto(
        [System.Runtime.InteropServices.Marshal]::SecureStringToBSTR($secure))
    if (-not (Test-MysqlReady -RootPassword $plain)) {
        Write-Log 'Could not connect to MySQL with the supplied root password.' 'ERROR'
        return $null
    }
    return $plain
}

# ----------------------------- configuration -----------------------------
function Read-EnvFile {
    $map = @{}
    if (Test-Path -LiteralPath $ENV_FILE) {
        Get-Content -LiteralPath $ENV_FILE | ForEach-Object {
            if ($_ -match '^\s*([^#=]+)=(.*)$') { $map[$matches[1].Trim()] = $matches[2] }
        }
    }
    return $map
}

function Write-EnvFile {
    param([hashtable]$Values)
    $lines = @(
        '# Attendance Management System environment (git-ignored)',
        "ATTENDANCE_DB_URL=$($Values['ATTENDANCE_DB_URL'])",
        "ATTENDANCE_DB_USER=$($Values['ATTENDANCE_DB_USER'])",
        "ATTENDANCE_DB_PASS=$($Values['ATTENDANCE_DB_PASS'])",
        '',
        '# Optional SMTP (leave blank to disable email notifications)',
        "ATTENDANCE_SMTP_HOST=$($Values['ATTENDANCE_SMTP_HOST'])",
        "ATTENDANCE_SMTP_PORT=$($Values['ATTENDANCE_SMTP_PORT'])",
        "ATTENDANCE_SMTP_USER=$($Values['ATTENDANCE_SMTP_USER'])",
        "ATTENDANCE_SMTP_PASS=$($Values['ATTENDANCE_SMTP_PASS'])",
        "ATTENDANCE_SMTP_AUTH=$($Values['ATTENDANCE_SMTP_AUTH'])",
        "ATTENDANCE_SMTP_STARTTLS=$($Values['ATTENDANCE_SMTP_STARTTLS'])",
        '',
        "SERVER_PORT=$APP_PORT"
    )
    $lines | Out-File -FilePath $ENV_FILE -Encoding utf8
}

function Setup-Environment {
    Write-Log 'Configuring .env...'
    $envMap = Read-EnvFile
    if (-not $envMap.ContainsKey('ATTENDANCE_DB_URL')) {
        $envMap['ATTENDANCE_DB_URL'] = "jdbc:mysql://localhost:$MYSQL_PORT/$DB_NAME?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC"
    }
    $envMap['ATTENDANCE_DB_USER'] = $DB_USER
    if (-not $envMap.ContainsKey('ATTENDANCE_SMTP_HOST')) { $envMap['ATTENDANCE_SMTP_HOST'] = '' }
    if (-not $envMap.ContainsKey('ATTENDANCE_SMTP_PORT')) { $envMap['ATTENDANCE_SMTP_PORT'] = '587' }
    if (-not $envMap.ContainsKey('ATTENDANCE_SMTP_USER')) { $envMap['ATTENDANCE_SMTP_USER'] = '' }
    if (-not $envMap.ContainsKey('ATTENDANCE_SMTP_PASS')) { $envMap['ATTENDANCE_SMTP_PASS'] = '' }
    if (-not $envMap.ContainsKey('ATTENDANCE_SMTP_AUTH')) { $envMap['ATTENDANCE_SMTP_AUTH'] = 'false' }
    if (-not $envMap.ContainsKey('ATTENDANCE_SMTP_STARTTLS')) { $envMap['ATTENDANCE_SMTP_STARTTLS'] = 'true' }

    $currentPass = $envMap['ATTENDANCE_DB_PASS']
    if (-not $currentPass -or $currentPass -match 'change_me' -or $currentPass -match '^$') {
        $newPass = New-RandomPassword
        $envMap['ATTENDANCE_DB_PASS'] = $newPass
        Write-Log 'Generated a new database password and saved it to .env (git-ignored).' 'OK'
    }
    Write-EnvFile -Values $envMap

    foreach ($key in $envMap.Keys) {
        Set-Item -Path "Env:$key" -Value $envMap[$key] -ErrorAction SilentlyContinue
    }
    return $envMap
}

function Configure-Database {
    param([string]$RootPassword, [hashtable]$EnvMap)
    Write-Log "Configuring database '$DB_NAME', test database '$TEST_DB' and user '$DB_USER'..."
    $appPass = $EnvMap['ATTENDANCE_DB_PASS']
    $appPassSql = $appPass.Replace("'", "''")

    $sql = @"
CREATE DATABASE IF NOT EXISTS $DB_NAME CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
CREATE DATABASE IF NOT EXISTS $TEST_DB CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
CREATE USER IF NOT EXISTS '$DB_USER'@'localhost' IDENTIFIED BY '$appPassSql';
ALTER USER '$DB_USER'@'localhost' IDENTIFIED BY '$appPassSql';
GRANT SELECT, INSERT, UPDATE, DELETE, CREATE, DROP, REFERENCES, INDEX, ALTER, LOCK TABLES, CREATE TEMPORARY TABLES, EXECUTE, TRIGGER ON $DB_NAME.* TO '$DB_USER'@'localhost';
GRANT ALL PRIVILEGES ON $TEST_DB.* TO '$DB_USER'@'localhost';
FLUSH PRIVILEGES;
"@

    $cmdArgs = @('-u', 'root')
    if ($RootPassword) { $cmdArgs += "--password=$RootPassword" }
    $cmdArgs += @('-e', $sql)
    try {
        $r = Invoke-Native { & $script:MYSQL_CLI @cmdArgs 2>&1 }
        if ($r.ExitCode -ne 0) {
            Write-Log "Database configuration failed: $($r.Output)" 'ERROR'
            return $false
        }
    } catch {
        Write-Log "Database configuration failed: $($_.Exception.Message)" 'ERROR'
        return $false
    }
    Write-Log 'Databases and restricted application user configured.' 'OK'
    return $true
}

function Invoke-SqlFile {
    param([string]$File, [string]$Database, [string]$RootPassword)
    if (-not (Test-Path -LiteralPath $File)) {
        Write-Log "SQL file not found: $File" 'ERROR'
        return $false
    }
    $cmdArgs = @('-u', 'root')
    if ($RootPassword) { $cmdArgs += "--password=$RootPassword" }
    $cmdArgs += $Database
    try {
        $r = Invoke-Native { Get-Content -LiteralPath $File -Raw | & $script:MYSQL_CLI @cmdArgs 2>&1 }
        if ($r.ExitCode -ne 0) { throw "mysql exited with code $($r.ExitCode)" }
        return $true
    } catch {
        Write-Log "Failed to run $File : $($_.Exception.Message)" 'ERROR'
        return $false
    }
}

function Initialize-Schema {
    Write-Log 'Initializing schema (safe, no existing data is dropped)...'
    $schema = Join-Path $PROJECT_DIR 'database\schema.sql'
    if (-not (Invoke-SqlFile -File $schema -Database $DB_NAME -RootPassword $script:ROOT_PASSWORD)) { return $false }
    if (-not (Invoke-SqlFile -File $schema -Database $TEST_DB -RootPassword $script:ROOT_PASSWORD)) { return $false }
    Write-Log 'Schema initialized in both databases.' 'OK'
    return $true
}

function Seed-DemoData {
    $seed = Join-Path $PROJECT_DIR 'database\seed.sql'
    if (-not (Test-Path -LiteralPath $seed)) { Write-Log 'No seed.sql found; skipping demo data.' 'WARN'; return $true }

    # Only seed a fresh database; never re-insert into an existing one.
    $cmdArgs = @('-u', 'root')
    if ($script:ROOT_PASSWORD) { $cmdArgs += "--password=$($script:ROOT_PASSWORD)" }
    $cmdArgs += @('-N', '-B', $DB_NAME, '-e', 'SELECT COUNT(1) FROM students')
    $r = Invoke-Native { & $script:MYSQL_CLI @cmdArgs 2>&1 }
    $countText = $r.Output
    $count = 0
    [void][int]::TryParse((("$countText" -split "`n" | Select-Object -First 1).Trim()), [ref]$count)
    if ($count -gt 0) {
        Write-Log "Database already contains $count students; skipping demo seed." 'INFO'
        return $true
    }

    Write-Log 'Seeding fictional demo data (courses, subjects, students)...'
    if (Invoke-SqlFile -File $seed -Database $DB_NAME -RootPassword $script:ROOT_PASSWORD) {
        Write-Log 'Demo data seeded.' 'OK'
    } else {
        Write-Log 'Demo data load failed (non-fatal).' 'WARN'
    }
    return $true
}

# ----------------------------- build / run -----------------------------
function Build-App {
    Write-Log 'Building application with Maven (this also runs the automated tests)...'
    Push-Location $PROJECT_DIR
    try {
        $r = Invoke-Native { & $script:MAVEN_CMD -q clean package 2>&1 }
        $output = $r.Output
        $code = $r.ExitCode
        $output | Out-File -FilePath "$env:TEMP\attendance_mvn_out.txt" -Encoding utf8
        if ($code -ne 0) {
            Write-Log 'Maven build/test failed.' 'ERROR'
            $text = ($output | Out-String)
            $text -split "`n" | Where-Object { $_ -notmatch 'OpenJDK 64-Bit Server VM warning' } | Select-Object -Last 60 | ForEach-Object { Write-Log $_ 'ERROR' }
            return $false
        }
    } finally { Pop-Location }

    $builtJar = Join-Path $PROJECT_DIR "target\$JAR_NAME"
    if (-not (Test-Path -LiteralPath $builtJar)) {
        Write-Log "Build did not produce $builtJar" 'ERROR'
        return $false
    }
    Copy-Item -LiteralPath $builtJar -Destination $JAR_PATH -Force
    Write-Log "Build complete: $JAR_PATH" 'OK'
    return $true
}

function Start-App {
    $listener = Get-NetTCPConnection -LocalPort $APP_PORT -State Listen -ErrorAction SilentlyContinue
    if ($listener) {
        Write-Log "Port $APP_PORT is already in use. Assuming the app is already running." 'WARN'
        return $true
    }

    Write-Log "Starting application on port $APP_PORT..."
    $outFile = Join-Path $PROJECT_DIR 'app.out.log'
    $errFile = Join-Path $PROJECT_DIR 'app.err.log'
    $pidFile = Join-Path $PROJECT_DIR 'app.pid'
    if (Test-Path -LiteralPath $pidFile) { Remove-Item -LiteralPath $pidFile -Force -ErrorAction SilentlyContinue }

    $javaArgs = @('-jar', "`"$JAR_PATH`"")
    $proc = Start-Process -FilePath $script:JAVA_EXE -ArgumentList $javaArgs -WorkingDirectory $PROJECT_DIR `
        -RedirectStandardOutput $outFile -RedirectStandardError $errFile -WindowStyle Hidden -PassThru
    $proc.Id | Out-File -FilePath $pidFile -Encoding ascii
    Write-Log "Application process started (PID $($proc.Id))." 'OK'

    for ($i = 0; $i -lt 45; $i++) {
        Start-Sleep -Seconds 2
        try {
            $r = Invoke-WebRequest -Uri "http://localhost:$APP_PORT/api/auth/csrf-token" -TimeoutSec 5 -UseBasicParsing
            if ($r.StatusCode -eq 200) {
                Write-Log "Application is ready at http://localhost:$APP_PORT" 'OK'
                return $true
            }
        } catch { }
    }
    Write-Log 'Application did not become ready within 90 seconds. Check app.out.log / app.err.log.' 'ERROR'
    return $false
}

# ----------------------------- main -----------------------------
function Main {
    Show-Banner
    try {
        $free = Get-FreeDiskGB
        Write-Log "Free disk space on $env:SystemDrive: $free GB"
        if ($free -lt $MIN_DISK_GB) {
            Write-Log "At least $MIN_DISK_GB GB free is recommended." 'WARN'
        }

        if (-not (Test-Internet)) {
            Write-Log 'No internet connectivity. Prerequisite downloads require internet.' 'ERROR'
            $script:EXIT_CODE = 1; return
        }
        Write-Log 'Internet connectivity: OK' 'OK'

        Write-Log 'Python is not required for this Java/Spring Boot project; skipping Python.' 'INFO'

        # Java
        $script:JAVA_EXE = Find-Java
        if ($script:JAVA_EXE) {
            Write-Log "Java 17 found at $script:JAVA_EXE" 'OK'
        } elseif (-not (Install-Java)) {
            $script:EXIT_CODE = 1; return
        }
        $javaHome = Split-Path -Parent (Split-Path -Parent $script:JAVA_EXE)
        Set-UserEnv 'JAVA_HOME' $javaHome
        Add-ToUserPath (Join-Path $javaHome 'bin')

        # Maven
        $script:MAVEN_CMD = Find-Maven
        if ($script:MAVEN_CMD) {
            Write-Log "Maven found at $script:MAVEN_CMD" 'OK'
        } elseif (-not (Install-Maven)) {
            $script:EXIT_CODE = 1; return
        }
        $mavenHome = Split-Path -Parent (Split-Path -Parent $script:MAVEN_CMD)
        Set-UserEnv 'MAVEN_HOME' $mavenHome
        Add-ToUserPath (Join-Path $mavenHome 'bin')

        # Git (optional)
        Install-Git | Out-Null

        # MySQL
        $mysql = Find-Mysql
        if ($mysql) {
            $script:MYSQLD_EXE = $mysql.Daemon
            $script:MYSQL_CLI  = $mysql.Client
            Write-Log "MySQL found at $($mysql.Daemon)" 'OK'
        } elseif (-not (Install-Mysql)) {
            Write-Log 'MySQL is required. Please install MySQL 8.x and re-run setup.' 'ERROR'
            $script:EXIT_CODE = 1; return
        }

        # Environment
        $envMap = Setup-Environment

        # Start MySQL
        if (-not (Start-MysqlIfNeeded)) {
            $script:EXIT_CODE = 1; return
        }

        # Root password (if needed)
        $rootPass = Get-RootPassword
        if ($null -eq $rootPass) { $script:EXIT_CODE = 1; return }
        $script:ROOT_PASSWORD = $rootPass

        # Database + schema + seed
        if (-not (Configure-Database -RootPassword $rootPass -EnvMap $envMap)) { $script:EXIT_CODE = 1; return }
        if (-not (Initialize-Schema)) { $script:EXIT_CODE = 1; return }
        Seed-DemoData | Out-Null

        # Build + test
        if (-not (Build-App)) { $script:EXIT_CODE = 1; return }

        # Run
        if (-not (Start-App)) { $script:EXIT_CODE = 1; return }

        Write-Host ''
        Write-Host '============================================================' -ForegroundColor Green
        Write-Host '  Setup complete!' -ForegroundColor Green
        Write-Host "  URL:       http://localhost:$APP_PORT" -ForegroundColor Green
        Write-Host '  Admin:     admin   / admin123' -ForegroundColor Green
        Write-Host '  Faculty:   faculty / faculty123' -ForegroundColor Green
        Write-Host '  Stop app:  Stop-AttendanceApp.bat' -ForegroundColor Green
        Write-Host "  Log file:  $LOG_FILE" -ForegroundColor Green
        Write-Host '============================================================' -ForegroundColor Green

        # Open the browser last so the console summary is never delayed by the browser.
        Write-Log 'Opening http://localhost:8080 in the default browser...'
        try { Start-Process "http://localhost:$APP_PORT" } catch { }
        Write-Log 'Setup finished successfully.'
    } catch {
        Write-Log "FATAL: $($_.Exception.Message)" 'ERROR'
        Write-Log $_.ScriptStackTrace 'ERROR'
        $script:EXIT_CODE = 1
    } finally {
        Save-Log
        exit $script:EXIT_CODE
    }
}

Main