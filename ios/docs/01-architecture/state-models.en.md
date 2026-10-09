# State and lifecycle models

## Purpose

This page describes how SwiftUI state, app lifecycle, and tracking lifecycle align. For CIMDriver this is crucial, because screen status, background execution, and system events directly influence each other.

## 1. SwiftUI state ownership

### View state

Use local `@State` only for temporary UI details, such as a selected filter, active sheet, or inline validation message. This state must not be a source for business logic or tracking status.

### Feature state

Every main feature gets its own `@StateObject`-backed viewmodel or feature store. This includes:

- Loaded data for the screen.
- Derived statistics or summaries.
- UI flags such as loading, refreshing, and error states.
- Actions that forward intents to use cases.

### App-wide state

Cross-feature state, such as permission status, tracking status, or active trip context, is managed in a higher object like `AppSessionState` or `TrackingSharedState`. Use this sparingly so that not every screen implicitly depends on global state.

## 2. App lifecycle

The iOS app lifecycle has at least the active, inactive, background, and terminated states. Every transition must make clear which SwiftUI state is volatile and which runtime state must be persistently saved.

| App status | Expected behavior |
| :--- | :--- |
| Active | Views render live status, permission changes, and tracking feedback instantly. |
| Inactive | Temporary transition, no heavy state mutations unless the system enforces it. |
| Background | Save checkpoints, limit observers, carefully schedule background tasks. |
| Terminated | Only persistent state remains; recovery must route through repositories and startup use cases. |

## 3. Tracking lifecycle

The tracking layer must be detached from individual screens. A recommended model is:

- **Idle** — No active trip candidate.
- **CandidateDetected** — There is enough context to suspect a trip.
- **TrackingActive** — A trip is running and checkpoints are updated.
- **TemporarilyPaused** — Signals temporarily drop or the user waits within a grace period.
- **CompletedPendingClassification** — The trip has stopped but has not yet been definitively classified.
- **Persisted** — The trip is saved and visible to UI, export, and dashboard.
- **RecoveryNeeded** — App start or system event requires reconstruction of an unfinished trip.

## 4. Work session lifecycle

Work hours follow a parallel state machine that can be influenced by trips, but is not identical to trip tracking:

- Not started.
- Active.
- Paused.
- Completed.
- Corrected.

This state must also support manual corrections without automatic recalculation undesirably reverting everything.

## 5. ViewModel status model

Ideally, every SwiftUI feature viewmodel gets a uniform status structure, for example:

```swift
struct ScreenState<Value> {
    var value: Value?
    var isLoading: Bool
    var isRefreshing: Bool
    var error: ScreenError?
    var emptyReason: EmptyStateReason?
}
```

This approach prevents fragmented booleans and makes states like loading, empty, error, and populated consistent across all features.

## 6. Event processing

User actions and system events are treated as intents. Examples:

- User opens trip detail.
- App receives new location.
- Bluetooth source connects.
- BGTask starts recovery routine.
- User changes permissions in Settings.

Every intent routes via viewmodel or coordinator to use cases; views do not directly mutate repositories or platform adapters.

## 7. Recovery after interruption

SwiftUI view state may be lost as soon as a scene is rebuilt. Therefore, recovery must always rely on persistent state such as:

- Active trip checkpoint.
- Last known tracking status.
- Pending classification.
- Workday in progress.
- Last permission snapshot and diagnostic hints.

## 8. Guidelines

- Keep feature state small and focused.
- Keep domain truth outside of views.
- Make lifecycle transitions explicitly testable.
- Avoid global mutable state without an owner.
- Model empty, error, and recovery states as first-class states.
