# Architectuurdiagram

Onderstaand diagram geeft een overzicht van de hoofdarchitectuur van CIMDriver. Het sluit aan op de gelaagde opzet met entrypoints, UI, services, dataopslag en integratielagen zoals widgets, workers en Android Auto/Automotive.

## Mermaid-diagram

```mermaid
flowchart TD
    A[Android systeem en gebruiker] --> B[Entry points]

    subgraph EP[Entry points]
        B1[MainActivity]
        B2[BootReceiver]
        B3[BluetoothReceiver]
        B4[NotificationActionReceiver]
        B5[GeofenceBroadcastReceiver]
        B6[Widget Receivers]
        B7[CIMDriverCarAppService]
    end

    B --> EP

    subgraph UI[Presentatielaag]
        C1[Compose Screens]
        C2[Navigation]
        C3[UI Components]
        C4[ViewModels]
        C5[DiagnosticsScreen]
    end

    subgraph SV[Service- en eventlaag]
        D1[TrackingService]
        D2[TrackingRecoveryManager]
        D3[TrackingStatus]
        D4[GeofenceManager]
        D5[GeocoderService]
        D6[Stores zoals VehicleSelection en OdometerCheck]
    end

    subgraph DM[Domein- en utilitylaag]
        E1[TripClassification]
        E2[TripDistance]
        E3[AddressMatching]
        E4[WorkHoursCalculator en normalisatie]
        E5[DashboardStatsCalculator]
        E6[Export en backup utilities]
    end

    subgraph DT[Data- en opslaglaag]
        F1[TripRepository]
        F2[AppDatabase]
        F3[DAO's]
        F4[Entities]
    end

    subgraph IN[Integratielaag]
        G1[Widgets]
        G2[Workers]
        G3[Android Auto en Automotive]
        G4[FileProvider]
        G5[Notificaties]
    end

    EP --> D1
    EP --> D2
    EP --> G1
    EP --> G3
    B1 --> C1
    C1 --> C3
    C1 --> C4
    C2 --> C1
    C4 --> F1
    C4 --> E1
    C4 --> E5
    C5 --> D3

    D1 --> D2
    D1 --> D3
    D1 --> D4
    D1 --> D5
    D1 --> F1
    D1 --> E1
    D1 --> E2
    D1 --> E3
    D1 --> G5

    D2 --> F1
    D2 --> D3
    D4 --> F1
    D5 --> E3

    F1 --> F2
    F2 --> F3
    F2 --> F4

    G1 --> F1
    G2 --> F1
    G2 --> E6
    G3 --> F1
    G3 --> C4
```

## Leeswijzer

Het diagram toont dat CIMDriver meerdere ingangen heeft: niet alleen de telefoon-UI, maar ook boot-events, Bluetooth-events, notificatieacties, widgets en car-integratie. De `TrackingService` vormt het operationele middelpunt van automatische ritregistratie en werkt samen met recovery-, status-, geofence- en geocodercomponenten.

Daarnaast laat het diagram zien dat ViewModels en services niet rechtstreeks op losse tabellen werken, maar via repository- en datalagen met Room-componenten. Domein- en utilitylogica zoals classificatie, afstandsbepaling, adresmatching en werkurenberekening ondersteunen zowel de UI als de achtergrondprocessen.
