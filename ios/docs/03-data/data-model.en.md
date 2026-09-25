# Data model

## Purpose

The iOS data model supports trips, work hours, addresses, vehicles, settings, and export history, with local storage as the baseline. The model must feed SwiftUI features, support runtime recovery, and enable functional parity with Android.

## Model layers

### Domain models

Domain models describe business concepts such as `Trip`, `WorkSession`, `AddressProfile`, `VehicleProfile`, and `ExportJob`. These models are independent of SwiftData or Core Data and are used in use cases, services, and viewmodels.

### Persistent models

Persistent models describe how data is stored locally. When SwiftData is used, these models stay as close as possible to storage needs, while mapping to domain models happens in repositories.

### Presentation models

Presentation models aggregate data for SwiftUI screens, for example dashboard cards, trip lists, or detail headers. They are not stored directly.

## Core entities

| Entity | Description | Key usage |
| :--- | :--- | :--- |
| Trip | A single registered trip with time, distance, classification, and status | Trip overview, detail, export, dashboard |
| WorkSession | Workday or work session with start, pause, end, and corrections | Work hours overview and reporting |
| AddressProfile | Known location with type, label, and recognition rules | Classification and automatic detection |
| VehicleProfile | Vehicle or bluetooth source linked to trip context | Trip start detection and vehicle management |
| UserSettings | User preferences, tolerances, and feature flags | Settings and runtime decisions |
| TrackingCheckpoint | Last known progress for recovery | Background recovery |
| ExportHistory | Metadata about executed exports | Audit and user feedback |

## Modeling guidelines

- Separate domain identity from storage-technical identifiers where necessary.
- Preserve classification origin so that automatic and manual overrides can be distinguished.
- Model timestamps unambiguously, including timezone and rounding context.
- Keep tracking checkpoint data compact but sufficient for recovery after an interruption.
- Avoid directly embedding SwiftUI-specific structures in persistent models.

## Relationships

- A vehicle can be linked to multiple trips.
- An address profile can influence both trip classification and the start/stop of work hours.
- A work session can be derived from multiple trips, but remains independently correctable.
- Export history refers to the source selection and output status, not necessarily to all exported records.
