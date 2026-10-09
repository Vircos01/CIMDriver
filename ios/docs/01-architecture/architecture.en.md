# Architecture

## Target architecture

The iOS app is set up as a modular SwiftUI application with a clear separation between presentation, domain logic, integrations, and storage. This keeps business logic testable, allows Apple frameworks to be placed behind adapters, and creates a structure that functionally aligns with the Android architecture without blindly adopting Android patterns.

## SwiftUI architectural principles

- **Feature-oriented structure**: every main flow gets its own feature folder with views, state, viewmodels, use cases, and repositories.
- **Unidirectional data flow**: UI fires intents, viewmodels process these intents, domain services return results as observable state.
- **Composition root in the app layer**: dependencies are centrally built in `AppContainer` or a similar composition root, so SwiftUI views do not construct services.
- **Protocol-first integrations**: adapters for Core Location, Core Bluetooth, notifications, and storage adhere to protocols described in the domain or infrastructure.
- **Making platform boundaries explicit**: background execution, CarPlay, and permissions are treated as infrastructure constraints, not as UI logic.

## Proposed layers

### 1. App and composition layer

This layer contains the `App` entry point, scene configuration, dependency wiring, environment injection, and global routing. Here `ModelContainer`, services, repositories, and feature coordinators are built.

### 2. Presentation layer

The presentation layer consists of SwiftUI `View`s, feature-oriented viewmodels, and presentation models. Views are as dumb as possible: they render state and forward user actions to a viewmodel or feature store.

### 3. Domain layer

The domain layer contains use cases such as trip detection, trip classification, hour calculation, dashboard aggregation, and export preparation. This layer knows no SwiftUI or Apple UI frameworks and therefore remains maximally testable.

### 4. Infrastructure layer

Here reside adapters for Core Location, Core Bluetooth, BGTaskScheduler, UserNotifications, geocoding, and persistence. This layer translates system events into domain concepts such as `TripCandidateDetected`, `TrackingRecovered`, or `WorkSessionStopped`.

### 5. Data and storage layer

This layer manages SwiftData or Core Data, repositories, mapping of persistent models to domain models, and migrations. Dashboard queries, active trip checkpoints, and export data belong here.

## Feature layout

A practical project structure for SwiftUI is feature-first, supplemented with shared infrastructure:

```text
CIMDriveriOS/
├── App/
│   ├── CIMDriverApp.swift
│   ├── AppContainer.swift
│   ├── AppRouter.swift
│   └── SceneCoordinator.swift
├── Features/
│   ├── Dashboard/
│   ├── Trips/
│   ├── WorkHours/
│   ├── Addresses/
│   ├── Vehicles/
│   └── Settings/
├── Domain/
│   ├── Models/
│   ├── UseCases/
│   ├── Services/
│   └── Protocols/
├── Infrastructure/
│   ├── Location/
│   ├── Bluetooth/
│   ├── Notifications/
│   ├── BackgroundTasks/
│   ├── Geocoding/
│   └── Persistence/
└── Shared/
    ├── DesignSystem/
    ├── Components/
    ├── Utilities/
    └── Extensions/
```

## Dependency injection

The implementation plan lists Swift Package Manager and manual dependency injection as the preferred direction. The recommended approach is a lightweight composition root with protocol-based constructor injection, because this aligns well with SwiftUI previews, testing, and modular feature development.

### Guidelines

- Initialize services and repositories in a single central container.
- Give feature root views only the dependencies they need.
- Use `@Environment` or custom environment keys for truly global services, not for feature-dependent domain logic.
- Avoid singleton usage except where Apple APIs practically enforce it, and then shield it behind protocols.

## State management

SwiftUI requires an explicit choice in state ownership. For CIMDriver, the following distribution makes the most sense:

- `@State` for purely local view state, such as sheet presentation or temporary selection.
- `@StateObject` for feature viewmodels that manage the lifespan of a screen or flow.
- `@ObservedObject` or bindable wrapper types for child views that consume state.
- `@EnvironmentObject` only for cross-feature context such as app status, session context, or global settings.
- `AsyncStream`, `Combine`, or `Observation` for continuous runtime events such as tracking and permission status.

## Navigation

SwiftUI `NavigationStack` is the starting point for detail navigation. Main navigation can be built with `TabView`, supplemented with feature coordinators for deep links, notification entry points, and CarPlay-specific entry routes.

## Concurrency and async work

Asynchronous domain processes are accommodated in `async/await` use cases and actors where shared mutable state needs to be protected. Long chains like tracking recovery, classification, and export must be cancellable and should only update UI state via the viewmodel layer.

## Design principles

- Android parity where functionally relevant.
- iOS-native architecture instead of a one-to-one Android translation.
- Privacy by design and local storage as default.
- Clear separation between feature UI, domain rules, and system platform code.
- Testability of use cases and infrastructure via protocols and mocks.
