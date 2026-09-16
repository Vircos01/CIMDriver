# Teststrategie

Dit document beschrijft hoe CIMDriver getest moet worden op functioneel, technisch en operationeel niveau. Omdat de applicatie gebruikmaakt van achtergrondservices, systeemevents, lokale opslag, widgets en car-integratie is een combinatie van geautomatiseerde en handmatige tests noodzakelijk.

## Testlagen

De minimale testpiramide voor CIMDriver bestaat uit:

- unit-tests voor utilities, classificatie, tijdslogica, berekeningen en normalisatie;
- database- en repositorytests voor Room-query's, entiteiten en opslaggedrag;
- ViewModel-tests voor state, filtering, gebruikersacties en afgeleide data;
- instrumentatietests voor Android-integraties zoals services, receivers, notificaties en widgets;
- handmatige systeemtests voor Bluetooth, locatie, reboot, recovery en Android Auto/Automotive.

## Kernscenario's

### Tracking

- Start tracking via UI.
- Start tracking via Bluetooth-event.
- Stop tracking via disconnect.
- Stop tracking handmatig.
- Verifieer dubbele startaanroepen.
- Verifieer correcte foreground notification.
- Controleer opslag van locatiepunten en ritmetadata.

### Recovery

- Herstart toestel tijdens actieve rit.
- Herstart app-proces tijdens actieve rit.
- Trigger `MY_PACKAGE_REPLACED` tijdens opgeslagen state.
- Start recovery zonder permissies.
- Start recovery met corrupte of ontbrekende state.
- Controleer dat recovery geen dubbele ritten produceert.

### Data

- Voeg ritten, adressen, voertuigen en werkuren toe.
- Controleer filtering, sortering en verwijdering.
- Test export- en backupgedrag.
- Controleer dat databasewijzigingen geen bestaande data breken.

### UI

- Controleer loading-, empty- en errorstates.
- Controleer schermnavigatie, teruggedrag en deeplink-achtige routes.
- Controleer weergave van dashboard, detailpagina's en formulieren.
- Controleer Nederlandstalige resources.

### Integraties

- Controleer widgets na update en na reboot.
- Controleer Android Auto/Automotive-schermen.
- Controleer workers voor backup en datalifecycle.
- Controleer notificatieacties en systeemreceivers.

## Acceptatiecriteria

Een wijziging mag pas als stabiel worden beschouwd wanneer de relevante unit-, repository-, ViewModel- en integratietests zijn uitgevoerd en de bijbehorende handmatige kritieke scenario's zijn gevalideerd. Voor tracking en recovery geldt dat fout- en onderbrekingspaden expliciet meegenomen moeten worden, niet alleen de normale flow.
