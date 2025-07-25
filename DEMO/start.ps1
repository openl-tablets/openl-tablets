# ===================================================================================
# OpenL Tablets & Jetty Runner Script for Windows
#
# This script automates the download, setup, and execution of OpenL Tablets
# WebStudio and RuleServices on an embedded Jetty server.
#
# Usage:
#   Double-click the 'start.cmd' file.
#
# Compatibility:
# - Windows 10/11
# - Architectures (x86_64/amd64, arm64)
# ===================================================================================

# Stop script on any error, similar to 'set -e'
$ErrorActionPreference = 'Stop'

# --- 1. SETTINGS ---
# Define the required versions for your environment.
# -----------------------------------------------------------------------------------
$SKIP_OS_JAVA = $false # To ignore system Java and use a local JRE
$MAVEN_URL = "https://repo1.maven.org/maven2"

$JAVA_MAJOR_VERSION = "21"
$JETTY_VERSION = "12.0.23"

# JDBC Driver Versions
$POSTGRES_VERSION = "42.7.7"
$ORACLE_VERSION = "23.8.0.25.04"
$MSSQL_VERSION = "12.10.1.jre11"


# --- SCRIPT INTERNAL VARIABLES ---
# -----------------------------------------------------------------------------------
$SCRIPT_DIR = $PSScriptRoot
Set-Location $SCRIPT_DIR
$JETTY_HOME = Join-Path $SCRIPT_DIR "jetty-home"
$JETTY_DOWNLOAD_URL = "$MAVEN_URL/org/eclipse/jetty/jetty-home/$JETTY_VERSION/jetty-home-$JETTY_VERSION.tar.gz"
$WEBAPPS_DIR = Join-Path $SCRIPT_DIR "webapps"
$JAVA_CMD = $null # This will be set by the find_or_download_java function

# --- READ OPENL VERSION FROM FILE ---
# -----------------------------------------------------------------------------------
$VersionFile = Join-Path $SCRIPT_DIR "openl.version"
if (-not (Test-Path $VersionFile)) {
    Write-Host "ERROR: Version file not found at $VersionFile" -ForegroundColor Red
    Write-Host "Please create a file named 'openl.version' with the desired OpenL version (e.g., 6.0.0)." -ForegroundColor Yellow
    exit 1
}
# Use -Raw to read the entire file as a single string, trimming any whitespace.
$OPENL_VERSION = (Get-Content -Path $VersionFile -Raw).Trim()
Write-Host "INFO: Using OpenL version '$OPENL_VERSION' from file." -ForegroundColor Green

# --- 2. DETERMINE AVAILABLE RAM and SET _JAVA_MEMORY ---
# Calculates 80% of total system RAM for the JVM heap space.
# -----------------------------------------------------------------------------------
Write-Host "INFO: Determining available RAM..." -ForegroundColor Green
try {
    $osInfo = Get-CimInstance -ClassName Win32_OperatingSystem
    $TotalRamMB = [math]::Round(($osInfo.TotalVisibleMemorySize / 1024))
    $XmxMB = [math]::Round($TotalRamMB * 0.8)
} catch {
    Write-Host "ERROR: Could not determine total RAM. Defaulting to 2GB." -ForegroundColor Yellow
    $TotalRamMB = "Unknown"
    $XmxMB = 2048
}

Write-Host "INFO: Setting JVM Max Heap Size (-Xmx) to $($XmxMB)M." -ForegroundColor Green
$_JAVA_MEMORY = "-Xmx${XmxMB}m"


# --- 3. FIND OR DOWNLOAD JAVA ---
# Checks for a suitable Java version and downloads a JRE if necessary.
# -----------------------------------------------------------------------------------
function Find-OrDownloadJava {
    param($requiredVersion)

    Write-Host "INFO: Checking for a valid Java installation..." -ForegroundColor Green
    $localJavaDir = Join-Path $SCRIPT_DIR "jre"

    # Function to check the version of a given java executable
    function Test-JavaVersion {
        param($javaPath)
        try {
            $versionOutput = & $javaPath -version 2>&1
            $versionString = $versionOutput | Select-String -Pattern 'version "(\d+)'
            $majorVersion = $versionString.Matches.Groups[1].Value

            if ($majorVersion -ge $requiredVersion) {
                Write-Host "INFO: Found suitable Java version $majorVersion at $javaPath." -ForegroundColor Green
                $script:JAVA_CMD = $javaPath
                return $true
            } else {
                Write-Host "INFO: Java at $javaPath is version $majorVersion, which is less than required $requiredVersion." -ForegroundColor Yellow
                return $false
            }
        } catch {
            return $false
        }
    }

    # 1. Check for a previously downloaded JRE
    if (Test-Path $localJavaDir) {
        $downloadedJavaPath = Get-ChildItem -Path $localJavaDir -Filter "java.exe" -Recurse | Select-Object -First 1
        if ($downloadedJavaPath -and (Test-JavaVersion $downloadedJavaPath.FullName)) {
            return
        } else {
            Write-Host "INFO: Previously downloaded JRE is not suitable. Removing it." -ForegroundColor Yellow
            Remove-Item -Path $localJavaDir -Recurse -Force
        }
    }

    # 2. Check system's default Java, unless skipped
    if (-not $SKIP_OS_JAVA) {
        $systemJava = Get-Command java -ErrorAction SilentlyContinue
        if ($systemJava -and (Test-JavaVersion $systemJava.Source)) {
            return
        } else {
            Write-Host "INFO: No suitable Java found in system PATH." -ForegroundColor Cyan
        }
    }

    # 3. Download a new JRE
    Write-Host "INFO: No suitable Java found. Downloading OpenJRE $requiredVersion..." -ForegroundColor Green
    $osName = "windows"
    $arch = ($env:PROCESSOR_ARCHITECTURE).ToLower()
    $osArch = ""
    switch ($arch) {
        "amd64" { $osArch = "x64" }
        "arm64" { $osArch = "aarch64" }
        default { throw "ERROR: Unsupported Windows architecture: $arch" }
    }

    $jreUrl = "https://api.adoptium.net/v3/binary/latest/$requiredVersion/ga/$osName/$osArch/jre/hotspot/normal/eclipse"
    $jreZipPath = Join-Path $SCRIPT_DIR "openjre.zip"

    Write-Host "INFO: Downloading from $jreUrl" -ForegroundColor Cyan
    Invoke-WebRequest -Uri $jreUrl -OutFile $jreZipPath -UseBasicParsing

    Write-Host "INFO: Unpacking Java..." -ForegroundColor Green
    $tempExtractDir = Join-Path $SCRIPT_DIR "jre_temp_extract"
    Expand-Archive -Path $jreZipPath -DestinationPath $tempExtractDir -Force
    # Move contents from the single sub-directory to the target jre directory
    $jreSubDir = Get-ChildItem -Path $tempExtractDir | Select-Object -First 1
    Move-Item -Path ($jreSubDir.FullName) -Destination $localJavaDir -Force
    Remove-Item $tempExtractDir -Recurse -Force
    Remove-Item $jreZipPath

    $script:JAVA_CMD = Get-ChildItem -Path $localJavaDir -Filter "java.exe" -Recurse | Select-Object -First 1 | ForEach-Object { $_.FullName }
    if (-not $script:JAVA_CMD) {
        throw "ERROR: Failed to find Java executable after download."
    }
    Write-Host "INFO: Java successfully downloaded and configured." -ForegroundColor Green
}

Find-OrDownloadJava $JAVA_MAJOR_VERSION


# --- 4. DOWNLOAD AND UNPACK JETTY ---
# Note: This requires tar.exe, which is included in modern Windows 10/11.
# -----------------------------------------------------------------------------------
if (-not (Test-Path $JETTY_HOME)) {
    Write-Host "INFO: Jetty not found. Downloading Jetty $JETTY_VERSION..." -ForegroundColor Green
    if (Test-Path "$SCRIPT_DIR/jetty-home*") { Remove-Item "$SCRIPT_DIR/jetty-home*" -Recurse -Force }
    $jettyTarPath = Join-Path $SCRIPT_DIR "jetty.tar.gz"
    Invoke-WebRequest -Uri $JETTY_DOWNLOAD_URL -OutFile $jettyTarPath -UseBasicParsing

    Write-Host "INFO: Unpacking Jetty..." -ForegroundColor Green
    tar -xzf $jettyTarPath -C $SCRIPT_DIR
    Remove-Item $jettyTarPath
    $extractedDir = Get-ChildItem -Path $SCRIPT_DIR -Directory -Filter "jetty-home-*" | Select-Object -First 1
    Rename-Item -Path $extractedDir.FullName -NewName "jetty-home"
    Write-Host "INFO: Jetty successfully installed at $JETTY_HOME" -ForegroundColor Green
} else {
    Write-Host "INFO: Jetty installation found at $JETTY_HOME." -ForegroundColor Cyan
}


# --- 5. DOWNLOAD JDBC DRIVERS ---
# -----------------------------------------------------------------------------------
function Download-Driver {
    param($downloadUrl)
    $driverJarName = [System.IO.Path]::GetFileName($downloadUrl)
    $targetDir = Join-Path $JETTY_HOME "lib/ext"
    $targetFile = Join-Path $targetDir $driverJarName

    New-Item -ItemType Directory -Path $targetDir -Force | Out-Null

    if (-not (Test-Path $targetFile)) {
        Write-Host "INFO: Downloading $driverJarName..." -ForegroundColor Green
        try {
            Invoke-WebRequest -Uri $downloadUrl -OutFile $targetFile -UseBasicParsing
            Write-Host "INFO: $driverJarName downloaded." -ForegroundColor Green
        } catch {
            Write-Host "ERROR: Failed to download $driverJarName." -ForegroundColor Red
            if (Test-Path $targetFile) { Remove-Item $targetFile }
            exit 1
        }
    } else {
        Write-Host "INFO: $driverJarName already present." -ForegroundColor Cyan
    }
}

Write-Host "INFO: Checking for JDBC drivers..." -ForegroundColor Green
Download-Driver "$MAVEN_URL/org/postgresql/postgresql/$POSTGRES_VERSION/postgresql-$POSTGRES_VERSION.jar"
Download-Driver "$MAVEN_URL/com/oracle/database/jdbc/ojdbc11/$ORACLE_VERSION/ojdbc11-$ORACLE_VERSION.jar"
Download-Driver "$MAVEN_URL/com/microsoft/sqlserver/mssql-jdbc/$MSSQL_VERSION/mssql-jdbc-$MSSQL_VERSION.jar"


# --- 6. DOWNLOAD AND UNPACK OPENL .WAR FILES ---
# -----------------------------------------------------------------------------------
function Download-War {
    param($componentName, $downloadUrl)
    $targetWar = Join-Path $WEBAPPS_DIR "$componentName.war"
    $targetDir = Join-Path $WEBAPPS_DIR $componentName

    # Check if the destination directory already exists
    if (-not (Test-Path -Path $targetDir -PathType Container)) {
        Write-Host "INFO: Downloading and unpacking '$componentName'..." -ForegroundColor Green
        try {
            # 1. Download the .war file
            Invoke-WebRequest -Uri $downloadUrl -OutFile $targetWar -UseBasicParsing

            # 2. Unpack the .war archive to the target directory
            Expand-Archive -Path $targetWar -DestinationPath $targetDir -Force

            # 3. Remove the temporary .war file after extraction
            Remove-Item -Path $targetWar -Force

            Write-Host "INFO: '$componentName' has been successfully downloaded and unpacked." -ForegroundColor Green
        } catch {
            Write-Host "ERROR: Failed to download or unpack '$componentName'." -ForegroundColor Red
            # Clean up any partial files if an error occurs
            if (Test-Path $targetWar) { Remove-Item -Path $targetWar -Force }
            if (Test-Path $targetDir) { Remove-Item -Path $targetDir -Recurse -Force }
            # Stop the script by re-throwing the exception
            throw
        }
    } else {
        Write-Host "INFO: '$componentName' already exists, skipping download." -ForegroundColor Cyan
    }
}

New-Item -ItemType Directory -Path $WEBAPPS_DIR -Force | Out-Null
Download-War "webstudio" "$MAVEN_URL/org/openl/rules/org.openl.rules.webstudio/$OPENL_VERSION/org.openl.rules.webstudio-$OPENL_VERSION.war"
Download-War "webservice" "$MAVEN_URL/org/openl/rules/org.openl.rules.ruleservice.ws/$OPENL_VERSION/org.openl.rules.ruleservice.ws-$OPENL_VERSION.war"

# Init Default repository
$OPENL_HOME = $env:OPENL_HOME
if (-not $OPENL_HOME) {
    Write-Host "INFO: OPENL_HOME environment variable has not been defined." -ForegroundColor Cyan
    $desktopOpenLHome = Join-Path $HOME "Desktop\OpenL_Home"
    if (Test-Path $desktopOpenLHome) {
        Write-Host "INFO: OpenL_Home folder has been found on the Desktop. Going to use it." -ForegroundColor Green
        $OPENL_HOME = $desktopOpenLHome
    } else {
        Write-Host "INFO: OpenL_Home folder has not been found on the Desktop. Going to use internal openl-demo folder." -ForegroundColor Green
        $OPENL_HOME = Join-Path $SCRIPT_DIR "openl-demo"
        if ((Test-Path $OPENL_HOME) -and (Get-ChildItem -Path $OPENL_HOME)) {
            Write-Host "INFO: Skip initialization. The openl-demo folder is not empty" -ForegroundColor Cyan
        } else {
            Write-Host "INFO: Initializing the openl-demo folder for demo purposes." -ForegroundColor Green
            New-Item -ItemType Directory -Path $OPENL_HOME -Force | Out-Null
            Set-Content -Path (Join-Path $OPENL_HOME "webstudio.properties") -Value ".version=LATEST`ndemo.init=true"
        }
    }
}

# --- 7. RUN JETTY SERVER ---
# -----------------------------------------------------------------------------------
Write-Host "============================================================" -ForegroundColor Magenta
Write-Host "Starting Jetty Server for OpenL Tablets..."
Write-Host "  Java Command: $JAVA_CMD"
Write-Host "    Jetty Home: $JETTY_HOME"
Write-Host "    OpenL Home: $OPENL_HOME"
Write-Host "   Webapps Dir: $WEBAPPS_DIR"
Write-Host "  Total Memory: $TotalRamMB MB"
Write-Host "   Memory Opts: $_JAVA_MEMORY"
Write-Host "============================================================" -ForegroundColor Magenta
Write-Host " Access OpenL Studio at: `e[94m http://localhost:8080/webstudio `e[0m"
Write-Host "Access Rule Services at: `e[94m http://localhost:8080/webservice `e[0m"
& $JAVA_CMD -version
Write-Host "Press Ctrl+C in this window to stop the server."

$Host.UI.RawUI.WindowTitle = "OpenL DEMO"
# Open the default browser
Start-Process "http://localhost:8080/"

# Execute the Java process
$javaArgs = @(
    "-Dopenl.home=$OPENL_HOME",
    "-Dorg.eclipse.jetty.server.Request.maxFormContentSize=-1",
    "-Dorg.eclipse.jetty.server.Request.maxFormKeys=-1",
    "-Djetty.httpConfig.requestHeaderSize=32768",
    "-Djetty.httpConfig.responseHeaderSize=32768",
    $_JAVA_MEMORY.Split(' '),
    "-Djetty.home=$JETTY_HOME",
    "-Djetty.base=$JETTY_HOME",
    "-Djetty.deploy.monitoredDir=$WEBAPPS_DIR",
    "-Djava.io.tmpdir=$env:TEMP",
    "-jar",
    (Join-Path $JETTY_HOME "start.jar"),
    "--module=http,ee10-jsp,ext,ee10-deploy,ee10-websocket-jakarta,ee10-cdi"
)

& $JAVA_CMD $javaArgs
