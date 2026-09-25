# Data reference

This document describes the data and repository layer of CIMDriver. This layer encompasses local storage, database access, entities, and the repository logic that higher application layers rely upon.

## Scope

The data-related source code is located in `data/local`, `data/local/dao`, `data/local/entity`, and `data/repository`. Thus, both storage structures and access and abstraction layers are explicitly present in the repository.

## File overview

### `AppDatabase.kt`

- File within `app/src/main/java/com/cimdriver/app/data/local`.
- Included in the current code catalog and main source set of the application.

### `ClassificationRuleDao.kt`

- File within `app/src/main/java/com/cimdriver/app/data/local/dao`.
- Included in the current code catalog and main source set of the application.

### `Daos.kt`

- File within `app/src/main/java/com/cimdriver/app/data/local/dao`.
- Included in the current code catalog and main source set of the application.

### `Entities.kt`

- File within `app/src/main/java/com/cimdriver/app/data/local/entity`.
- Included in the current code catalog and main source set of the application.

### `TripRepository.kt`

- File within `app/src/main/java/com/cimdriver/app/data/repository`.
- Included in the current code catalog and main source set of the application.
