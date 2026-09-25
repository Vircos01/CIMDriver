# Getting started guide for new iOS developers

## Purpose

This onboarding helps new developers quickly understand how the iOS variant of CIMDriver is structured, which platform choices have been made, and where functional or technical documentation can be found.

## Initial reading order

1. First read [overview.md](overview.md) for product purpose, scope, and core scenarios.
2. Next, review `../../idea_ios/FUNCTIONEEL_OVERZICHT.md` and `../../idea_ios/IOS_IMPLEMENTATIEPLAN.md` for the functional and technical direction.
3. Then use the architecture and runtime chapters to substantiate implementation decisions.

## Expected iOS stack

- Swift and SwiftUI for the presentation layer and screen composition.
- Core Location, Core Bluetooth, and background execution APIs for tracking.
- SwiftData or Core Data for local storage, depending on the final target version.
- XCTest and XCUITest for unit, integration, and UI validation.

## Working agreements

- Explicitly document Android parity when behavior on iOS deviates.
- Keep privacy and battery impact visible in tracking-related choices.
- Describe every new capability, entitlement, or background mode in the architecture documentation.
