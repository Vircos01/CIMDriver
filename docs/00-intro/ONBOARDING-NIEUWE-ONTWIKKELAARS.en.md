# CIMDriver: getting started guide for new developers

Welcome to CIMDriver. This document provides a practical first orientation to the application, its main code components, and a safe way to make changes.

## What is CIMDriver?

CIMDriver is an Android app for automatic trip registration and work hours support. The app combines Bluetooth detection, location tracking, local storage, trip classification, dashboards, widgets, and support for Android Auto and Android Automotive.

The core functionality runs locally on the Android device. Trips and locations are stored locally according to the project description and can be exported by the user.

## Read first

Please read the documentation in this order:

1. [`overview.en.md`](../00-intro/overview.en.md) — functional goal and main components.
2. [`architecture.en.md`](../01-architecture/architecture.en.md) — package and layer structure.
3. [`tracking.en.md`](../02-runtime/tracking.en.md) — core runtime process.
4. [`data-model.en.md`](../03-data/data-model.en.md) — local storage and repositories.
5. [`ui-navigation.en.md`](../04-ui/ui-navigation.en.md) — screens and navigation.
6. [`manifest-permissions.en.md`](../01-architecture/manifest-permissions.en.md) — Android contracts and permissions.
7. [`diagnostics.en.md`](../02-runtime/diagnostics.en.md) — status and troubleshooting.

Use the reference documents afterwards when working on a specific domain.

## Project structure

The main project components are:

| Location | Meaning |
|---|---|
| `app/src/main/java/com/cimdriver/app` | Kotlin source code of the application. |
| `app/src/main/java/com/cimdriver/app/service` | Tracking, boot, Bluetooth, geofencing, notifications and recovery. |
| `app/src/main/java/com/cimdriver/app/data` | Room database, DAOs, entities and repositories. |
| `app/src/main/java/com/cimdriver/app/ui` | Compose screens, components, navigation, theme and ViewModels. |
| `app/src/main/java/com/cimdriver/app/car` | Android Auto and Automotive functionality. |
| `app/src/main/java/com/cimdriver/app/worker` | Background tasks for backup and data lifecycle among others. |
| `app/src/main/java/com/cimdriver/app/util` | Reusable domain and utility logic. |
| `app/src/main/res` | Android resources, strings, themes, layouts, icons and XML configuration. |
| `docs` | Project documentation. |
| `release` | Release information and distribution artifacts. |

## Architecture in brief

The application can be roughly understood in five layers:

1. **UI layer** — displays data and collects user actions.
2. **ViewModel layer** — manages screen state and coordinates actions.
3. **Domain and utility layer** — performs classification, calculations, matching and normalization.
4. **Repository and data layer** — reads and writes local data via Room.
5. **Service and integration layer** — reacts to location, Bluetooth, boot, geofences, widgets and car events.

Not every flow goes through every layer. Tracking, for example, is started from Android events and uses services and storage directly, while regular screens mostly work via ViewModels and repositories.

## Main runtime flow

A typical automatic trip conceptually runs as follows:

1. A Bluetooth event or user action is received.
2. The app determines if tracking may start.
3. `TrackingService` is started as a foreground service.
4. Location points are collected and linked to the active trip.
5. Status, distance and context are tracked during the trip.
6. Upon disconnection or explicit stop, the trip is finalized.
7. The trip is classified and made available in the UI.
8. In case of interruption, recovery can try to resume tracking in a controlled manner.

The exact details of start conditions, state and error handling are in [`tracking.en.md`](../02-runtime/tracking.en.md) and [`service-reference.en.md`](../06-reference/service-reference.en.md).

## Main files

- `MainActivity.kt` — main entry point for the phone UI.
- `CIMDriverApplication.kt` — application initialization.
- `TrackingService.kt` — active location tracking and service lifecycle.
- `TrackingRecoveryManager.kt` — recovery behavior around tracking.
- `BootReceiver.kt` — boot and package replacement events.
- `BluetoothReceiver.kt` — Bluetooth connection and disconnect events.
- `AppDatabase.kt` — local Room database.
- `TripRepository.kt` — access to trip data.
- `TripsViewModel.kt` — trips and dashboard state.
- `CIMDriverCarAppService.kt` — Android Auto/Automotive entry point.
- `BackupWorker.kt` and `DataLifecycleWorker.kt` — maintenance-oriented background tasks.

A complete file overview is available in [`code-catalog.en.md`](../06-reference/code-catalog.en.md).

## Working on tracking

Changes to tracking deserve extra caution. Always check:

- if start, stop and recovery remain idempotent;
- if the correct runtime permissions are present;
- if the foreground notification remains correct;
- if reboot and package replacement do not lead to unintended starts;
- if an active trip is not created twice;
- if errors remain visible and diagnosable;
- if location and Bluetooth drops are handled in a controlled manner.

Test tracking not only from the UI, but also with system events and interruptions.

## Working on data

When making changes to entities, DAOs or repositories, existing trips, classification rules, vehicles, addresses and work hours must be taken into account. When making data model changes, always check:

- database version and migrations;
- existing records and nullable values;
- impact on export and backup;
- queries from ViewModels and widgets;
- consequences for recovery and active tracking.

Read [`data-model.en.md`](../03-data/data-model.en.md) and [`data-reference.en.md`](../03-data/data-reference.en.md) before modifying the storage model.

## Working on the UI

New or modified screens should preferably follow the existing separation between screen, component, navigation and ViewModel. Avoid database or service logic directly in composables when that logic belongs in a ViewModel, repository or domain utility.

When making UI changes, check:

- navigation route and back behavior;
- loading, empty and error states;
- rotation and lifecycle behavior;
- localized strings and resource usage;
- impact on widgets or car screens;
- accessibility and usability on different screen sizes.

## Manifest and permissions

The manifest contains, among other things, location, Bluetooth, notification, boot and foreground service permissions. Components such as receivers, services, widgets and car integration have different export settings.

Never adapt a manifest component based solely on the XML line. Also check the receiving Kotlin code, the runtime permission flow and Android version differences. See [`manifest-permissions.en.md`](../01-architecture/manifest-permissions.en.md).

## Test strategy

New developers must at least distinguish between:

- unit tests for pure calculations and utilities;
- repository and database tests;
- ViewModel and state tests;
- instrumentation tests for Android integration;
- manual tests for Bluetooth, location, boot, notifications, widgets and car integration.

For tracking, interruptions are at least as important as the normal happy path. Therefore, also test process kill, reboot, missing permissions, Bluetooth drop, location drop and failed recovery.

## Diagnostics

Use the diagnostics screen and relevant log lines when experiencing problems with tracking or background behavior. When reporting a bug, note at least:

- Android version and device model;
- application version;
- active permissions;
- current tracking status;
- time and context of the incident;
- last user action or system event;
- relevant error message or log line.

A problem report without lifecycle context is often insufficient for background services to determine the cause.

## Safe modification workflow

1. First read the relevant theme documentation.
2. Find the existing flow in the source code before adding new logic.
3. Determine which other entries can call the same functionality.
4. Make the smallest possible change.
5. Add or update tests.
6. Build the app and check for lint and compile errors.
7. Test the normal flow AND the error and recovery paths.
8. Update the documentation when behavior, configuration or architecture changes.

## Definition of done

A change is only ready when:

- the code compiles;
- relevant tests pass;
- lifecycle and error paths have been checked;
- permissions and manifest impact have been assessed;
- UI texts and resources are correctly updated;
- documentation and code do not contradict;
- logging does not leak sensitive data;
- any migration or release impact has been documented.

## Where to find help

Start at [`docs/README.md`](../README.md) for the full documentation index. Then use the reference document of the package involved and consult the code catalog to find all related files.
