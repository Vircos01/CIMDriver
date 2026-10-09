# Components

## Purpose

This page describes the reusable SwiftUI components that can be deployed across features. The component level must support visual consistency and reuse of state patterns.

## Basic categories

- Dashboard cards for summaries and KPIs.
- List cells for trips, work hours, addresses, and vehicles.
- Form fields and selectors for settings and management.
- Status banners for permissions, tracking, and errors.
- Empty state and diagnostic components.

## Architecture guidelines

- Components receive simple, explicit input models.
- Components contain no business logic or repository calls.
- Formatting and display logic may reside in components to a limited extent, domain decisions may not.
- Accessibility and dynamic text are part of the component contract.

## Alignment with design system

Place generic visual building blocks in `Shared/DesignSystem` or `Shared/Components` and feature-specific compositions in the respective feature folder. This allows for reuse without the entire app leaning on a single massive component library.
