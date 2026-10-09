# Integration reference

## Purpose

This reference describes how Apple frameworks and external services are incorporated into the iOS architecture. Integrations are always placed behind adapters and protocols, so that SwiftUI features and domain services remain decoupled from framework details.

## Relevant integrations

| Integration | Role in architecture |
| :--- | :--- |
| Core Location | Location updates, regions, and background signals |
| Core Bluetooth | Detection of vehicle context or paired sources |
| UserNotifications | Status messages and quick actions |
| BGTaskScheduler | Recovery, maintenance, and scheduled background processing |
| CLGeocoder or external fallback | Address resolution and recognition |
| CarPlay | Vehicle experience via a separate scene |
| File and sharing services | Export of CSV or PDF |

## Adapter pattern

Each integration should have an adapter layer with:

- Protocol definition for the domain or application layer.
- Concrete framework-specific implementation type.
- Mapping of system types to domain models or events.
- Clear error and fallback behavior.

## Guideline

Prevent viewmodels from directly consuming framework types like `CLLocation`, `CBPeripheral`, or notification objects. First translate these into CIMDriver-specific models or intents.
