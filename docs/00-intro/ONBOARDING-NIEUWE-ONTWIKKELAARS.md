# CIMDriver: startgids voor nieuwe ontwikkelaars

Welkom bij CIMDriver. Dit document geeft een praktische eerste oriëntatie op de applicatie, de belangrijkste codeonderdelen en een veilige manier om wijzigingen te maken.

## Wat is CIMDriver?

CIMDriver is een Android-app voor automatische ritregistratie en ondersteuning van werkurenregistratie. De app combineert Bluetooth-detectie, locatie-tracking, lokale opslag, ritclassificatie, dashboards, widgets en ondersteuning voor Android Auto en Android Automotive.

De kernfunctionaliteit draait lokaal op het Android-toestel. Ritten en locaties worden volgens de projectbeschrijving lokaal opgeslagen en kunnen door de gebruiker worden geëxporteerd.

## Eerst lezen

Lees de documentatie bij voorkeur in deze volgorde:

1. [`overview.md`](../00-intro/overview.md) — functioneel doel en hoofdonderdelen.
2. [`architecture.md`](../01-architecture/architecture.md) — package- en laagindeling.
3. [`tracking.md`](../02-runtime/tracking.md) — belangrijkste runtimeproces.
4. [`data-model.md`](../03-data/data-model.md) — lokale opslag en repositories.
5. [`ui-navigation.md`](../04-ui/ui-navigation.md) — schermen en navigatie.
6. [`manifest-permissions.md`](../01-architecture/manifest-permissions.md) — Android-contracten en permissies.
7. [`diagnostics.md`](../02-runtime/diagnostics.md) — status en probleemoplossing.

Gebruik daarna de referentiedocumenten wanneer je aan een specifiek domein werkt.

## Projectstructuur

De belangrijkste projectonderdelen zijn:

| Locatie | Betekenis |
|---|---|
| `app/src/main/java/com/cimdriver/app` | Kotlin-broncode van de applicatie. |
| `app/src/main/java/com/cimdriver/app/service` | Tracking, boot, Bluetooth, geofencing, notificaties en recovery. |
| `app/src/main/java/com/cimdriver/app/data` | Room-database, DAO's, entiteiten en repositories. |
| `app/src/main/java/com/cimdriver/app/ui` | Compose-schermen, componenten, navigatie, thema en ViewModels. |
| `app/src/main/java/com/cimdriver/app/car` | Android Auto- en Automotive-functionaliteit. |
| `app/src/main/java/com/cimdriver/app/worker` | Achtergrondtaken voor onder meer backup en datalifecycle. |
| `app/src/main/java/com/cimdriver/app/util` | Herbruikbare domein- en hulplogica. |
| `app/src/main/res` | Android-resources, strings, thema's, layouts, icons en XML-configuratie. |
| `docs` | Nederlandstalige projectdocumentatie. |
| `release` | Release-informatie en distributieartefacten. |

## Architectuur in het kort

De applicatie is grofweg in vijf lagen te begrijpen:

1. **UI-laag** — toont gegevens en verzamelt gebruikersacties.
2. **ViewModel-laag** — beheert schermstate en coördineert acties.
3. **Domein- en utilitylaag** — voert classificatie, berekeningen, matching en normalisatie uit.
4. **Repository- en datalaag** — leest en schrijft lokale gegevens via Room.
5. **Service- en integratielaag** — reageert op locatie, Bluetooth, boot, geofences, widgets en car-events.

Niet elke flow loopt door iedere laag. Tracking wordt bijvoorbeeld vanuit Android-events gestart en gebruikt services en opslag rechtstreeks, terwijl reguliere schermen meestal via ViewModels en repositories werken.

## Belangrijkste runtimeflow

Een typische automatische rit verloopt conceptueel als volgt:

1. Een Bluetooth-event of gebruikersactie wordt ontvangen.
2. De app bepaalt of tracking mag starten.
3. `TrackingService` wordt als foreground service gestart.
4. Locatiepunten worden verzameld en gekoppeld aan de actieve rit.
5. Tijdens de rit worden status, afstand en context bijgehouden.
6. Bij verbreking of expliciete stop wordt de rit afgesloten.
7. De rit wordt geclassificeerd en beschikbaar gemaakt in de UI.
8. Bij een onderbreking kan recovery proberen de tracking gecontroleerd te hervatten.

De exacte details van startvoorwaarden, state en foutafhandeling staan in [`tracking.md`](../02-runtime/tracking.md) en [`service-reference.md`](../06-reference/service-reference.md).

## Belangrijkste bestanden

- `MainActivity.kt` — hoofdentrypoint voor de telefoon-UI.
- `CIMDriverApplication.kt` — applicatie-initialisatie.
- `TrackingService.kt` — actieve locatie-tracking en servicelifecycle.
- `TrackingRecoveryManager.kt` — herstelgedrag rond tracking.
- `BootReceiver.kt` — boot- en package-replacement-events.
- `BluetoothReceiver.kt` — Bluetooth connectie- en disconnect-events.
- `AppDatabase.kt` — lokale Room-database.
- `TripRepository.kt` — toegang tot ritgegevens.
- `TripsViewModel.kt` — ritten- en dashboardstate.
- `CIMDriverCarAppService.kt` — Android Auto/Automotive-entrypoint.
- `BackupWorker.kt` en `DataLifecycleWorker.kt` — onderhoudsgerichte achtergrondtaken.

Een volledig bestandsoverzicht staat in [`code-catalog.md`](../06-reference/code-catalog.md).

## Werken aan tracking

Wijzigingen aan tracking verdienen extra voorzichtigheid. Controleer altijd:

- of start, stop en recovery idempotent blijven;
- of de juiste runtime-permissies aanwezig zijn;
- of de foreground notification correct blijft;
- of reboot en package replacement niet tot onbedoelde starts leiden;
- of een actieve rit niet dubbel wordt aangemaakt;
- of fouten zichtbaar en diagnosticeerbaar blijven;
- of locatie- en Bluetooth-uitval gecontroleerd worden afgehandeld.

Test tracking niet alleen vanuit de UI, maar ook met systeemevents en onderbrekingen.

## Werken aan data

Bij wijzigingen aan entities, DAO's of repositories moet rekening worden gehouden met bestaande ritten, classificatieregels, voertuigen, adressen en werkuren. Controleer bij datamodelwijzigingen altijd:

- databaseversie en migraties;
- bestaande records en nullable waarden;
- impact op export en backup;
- queries van ViewModels en widgets;
- gevolgen voor recovery en actieve tracking.

Lees [`data-model.md`](../03-data/data-model.md) en [`data-reference.md`](../03-data/data-reference.md) voordat je het opslagmodel wijzigt.

## Werken aan de UI

Nieuwe of gewijzigde schermen horen bij voorkeur de bestaande scheiding tussen scherm, component, navigatie en ViewModel te volgen. Vermijd database- of servicelogica rechtstreeks in composables wanneer die logica in een ViewModel, repository of domeinutility thuishoort.

Controleer bij UI-wijzigingen:

- navigatieroute en teruggedrag;
- loading-, empty- en errorstates;
- rotatie en lifecyclegedrag;
- Nederlandstalige strings en resourcegebruik;
- impact op widgets of car-schermen;
- toegankelijkheid en bruikbaarheid op verschillende schermgroottes.

## Manifest en permissies

Het manifest bevat onder meer locatie-, Bluetooth-, notificatie-, boot- en foreground-servicepermissies. Componenten zoals receivers, services, widgets en car-integratie hebben verschillende exportinstellingen.

Pas een manifestcomponent nooit alleen aan op basis van de XML-regel. Controleer ook de ontvangende Kotlin-code, de runtime-permissieflow en Android-versieverschillen. Zie [`manifest-permissions.md`](../01-architecture/manifest-permissions.md).

## Teststrategie

Nieuwe ontwikkelaars moeten minimaal onderscheid maken tussen:

- unit-tests voor pure berekeningen en utilities;
- repository- en database-tests;
- ViewModel- en state-tests;
- instrumentatietests voor Android-integratie;
- handmatige tests voor Bluetooth, locatie, boot, notificaties, widgets en car-integratie.

Voor tracking zijn onderbrekingen minstens zo belangrijk als de normale happy path. Test daarom ook process kill, reboot, ontbrekende permissies, Bluetooth-uitval, locatie-uitval en mislukte recovery.

## Diagnostiek

Gebruik het diagnostics-scherm en relevante logregels bij problemen met tracking of achtergrondgedrag. Noteer bij een bug minimaal:

- Android-versie en toestelmodel;
- applicatieversie;
- actieve permissies;
- huidige trackingstatus;
- tijdstip en context van het incident;
- laatste gebruikersactie of systeemevent;
- relevante foutmelding of logregel.

Een probleemrapport zonder lifecycle-context is bij background services vaak onvoldoende om de oorzaak te bepalen.

## Veilige wijzigingsworkflow

1. Lees eerst de relevante themadocumentatie.
2. Zoek de bestaande flow in de broncode voordat je nieuwe logica toevoegt.
3. Bepaal welke andere ingangen dezelfde functionaliteit kunnen aanroepen.
4. Maak de kleinst mogelijke wijziging.
5. Voeg of actualiseer tests.
6. Bouw de app en controleer lint- en compileerfouten.
7. Test de normale flow én de fout- en herstelpaden.
8. Werk de documentatie bij wanneer gedrag, configuratie of architectuur verandert.

## Definition of done

Een wijziging is pas klaar wanneer:

- de code compileert;
- relevante tests slagen;
- lifecycle- en foutpaden zijn gecontroleerd;
- permissies en manifestimpact zijn beoordeeld;
- UI-teksten en resources correct zijn bijgewerkt;
- documentatie en code niet tegenstrijdig zijn;
- logging geen gevoelige gegevens lekt;
- eventuele migratie- of release-impact is vastgelegd.

## Waar hulp te vinden is

Begin bij [`docs/README.md`](../README.md) voor de volledige documentatie-index. Gebruik daarna het referentiedocument van het betrokken package en raadpleeg de codecatalogus om alle verwante bestanden te vinden.
