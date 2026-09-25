# State- en lifecyclemodellen

## Doel

Deze pagina beschrijft hoe SwiftUI-state, app lifecycle en tracking lifecycle op elkaar aansluiten. Voor CIMDriver is dat cruciaal, omdat schermstatus, background execution en systeemevents elkaar direct beïnvloeden.

## 1. SwiftUI state-eigenaarschap

### View state

Gebruik lokale `@State` alleen voor tijdelijke UI-details, zoals een geselecteerde filter, actieve sheet of inline validatiemelding. Deze state mag geen bron zijn voor bedrijfslogica of trackingstatus.

### Feature state

Elke hoofdfeature krijgt een eigen `@StateObject`-gedragen viewmodel of feature store. Die bevat onder meer:

- Loaded data voor het scherm.
- Afgeleide statistieken of samenvattingen.
- UI flags zoals loading, refreshing en error states.
- Actions die intenties doorzetten naar use-cases.

### App-brede state

Cross-feature state, zoals permissiestatus, trackingstatus of actieve ritcontext, wordt beheerd in een hoger object zoals `AppSessionState` of `TrackingSharedState`. Gebruik dit spaarzaam, zodat niet elk scherm impliciet van globale state afhankelijk wordt.

## 2. App lifecycle

De iOS app lifecycle kent minimaal de toestanden actief, inactief, achtergrond en beëindigd. Iedere overgang moet duidelijk maken welke SwiftUI-state vluchtig is en welke runtime-state duurzaam bewaard moet worden.

| Appstatus | Verwacht gedrag |
| :--- | :--- |
| Actief | Views renderen live status, permissiewijzigingen en trackingfeedback direct. |
| Inactief | Tijdelijke overgang, geen zware statusmutaties tenzij systeem dat afdwingt. |
| Achtergrond | Checkpoints opslaan, observers beperken, background tasks zorgvuldig plannen. |
| Beëindigd | Alleen persistente state blijft over; herstel moet via repositories en startup use-cases lopen. |

## 3. Tracking lifecycle

De trackinglaag moet los staan van individuele schermen. Een aanbevolen model is:

- **Idle** — Geen actieve ritkandidaat.
- **CandidateDetected** — Er is voldoende context om een rit te vermoeden.
- **TrackingActive** — Een rit loopt en checkpoints worden bijgewerkt.
- **TemporarilyPaused** — Signalen vallen tijdelijk weg of de gebruiker wacht binnen een grace period.
- **CompletedPendingClassification** — De rit is gestopt maar nog niet definitief geclassificeerd.
- **Persisted** — De rit is opgeslagen en zichtbaar voor UI, export en dashboard.
- **RecoveryNeeded** — Appstart of systeemevent vereist reconstructie van een niet-afgeronde rit.

## 4. Work session lifecycle

Werkuren volgen een parallelle state machine die wel door ritten beïnvloed kan worden, maar niet identiek is aan rittracking:

- Niet gestart.
- Actief.
- Gepauzeerd.
- Afgerond.
- Gecorrigeerd.

Deze state moet ook handmatige correcties ondersteunen zonder automatische herberekening ongewenst alles terugdraait.

## 5. ViewModel statusmodel

Ieder SwiftUI feature-viewmodel krijgt idealiter een uniforme statusopbouw, bijvoorbeeld:

```swift
struct ScreenState<Value> {
    var value: Value?
    var isLoading: Bool
    var isRefreshing: Bool
    var error: ScreenError?
    var emptyReason: EmptyStateReason?
}
```

Deze aanpak voorkomt versnipperde booleans en maakt states als laden, leeg, fout en gevuld consistent over alle features.

## 6. Eventverwerking

Gebruikersacties en systeemevents worden als intenties behandeld. Voorbeelden:

- Gebruiker opent ritdetail.
- App ontvangt nieuwe locatie.
- Bluetoothbron wordt verbonden.
- BGTask start herstelroutine.
- Gebruiker wijzigt permissies in Settings.

Iedere intentie gaat via viewmodel of coordinator naar use-cases; views muteren niet direct repositories of platformadapters.

## 7. Herstel na onderbreking

SwiftUI-viewstate mag verloren gaan zodra een scene wordt herbouwd. Daarom moet herstel altijd leunen op persistente state zoals:

- Actieve ritcheckpoint.
- Laatste bekende trackingstatus.
- Openstaande classificatie.
- Werkdag in voortgang.
- Laatste permissiesnapshot en diagnostische hints.

## 8. Richtlijnen

- Houd feature-state klein en doelgericht.
- Bewaar domeinwaarheid buiten views.
- Maak lifecycle-overgangen expliciet testbaar.
- Vermijd globale mutable state zonder eigenaar.
- Modelleer lege, fout- en herstelstates als first-class states.
