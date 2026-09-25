# Architecture Diagram

The diagram below provides an overview of the main architecture of CIMDriver. It aligns with the layered setup featuring entry points, UI, services, data storage, and integration layers such as widgets, workers, and Android Auto/Automotive.

## Mermaid diagram

```mermaid
flowchart TD
    A[Android system and user] --> B[Entry points]

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

    subgraph UI[Presentation layer]
        C1[Compose Screens]
        C2[Navigation]
        C3[UI Components]
        C4[ViewModels]
        C5[DiagnosticsScreen]
    end

    subgraph SV[Service and event layer]
        D1[TrackingService]
        D2[TrackingRecoveryManager]
        D3[TrackingStatus]
        D4[GeofenceManager]
        D5[GeocoderService]
        D6[Stores like VehicleSelection and OdometerCheck]
    end

    subgraph DM[Domain and utility layer]
        E1[TripClassification]
        E2[TripDistance]
        E3[AddressMatching]
        E4[WorkHoursCalculator and normalization]
        E5[DashboardStatsCalculator]
        E6[Export and backup utilities]
    end

    subgraph DT[Data and storage layer]
        F1[TripRepository]
        F2[AppDatabase]
        F3[DAOs]
        F4[Entities]
    end

    subgraph IN[Integration layer]
        G1[Widgets]
        G2[Workers]
        G3[Android Auto and Automotive]
        G4[FileProvider]
        G5[Notifications]
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

## Reading guide

The diagram shows that CIMDriver has multiple entry points: not only the phone UI, but also boot events, Bluetooth events, notification actions, widgets, and car integration. The `TrackingService` is the operational centerpiece of automatic trip registration and collaborates with recovery, status, geofence, and geocoder components.

Additionally, the diagram illustrates that ViewModels and services do not operate directly on loose tables, but via repository and data layers with Room components. Domain and utility logic such as classification, distance determination, address matching, and work hours calculation support both the UI and background processes.
