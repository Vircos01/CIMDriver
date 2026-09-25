# Process Flows

This document describes the main functional and technical process flows of CIMDriver in context. The goal is to quickly give developers insight into how events move through the application, including the recently added robustness and security checks.

## Bluetooth to tracking

1. A Bluetooth connect event is received by the receiver layer (`BluetoothReceiver`).
2. The receiver explicitly validates whether the incoming `Intent` matches allowed actions to prevent spoofing.
3. The app determines if the connection is relevant for trip detection.
4. The `TrackingService` is started or resumed via the `TrackingRecoveryManager`.
5. The service goes into foreground mode and starts location updates.
6. An active trip is saved and updated in the database.
7. The UI (`DiagnosticsScreen`) and the dynamic `Notification` reflect the new status by reading the `TrackingStatusStore` StateFlow.

## Tracking to classification

1. A trip is terminated via disconnect (after the grace period), explicit stop, or another lifecycle event.
2. The service closes the active trip in the database.
3. Classification logic determines the trip type using matching algorithms on start and end locations.
4. Results are stored in the local database (Room).
5. The trip becomes visible in overviews (`TripsScreen`), dashboards, and detail screens, and initially receives a "To Review" status if manual approval is required.

## Recovery flow (Boot & Crash Recovery)

1. A reboot, process kill, or package replacement interrupts tracking.
2. The `BootReceiver` receives the event, validates the action, and awakens the `TrackingRecoveryManager`.
3. The `TrackingRecoveryManager` checks the stored `maxRecoveryAttempts` in the database.
4. It checks whether the limit of consecutive attempts has not yet been reached.
5. If so: A recovery intent is scheduled with exponential back-off timers to prevent system drain.
6. The `TrackingService` is started with recovery intent. The state in the store changes to `RECOVERING` and then to `STARTING`.
7. Status and notification are updated immediately to inform the user of the recovery.
8. Diagnostics (`DiagnosticsScreen`) make visible what happened, including any error messages (`errorCause`).

## UI flow

1. User opens a screen via the navigation structure in `Navigation.kt`.
2. Navigation activates the corresponding Compose screen and Hilt ViewModel.
3. ViewModel reads data via repositories (via Room or `TrackingStatusStore` StateFlow).
4. Compose redraws (recomposition) based on State changes.
5. User actions mutate state, start underlying processes via coroutines, or store new data in the repository.
