# Screens and navigation

## Purpose

This document describes how the SwiftUI app structures its main screens and which navigation patterns are used. The goal is a clear, testable, and extensible flow that works well on iPhone and remains extensible later for widget and CarPlay entry points.

## Expected main screens

- Dashboard
- Trip overview
- Trip detail
- Work hours overview
- Address book
- Vehicles
- Settings
- Onboarding and permission flow

## Main navigation

The preferred setup is a `TabView` for primary product domains, combined with a `NavigationStack` per tab for depth. This keeps feature boundaries clear and aligns well with SwiftUI state retention per section.

## Navigation rules

- Each tab manages its own path or route collection.
- Detail navigation uses typed routes instead of loose string-based identifiers.
- Cross-feature navigation runs via a coordinator or router, not via direct knowledge of other views.
- Modal flows such as filters, editors, or permission explanations run via sheets or full-screen covers.

## Example structure

```text
TabView
├── DashboardNavigationStack
├── TripsNavigationStack
├── WorkHoursNavigationStack
├── AddressesNavigationStack
└── SettingsNavigationStack
```

## Deep links and notifications

Notifications, widgets, or future quick actions first land in an app-wide router. This translates external triggers into a feature entry point, for example directly to trip detail or a permission screen.

## CarPlay and alternative entry points

CarPlay and widget extensions do not get their own business logic, but reuse the same use cases and repositories. Navigation there is more restricted and is therefore coordinated separately outside the regular iPhone tab structure.
