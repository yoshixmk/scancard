This pack holds part 1/2 of the split Gemma 4 E2B model (Play 1.5GB/pack limit).
Otherwise Play serves an empty pack and the app fails with PACK_UNAVAILABLE(-2).

Required files (do NOT commit large binaries):
  gemma-4-E2B-it.litertlm.part0  (first ~1.3GB half)

Part 2/2 goes to gemma-ai-pack-2/src/main/assets/gemma-4-E2B-it.litertlm.part1.

Split before bundleRelease (PowerShell):
  .\scripts\Split-GemmaModel.ps1 -Source gemma-ai-pack\src\main\assets\gemma-4-E2B-it.litertlm
Then build:
  ./gradlew :app:bundleRelease
