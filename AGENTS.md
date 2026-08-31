# AGENTS — ScanCard Project Rules

## Spec Rule (Mandatory)
- `.agent/specs/**` (`requirements.md`, `design.md`, `tasks.md`) retains **only final decided rules**.
- Do not include history, proposals, TODOs, past context, or commented-out alternatives. Describe only the decided specification.
- Keep code and spec in sync. When code changes, update the corresponding spec in the same PR; when spec changes, follow up with implementation.
- Automate E2E (`appium/specs/*.e2e.js`) as a replacement for manual verification and run `@slow` only via `wdio.slow.conf.js`.

## Build / Test
- Ensure `./gradlew :app:compileDebugKotlin :app:testDebugUnitTest` passes on AGP 9.3.1 / Kotlin 2.x / Gradle 9.x / JDK17+ (current: JDK25).
- Do not use `fallbackToDestructiveMigration()` (guarantee production DB persistence).

## Reference
- See `.agent/skills/spec-final-rule/SKILL.md` for details.
