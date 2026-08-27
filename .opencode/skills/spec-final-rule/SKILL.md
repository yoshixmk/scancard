---
name: spec-final-rule
description: Use when editing .kiro/specs/** or verifying spec-code sync. Enforces that spec files keep only final decided rules with no history, drafts, or TODO remnants.
---

# Spec Final Rule Skill

## Purpose
`.kiro/specs/**` は意思決定後の**最終的なルールのみ**を保持する。

## Rules
1. **最終ルールのみ**: `requirements.md` / `design.md` / `tasks.md` に履歴・検討ログ・未確定案・TODO・コメントアウト代替案を残さない。
2. **コード同期**: コードを変更したら同PRで対応するspecを更新し、specを変更したら実装を追従させる。乖離を許さない。
3. **検証**: `./gradlew :app:compileDebugKotlin :app:testDebugUnitTest` が通る状態を維持する。
4. **DB永続化**: `fallbackToDestructiveMigration()` は本番で使用しない。マイグレーションは `AutoMigration` または手動 `Migration` で担保する。
5. **E2E**: 手動確認項目は `appium/specs/*.e2e.js` に自動化し、長時間テストは `@slow` + `wdio.slow.conf.js` に分離する。

## Checklist (before commit)
- [ ] spec に「過去の経緯・TODO・検討メモ」が残っていないか
- [ ] 変更したコードに対応するspec記述があるか
- [ ] `tasks.md` の該当タスクが `completed` に更新されているか
