# State and lifecycle models

This document describes the main runtime and lifecycle states of CIMDriver. Because CIMDriver relies on automatic trip registration, background processes, and crash recovery, correctly managing state is crucial for the app's stability.

## Tracking service (`ServiceState`)

The core of the tracking service is the `TrackingStatusStore`, which functions as the single-source-of-truth for the app's status. This store manages the state via the enum `ServiceState`, which contains the following specific statuses:

- `CREATED`: The service has been initialized but is not yet performing tracking.
- `STARTING`: The service is starting up and claiming resources (like location updates).
- `RUNNING`: The service is active and currently registering a trip (contains an active `tripId`).
- `STOPPING`: The service is safely shutting down and saving the current trip.
- `STOPPED`: The service is stopped or on standby waiting for an automatic trigger (e.g., a Bluetooth connection).
- `FAILED`: A critical error occurred, causing tracking to abort (contains an `errorCause`).
- `RECOVERING`: The service is attempting to recover itself after an unexpected interruption.

Transitions between these statuses are strictly monitored. For example, a duplicate transition to `STARTING` when the state is already `RUNNING` is ignored to guarantee idempotent actions.

## Recovery

Recovery is not a single action, but a specific lifecycle path coordinated by the `TrackingRecoveryManager`. The RecoveryManager distinguishes between:
- **Automatic recovery allowed:** The service stopped unexpectedly but the maximum number of recovery attempts has not yet been exceeded.
- **Recovery in progress:** The service state changes to `RECOVERING`, with exponential back-off timers via the `WorkManager`.
- **Manual intervention needed:** If the maximum number of recovery attempts (`maxRecoveryAttempts`) is reached, automatic recovery fails definitively and the user must intervene manually.

## UI state

The ViewModels in the application (like `TripsViewModel` and `SettingsViewModel`) use Kotlin's `StateFlow` to pass UI state to the screens (Jetpack Compose). This ensures that screen states like 'loading', 'empty', or 'error' automatically react to the underlying data, without the need for callbacks.

## Data state

Trips and addresses have specific statuses in the database. Trips that are automatically registered initially receive a status to be checked ("To Review"). Depending on classification rules (such as 'Home' to 'Work'), the final category (Business, Private, Commute) is applied directly or only after approval.
