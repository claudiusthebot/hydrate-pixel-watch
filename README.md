# Hydrate — water intake tracker for Pixel Watch + phone

A small Android app pair that logs water intake, syncs it between your watch
and phone, and writes every entry to Health Connect so any other health app
sees the same data.

- **Phone app** (`:phone`) — Material 3 Expressive, daily progress ring,
  quick-adds, 7-day history, goal slider.
- **Watch app** (`:wear`) — Wear Compose Material 3, tappable quick-add
  buttons, progress hero, most-recent entries.
- **Shared sync** — Wearable Data Layer messages for live updates + Health
  Connect as the source of truth across devices.

## Target hardware

- **Phone**: Android 8+ (min SDK 26), Pixel family tested
- **Watch**: Wear OS 4+ (min SDK 30), Pixel Watch 3 / Wear OS 5 target
- Requires **Health Connect** installed on both (comes built-in on Wear OS 5
  and Android 14+).

## Build

Requirements: JDK 17+, Android SDK platform 35 + build-tools 35.0.0,
Gradle 8.9 (via wrapper).

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
`HydrationRecord`. Tap allow.

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

## Modules

```
shared/
  WaterEntry, DaySummary, UserSettings, SyncConstants — plain data shared by
  both platforms.
phone/
  ui/          — Compose UI: Today / History / Settings screens
  health/      — HealthConnectManager (read / write / delete HydrationRecord)
  sync/        — WearSync + IntakeWearListener (phone ↔ watch)
  data/        — WaterRepository (DataStore + HC glue)
  WaterViewModel, MainActivity
wear/
  ui/          — Compose UI with ScalingLazyColumn
  health/      — WearHealthConnect (same HC API on Wear OS 5)
  sync/        — PhoneSync + IntakeWearListener (watch ↔ phone)
  WaterStore, MainActivity
```

## Design notes

- **Material 3 Expressive** on the phone — chunkier rounding (via
  `ExpressiveShapes`), bolder display weights (via `ExpressiveTypography`), and
  dynamic color (`dynamicLightColorScheme` / `dynamicDarkColorScheme`) pulling
  from the wallpaper on Android 12+.
- **Wear Compose Material 3** on the watch — uses `ScalingLazyColumn` so the
  list items scale at the edges of the round display, with tonal buttons sized
  for a fingertip on a 1.2" panel.
- Entries are millilitres throughout — the UI only formats at the edge.

## Known limits / future

- The custom-amount sheet on the phone is a simple numeric input. A slider
  picker would feel more "expressive".
- Weekly bar chart sums from HC rather than caching per-day — fine at the
  7-day window, would need to chunk for monthly views.
- No water reminders / nudges (easy to add via WorkManager + a notification).
- Watch app reuses the `com.google.android.apps.healthdata` query — if the
  watch build of Health Connect is older than expected the app falls back to
  a "not available" screen.

## License

MIT.
