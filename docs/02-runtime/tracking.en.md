# Tracking and recovery

Tracking is the core process of CIMDriver. The source structure and manifest show a combination of a foreground service, robust boot recovery logic, system receivers, and a centralized status store (`TrackingStatusStore`) that together enable automatic trip registration.

## Core components

| Component | Responsibility |
|---|---|
| `TrackingService` | Executing active tracking with location updates, dynamic notifications via StateFlow, and status transitions in the `TrackingStatusStore`. |
| `TrackingRecoveryManager` | Coordinating back-off logic and recovery behavior (via `WorkManager`) following interruptions. Reads `maxRecoveryAttempts` from the database. |
| `TrackingStatusStore` | Singleton object with a `MutableStateFlow` serving as the Single Source of Truth for real-time status (e.g., `RUNNING`, `FAILED`). |
| `BootReceiver` | Responds to authorized `BOOT_COMPLETED` intents and activates the recovery cycle. |
| `BluetoothReceiver` | Detecting validated connect and disconnect events of paired Bluetooth car kits. |
| `GeofenceManager` | Processing and managing geofence-related tracking context (e.g., when parking). |
| `NotificationActionReceiver` | Processing actions initiated via notifications (such as force stopping). |

## Service initialization

The `TrackingService` initializes a database, a `LocationManager`, a notification channel in `onCreate`, and immediately starts its own `serviceScope`. Within this coroutine scope, the `TrackingStatusStore.status` StateFlow is observed, ensuring the foreground notification always remains perfectly synchronized with the internal state of the service, and provides immediate warnings upon errors (such as dropping GPS).

## Service actions

Inside `onStartCommand`, the service processes actions like `START_TRACKING` and `RECOVER_TRACKING`. Idempotency is built into both paths by checking the `TrackingStatusStore` (no duplicate actions if the status is already `RUNNING` or `STARTING`).

When a trip is started, the service requests active GPS updates, moves itself to foreground mode, and transitions the state to `RUNNING`. Upon errors, the service stores an `errorCause` in the status and safely shuts down its resources.

## Notification behavior

Thanks to the Phase 5 implementation, the notification is entirely reactively driven from the `TrackingStatusStore`. Users are shown in real-time if there is an issue, and the "System Diagnostics" screens listen to the exact same StateFlow, preventing duplication of logic.

## Triggers for tracking

Tracking is driven by events. Possible triggers:
- Manually in the UI.
- A `BluetoothReceiver` connect event with a known car kit.
- A geofence trigger.
- A `BootReceiver` event in case of recovery after device failure.

The grace period for Bluetooth disconnects and standing still can be dynamically configured via the `Settings` database.

## Recovery

Recovery is designed with reliability and system friendliness in mind. It works as follows:
1. The `BootReceiver` intercepts the trigger (and validates the Intent).
2. The `TrackingRecoveryManager` checks the dynamic `maxRecoveryAttempts` setting in the Room Database.
3. An exponential back-off worker is scheduled in the `WorkManager` (to prevent the app from endlessly crashing and entering a boot loop if, for example, location permissions have been revoked).
4. As long as the limit is not reached, the Worker will attempt a safe restart of the `TrackingService`.

This centralized design ensures that system errors and app crashes can be intercepted and analyzed much better, and prevents "invisible" failing background services.
