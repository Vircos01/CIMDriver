# Manifest en permissies

Het Android-manifest beschrijft de externe contracten van de applicatie: permissies, componentdeclaraties, providers, car-metadata en systeemintegratie. Voor CIMDriver is het manifest een cruciaal document omdat tracking, recovery, widgets en car-integratie rechtstreeks afhankelijk zijn van correcte manifestconfiguratie.

## Gedeclareerde permissies

De app declareert permissies voor Bluetooth, locatie, achtergrondlocatie, foreground services, notificaties, boot-events en netwerktoegang. Deze combinatie past functioneel bij automatische ritregistratie met systeemintegratie.

| Permissie | Doel |
|---|---|
| `BLUETOOTH` en `BLUETOOTH_ADMIN` met `maxSdkVersion=30` | Ondersteuning voor oudere Android-versies met legacy Bluetooth-permissiemodel. |
| `BLUETOOTH_CONNECT` | Verbinden met en uitlezen van Bluetooth-apparaten op nieuwere Android-versies. |
| `ACCESS_FINE_LOCATION` en `ACCESS_COARSE_LOCATION` | Locatiebepaling voor tracking en adres-/routefunctionaliteit. |
| `ACCESS_BACKGROUND_LOCATION` | Locatiegebruik buiten zichtbare foreground-UI voor automatische ritregistratie. |
| `FOREGROUND_SERVICE` en `FOREGROUND_SERVICE_LOCATION` | Toestaan van een foreground location-service, essentieel voor stabiele tracking op de achtergrond. |
| `POST_NOTIFICATIONS` | Notificaties voor actieve tracking en het synchroniseren van de status naar de gebruiker. |
| `RECEIVE_BOOT_COMPLETED` | Reageren op herstartscenario's (Device boot/reboot) om ritten automatisch te herstellen via de `BootReceiver`. |
| `INTERNET` en `ACCESS_NETWORK_STATE` | Externe netwerkfunctionaliteit zoals geocoding en routeberekening. |

## Queries

De app declareert query-intents voor `geo` en `google.navigation`, wat erop wijst dat externe kaart- of navigatie-integratie (zoals het direct openen van een routebeschrijving vanuit het adresboek) deel uitmaakt van het platformcontract.

## Applicatieniveau

De `application`-declaratie gebruikt `CIMDriverApplication`, ondersteunt backup- en data-extraction-regels, verbiedt cleartext-verkeer en verwijst naar eigen thema- en iconresources. Daarmee zijn backupgedrag, netwerkbeleid en branding op manifestniveau vastgelegd.

## Activities, services en receivers

`MainActivity` is de launcher-activity en is geëxporteerd zoals vereist voor een startpunt van de app. 

De `TrackingService` is **niet** geëxporteerd (veiligheid) en is gedeclareerd als `location` foreground service. Dit garandeert dat Android de service de benodigde prioriteit en locatietoegang geeft.

De manifestdeclaraties tonen verder diverse receivers. Een belangrijk architecturaal en security-principe (doorgevoerd in Fase 5) is dat geëxporteerde receivers zoals `BluetoothReceiver` en `BootReceiver` in de Kotlin-code hun inkomende `Intent.action` streng valideren tegen een vastgestelde whitelist (bijv. `Intent.ACTION_BOOT_COMPLETED`). Dit voorkomt intent spoofing door externe, mogelijk malafide, apps op het toestel.

Overige receivers: `NotificationActionReceiver`, `GeofenceBroadcastReceiver`, en widget-receivers maken de app bereikbaar vanuit systeemevents en appwidget-updates.

## Providers en metadata

De app gebruikt een `FileProvider` met een authority op basis van `${applicationId}.fileprovider`, niet geëxporteerd en met URI-permissieverlening (voor veilige PDF- en CSV-export). Daarnaast zijn metadata aanwezig voor Android Auto, Android Automotive en minimale car API-versie.

## Documentatierichtlijnen

Voor toekomstig beheer moet per component worden vastgelegd waarom deze geëxporteerd of niet geëxporteerd is, welke intent-actions worden geaccepteerd en gecontroleerd, welke runtime-permissies nodig zijn, en welke platformversies speciale behandeling vereisen. Manifestdocumentatie en strikte security checks zijn in deze app geen bijzaak maar onderdeel van de robuuste runtime-architectuur.
