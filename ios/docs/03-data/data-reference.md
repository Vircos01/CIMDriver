# Data-referentie

## Doel

Deze referentie beschrijft welke gegevensvelden, validatieregels en conventies per hoofdentiteit gelden. Daarmee ontstaat een stabiele basis voor repositories, use-cases, SwiftUI viewmodels en exportlogica.

## Algemene regels

- Timestamps worden intern uniform opgeslagen en in de presentatielaag gelokaliseerd weergegeven.
- Handmatige wijzigingen blijven herleidbaar via bron- of mutatiemetadata.
- Optionele velden worden alleen gebruikt wanneer functioneel echt relevant.
- Migraties moeten backwards compatible zijn met bestaande lokale data.

## Trip

Vast te leggen velden:

- Unieke identifier.
- Start- en eindtijd.
- Start- en eindlocatie of referentie daarnaar.
- Afstand, duur en optionele route-informatie.
- Classificatie en bron van classificatie.
- Status zoals concept, actief, afgerond of gecorrigeerd.
- Gekoppeld voertuig.

## WorkSession

Vast te leggen velden:

- Unieke identifier.
- Werkstart, pauze-intervallen en eindtijd.
- Automatisch of handmatig bepaald.
- Tolerantieregels of toegepaste correcties.
- Notitie of toelichting waar nodig.

## AddressProfile

Vast te leggen velden:

- Naam of label.
- Type, zoals thuis, kantoor, klant of overig.
- Coördinaten of geofence-definitie.
- Detectieradius.
- Prioriteit voor classificatie.

## VehicleProfile

Vast te leggen velden:

- Naam en beschrijving.
- Nummerplaat indien beschikbaar.
- Gekoppelde bluetooth-identifiers of herkenningssleutels.
- Startkilometer of referentiewaarden voor rapportage.

## UserSettings

Vast te leggen velden:

- Defaultclassificatie of fallbackgedrag.
- Werkurenvoorkeuren en toleranties.
- Meldingsinstellingen.
- Exportvoorkeuren.
- Eventuele featureflags voor experimentele functies.

## Richtlijn

Werk per entiteit later een concrete veldmatrix uit zodra de definitieve persistencekeuze en naamgeving vastligt. Gebruik dit document als contract tussen data-opslag, domeinlogica en UI-projectie.
