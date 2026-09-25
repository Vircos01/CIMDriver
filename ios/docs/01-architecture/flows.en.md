# Process flows

## Purpose

These process flows describe the primary SwiftUI and runtime paths of the iOS app. They connect user interaction, viewmodel logic, domain services, and platform events into a single cohesive model.

## 1. App start and dependency bootstrapping

1. `CIMDriverApp` boots up.
2. `AppContainer` initializes persistence, repositories, services, and coordinators.
3. `AppSessionState` loads global context such as permissions, settings, and active tracking status.
4. The root `TabView` is constructed with feature root views.
5. Feature viewmodels execute their initial loading actions.

## 2. Onboarding and permission flow

1. User goes through onboarding screens in SwiftUI.
2. An onboarding viewmodel determines which permissions are requested and when.
3. Results of permission requests are stored in app-wide state and diagnostics.
4. The app transitions to operational screens once minimum permissions or fallback paths are available.

## 3. Trip detection flow

1. A platform adapter receives location, bluetooth, or vehicle context.
2. `TrackingCoordinator` translates this into domain events.
3. The trip detection use case determines whether a trip candidate or active trip should be initiated.
4. Repository and diagnostics are updated.
5. Shared tracking state and relevant SwiftUI screens refresh.
6. Optionally, a notification or widget update follows.

## 4. Trip classification flow

1. A completed trip enters the status `CompletedPendingClassification`.
2. The classification use case combines addresses, time windows, known locations, and user settings.
3. The result is saved as business, private, or commute, potentially with a manual override.
4. Dashboard and list features receive updated data via repository observation or refresh.

## 5. Work hours flow

1. Location and trip context provide signals for work start or work stop.
2. The work hours use case applies tolerances, break rules, and known locations.
3. A work session repository saves intermediate states and corrections.
4. The work hours overview and dashboard display the updated summary.

## 6. Navigation flow in SwiftUI

- Main navigation runs via `TabView`.
- Detail screens run via `NavigationStack` and typed destinations.
- Modal interactions like filters, settings, or manual corrections run via sheets and alerts.
- Deep links or notifications land first in a coordinator, and only then in a specific view route.

## 7. Recovery flow after background or app restart

1. The app becomes active or a scheduled task triggers recovery.
2. Startup use cases inspect persistent checkpoints.
3. Ongoing trips or work hours are reconstructed or safely closed.
4. Shared state and feature viewmodels synchronize with the recovered truth.
5. The UI optionally shows a warning or diagnostic message when automatic recovery was limited.

## 8. Export flow

1. User starts export from settings or an overview screen.
2. The export viewmodel collects filters and format choice.
3. An export use case reads data from repositories, transforms it to CSV or PDF, and writes a file.
4. The UI shows completion, error status, or share options.

## Guideline

Use these flows as a source for sequence diagrams, UI state validation, test scenarios, and acceptance criteria. Each flow must continue to name both the SwiftUI route and the underlying domain and infrastructure steps.
