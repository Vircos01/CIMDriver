# Glossary

This glossary defines the main terms of CIMDriver based on the current codebase and the existing product description. The goal is a shared vocabulary for development, maintenance, and further documentation growth.

## Core concepts

- **TrackingService**: foreground Android service for active trip tracking.
- **Recovery**: recovery behavior with which tracking is controlled, resumed, or handled after interruption or restart.
- **TripRepository**: repository layer for trip data.
- **AppDatabase**: local Room database of the application.
- **BootReceiver**: receiver for boot and package-replacement events.
- **BluetoothReceiver**: receiver for connect and disconnect events of Bluetooth devices.
- **CarAppService**: service that makes CIMDriver available in an Android Auto or Automotive context.
- **Widget**: homescreen component outside the main app.
- **Worker**: background task for non-UI-bound processes like backup or data lifecycle.
