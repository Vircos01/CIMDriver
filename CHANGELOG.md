# Changelog

Alle noemenswaardige wijzigingen aan dit project zullen in dit bestand worden bijgehouden.

## [1.0.4] - 2026-09-16

### Toegevoegd
- **Instelbare Vergoeding:** De zakelijke vergoeding per kilometer is nu instelbaar via de instellingenpagina en wordt direct doorgerekend op het dashboard.

## [1.0.3] - 2026-09-16

### Opgelost
- **Swipe Acties:** Bij het naar rechts swipen van een rit in het Ritten-overzicht wordt er nu netjes een pop-up menu geopend waarin je direct een keuze kunt maken (Zakelijk, Privé of Woon-werk).

## [1.0.2] - 2026-09-16

### Toegevoegd
- **Systeemdiagnostiek Uitbreiding:** Het diagnostisch scherm is flink uitgebreid met live batterij-optimalisatie controles, app-versie, actieve werkdag-status en recovery manager failsafe status.
- **In-App Updater:** De app checkt nu automatisch via GitHub of er een nieuwere versie beschikbaar is en toont een handige download-knop op het dashboard.

## [1.0.1] - 2026-09-16

### Toegevoegd
- **Systeemdiagnostiek:** Nieuw diagnostisch scherm voor direct inzicht in de state van de background trackingservice.
- **Boot Recovery:** Robuuste automatische hervatting van tracking na reboots of crashes, inclusief veilige back-off timers.

### Gewijzigd
- **Adresherkenning:** De maximale zoekradius voor proximity matching (kantoorpanden) is vergroot van 100m naar 250m om GPS-drift binnen panden beter te ondervangen.

### Opgelost
- **Deprecation:** Oplossing voor Material 3 deprecation waarschuwingen (vervanging van `Icons.Default.ArrowBack` door `Icons.AutoMirrored.Filled.ArrowBack`).

## [1.0.0] - 2026-09-16

### Toegevoegd
- **Eerste officiële release van CIMDriver!**
- Volledig geautomatiseerde ritregistratie via Bluetooth triggers.
- Automatische urenregistratie voor kantoor- en klantbezoeken.
- Slimme adres-classificatie (Thuis, Werk, Klant) via Room-database.
- Modern Material 3 (Compose) Dashboard en overzichten.
- Volledige, gestructureerde referentie- en architectuurdocumentatie.
