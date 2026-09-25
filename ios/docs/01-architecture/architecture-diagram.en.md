# Architecture diagram

## Logical overview

```text
CIMDriverApp
    ↓
AppContainer / Composition Root
    ↓
TabView + NavigationStack + Feature Coordinators
    ↓
Feature Views
    ↓
Feature ViewModels / State Stores
    ↓
Use Cases / Domain Services
    ↓
Repository Protocols
    ↓
Infrastructure Adapters + Persistence
    ↓
Apple Frameworks (Core Location, Core Bluetooth, BGTaskScheduler, SwiftData)
```

## Layer boundaries

The UI only knows feature state and user intents. ViewModels know use cases, use cases know repositories and domain protocols, and only the infrastructure layer communicates directly with Apple frameworks.

## Feature interaction

```text
Dashboard Feature ─┐
Trips Feature ─────┼──→ Shared Domain Services → Repository Layer
WorkHours Feature ─┤
Settings Feature ──┘
```

Features share domain services and repositories, but not each other's ViewModels. This ensures that every feature remains independently testable and extensible.

## Runtime event flow

```text
Core Location / Bluetooth event
    ↓
Platform Adapter
    ↓
TrackingCoordinator
    ↓
Trip Detection Use Case
    ↓
Repository update + diagnostics log
    ↓
Observable state update
    ↓
SwiftUI screen refresh / notification / widget refresh
```

## Navigation and scene overview

```text
Main App Scene
├── Dashboard Tab
├── Trips Tab
├── WorkHours Tab
├── Addresses Tab
├── Settings Tab
└── Modals / Sheets / Alerts

Optional Scenes
├── Widget extension
└── CarPlay scene
```

## Explanation

The diagram makes explicit that SwiftUI is merely the presentation layer and not the place where tracking, classification, or storage logic belongs. It also shows that CarPlay and widgets can have their own entry points, but must reuse the same domain and repository layer to prevent inconsistent behavior.
