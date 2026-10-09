# Overview

CIMDriver is an Android application for automatic trip registration, work hours support, and related vehicle and address functionality. The application combines a foreground tracking service, local data storage, dashboards, widgets, and car integration within a single Android project.

## Product goal

The existing product description indicates that trips are automatically started upon relevant Bluetooth connections, are terminated upon disconnection, and are then smartly classified. In addition, the app supports automated work hours, dashboard insights, and local privacy-friendly storage with export capabilities.

## Main functional domains

The repository structure shows that the application contains the following main areas:

- Tracking and recovery via services, receivers, and status stores.
- Data management via Room components, repositories, and utilities.
- User interface via Compose screens, components, dialogs, and ViewModels.
- Widgets for homescreen use.
- Android Auto and Automotive interfaces via separate car components.
- Background tasks via workers for backup and data lifecycle.

## Scope of this documentation

This documentation is intended as a central reference for developers and maintainers who want to understand, maintain, or expand the application. The documents describe both functional behavior and technical structure, so that the codebase is not only readable in source code form but is also explained in context.
