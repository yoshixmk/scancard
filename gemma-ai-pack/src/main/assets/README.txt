This directory must contain the release model files before building the .aab,
otherwise Play serves an empty pack and the app fails with PACK_UNAVAILABLE(-2).

Required files (do NOT commit large binaries):
  gemma-4-E2B-it.litertlm
  gemma-4-E4B-it.litertlm
  gemma-2-2b-it-cpu-int4.bin

Place the files here, then build:
  ./gradlew :app:bundleRelease
