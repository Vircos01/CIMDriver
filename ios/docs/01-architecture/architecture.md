# Architectuur

## Doelarchitectuur

De iOS-app wordt opgezet als een modulaire SwiftUI-applicatie met een duidelijke scheiding tussen presentatie, domeinlogica, integraties en opslag. Daarmee blijft businesslogica testbaar, kunnen Apple-frameworks achter adapters worden geplaatst en ontstaat een structuur die functioneel aansluit op de Android-architectuur zonder Android-patronen klakkeloos over te nemen.

## SwiftUI architectuurprincipes

- **Feature-georiënteerde opbouw**: iedere hoofdflow krijgt een eigen featuremap met views, state, viewmodels, use-cases en repositories.
- **Unidirectionele datastroom**: UI vuurt intenties af, viewmodels verwerken die intenties, domeinservices leveren resultaten terug als observeerbare state.
- **Composition root in de applaag**: afhankelijkheden worden centraal opgebouwd in `AppContainer` of een vergelijkbare composition root, zodat SwiftUI views geen services construeren.
- **Protocol-first integraties**: adapters voor Core Location, Core Bluetooth, notificaties en opslag voldoen aan protocollen die in domein of infrastructuur beschreven zijn.
- **Platformgrenzen expliciet maken**: background execution, CarPlay en permissies worden als infrastructuurbeperkingen behandeld, niet als UI-logica.

## Voorgestelde lagen

### 1. App- en compositielaag

Deze laag bevat de `App` entrypoint, scene-configuratie, dependency wiring, environment injection en globale routering. Hier worden `ModelContainer`, services, repositories en featurecoördinatoren opgebouwd.

### 2. Presentatielaag

De presentatielaag bestaat uit SwiftUI `View`s, featuregerichte viewmodels en presentatiemodellen. Views zijn zo dom mogelijk: ze renderen state en sturen gebruikersacties door naar een viewmodel of feature store.

### 3. Domeinlaag

De domeinlaag bevat use-cases zoals ritdetectie, ritclassificatie, urenberekening, dashboardaggregatie en exportvoorbereiding. Deze laag kent geen SwiftUI of Apple UI-frameworks en blijft daardoor maximaal testbaar.

### 4. Infrastructuurlaag

Hier zitten adapters voor Core Location, Core Bluetooth, BGTaskScheduler, UserNotifications, geocoding en persistence. Deze laag vertaalt systeemevents naar domeinbegrippen zoals `TripCandidateDetected`, `TrackingRecovered` of `WorkSessionStopped`.

### 5. Data- en opslaglaag

Deze laag beheert SwiftData of Core Data, repositories, mapping van persistente modellen naar domeinmodellen en migraties. Dashboard-queries, actieve ritcheckpoints en exportdata horen hier thuis.

## Feature-indeling

Een praktische projectstructuur voor SwiftUI is feature-first, aangevuld met gedeelde infrastructuur:

```text
CIMDriveriOS/
├── App/
│   ├── CIMDriverApp.swift
│   ├── AppContainer.swift
│   ├── AppRouter.swift
│   └── SceneCoordinator.swift
├── Features/
│   ├── Dashboard/
│   ├── Trips/
│   ├── WorkHours/
│   ├── Addresses/
│   ├── Vehicles/
│   └── Settings/
├── Domain/
│   ├── Models/
│   ├── UseCases/
│   ├── Services/
│   └── Protocols/
├── Infrastructure/
│   ├── Location/
│   ├── Bluetooth/
│   ├── Notifications/
│   ├── BackgroundTasks/
│   ├── Geocoding/
│   └── Persistence/
└── Shared/
    ├── DesignSystem/
    ├── Components/
    ├── Utilities/
    └── Extensions/
```

## Dependency injection

Het implementatieplan noemt Swift Package Manager en handmatige dependency injection als voorkeursrichting. De aanbevolen aanpak is een lichte composition root met protocolgebaseerde constructorinjectie, omdat dat goed aansluit op SwiftUI previews, testen en modulaire feature-ontwikkeling.

### Richtlijnen

- Initialiseer services en repositories in één centrale container.
- Geef feature-rootviews alleen de dependencies mee die ze nodig hebben.
- Gebruik `@Environment` of custom environment keys voor echt globale services, niet voor feature-afhankelijke domeinlogica.
- Vermijd singletongebruik behalve waar Apple APIs daar praktisch toe dwingen, en scherm dat dan af achter protocollen.

## State management

SwiftUI vraagt om een expliciete keuze in state-eigenaarschap. Voor CIMDriver is de volgende verdeling het meest logisch:

- `@State` voor puur lokale viewstatus, zoals sheet-presentatie of tijdelijke selectie.
- `@StateObject` voor feature-viewmodels die de levensduur van een scherm of flow beheren.
- `@ObservedObject` of bindbare wrappertypes voor child views die state consumeren.
- `@EnvironmentObject` alleen voor cross-feature context zoals appstatus, sessiecontext of globale instellingen.
- `AsyncStream`, `Combine` of `Observation` voor doorlopende runtime-events zoals tracking- en permissiestatus.

## Navigatie

SwiftUI `NavigationStack` is het uitgangspunt voor detailnavigatie. Hoofdnavigatie kan worden opgebouwd met `TabView`, aangevuld met featurecoördinatoren voor deep links, notificatie-ingangen en CarPlay-specifieke instaproutes.

## Concurrency en async werk

Asynchrone domeinprocessen worden ondergebracht in `async/await` use-cases en actors waar gedeelde mutable state beschermd moet worden. Lange ketens zoals trackingherstel, classificatie en export moeten cancelbaar zijn en mogen UI-state pas updaten via de viewmodel-laag.

## Ontwerpprincipes

- Android-pariteit waar functioneel relevant is.
- iOS-native architectuur in plaats van één-op-één Android-translatie.
- Privacy by design en lokale opslag als standaard.
- Heldere scheiding tussen feature-UI, domeinregels en systeemplatformcode.
- Testbaarheid van use-cases en infrastructuur via protocollen en mocks.
