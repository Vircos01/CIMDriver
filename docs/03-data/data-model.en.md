# Data model

CIMDriver uses a local Room database as the core of its data processing. The codebase contains `AppDatabase`, DAOs, entities, and a repository layer around trip data.

## Core components

| Component | Role |
|---|---|
| `AppDatabase` | Central local database of the application. |
| `ClassificationRuleDao` and other DAO files | CRUD and query access to stored data. |
| `Entities.kt` | Data models that are persistently stored. |
| `TripRepository` | Access point between higher layers and trip data storage. |

## Storage philosophy

The existing README explicitly describes that trips and locations remain locally on the device in a Room database until the user explicitly exports them. Thus, the data layer is a main component of the app not only technically, but also functionally and from a privacy perspective.

## Data flows

Tracking, classification, dashboards, vehicles, addresses, work hours, and export functionality rely on the same local data layer. Therefore, documentation must clarify which entities are used by which processes and what derivations or aggregations take place in higher layers.

## Documentation guidelines

Complete data documentation must at a minimum record per entity and DAO: purpose, key fields, relationships, data lifecycle, data source, update paths, and export relevance. The current documentation provides the framework for this; further detailed documentation can be added per table or entity when maintenance requires it.
