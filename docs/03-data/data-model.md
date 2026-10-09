# Gegevensmodel

CIMDriver gebruikt een lokale Room-database als kern van de gegevensverwerking. De codebasis bevat `AppDatabase`, DAO's, entiteiten en een repositorylaag rond ritgegevens.

## Kernonderdelen

| Onderdeel | Rol |
|---|---|
| `AppDatabase` | Centrale lokale database van de applicatie. |
| `ClassificationRuleDao` en overige DAO-bestanden | CRUD- en querytoegang tot opgeslagen gegevens. |
| `Entities.kt` | Datamodellen die persistent worden opgeslagen. |
| `TripRepository` | Toegangspunt tussen hogere lagen en ritgegevensopslag. |

## ER Diagram (Klanten, Projecten & Uren Targets)

Hieronder staat het entity-relationship diagram voor de nieuwe functionaliteit rondom klanten, projectcodes en targets voor declarabele uren.

```mermaid
erDiagram
    CLIENT ||--o{ PROJECT_CODE : has
    CLIENT ||--o{ HOURS_TARGET : has
    PROJECT_CODE ||--o{ HOURS_TARGET : "optionally filters"
    PROJECT_CODE ||--o{ WORK_DAY : "assigned to"
    PROJECT_CODE ||--o{ TRIP : "assigned to"
    PROJECT_CODE ||--o{ SAVED_ADDRESS : "default for"

    CLIENT {
        long id PK
        string name "CIMSOLUTIONS"
        string color "#1976D2"
        boolean isActive
    }

    PROJECT_CODE {
        long id PK
        long clientId FK
        string code "CIM-2026-001"
        string description "Detachering Klant A"
        boolean isBillable "true"
        boolean isActive
    }

    HOURS_TARGET {
        long id PK
        long clientId FK
        long projectCodeId FK "nullable"
        string name "Jaartarget 2026"
        double targetHours "1600"
        int year "2026"
        string color "#4CAF50"
        boolean isActive
    }

    WORK_DAY {
        long id PK
        long projectCodeId FK "nullable"
        string projectCode "legacy, deprecated"
        long date
        int breakMinutes
        string status
    }

    TRIP {
        long id PK
        long projectCodeId FK "nullable"
        string projectCode "legacy, deprecated"
    }

    SAVED_ADDRESS {
        long id PK
        long defaultProjectCodeId FK "nullable"
        string projectCode "legacy, deprecated"
    }
```

## Opslagfilosofie

De bestaande README beschrijft expliciet dat ritten en locaties lokaal op het toestel blijven in een Room-database totdat de gebruiker zelf exporteert. Daarmee is de datalaag niet alleen technisch maar ook functioneel en privacygerelateerd een hoofdonderdeel van de app.

## Datastromen

Tracking, classificatie, dashboards, voertuigen, adressen, werkuren en exportfunctionaliteit vertrouwen op dezelfde lokale gegevenslaag. Daardoor moet documentatie duidelijk maken welke entiteiten door welke processen worden gebruikt en welke afleidingen of aggregaties in hogere lagen plaatsvinden.

## Documentatierichtlijnen

Een volledige datadocumentatie moet per entiteit en DAO minimaal vastleggen: doel, sleutelvelden, relaties, lifecycle van de gegevens, bron van de data, updatepaden en exportrelevantie. De huidige documentatie vormt hiervoor het kader; nadere detaildocumentatie kan per tabel of entiteit worden toegevoegd wanneer het onderhoud dat vraagt.
