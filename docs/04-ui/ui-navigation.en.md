# Screens and navigation

The user interface of CIMDriver is extensive and built from Compose screens, reusable components, dialogs, navigation files, theme files, and ViewModels. This clearly separates the UI from services and storage layers.

## Screens

The source structure includes the following screens, among others: dashboard, trips, trip detail, vehicles, address book, settings, help, diagnostics, work hours, and forms for adding addresses, trips, vehicles, and workdays. This indicates a fully operational app rather than a limited MVP interface.

## Components and navigation

The `ui.components` package contains components for trips, vehicles, permissions, settings, charts, and swipe-to-delete behavior. In addition, `AppNavigation.kt`, `NavRoutes.kt`, and `Navigation.kt` form the backbone for screen routing within the app.

## ViewModels

The codebase contains specific ViewModels for addresses, settings, trip details, trips, vehicles, and work hours. This classification supports a screen- or domain-oriented state approach where presentation logic is extracted from the UI itself.

## Theme and localization

With theme files under `ui.theme`, Compose localization support, and resources in `values` and `values-nl` among others, the application is set up for a consistent visual style and localized user content. This aligns with the choice to also conduct technical documentation in multiple languages.
