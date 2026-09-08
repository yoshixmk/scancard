---
name: spec-final-rule
description: Use when editing .agent/specs/** or verifying spec-code sync. Enforces that spec files keep only final decided rules with no history, drafts, or TODO remnants.
---

# Spec Final Rule Skill

## Purpose
`.agent/specs/**` retains only **final decided rules** after decisions are made.

## Rules
1. **Final rules only**: Do not leave history, discussion logs, undecided proposals, TODOs, or commented-out alternatives in `requirements.md` / `design.md` / `tasks.md`.
2. **Code sync**: When code is changed, update the corresponding spec in the same PR; when the spec is changed, follow up with implementation. No drift is allowed.
3. **Verification**: Keep `./gradlew :app:compileDebugKotlin :app:testDebugUnitTest` passing via the `gradle-check` skill (`.agent/skills/gradle-check/SKILL.md`).
4. **DB persistence**: Do not use `fallbackToDestructiveMigration()` in production. Guarantee migrations via `AutoMigration` or manual `Migration`.
5. **E2E**: Automate manual verification items in `appium/specs/*.e2e.js` and isolate long-running tests with `@slow` + `wdio.slow.conf.js`.

## Checklist (before commit)
- [ ] No history, TODOs, or discussion notes left in spec?
- [ ] Corresponding spec updated for changed code?
- [ ] Relevant task in `tasks.md` marked `completed`?
- [ ] Commit messages in English?
