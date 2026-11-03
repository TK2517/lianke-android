param(
    [string]$SdkPath = $(if ($env:ANDROID_SDK_ROOT) { $env:ANDROID_SDK_ROOT } elseif ($env:ANDROID_HOME) { $env:ANDROID_HOME } else { Join-Path $env:LOCALAPPDATA 'Android\Sdk' })
)
$ErrorActionPreference = 'Stop'
$root = Split-Path -Parent $MyInvocation.MyCommand.Path
$tools = Join-Path $SdkPath 'build-tools\35.0.0'
$platform = Join-Path $SdkPath 'platforms\android-35\android.jar'
foreach ($file in @($platform, (Join-Path $tools 'aapt2.exe'), (Join-Path $tools 'd8.bat'), (Join-Path $tools 'zipalign.exe'), (Join-Path $tools 'apksigner.bat'))) {
    if (-not (Test-Path -LiteralPath $file)) { throw "Android SDK component missing: $file" }
}
$build = Join-Path $root 'build'
$dist = Join-Path $root 'dist'
New-Item -ItemType Directory -Force -Path $build,$dist,(Join-Path $build 'classes'),(Join-Path $build 'dex') | Out-Null
$aapt = Join-Path $tools 'aapt2.exe'
$unsigned = Join-Path $build 'unsigned.apk'
$compiled = Join-Path $build 'resources.zip'
& $aapt compile --dir (Join-Path $root 'app\src\main\res') -o $compiled
if ($LASTEXITCODE -ne 0) { throw 'aapt2 compile failed' }
& $aapt link -o $unsigned -I $platform --manifest (Join-Path $root 'app\src\main\AndroidManifest.xml') --min-sdk-version 26 --target-sdk-version 34 -A (Join-Path $root 'app\src\main\assets') $compiled
if ($LASTEXITCODE -ne 0) { throw 'aapt2 link failed' }
$sources = @(Get-ChildItem -LiteralPath (Join-Path $root 'app\src\main\java') -Recurse -Filter '*.java' | ForEach-Object FullName)
& javac --release 8 -encoding UTF-8 -cp $platform -d (Join-Path $build 'classes') $sources
if ($LASTEXITCODE -ne 0) { throw 'javac failed' }
$classesJar = Join-Path $build 'classes.jar'
& jar cf $classesJar -C (Join-Path $build 'classes') .
if ($LASTEXITCODE -ne 0) { throw 'jar failed' }
& (Join-Path $tools 'd8.bat') --min-api 26 --lib $platform --output (Join-Path $build 'dex') $classesJar
if ($LASTEXITCODE -ne 0) { throw 'd8 failed' }
& jar uf $unsigned -C (Join-Path $build 'dex') classes.dex
if ($LASTEXITCODE -ne 0) { throw 'APK dex insertion failed' }
$aligned = Join-Path $build 'aligned.apk'
& (Join-Path $tools 'zipalign.exe') -f 4 $unsigned $aligned
if ($LASTEXITCODE -ne 0) { throw 'zipalign failed' }
$keyStore = Join-Path $env:USERPROFILE '.android\debug.keystore'
if (-not (Test-Path -LiteralPath $keyStore)) {
    New-Item -ItemType Directory -Force -Path (Split-Path -Parent $keyStore) | Out-Null
    & keytool -genkeypair -v -keystore $keyStore -storepass android -alias androiddebugkey -keypass android -keyalg RSA -keysize 2048 -validity 10000 -dname 'CN=Android Debug,O=Android,C=US'
    if ($LASTEXITCODE -ne 0) { throw 'debug keystore creation failed' }
}
$apk = Join-Path $dist 'fitlog-debug.apk'
& (Join-Path $tools 'apksigner.bat') sign --ks $keyStore --ks-pass pass:android --key-pass pass:android --out $apk $aligned
if ($LASTEXITCODE -ne 0) { throw 'APK signing failed' }
& (Join-Path $tools 'apksigner.bat') verify --verbose $apk
if ($LASTEXITCODE -ne 0) { throw 'APK signature verification failed' }
Write-Host "APK: $apk"
