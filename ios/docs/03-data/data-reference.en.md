# Data reference

## Purpose

This reference describes which data fields, validation rules, and conventions apply to each main entity. This creates a stable foundation for repositories, use cases, SwiftUI viewmodels, and export logic.

## General rules

- Timestamps are stored uniformly internally and displayed localized in the presentation layer.
- Manual changes remain traceable via source or mutation metadata.
- Optional fields are only used when functionally truly relevant.
- Migrations must be backwards compatible with existing local data.

## Trip

Fields to record:

- Unique identifier.
- Start and end time.
- Start and end location or reference to them.
- Distance, duration, and optional route information.
- Classification and source of classification.
- Status such as draft, active, completed, or corrected.
- Linked vehicle.

## WorkSession

Fields to record:

- Unique identifier.
- Work start, pause intervals, and end time.
- Automatically or manually determined.
- Tolerance rules or applied corrections.
- Note or explanation where necessary.

## AddressProfile

Fields to record:

- Name or label.
- Type, such as home, office, client, or other.
- Coordinates or geofence definition.
- Detection radius.
- Priority for classification.

## VehicleProfile

Fields to record:

- Name and description.
- License plate if available.
- Linked bluetooth identifiers or recognition keys.
- Starting mileage or reference values for reporting.

## UserSettings

Fields to record:

- Default classification or fallback behavior.
- Work hours preferences and tolerances.
- Notification settings.
- Export preferences.
- Any feature flags for experimental functions.

## Guideline

Work out a concrete field matrix per entity later once the definitive persistence choice and naming have been established. Use this document as a contract between data storage, domain logic, and UI projection.
