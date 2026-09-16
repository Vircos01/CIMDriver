# Android Auto en Automotive

CIMDriver bevat expliciete en native ondersteuning voor Android Auto en Android Automotive OS. Het manifest bevat hiervoor de vereiste metadata, een minimale car API-leveldeclaratie en een geëxporteerde `CIMDriverCarAppService`. Dit stelt de gebruiker in staat om via het display van de auto basisacties uit te voeren met betrekking tot hun ritregistratie.

## Car-componenten

De codebasis implementeert een aparte, specifieke UI-laag voor in de auto, gestuurd door de `androidx.car.app` library. De flow begint bij de `CIMDriverCarAppService` en `CIMDriverSession`, die de onderliggende schermen aansturen:
- `MainCarScreen`: Het hoofdmenu en overzicht.
- `StartTripScreen` / `ReviewTripScreen`: Schermen voor het starten of handmatig goedkeuren (classificeren) van een net beëindigde rit.
- `RecentTripsCarScreen`: Een lijst van de laatst gemaakte ritten.
- `AddressBookCarScreen`: Bekijken van bekende, gekoppelde adressen (zoals klanten of thuislocaties) en de mogelijkheid om navigatie direct naar deze punten te starten via de ingebouwde navigatie-apps van de auto.

## Integratieniveau

De combinatie van manifestmetadata voor projected Android Auto en built-in Android Automotive OS toont dat de app zich niet beperkt tot één automodus. Daarmee is auto-integratie een volwaardig productspoor en geen experimenteel zijpad. De logica van de Car app leunt zwaar op dezelfde achterliggende classificatie-utilities (Dynamics-rittypes) en Room-database queries als de hoofd-app, wat zorgt voor een consistente ervaring tussen de smartphone- en auto-interface.
