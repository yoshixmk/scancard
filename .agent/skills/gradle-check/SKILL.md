---
name: gradle-check
description: Use when running Gradle builds or tests. Runs the bounded Invoke-GradleCheck.ps1 wrapper (visible progress, log, timeout) instead of raw ./gradlew. Prevents silent-looking hangs.
---

# Gradle Check Skill

## Purpose
Run Gradle builds/tests through a bounded wrapper so a normal multi-minute
Kotlin compilation is never mistaken for a hang again.

## Background (final rule only)
- Raw `./gradlew ... -q` hides all task progress; after an AGP bump the first
  build recompiles everything via the Kotlin daemon for minutes with zero output.
- `--offline` must not be used on the first build after an AGP bump.
- Stale `app/build` outputs must be dropped after branch switch/rebase (`-Clean`).

## Usage (run from repo root)
- Default check: `pwsh .agent/skills/gradle-check/scripts/Invoke-GradleCheck.ps1`
- After branch switch/rebase: add `-Clean`
- Offline only when an online build already passed: add `-Offline`
- Single task: `-Tasks ":app:compileDebugKotlin" -TimeoutMin 10`

## Rules
1. Never pass `-q`/`--quiet` (the wrapper refuses it).
2. Always keep the log under `.artifacts/gradle-check/`.
3. On timeout, kill only the Gradle client (`GradleWrapperMain`/`GradleMain`), never the daemon.
4. On failure, print the log tail and retry with `-Clean`, or `.\gradlew.bat --stop` for a stale daemon, then rerun online.

## Reference
- Script: `.agent/skills/gradle-check/scripts/Invoke-GradleCheck.ps1`
- Config: `org.gradle.console=plain` in `gradle.properties`
