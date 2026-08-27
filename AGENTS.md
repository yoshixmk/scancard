# AGENTS — ScanCard Project Rules

## Spec Rule (Mandatory)
- `.kiro/specs/**` (`requirements.md`, `design.md`, `tasks.md`) は**最終的なルールのみ**を保持する。
- 履歴、検討案、TODO、過去の経緯、コメントアウトされた代替案は含めない。決定した仕様だけを記述する。
- コードと spec は常に同期する。コード変更時は対応する spec を同PRで更新し、spec 変更時は実装を追従させる。
- E2E (`appium/specs/*.e2e.js`) は手動確認の代替として自動化し、`@slow` は `wdio.slow.conf.js` 経由でのみ実行する。

## Build / Test
- AGP 9.3.1 / Kotlin 2.x / Gradle 9.x / JDK17+（現行: JDK25）で `./gradlew :app:compileDebugKotlin :app:testDebugUnitTest` が通ることを担保する。
- `fallbackToDestructiveMigration()` は使用しない（本番DBの永続化担保）。

## Reference
- 詳細は `.opencode/skills/spec-final-rule/SKILL.md` を参照。
