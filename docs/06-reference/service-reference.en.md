# Service reference

This document describes the service and receiver layer of CIMDriver. This layer forms the operational core of automatic trip registration, status management, recovery, geofencing, and system reactions.

## Scope

The service package contains components for tracking, Bluetooth events, boot events, geocoder functionality, geofence management, notification actions, and status-related storage. Thus, this package acts as the bridge between Android system events and the functional logic of the application.

## File overview

### `BluetoothReceiver.kt`

- File within `app/src/main/java/com/cimdriver/app/service`.
- Included in the current code catalog and main source set of the application.

### `BootReceiver.kt`

- File within `app/src/main/java/com/cimdriver/app/service`.
- Included in the current code catalog and main source set of the application.

### `GeocoderService.kt`

- File within `app/src/main/java/com/cimdriver/app/service`.
- Included in the current code catalog and main source set of the application.

### `GeofenceBroadcastReceiver.kt`

- File within `app/src/main/java/com/cimdriver/app/service`.
- Included in the current code catalog and main source set of the application.

### `GeofenceManager.kt`

- File within `app/src/main/java/com/cimdriver/app/service`.
- Included in the current code catalog and main source set of the application.

### `NotificationActionReceiver.kt`

- File within `app/src/main/java/com/cimdriver/app/service`.
- Included in the current code catalog and main source set of the application.

### `OdometerCheckStore.kt`

- File within `app/src/main/java/com/cimdriver/app/service`.
- Included in the current code catalog and main source set of the application.

### `TrackingRecoveryManager.kt`

- File within `app/src/main/java/com/cimdriver/app/service`.
- Included in the current code catalog and main source set of the application.

### `TrackingService.kt`

- File within `app/src/main/java/com/cimdriver/app/service`.
- Included in the current code catalog and main source set of the application.

### `TrackingStatus.kt`

- File within `app/src/main/java/com/cimdriver/app/service`.
- Included in the current code catalog and main source set of the application.

### `VehicleSelectionStore.kt`

- File within `app/src/main/java/com/cimdriver/app/service`.
- Included in the current code catalog and main source set of the application.
