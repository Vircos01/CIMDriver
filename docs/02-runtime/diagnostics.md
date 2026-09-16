# Diagnostiek

Diagnostiek is een expliciet onderdeel van CIMDriver, geïntroduceerd om de stabiliteit en voorspelbaarheid van automatische ritregistratie in een complexe (Android-beperkte) omgeving te garanderen. Er is een toegewijd `DiagnosticsScreen` toegevoegd om operationele status en foutinformatie direct inzichtelijk te maken voor ontwikkelaars én (pro)gebruikers.

## Doel van diagnostiek

In een applicatie met foreground tracking, boot recovery, Bluetooth-events en car-integratie is diagnosevermogen essentieel. Omdat het Android-systeem services soms ongevraagd afsluit ter optimalisatie van batterijgebruik (Doze mode, process limits), is het noodzakelijk om transparant te maken *waarom* een rit is afgebroken of niet is opgestart. Het `DiagnosticsScreen` bevestigt dat deze app ontworpen is met aandacht voor beheerbaarheid en probleemopvolging.

## Verwachte inhoud en de UI

Het `DiagnosticsScreen` is ontworpen om in één oogopslag de algehele "gezondheid" van het systeem en de achtergrondprocessen inzichtelijk te maken. Waar het voorheen exclusief was gekoppeld aan de `TrackingStatusStore`, fungeert het nu als een dashboard dat samenkomt in de `DiagnosticsViewModel`.

Via de `DiagnosticsViewModel` worden vier verschillende categorieën uitgelezen en weergegeven in afzonderlijke overzichtskaarten:

### 1. App & Systeem Info
- **App Versie:** De exacte actieve build versie (`BuildConfig.VERSION_NAME`).
- **Batterij Optimalisatie:** Achtergrondtracking via GPS stelt hoge eisen aan Android's resource-management. Deze check valideert via `PowerManager` of de app is uitgesloten van Doze mode/batterij-optimalisaties. Staat dit nog aan, dan toont het systeem direct een waarschuwing.

### 2. Tracking Service (Live Status)
Het leest rechtstreeks (en reactief) de singleton `TrackingStatusStore` uit.
- **Huidige State:** De exacte `ServiceState` (bijv. `RUNNING`, `FAILED`, `RECOVERING`).
- **Bericht:** Een door het systeem vertaalde statusboodschap.
- **Actieve Rit ID:** Als er een rit actief is, wordt deze referentie direct getoond.
- **GPS Nauwkeurigheid:** Gedurende de rit toont het direct de afwijking in meters, essentieel voor het opsporen van "lege" ritten bij onvoldoende dekking.
- **Laatste Fout:** Mocht de state naar `FAILED` gaan, bevat dit veld de technische oorzaak (bijv. `SecurityException` of netwerk timeout).

### 3. Actieve Werkdag
Voor de urenregistratie is het van cruciaal belang te zien in welke status het systeem verkeert.
- **Status:** Checkt via `WorkDayDao` of de gebruiker momenteel geregistreerd staat als 'ingelogd' bij een kantoor of klant.
- **Locatie & Aankomsttijd:** Toont het gekoppelde label en het precieze tijdstip van inloggen, zodat je altijd visuele bevestiging hebt dat de werkuren op de achtergrond lopen, ook zonder dat GPS op dat moment meedraait.

### 4. Boot Recovery & Failsafe
Integratie met `TrackingRecoveryManager`.
- **Wachtrij Status:** Toont direct of er een afgebroken of onvolledige rit klaar staat voor automatisch herstel.
- **Herstelpogingen:** Houdt bij hoe vaak het systeem heeft geprobeerd deze te herstellen om te voorkomen dat de app oneindig vast blijft zitten in een crash-loop.

Deze diagnostische UI kan worden geopend via het Instellingenmenu of via de pop-up statusindicator op het Dashboard en is een krachtige tool gebleken voor zowel beheerders als probleemoplossing bij eindgebruikers.
