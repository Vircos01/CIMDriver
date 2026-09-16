# Overzicht

CIMDriver is een Android-applicatie voor automatische ritregistratie, werkurenondersteuning en gerelateerde voertuig- en adresfunctionaliteit. De applicatie combineert een foreground trackingservice, lokale gegevensopslag, dashboards, widgets en auto-integratie binnen één Android-project.

## Productdoel

De bestaande productbeschrijving geeft aan dat ritten automatisch worden gestart bij relevante Bluetooth-verbindingen, worden beëindigd bij ontkoppeling en daarna slim worden geclassificeerd. Daarnaast ondersteunt de app geautomatiseerde werkuren, dashboardinzichten en lokale privacyvriendelijke opslag met exportmogelijkheden.

## Belangrijkste functionele domeinen

De repositorystructuur laat zien dat de applicatie de volgende hoofdgebieden bevat:

- Tracking en herstel via services, receivers en statusstores.
- Databeheer via Room-componenten, repositories en utilities.
- Gebruikersinterface via Compose-schermen, componenten, dialogs en ViewModels.
- Widgets voor homescreengebruik.
- Android Auto en Automotive-interfaces via aparte car-componenten.
- Achtergrondtaken via workers voor backup en data lifecycle.

## Reikwijdte van deze documentatie

Deze documentatie is bedoeld als centrale referentie voor ontwikkelaars en beheerders die de applicatie willen begrijpen, onderhouden of uitbreiden. De documenten beschrijven zowel functioneel gedrag als technische structuur, zodat de codebasis niet alleen leesbaar is in broncodevorm maar ook in samenhang verklaard wordt.
