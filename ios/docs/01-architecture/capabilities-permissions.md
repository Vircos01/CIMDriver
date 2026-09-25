# Capabilities en permissies

## Doel

Dit document beschrijft welke iOS-capabilities, entitlements en runtime-permissies nodig zijn voor de SwiftUI-architectuur van CIMDriver. Het doel is om systeemtoegang expliciet te modelleren als onderdeel van de apparchitectuur, niet als losse implementatiedetails in views.

## Architectuurprincipe

Permissies worden gecentraliseerd afgehandeld via een `PermissionCoordinator` of vergelijkbare service in de app- en infrastructuurlaag. SwiftUI-views vragen nooit direct permissies op zonder tussenliggende coordinator of use-case, zodat flows testbaar blijven en dezelfde logica herbruikbaar is in onboarding, instellingen en herstelpaden.

## Verwachte permissies

| Permissie of capability | Doel binnen CIMDriver | Architectuurplaats |
| :--- | :--- | :--- |
| Locatie tijdens gebruik | Ritdetectie en kaartcontext wanneer app actief is | Location adapter + onboarding flow |
| Altijd locatie | Achtergrondtracking en herstel van ritcontext | Location adapter + permission coordinator |
| Bluetooth | Herkennen van voertuig- of carkitcontext | Bluetooth adapter |
| Notificaties | Trackingstatus, herstelmeldingen en snelle acties | Notification service |
| Background modes | Verwerking van locatie, background refresh of taken | App target capabilities + background coordinators |
| CarPlay entitlement | Voertuigervaring op ondersteunde flows | Integratielaag + aparte scene |

## SwiftUI flow voor permissies

### 1. Onboarding

Een SwiftUI onboarding feature laat per stap zien waarom een permissie nodig is, welke waarde die toevoegt en wat het fallbackgedrag is als de gebruiker weigert. Het viewmodel bepaalt de volgorde en leest actuele autorisatiestatus via infrastructuurprotocollen.

### 2. Instellingen en herstel

Instellingenschermen tonen de actuele permissiestatus en leiden gebruikers indien nodig naar systeeminstellingen. Na terugkeer naar de app synchroniseert `AppSessionState` de gewijzigde status, waarna relevante features opnieuw laden.

### 3. Runtimebewaking

Wanneer permissies tijdens gebruik veranderen, publiceert de permission coordinator events die viewmodels kunnen vertalen naar waarschuwingen, banners of beperkte functionaliteit. Zo blijft de UI declaratief en hoeft geen scherm zelf permissiestatus te pollen.

## Voorgestelde componenten

- `PermissionCoordinator` voor centrale statusopvraging en permissieverzoeken.
- `LocationAuthorizationAdapter` voor Core Location autorisatiestatus.
- `BluetoothAuthorizationAdapter` voor bluetoothstatus.
- `PermissionBannerViewModel` voor UI-waarschuwingen bij ontbrekende toegang.
- `PermissionStatus` domeinmodel voor uniforme representatie in alle features.

## Info.plist en capabilities

Leg per capability vast:

- Welke user-facing uitleg in Info.plist staat.
- Welke feature of use-case afhankelijk is van die capability.
- Welk fallbackpad bestaat als toestemming ontbreekt.
- Welke App Store review-risico's of privacy-impact gelden.

## Reviewpunten

- Eén centrale bron van waarheid voor permissiestatus.
- Geen permissielogica verstopt in losse SwiftUI views.
- Heldere scheiding tussen permissie-aanvraag, statusweergave en fallbackgedrag.
- Diagnostiek logt statusovergangen zonder privacygevoelige details onnodig vast te leggen.
