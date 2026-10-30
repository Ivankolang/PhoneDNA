# Phone DNA

Phone DNA is an Android automation MVP, initially targeting HONOR X6c Global / MagicOS 9.

## MVP features
- Compose dashboard for saved rules
- Rule creation using simple Russian/English time phrases (daily reminder at a specified time)
- Visual rule builder
- Enable/disable and delete rules
- Local rule storage
- WorkManager-based periodic rule evaluator
- Android notifications (notification permission requested on Android 13+)

## Build
GitHub Actions builds a debug APK on each push to `main` and uploads it as the `PhoneDNA-debug` artifact. Open **Actions → Android APK → latest successful run → Artifacts**.

## Current limitations
- Time rules are checked periodically by WorkManager, not at an exact second. Android battery restrictions, especially on some OEM builds, can delay execution.
- Natural-language parsing is local and intentionally limited to daily reminders with a valid time, e.g. `Каждый день в 21:00 напомни проверить заряд`.
- Arbitrary system settings are not changed silently. System-level actions may require user confirmation or be restricted by Android.
- This is an unsigned debug APK for testing, not a Play Store release.
