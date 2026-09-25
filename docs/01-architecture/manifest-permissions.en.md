# Manifest and permissions

The Android manifest describes the application's external contracts: permissions, component declarations, providers, car metadata, and system integration. For CIMDriver, the manifest is a crucial document because tracking, recovery, widgets, and car integration depend directly on correct manifest configuration.

## Declared permissions

The app declares permissions for Bluetooth, location, background location, foreground services, notifications, boot events, and network access. This combination functionally fits automatic trip registration with system integration.

| Permission | Purpose |
|---|---|
| `BLUETOOTH` and `BLUETOOTH_ADMIN` with `maxSdkVersion=30` | Support for older Android versions with legacy Bluetooth permission model. |
| `BLUETOOTH_CONNECT` | Connecting to and reading from Bluetooth devices on newer Android versions. |
| `ACCESS_FINE_LOCATION` and `ACCESS_COARSE_LOCATION` | Location determination for tracking and address/route functionality. |
| `ACCESS_BACKGROUND_LOCATION` | Location usage outside visible foreground UI for automatic trip registration. |
| `FOREGROUND_SERVICE` and `FOREGROUND_SERVICE_LOCATION` | Allowing a foreground location service, essential for stable tracking in the background. |
| `POST_NOTIFICATIONS` | Notifications for active tracking and synchronizing the status to the user. |
| `RECEIVE_BOOT_COMPLETED` | Responding to restart scenarios (Device boot/reboot) to automatically recover trips via the `BootReceiver`. |
| `INTERNET` and `ACCESS_NETWORK_STATE` | External network functionality such as geocoding and route calculation. |

## Queries

The app declares query intents for `geo` and `google.navigation`, which indicates that external map or navigation integration (such as directly opening directions from the address book) is part of the platform contract.

## Application level

The `application` declaration uses `CIMDriverApplication`, supports backup and data extraction rules, forbids cleartext traffic, and refers to custom theme and icon resources. Thus, backup behavior, network policy, and branding are established at the manifest level.

## Activities, services and receivers

`MainActivity` is the launcher activity and is exported as required for an entry point of the app.

The `TrackingService` is **not** exported (security) and is declared as a `location` foreground service. This guarantees that Android gives the service the necessary priority and location access.

The manifest declarations also show various receivers. An important architectural and security principle (implemented in Phase 5) is that exported receivers like `BluetoothReceiver` and `BootReceiver` strictly validate their incoming `Intent.action` in the Kotlin code against an established whitelist (e.g., `Intent.ACTION_BOOT_COMPLETED`). This prevents intent spoofing by external, potentially malicious, apps on the device.

Other receivers: `NotificationActionReceiver`, `GeofenceBroadcastReceiver`, and widget receivers make the app accessible from system events and appwidget updates.

## Providers and metadata

The app uses a `FileProvider` with an authority based on `${applicationId}.fileprovider`, not exported and with URI permission granting (for safe PDF and CSV export). In addition, metadata is present for Android Auto, Android Automotive, and minimum car API version.

## Documentation guidelines

For future maintenance, it must be documented per component why it is exported or not exported, which intent actions are accepted and checked, which runtime permissions are required, and which platform versions require special treatment. Manifest documentation and strict security checks are not an afterthought in this app, but part of the robust runtime architecture.
