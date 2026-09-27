#Requires -Version 5.1
[CmdletBinding()]
param(
    [string]$ClientPath,
    [string]$ToolchainPath,
    [switch]$CheckOnly,
    [switch]$NetworkSpike
)
$ErrorActionPreference = 'Stop'
if (-not $ClientPath) { $ClientPath = Join-Path $PSScriptRoot '../i-want-you-to-build-a/work/client' }
if (-not $ToolchainPath) { $ToolchainPath = Join-Path $PSScriptRoot '../i-want-you-to-build-a/work/toolchain' }
$client = (Resolve-Path -LiteralPath $ClientPath).Path
$toolchain = (Resolve-Path -LiteralPath $ToolchainPath).Path
$jdk = Join-Path $toolchain 'jdk/jdk-17.0.20.1+1'
$sdk = Join-Path $toolchain 'sdk'
$cache = Join-Path $toolchain 'gradle-cache'
foreach ($path in @(
    (Join-Path $jdk 'bin/java.exe'),
    (Join-Path $sdk 'platforms/android-37.0/android.jar'),
    (Join-Path $sdk 'ndk/29.0.14206865/source.properties'),
    (Join-Path $client 'gradlew.bat'),
    (Join-Path $client 'app/src/main/java/com/limelight/binding/input/StreamInputConnection.java')
)) {
    if (-not (Test-Path -LiteralPath $path)) { throw "Required build input missing: $path" }
}
$properties = Join-Path $client 'local.properties'
if (-not (Test-Path -LiteralPath $properties)) {
    throw 'Create client/local.properties with sdk.dir pointing to the selected SDK.'
}
$sdkLines = @(Get-Content -LiteralPath $properties | Where-Object { $_ -match '^\s*sdk\.dir\s*=' })
if ($sdkLines.Count -ne 1) { throw 'Expected exactly one sdk.dir in local.properties.' }
$configuredSdk = ($sdkLines[0] -replace '^\s*sdk\.dir\s*=\s*','').Trim().Replace('\:', ':').Replace('\\','\')
if (-not (Test-Path -LiteralPath $configuredSdk)) { throw 'The SDK path in local.properties does not exist; update the relocated path.' }
if ((Resolve-Path -LiteralPath $configuredSdk).Path.TrimEnd('\','/') -ne (Resolve-Path -LiteralPath $sdk).Path.TrimEnd('\','/')) {
    throw 'local.properties SDK differs from the selected toolchain. Update it explicitly before building.'
}
if ($CheckOnly) {
    Write-Output 'PASS: custom client and pinned Android toolchain inputs are available.'
    return
}
if ($NetworkSpike) {
    $aar = Join-Path $client 'app/libs/libtailscale.aar'
    if (!(Test-Path $aar) -or (Get-FileHash $aar -Algorithm SHA256).Hash -ne 'a2958859073a3a9dc938141fc71300281222d446db9ce311623e33efad738c52') {
        throw 'Network library missing or different from the verified pinned build.'
    }
}
$savedEnvironment = @{}
foreach ($name in @('JAVA_HOME','ANDROID_HOME','ANDROID_SDK_ROOT','GRADLE_USER_HOME')) {
    $savedEnvironment[$name] = [Environment]::GetEnvironmentVariable($name, 'Process')
}
try {
    $env:JAVA_HOME = $jdk
    $env:ANDROID_HOME = $sdk
    $env:ANDROID_SDK_ROOT = $sdk
    $env:GRADLE_USER_HOME = $cache
    Push-Location -LiteralPath $client
    try {
        $tasks = @(':app:testNonRootDebugUnitTest', ':app:lintNonRootRelease', ':app:assembleNonRootRelease')
        if ($NetworkSpike) { $tasks += @(':app:testNetworkDebugUnitTest', ':app:lintNetworkDebug', ':app:assembleNetworkDebug') }
        & './gradlew.bat' @tasks '--no-daemon'
        if ($LASTEXITCODE -ne 0) { throw "Android validation failed with exit code $LASTEXITCODE." }
    } finally { Pop-Location }
} finally {
    foreach ($name in $savedEnvironment.Keys) {
        [Environment]::SetEnvironmentVariable($name, $savedEnvironment[$name], 'Process')
    }
}
Write-Output 'IME tests, lint and unsigned release assembly completed. No APK installed.'
if ($NetworkSpike) { Write-Output 'Network tests, lint and debug APK assembly completed (Gradle debug signing only).' }
