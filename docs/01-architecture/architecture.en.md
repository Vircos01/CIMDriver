# Architecture

The architecture of CIMDriver is multi-layered with distinct responsibilities for application entry points, services, data, UI, widgets, workers, and car integration. This layering is visible in the package structure of the main module.

## Main structure

The repository contains one primary Android app module under `app`, supplemented with configuration files, release content, and project documentation. Within the main module, there are 79 Kotlin source files and 25 XML files in the main source set.

## Packages

| Package | Responsibility |
|---|---|
| `com.cimdriver.app` | Application initialization and main entry points. |
| `com.cimdriver.app.service` | Tracking, recovery, geocoding, geofencing, notification actions, and status-related runtime processes. |
| `com.cimdriver.app.data.local` | Local Room database, DAOs, and entities. |
| `com.cimdriver.app.data.repository` | Repository logic for trip data among other things. |
| `com.cimdriver.app.ui.*` | Compose screens, components, dialogs, theme, navigation, and ViewModels. |
| `com.cimdriver.app.car` | Screens and services for Android Auto and Automotive. |
| `com.cimdriver.app.widget` and `com.cimdriver.app.ui.widget` | Widget providers and widget-related logic. |
| `com.cimdriver.app.worker` | Background tasks such as backup and data lifecycle. |
| `com.cimdriver.app.di` | Dependency injection configuration. |
| `com.cimdriver.app.domain` | Domain calculations such as dashboard statistics. |
| `com.cimdriver.app.util` | Utility logic for address matching, export, classification, time, and calculations. |

## Runtime entry points

The manifest shows multiple entry points into the app: the launcher activity, a foreground tracking service, boot and Bluetooth receivers, widget receivers, a FileProvider, and a car app service. This means the app is not only UI-driven but also event- and system-driven.

## Responsibilities per layer

The presentation layer consists of screens, navigation, components, and ViewModels. The service layer processes tracking, recovery, and system events. The data layer stores and exposes information via Room and repositories. The integration layer connects the app to widgets, workers, and car interfaces.
