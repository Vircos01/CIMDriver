# ViewModel and state reference

## Purpose

This document describes how SwiftUI viewmodels are constructed and which state patterns must be consistently used throughout the app. This keeps feature flows predictable, testable, and consistent across all screens.

## Core responsibilities of a viewmodel

- Loading data via use cases or query services.
- Translating domain results into presentable screen state.
- Handling user intents such as refresh, filter, save, or correct.
- Publishing UI events such as alerts, sheets, or navigation intents.
- Monitoring loading, error, empty, and success states.

## Recommended structure

```swift
@MainActor
final class TripsViewModel: ObservableObject {
    @Published private(set) var state: TripsScreenState

    func onAppear()
    func refresh()
    func applyFilter(_ filter: TripFilter)
    func openTrip(id: Trip.ID)
    func correctTrip(id: Trip.ID)
}
```

## Screen state pattern

Preferably use one composite `ScreenState` per screen instead of many loose booleans. This prevents inconsistent combinations such as simultaneously showing loading and error without a clear priority.

### Minimal components

- Content or view data.
- Loading status.
- Refresh status.
- Empty reason.
- Recoverable error status.
- Potential transient UI events.

## UI events versus persistent state

Not everything belongs in persistent view state. Use a separate event channel for one-time occurrences such as toast-like notifications, sheet presentation, or deep link routing.

## Collaboration with SwiftUI

- Root views usually own their viewmodel via `@StateObject`.
- Child views only receive the state or bindable substate they need.
- Viewmodels remain `@MainActor` when they perform UI publications.
- Async work is delegated to use cases, not constructed in views.

## Testability

Every viewmodel action must be testable in isolation using mocks or in-memory repositories. Above all, validate state transitions, error paths, and race conditions during async refresh or recovery.
