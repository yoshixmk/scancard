param(
    [string]$Tasks = ":app:compileDebugKotlin :app:testDebugUnitTest",
    [int]$TimeoutMin = 20,
    [switch]$Offline,
    [switch]$Clean,
    [string]$LogDir = ".artifacts/gradle-check"
)
# Bounded Gradle check: visible progress, log file, timeout, stale-output cleanup.
# Must run from the repo root. See .agent/skills/gradle-check/SKILL.md.
# This wrapper forbids -q, forces --console=plain, tees to a log, prints a
# heartbeat, and kills only the client (never the daemon) on timeout.
# Usage:
#   pwsh .agent/skills/gradle-check/scripts/Invoke-GradleCheck.ps1
#   pwsh .agent/skills/gradle-check/scripts/Invoke-GradleCheck.ps1 -Clean        # after branch switch/rebase
#   pwsh .agent/skills/gradle-check/scripts/Invoke-GradleCheck.ps1 -Offline      # only when online build already passed
#   pwsh .agent/skills/gradle-check/scripts/Invoke-GradleCheck.ps1 -Tasks ":app:compileDebugKotlin" -TimeoutMin 10
$ErrorActionPreference = "Stop"

if ($Tasks -match '(^|\s)-q(\s|$)') {
    Write-Error "Refusing -q/--quiet: it hides task progress and makes normal multi-minute Kotlin compilation look hung. Remove -q and rerun."
    exit 1
}

$stamp = Get-Date -Format "yyyyMMdd-HHmmss"
New-Item -ItemType Directory -Path $LogDir -Force | Out-Null
$logFile = Join-Path $LogDir "gradle-check-$stamp.log"

Write-Output "=== Preflight ==="
Write-Output ("Branch: " + (git branch --show-current))
git status --short | Select-Object -First 20 | ForEach-Object { Write-Output ("  status: " + $_) }
Write-Output ("Log: " + $logFile)
& .\gradlew.bat --status 2>&1 | Select-Object -First 15 | ForEach-Object { Write-Output ("  daemon: " + $_) }

$taskArgs = "$Tasks --console=plain"
if ($Offline) { $taskArgs += " --offline" }
if ($Clean) {
    Write-Output "=== Clean (stale outputs after rebase) ==="
    & .\gradlew.bat ":app:clean" --console=plain 2>&1 | Tee-Object -FilePath $logFile -Append | Select-Object -Last 5
}

Write-Output "=== Run: .\gradlew.bat $taskArgs (timeout ${TimeoutMin}min) ==="
$psi = New-Object System.Diagnostics.ProcessStartInfo
$psi.FileName = "$PWD\gradlew.bat"
$psi.Arguments = $taskArgs
$psi.RedirectStandardOutput = $true
$psi.RedirectStandardError = $true
$psi.UseShellExecute = $false
$psi.CreateNoWindow = $true
$proc = New-Object System.Diagnostics.Process
$proc.StartInfo = $psi
[void]$proc.Start()

$deadline = (Get-Date).AddMinutes($TimeoutMin)
$lastBeat = Get-Date
$logStream = [IO.File]::AppendText($logFile)
try {
    while (-not $proc.HasExited) {
        while (-not $proc.StandardOutput.EndOfStream) {
            $line = $proc.StandardOutput.ReadLine()
            $logStream.WriteLine($line)
            if ($line -match '^(> Task|BUILD|FAILED|PASSED|.*tests? (completed|failed))') { Write-Output ("  " + $line) }
        }
        while (-not $proc.StandardError.EndOfStream) {
            $line = $proc.StandardError.ReadLine()
            $logStream.WriteLine($line)
        }
        if ((Get-Date) -gt $deadline) {
            Write-Output "TIMEOUT after ${TimeoutMin}min. Killing Gradle client only (daemon is kept)."
            Get-CimInstance Win32_Process -Filter "Name='java.exe'" | Where-Object {
                $_.CommandLine -match 'GradleWrapperMain|GradleMain'
            } | ForEach-Object {
                Write-Output ("  killing client pid " + $_.ProcessId)
                Stop-Process -Id $_.ProcessId -Force -ErrorAction SilentlyContinue
            }
            $proc.WaitForExit(15000) | Out-Null
            if (-not $proc.HasExited) { $proc.Kill() }
            break
        }
        if (((Get-Date) - $lastBeat).TotalSeconds -ge 30) {
            $elapsed = [int]((Get-Date) - ($deadline.AddMinutes(-$TimeoutMin))).TotalSeconds
            $tail = Get-Content $logFile -Tail 1 -ErrorAction SilentlyContinue | Select-Object -Last 1
            Write-Output ("  ... ${elapsed}s elapsed, still running. last log: " + $tail)
            $lastBeat = Get-Date
        }
        Start-Sleep -Seconds 2
    }
    while (-not $proc.StandardOutput.EndOfStream) { $logStream.WriteLine($proc.StandardOutput.ReadLine()) }
    while (-not $proc.StandardError.EndOfStream) { $logStream.WriteLine($proc.StandardError.ReadLine()) }
} finally {
    $logStream.Close()
}

Write-Output "=== Exit: $($proc.ExitCode) ==="
if ($proc.ExitCode -eq 0 -and $Tasks -match 'testDebugUnitTest') {
    $xmls = Get-ChildItem app/build/test-results/testDebugUnitTest/*.xml -ErrorAction SilentlyContinue
    $t = 0; $f = 0; $e = 0; $s = 0
    foreach ($x in $xmls) {
        $m = Select-String -Path $x.FullName -Pattern '<testsuite[^>]*tests="(\d+)"[^>]*skipped="(\d+)"[^>]*failures="(\d+)"[^>]*errors="(\d+)"' | Select-Object -First 1
        if ($m -and $m.Matches[0].Groups.Count -ge 5) {
            $t += [int]$m.Matches[0].Groups[1].Value
            $s += [int]$m.Matches[0].Groups[2].Value
            $f += [int]$m.Matches[0].Groups[3].Value
            $e += [int]$m.Matches[0].Groups[4].Value
        }
    }
    Write-Output ("Tests: total=$t skipped=$s failures=$f errors=$e (log: $logFile)")
    if ($f -gt 0 -or $e -gt 0) { exit 1 }
}
if ($proc.ExitCode -ne 0) {
    Write-Output "--- log tail ---"
    Get-Content $logFile -Tail 30 | ForEach-Object { Write-Output ("  " + $_) }
    Write-Output "Recovery: rerun with -Clean (stale outputs), or .\\gradlew.bat --stop (stale daemon), then rerun online (no -Offline on first build after AGP bump)."
    exit 1
}
