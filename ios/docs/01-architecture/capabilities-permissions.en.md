# Capabilities and permissions

## Purpose

This document describes which iOS capabilities, entitlements, and runtime permissions are required for the SwiftUI architecture of CIMDriver. The goal is to explicitly model system access as part of the app architecture, not as separate implementation details in views.

## Architecture principle

Permissions are handled centrally via a `PermissionCoordinator` or similar service in the app and infrastructure layer. SwiftUI views never directly request permissions without an intermediary coordinator or use case, ensuring flows remain testable and the same logic is reusable in onboarding, settings, and recovery paths.

## Expected permissions

| Permission or capability | Purpose within CIMDriver | Architecture location |
| :--- | :--- | :--- |
| Location when in use | Trip detection and map context when app is active | Location adapter + onboarding flow |
| Always location | Background tracking and recovery of trip context | Location adapter + permission coordinator |
| Bluetooth | Recognizing vehicle or car kit context | Bluetooth adapter |
| Notifications | Tracking status, recovery notifications, and quick actions | Notification service |
| Background modes | Processing location, background refresh, or tasks | App target capabilities + background coordinators |
| CarPlay entitlement | Vehicle experience on supported flows | Integration layer + separate scene |

## SwiftUI flow for permissions

### 1. Onboarding

A SwiftUI onboarding feature shows per step why a permission is needed, what value it adds, and what the fallback behavior is if the user declines. The viewmodel determines the order and reads actual authorization status via infrastructure protocols.

### 2. Settings and recovery

Settings screens show the current permission status and guide users to system settings if necessary. After returning to the app, `AppSessionState` synchronizes the changed status, after which relevant features reload.

### 3. Runtime monitoring

When permissions change during usage, the permission coordinator publishes events that viewmodels can translate into warnings, banners, or restricted functionality. This keeps the UI declarative and prevents any single screen from having to poll permission status itself.

## Proposed components

- `PermissionCoordinator` for central status querying and permission requests.
- `LocationAuthorizationAdapter` for Core Location authorization status.
- `BluetoothAuthorizationAdapter` for bluetooth status.
- `PermissionBannerViewModel` for UI warnings when access is missing.
- `PermissionStatus` domain model for uniform representation across all features.

## Info.plist and capabilities

Record per capability:

- What user-facing explanation is in Info.plist.
- Which feature or use case depends on that capability.
- What fallback path exists if permission is missing.
- What App Store review risks or privacy impacts apply.

## Review points

- One central source of truth for permission status.
- No permission logic hidden in individual SwiftUI views.
- Clear separation between permission request, status display, and fallback behavior.
- Diagnostics log status transitions without unnecessarily recording privacy-sensitive details.
