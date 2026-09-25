# Repository- en storage-referentie

## Doel

Deze referentie beschrijft hoe repositories de brug vormen tussen domeinlogica, SwiftUI-viewmodels en lokale opslag. Ze vervangen Android-DAO-patronen niet letterlijk, maar vertalen dezelfde verantwoordelijkheden naar een iOS-geschikte structuur.

## Architectuurpositie

Repositories horen thuis tussen use-cases en persistence-adapters. SwiftUI-viewmodels spreken alleen met use-cases of hooguit read-only facade-services, nooit direct met SwiftData of Core Data.

## Repositorytypen

| Repository | Verantwoordelijkheid |
| :--- | :--- |
| TripRepository | Lezen, opslaan, afronden en corrigeren van ritten |
| WorkSessionRepository | Beheer van werkuren, pauzes en correcties |
| AddressRepository | Bekende locaties, detectieradii en classificatieregels |
| VehicleRepository | Voertuigen en bluetoothkoppelingen |
| SettingsRepository | Gebruikersvoorkeuren en runtime-instellingen |
| TrackingCheckpointRepository | Opslag van herstelstatus en actieve trackingcontext |
| ExportRepository | Voorbereiden van datasets en bewaren van exportmetadata |

## Contractrichtlijnen

- Repositories bieden domeinmodellen of domeinspecifieke queryresultaten terug.
- Schrijfoperaties zijn expliciet en side-effects worden gedocumenteerd.
- Leesoperaties voor dashboard- en lijstschermen zijn geoptimaliseerd voor SwiftUI-hertekening.
- Observatie van wijzigingen gebeurt via async streams, publishers of fetch-refreshpatronen.

## SwiftData of Core Data

Welke persistencekeuze ook gemaakt wordt, scherm die af achter repositoryprotocollen. Daardoor blijven tests en migratie naar een andere opslagoptie beheersbaar.

## Mapping

Gebruik mappers of dedicated initializers om persistente modellen om te zetten naar domeinmodellen. Stop mappinglogica niet in SwiftUI views of viewmodels, behalve lichte formattering voor presentatie.

## Recovery- en querypaden

Documenteer per repository:

- Welke queries nodig zijn voor initiële schermvulling.
- Welke queries nodig zijn voor dashboardaggregatie.
- Hoe actieve ritten en checkpoints atomair worden opgeslagen.
- Hoe handmatige correcties auditbaar blijven.

## Teststrategie

Voorzie repositories van in-memory implementaties of mocks voor preview- en unit-tests. Daarmee kunnen SwiftUI-features getest worden zonder echte persistence of Apple-frameworks.
