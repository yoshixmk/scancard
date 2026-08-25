# Foreground Service (targetSDK 35)

`CardExtractionWorker` calls `setForeground(getForegroundInfo())` to prevent `SystemJobService onStopJob` after 10s.

- **Manifest**: merge `androidx.work.impl.foreground.SystemForegroundService` with `android:foregroundServiceType="shortService"` (`tools:node="merge"`). Without it, `FOREGROUND_SERVICE_TYPE_SHORT_SERVICE (0x800)` crashes: `not a subset of 0x0`.
- **Code**: `getForegroundInfo()` returns `ForegroundInfo(id, notification, SHORT_SERVICE)` on `UPSIDE_DOWN_CAKE+`, plain `ForegroundInfo` otherwise.
- **Type none**: Starting FGS with type `none` is prohibited on targetSDK 35 → `InvalidForegroundServiceTypeException`; always declare `shortService`.
- **Permission**: Grant `POST_NOTIFICATIONS` via `pm grant` in `clearStateAndLaunch()`; otherwise foreground notification fails.
