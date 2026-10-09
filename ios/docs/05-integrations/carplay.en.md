# CarPlay and vehicle experience

## Purpose

This document describes how CarPlay fits into the SwiftUI architecture without duplicating the iPhone app structure. CarPlay gets a separate scene and its own template-driven UI, but must use the same domain services and repositories as the main app.

## Architectural position

CarPlay does not use a SwiftUI screen hierarchy like the iPhone app, but it does use the same use cases for trip status, vehicle context, and overviews. The integration layer translates CarPlay events into domain intents and back into allowed template updates.

## Guidelines

- No unique business logic in CarPlay scenes.
- Only make safe, limited interactions available while driving.
- Expose shared state via read-only projections where possible.
- Explicitly separate navigation and templates from the regular app router.

## Possible CarPlay scenarios

- Show current trip status.
- Quick insight into vehicle or tracking status.
- Limited actions such as stopping or flagging a trip, only if platform guidelines allow it.
