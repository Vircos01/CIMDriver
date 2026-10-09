# Diagnostics

Diagnostics are an explicit part of CIMDriver, introduced to guarantee the stability and predictability of automatic trip registration in a complex (Android-restricted) environment. A dedicated `DiagnosticsScreen` has been added to make operational status and error information directly transparent to developers and (pro) users.

## Purpose of diagnostics

In an application with foreground tracking, boot recovery, Bluetooth events, and car integration, diagnostic capabilities are essential. Because the Android system sometimes unexpectedly shuts down services to optimize battery usage (Doze mode, process limits), it is necessary to make it transparent *why* a trip was aborted or did not start. The `DiagnosticsScreen` confirms that this app is designed with attention to manageability and issue follow-up.

## Expected content and the UI

The `DiagnosticsScreen` is designed to provide an at-a-glance overview of the overall "health" of the system and background processes. Whereas previously it was exclusively tied to the `TrackingStatusStore`, it now acts as a dashboard that converges in the `DiagnosticsViewModel`.

Via the `DiagnosticsViewModel`, four different categories are read and displayed in separate overview cards:

### 1. App & System Info
- **App Version:** The exact active build version (`BuildConfig.VERSION_NAME`).
- **Battery Optimization:** Background tracking via GPS places high demands on Android's resource management. This check validates via `PowerManager` whether the app is excluded from Doze mode/battery optimizations. If this is still enabled, the system immediately displays a warning.

### 2. Tracking Service (Live Status)
It reads directly (and reactively) from the singleton `TrackingStatusStore`.
- **Current State:** The exact `ServiceState` (e.g., `RUNNING`, `FAILED`, `RECOVERING`).
- **Message:** A status message translated by the system.
- **Active Trip ID:** If a trip is active, this reference is shown directly.
- **GPS Accuracy:** During the trip, it directly shows the deviation in meters, essential for detecting "empty" trips due to insufficient coverage.
- **Last Error:** Should the state change to `FAILED`, this field contains the technical cause (e.g., `SecurityException` or network timeout).

### 3. Active Workday
For time registration, it is crucial to see what state the system is in.
- **Status:** Checks via `WorkDayDao` whether the user is currently registered as 'logged in' at an office or client.
- **Location & Arrival Time:** Shows the linked label and the exact time of login, so you always have visual confirmation that work hours are running in the background, even if GPS is not actively running at that moment.

### 4. Boot Recovery & Failsafe
Integration with `TrackingRecoveryManager`.
- **Queue Status:** Shows instantly whether an aborted or incomplete trip is queued for automatic recovery.
- **Recovery Attempts:** Keeps track of how often the system has attempted to recover this to prevent the app from getting permanently stuck in a crash loop.

This diagnostic UI can be opened via the Settings menu or via the pop-up status indicator on the Dashboard and has proven to be a powerful tool for both administrators and troubleshooting by end users.
