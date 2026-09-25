# ViewModel- en state-referentie

## Doel

Dit document beschrijft hoe SwiftUI-viewmodels opgebouwd worden en welke statepatronen consequent door de app gebruikt moeten worden. Daarmee blijven featureflows voorspelbaar, testbaar en consistent over alle schermen heen.

## Kernverantwoordelijkheden van een viewmodel

- Laden van data via use-cases of queryservices.
- Vertalen van domeinresultaten naar presentabele screen state.
- Afhandelen van gebruikersintenties zoals refresh, filter, save of correctie.
- Publiceren van UI-events zoals alerts, sheets of navigatie-intenties.
- Bewaken van loading-, error-, empty- en success-states.

## Aanbevolen opbouw

```swift
@MainActor
final class TripsViewModel: ObservableObject {
    @Published private(set) var state: TripsScreenState

    func onAppear()
    func refresh()
    func applyFilter(_ filter: TripFilter)
    func openTrip(id: Trip.ID)
    func correctTrip(id: Trip.ID)
}
```

## Screen state patroon

Gebruik bij voorkeur één samengestelde `ScreenState` per scherm in plaats van veel losse booleans. Dat voorkomt inconsistente combinaties zoals tegelijk laden en fout tonen zonder duidelijke prioriteit.

### Minimale onderdelen

- Inhoud of view data.
- Loadingstatus.
- Refreshstatus.
- Empty reason.
- Herstelbare foutstatus.
- Eventuele transient UI-events.

## UI-events versus persistente state

Niet alles hoort in persistente viewstate. Gebruik een apart eventkanaal voor eenmalige gebeurtenissen zoals toast-achtige meldingen, sheetpresentatie of deep link routing.

## Samenwerking met SwiftUI

- Root views bezitten hun viewmodel meestal via `@StateObject`.
- Child views krijgen alleen de state of bindbare substate die ze nodig hebben.
- Viewmodels blijven `@MainActor` wanneer ze UI-publicaties doen.
- Async werk wordt gedelegeerd aan use-cases, niet in views opgebouwd.

## Testbaarheid

Iedere viewmodelactie moet in isolatie te testen zijn met mocks of in-memory repositories. Valideer vooral state-overgangen, foutpaden en race conditions bij async refresh of herstel.
