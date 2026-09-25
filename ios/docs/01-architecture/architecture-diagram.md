# Architectuurdiagram

## Logisch overzicht

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

## Laaggrenzen

De UI kent alleen feature-state en user intents. Viewmodels kennen use-cases, use-cases kennen repositories en domeinprotocollen, en alleen de infrastructuurlaag spreekt direct met Apple-frameworks.

## Feature interactie

```text
Dashboard Feature ─┐
Trips Feature ─────┼──→ Shared Domain Services → Repository Layer
WorkHours Feature ─┤
Settings Feature ──┘
```

Features delen domeinservices en repositories, maar niet elkaars viewmodels. Daarmee blijft iedere feature zelfstandig testbaar en uitbreidbaar.

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

## Navigatie- en scene-overzicht

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

## Uitleg

Het diagram maakt expliciet dat SwiftUI slechts de presentatielaag is en niet de plek waar tracking-, classificatie- of opslaglogica hoort. Ook laat het zien dat CarPlay en widgets eigen entrypoints kunnen hebben, maar dezelfde domein- en repositorylaag moeten hergebruiken om inconsistent gedrag te voorkomen.
