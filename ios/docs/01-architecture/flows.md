# Procesflows

## Doel

Deze procesflows beschrijven de belangrijkste SwiftUI- en runtimepaden van de iOS-app. Ze verbinden gebruikersinteractie, viewmodel-logica, domeinservices en platformevents in één samenhangend model.

## 1. Appstart en dependency bootstrapping

1. `CIMDriverApp` start op.
2. `AppContainer` initialiseert persistence, repositories, services en coordinators.
3. `AppSessionState` laadt globale context zoals permissies, instellingen en actieve trackingstatus.
4. De root `TabView` wordt opgebouwd met feature-rootviews.
5. Feature-viewmodels voeren hun initiële laadacties uit.

## 2. Onboarding en permissieflow

1. Gebruiker doorloopt onboarding-schermen in SwiftUI.
2. Een onboarding viewmodel bepaalt welke toestemmingen wanneer worden uitgevraagd.
3. Resultaten van permissieverzoeken worden opgeslagen in appbrede state en diagnostiek.
4. De app schakelt door naar operationele schermen zodra minimale permissies of fallbackpaden beschikbaar zijn.

## 3. Ritdetectieflow

1. Een platformadapter ontvangt locatie-, bluetooth- of voertuigcontext.
2. `TrackingCoordinator` vertaalt dit naar domeingebeurtenissen.
3. De ritdetectie use-case bepaalt of een ritkandidaat of actieve rit moet ontstaan.
4. Repository en diagnostiek worden bijgewerkt.
5. Gedeelde trackingstate en relevante SwiftUI-schermen verversen.
6. Optioneel volgt een notificatie of widgetupdate.

## 4. Ritenclassificatieflow

1. Een afgeronde rit komt in de status `CompletedPendingClassification`.
2. De classificatie use-case combineert adressen, tijdvensters, bekende locaties en gebruikersinstellingen.
3. Het resultaat wordt opgeslagen als zakelijk, privé of woon-werk, eventueel met een handmatige override.
4. Dashboard- en lijstfeatures ontvangen vernieuwde data via repository-observatie of refresh.

## 5. Werkurenflow

1. Locatie- en ritcontext leveren signalen voor werkstart of werkstop.
2. De werkuren use-case past toleranties, pauzeregels en bekende locaties toe.
3. Een work session repository bewaart tussenstanden en correcties.
4. Het werkurenoverzicht en dashboard tonen de bijgewerkte samenvatting.

## 6. Navigatieflow in SwiftUI

- Hoofdnavigatie verloopt via `TabView`.
- Detailschermen lopen via `NavigationStack` en typed destinations.
- Modale interacties zoals filters, instellingen of handmatige correcties verlopen via sheets en alerts.
- Deep links of notificaties landen eerst in een coordinator, pas daarna in een specifieke viewroute.

## 7. Herstelflow na background of app-herstart

1. De app wordt actief of een geplande task triggert herstel.
2. Startup use-cases inspecteren persistente checkpoints.
3. Lopende ritten of werkuren worden gereconstrueerd of veilig afgesloten.
4. Gedeelde state en feature-viewmodels synchroniseren met de herstelde waarheid.
5. De UI toont eventueel een waarschuwing of diagnostische melding wanneer automatisch herstel beperkt was.

## 8. Exportflow

1. Gebruiker start export vanuit instellingen of een overzichtsscherm.
2. Het export viewmodel verzamelt filters en formaatkeuze.
3. Een export use-case leest data uit repositories, transformeert deze naar CSV of PDF en schrijft een bestand weg.
4. De UI toont afronding, foutstatus of deelopties.

## Richtlijn

Gebruik deze flows als bron voor sequence diagrams, UI-statevalidatie, testscenario's en acceptance criteria. Elke flow moet zowel de SwiftUI route als de onderliggende domein- en infrastructuurstappen blijven benoemen.
