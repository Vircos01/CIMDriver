# Integratiereferentie

## Doel

Deze referentie beschrijft hoe Apple-frameworks en externe diensten in de iOS-architectuur worden opgenomen. Integraties worden altijd achter adapters en protocollen geplaatst, zodat SwiftUI-features en domeinservices los blijven van frameworkdetails.

## Relevante integraties

| Integratie | Rol in architectuur |
| :--- | :--- |
| Core Location | Locatie-updates, regio's en achtergrondsignalen |
| Core Bluetooth | Detectie van voertuigcontext of gekoppelde bronnen |
| UserNotifications | Statusmeldingen en snelle acties |
| BGTaskScheduler | Herstel, onderhoud en geplande achtergrondverwerking |
| CLGeocoder of externe fallback | Adresresolutie en herkenning |
| CarPlay | Voertuigervaring via aparte scene |
| Bestands- en deelservices | Export van CSV of PDF |

## Adapterpatroon

Elke integratie hoort een adapterlaag te hebben met:

- Protocoldefinitie voor domein of applicatielaag.
- Concreet frameworkspecifiek implementatietype.
- Mapping van systeemtypes naar domeinmodellen of events.
- Duidelijk fout- en fallbackgedrag.

## Richtlijn

Voorkom dat viewmodels frameworktypes zoals `CLLocation`, `CBPeripheral` of notificatieobjecten direct consumeren. Vertaal die eerst naar CIMDriver-specifieke modellen of intenties.
