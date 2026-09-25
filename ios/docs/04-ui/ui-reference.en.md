# UI reference

## Purpose

This reference describes per main screen which SwiftUI structure, states, and interactions are expected. The focus is on information architecture and state representation, not on visual pixel perfection.

## Dashboard

- Shows summaries of trips, work hours, and recent status.
- Uses composite presentation models from multiple repositories.
- Must handle empty, error, and loading states clearly.

## Trip overview

- List or grouped overview of trips.
- Supports filtering, searching, and manual correction entry points.
- Has a route to trip detail and potentially quick actions.

## Trip detail

- Shows timeline, classification, addresses, vehicle, and correction history.
- Contains actions for manual adjustment and export-related context.

## Work hours overview

- Shows daily, weekly, or monthly overviews.
- Displays automatic calculation and manual corrections side-by-side.

## Address book and vehicles

- Manage recognition rules that influence the automatic flows.
- Require forms, validation, and clear feedback upon changes.

## Settings

- Contains permission status, tracking preferences, export options, and app information.
- Also acts as an entry point for diagnostics and fallback scenarios.
