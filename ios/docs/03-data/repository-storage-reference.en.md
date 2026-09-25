# Repository and storage reference

## Purpose

This reference describes how repositories form the bridge between domain logic, SwiftUI viewmodels, and local storage. They do not literally replace Android DAO patterns, but rather translate the same responsibilities to an iOS-appropriate structure.

## Architectural position

Repositories belong between use cases and persistence adapters. SwiftUI viewmodels only speak with use cases or at most read-only facade services, never directly with SwiftData or Core Data.

## Repository types

| Repository | Responsibility |
| :--- | :--- |
| TripRepository | Reading, saving, finalizing, and correcting trips |
| WorkSessionRepository | Management of work hours, breaks, and corrections |
| AddressRepository | Known locations, detection radii, and classification rules |
| VehicleRepository | Vehicles and bluetooth pairings |
| SettingsRepository | User preferences and runtime settings |
| TrackingCheckpointRepository | Storage of recovery status and active tracking context |
| ExportRepository | Preparing datasets and saving export metadata |

## Contract guidelines

- Repositories return domain models or domain-specific query results.
- Write operations are explicit and side-effects are documented.
- Read operations for dashboard and list screens are optimized for SwiftUI redraws.
- Observation of changes happens via async streams, publishers, or fetch-refresh patterns.

## SwiftData or Core Data

Whatever persistence choice is made, shield it behind repository protocols. This keeps tests and migration to another storage option manageable.

## Mapping

Use mappers or dedicated initializers to convert persistent models into domain models. Do not put mapping logic in SwiftUI views or viewmodels, except for lightweight formatting for presentation.

## Recovery and query paths

Document per repository:

- Which queries are needed for initial screen population.
- Which queries are needed for dashboard aggregation.
- How active trips and checkpoints are saved atomically.
- How manual corrections remain auditable.

## Test strategy

Provide repositories with in-memory implementations or mocks for preview and unit tests. This allows SwiftUI features to be tested without actual persistence or Apple frameworks.
