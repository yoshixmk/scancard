# setup.ps1 - Appium E2E setup for Windows (mise + Node 20)
# Usage: .\setup.ps1 -Mise {MISE_BIN} -JavaHome {JAVA_HOME} -Avd {AVD_NAME} -AndroidSdk {ANDROID_SDK}
param(
  [string]$Mise = $env:MISE_BIN,          # e.g. via `where mise` or WinGet path; fallback to `mise`
  [string]$JavaHome = $env:JAVA_HOME,     # e.g. JDK 17+ path, example: JDK 25
  [string]$Avd = $env:AVD_NAME,           # e.g. Medium_Phone_API_36.0
  [string]$AndroidSdk = $env:ANDROID_HOME # e.g. %LOCALAPPDATA%\Android\Sdk
)
if (-not $Mise) { $Mise = "mise" }
if (-not $JavaHome) { $JavaHome = $env:JAVA_HOME }
if (-not $Avd) { $Avd = "Medium_Phone_API_36.0" }
if (-not $AndroidSdk) { $AndroidSdk = "$env:LOCALAPPDATA\Android\Sdk" }

$env:JAVA_HOME = $JavaHome
$env:ANDROID_HOME = $AndroidSdk
$env:ANDROID_SDK_ROOT = $AndroidSdk

Write-Host "== mise node@20 =="
& $Mise install node@20
& $Mise exec node@20 -- node --version

Write-Host "== Build APK =="
./gradlew :app:assembleDebug
if ($LASTEXITCODE -ne 0) { throw "assembleDebug failed" }

Write-Host "== Start emulator $Avd =="
$emu = "$env:ANDROID_HOME\emulator\emulator.exe"
$adb = "$env:ANDROID_HOME\platform-tools\adb.exe"
& $adb kill-server; & $adb start-server
Start-Process -FilePath $emu -ArgumentList "-avd",$Avd,"-no-snapshot-save","-gpu","swiftshader_indirect" -WindowStyle Minimized
Write-Host "Waiting for boot..."
for ($i=0; $i -lt 40; $i++) {
  $boot = & $adb shell getprop sys.boot_completed 2>$null
  if ($boot.Trim() -eq "1") { Write-Host "Booted"; break }
  Start-Sleep 5
}
& $adb devices

Write-Host "== Appium deps =="
Set-Location appium
& $Mise exec node@20 -- npm install
& $Mise exec node@20 -- npx appium driver install uiautomator2
& $Mise exec node@20 -- npx appium driver list --installed

Write-Host "Done. Run: mise exec node@20 -- npx wdio run wdio.conf.js --spec specs/home.e2e.js"
