# Component reference

## Purpose

Record the input, output, states, and accessibility requirements for each reusable SwiftUI component. This reference helps with the consistent implementation of screens and prevents the same component from behaving slightly differently per feature.

## Record per component

- Name and area of responsibility.
- Input model or parameters.
- Expected interactions.
- Variations for loading, empty, error, or disabled.
- Accessibility and dynamic type considerations.
- Example of usage within a feature.

## Examples of component types

- `TripRowView`
- `DashboardSummaryCard`
- `PermissionBanner`
- `WorkSessionHeader`
- `VehicleFormSection`
- `EmptyStateView`

## Guideline

Only document components as contracts, not just as visual sketches. A component is only reusable if it is clear what state it expects and what guarantees it provides to the rest of the UI.
