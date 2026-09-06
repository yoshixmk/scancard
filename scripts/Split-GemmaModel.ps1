param(
    [string]$Source = "gemma-ai-pack\src\main\assets\gemma-4-E2B-it.litertlm",
    [long]$PartSize = 1294MB
)
# Splits Gemma 4 E2B (2.6GB) into 2 parts to stay under Play 1.5GB/pack limit.
# Output:
#   gemma-ai-pack\src\main\assets\gemma-4-E2B-it.litertlm.part0
#   gemma-ai-pack-2\src\main\assets\gemma-4-E2B-it.litertlm.part1
# Reassemble check (optional):
#   copy /b part0+part1 full.litertlm && fc /b full.litertlm $Source
$ErrorActionPreference = "Stop"
$pack1Dir = "gemma-ai-pack\src\main\assets"
$pack2Dir = "gemma-ai-pack-2\src\main\assets"
New-Item -ItemType Directory -Path $pack1Dir, $pack2Dir -Force | Out-Null

if (-not (Test-Path -LiteralPath $Source)) {
    Write-Error "Source not found: $Source. Place the full model first."
    exit 1
}
$part0 = Join-Path $pack1Dir "gemma-4-E2B-it.litertlm.part0"
$part1 = Join-Path $pack2Dir "gemma-4-E2B-it.litertlm.part1"

$src = [IO.File]::OpenRead($Source)
try {
    $buf = New-Object byte[] (8MB)
    # part0
    $o0 = [IO.File]::Create($part0)
    try {
        $remaining = $PartSize
        while ($remaining -gt 0) {
            $want = [Math]::Min($buf.Length, $remaining)
            $read = $src.Read($buf, 0, $want)
            if ($read -le 0) { break }
            $o0.Write($buf, 0, $read)
            $remaining -= $read
        }
    } finally { $o0.Close() }
    # part1 (rest)
    $o1 = [IO.File]::Create($part1)
    try {
        while (($read = $src.Read($buf, 0, $buf.Length)) -gt 0) {
            $o1.Write($buf, 0, $read)
        }
    } finally { $o1.Close() }
} finally { $src.Close() }

$p0 = (Get-Item $part0).Length
$p1 = (Get-Item $part1).Length
Write-Output ("part0: {0:N0} bytes ({1:N1} MB)" -f $p0, ($p0/1MB))
Write-Output ("part1: {0:N0} bytes ({1:N1} MB)" -f $p1, ($p1/1MB))
$limit = 1500MB
if ($p0 -ge $limit -or $p1 -ge $limit) {
    Write-Warning "A part still exceeds Play 1.5GB compressed limit. Reduce -PartSize."
} else {
    Write-Output "OK: both parts under 1.5GB."
}
Write-Output "Next: delete the full $Source (keep only .part0/.part1), then run ./gradlew :app:bundleRelease"
