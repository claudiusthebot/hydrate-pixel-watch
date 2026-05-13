# Hydrate — water intake tracker for Pixel Watch + phone

A small Android app pair that logs water intake, syncs it between your watch
and phone, and writes every entry to Health Connect so any other health app
sees the same data.

- **Phone app** (`:phone`) — Material 3 Expressive UI: daily progress ring,
  quick-add grid, slider-based custom-amount sheet (with an animated glass
  illustration that fills as you drag), 7-day history, goal slider,
  configurable reminders, onboarding, and a diagnostics screen for Health
  Connect / Wear Data Layer.
- **Watch app** (`:wear`) — Wear Compose Material 3: tappable quick-add
  buttons, custom-amount dialog, progress hero, most-recent entries, goal
  adjuster, a hit-the-goal celebrate overlay, plus a **Tile** for the Pixel
  Watch carousel and an **ongoing-activity** chip on the watch face.
- **Shared sync** — Wearable Data Layer messages for live updates + Health
  Connect as the source of truth across devices.

## Target hardware

- **Phone**: Android 8+ (min SDK 26), Pixel family tested
- **Watch**: Wear OS 5+ (min SDK 34), Pixel Watch 3 / Wear OS 5 / Wear OS 6
  target
- Requires **Health Connect** installed on both (built-in on Wear OS 5+ and
  Android 14+).

## Build

Requirements: JDK 17+, Android SDK platform 36 + build-tools 36.0.0,
Gradle 8.11.1 (via wrapper). AGP 8.9.2, Kotlin 2.1.0, Compose Compiler plugin
2.1.0.

```bash
./gradlew :phone:assembleDebug      # phone/build/outputs/apk/debug/phone-debug.apk
./gradlew :wear:assembleDebug       # wear/build/outputs/apk/debug/wear-debug.apk
./gradlew assembleDebug             # both
```

## Install

```bash
# Phone
adb install -r phone/build/outputs/apk/debug/phone-debug.apk

# Watch (paired via Wireless Debugging)
adb connect <watch-ip>:<adb-port>
adb -s <watch-ip>:<adb-port> install -r wear/build/outputs/apk/debug/wear-debug.apk
```

On first launch each app asks for Health Connect read + write permissions for
`HydrationRecord`, and (Android 13+) for `POST_NOTIFICATIONS` so reminders and
the progress chip can render. Tap allow.

## How the sync works

Health Connect is the **source of truth**. Both devices write intake events to
their local Health Connect database. The Wearable Data Layer carries two kinds
of messages so the far side stays in sync without waiting for Health Connect's
cross-device replication:

- `/water/intake/add` — sent when one side logs a new drink. The receiver
  mirrors the write into its own Health Connect.
- `/water/total/update` — sent on changes (add / delete / goal change). The
  receiver uses this to refresh the hero UI immediately even before its HC
  round-trip completes.

Listeners are registered via `WearableListenerService` on each side. No
permanent foreground service needed — the system delivers messages when they
arrive.

## Reminders

The phone app schedules a periodic `WorkManager` job (`ReminderWorker`) that
fires a notification on the configured interval. The notification carries a
**Log Nml** action that records a drink directly from the shade — no need to
open the app.

Configurable from `Settings → Reminders`:

- **Enable / disable** the periodic nudge.
- **Interval** — every 30 min up to every 4 h, in 30 min steps (WorkManager's
  15 min minimum is enforced).
- **Quiet hours** — wraparound-aware (e.g. 22:00 → 07:00). Reminders are
  silently dropped during the window.
- **Quick-log amount** — the volume the notification action records (default
  250 ml).

Three notification channels are created on first run so the user can
customise them independently in system settings:

- `Reminders` (default importance) — the nudge.
- `Ongoing progress` (low) — a sticky progress card when the app is active.
- `Goal reached` (high) — the one-shot celebration when the daily total
  crosses 100 %.

## Modules

```
shared/
  Model.kt — WaterEntry, DaySummary, UserSettings, SyncConstants. Plain data
             shared by both platforms.
phone/
  ui/          — Compose UI: Today / History / Settings / About / Diagnostics
                 screens, custom-amount sheet, animated tab pill, water-fill
                 hero, quick-drink grid, onboarding.
  health/      — HealthConnectManager (read / write / delete HydrationRecord).
  sync/        — WearSync + IntakeWearListener (phone ↔ watch).
  data/        — WaterRepository (DataStore + HC glue).
  notif/       — HydrateNotifications, ReminderPrefs, ReminderWorker,
                 LogActionReceiver. WorkManager-driven reminders + the
                 quick-log broadcast handler.
  WaterViewModel, MainActivity, WaterApp.
wear/
  ui/          — Compose UI with ScalingLazyColumn, hero tile, drink chips,
                 goal adjuster, custom-amount dialog, celebrate overlay,
                 haptics helpers.
  health/      — WearHealthConnect (same HC API on Wear OS 5+).
  sync/        — PhoneSync + IntakeWearListener (watch ↔ phone).
  tile/        — HydrationTileService — Material3 protolayout Tile for the
                 Pixel Watch carousel.
  ongoing/     — OngoingHydration — Wear Ongoing-Activity status chip on the
                 watch face while the goal is in progress.
  WaterStore, MainActivity, WearWaterApp.
```

## Design notes

- **Material 3 Expressive** on the phone — chunkier rounding (via
  `ExpressiveShapes`), bolder display weights (via `ExpressiveTypography`), and
  dynamic color (`dynamicLightColorScheme` / `dynamicDarkColorScheme`) pulling
  from the wallpaper on Android 12+.
- **Wear Compose Material 3** on the watch — `ScalingLazyColumn` so list items
  scale at the edges of the round display, with tonal buttons sized for a
  fingertip on a 1.2" panel.
- **Material 3 protolayout** for the Tile — same design language as the app,
  rendered server-side by Wear OS.
- **Haptics everywhere they help** — slider ticks, segment crosses, confirms.
  Tuned via `SliderHaptics` (phone) and `WearHaptics` (watch).
- Entries are millilitres throughout — the UI only formats at the edge.

## Known limits / future

- Weekly bar chart sums from HC rather than caching per-day — fine at the
  7-day window, would need a daily-summary table to chunk for monthly views.
- No phone homescreen widget. The Wear tile is the equivalent affordance on
  the watch; a Glance widget on the phone would mirror it.
- Watch app reuses the `com.google.android.apps.healthdata` query — if the
  watch build of Health Connect is older than expected the app falls back to
  a "not available" screen with a Play Store deep link.

## License

MIT.
