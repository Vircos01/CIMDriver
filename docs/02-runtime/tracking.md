# Tracking en herstel

Tracking is het kernproces van CIMDriver. De bronstructuur en het manifest tonen een combinatie van een foreground service, robuuste boot-recoverylogica, systeemreceivers, en een gecentraliseerde statusopslag (`TrackingStatusStore`) die samen de automatische ritregistratie mogelijk maken.

## Kerncomponenten

| Component | Verantwoordelijkheid |
|---|---|
| `TrackingService` | Uitvoeren van actieve tracking met locatie-updates, dynamische notificaties via StateFlow, en statusovergangen in de `TrackingStatusStore`. |
| `TrackingRecoveryManager` | Coördineren van de back-off logica en herstelgedrag (via `WorkManager`) na onderbrekingen. Leest `maxRecoveryAttempts` uit de database. |
| `TrackingStatusStore` | Singleton object met een `MutableStateFlow` die als Single Source of Truth dient voor de realtime status (bijv. `RUNNING`, `FAILED`). |
| `BootReceiver` | Reageert op geautoriseerde `BOOT_COMPLETED` intents en activeert de recovery cyclus. |
| `BluetoothReceiver` | Detecteren van gevalideerde connect- en disconnect-events van gekoppelde Bluetooth carkits. |
| `GeofenceManager` | Verwerken en beheren van geofencegerelateerde trackingcontext (bijv. bij parkeren). |
| `NotificationActionReceiver` | Verwerken van acties die via notificaties worden gestart (zoals geforceerd stoppen). |

## Service-initialisatie

De `TrackingService` initialiseert in `onCreate` een database, een `LocationManager`, een notification channel, en start direct een eigen `serviceScope` op. In deze coroutine scope wordt de `TrackingStatusStore.status` StateFlow geobserveerd, zodat de foreground notificatie altijd perfect synchroon blijft met de interne staat van de service, en direct waarschuwt bij fouten (zoals wegvallende GPS).

## Service-acties

Binnen `onStartCommand` verwerkt de service acties als `START_TRACKING` en `RECOVER_TRACKING`. Voor beide paden is idempotentie ingebouwd door de `TrackingStatusStore` te controleren (geen dubbele acties als de status al `RUNNING` of `STARTING` is). 

Wanneer een rit wordt gestart, vraagt de service actieve GPS-updates op, brengt zichzelf naar foreground-modus, en verplaatst de state naar `RUNNING`. Bij fouten slaat de service een `errorCause` op in de status en bouwt deze de resources veilig af.

## Notificatiegedrag

Dankzij de implementatie in Fase 5 wordt de notificatie volledig reactief aangestuurd vanuit de `TrackingStatusStore`. Gebruikers krijgen realtime te zien of er een probleem is, en de "Systeemdiagnostiek"-schermen luisteren naar exact dezelfde StateFlow, wat duplicatie in logica voorkomt.

## Triggers voor tracking

Tracking wordt event-gedreven aangestuurd. Mogelijke triggers:
- Handmatig in de UI.
- Een `BluetoothReceiver` connect-event met een bekende carkit.
- Een geofence trigger.
- Een `BootReceiver` event in geval van herstel na uitval van het toestel.

De uitlooptijd (grace period) voor Bluetooth-disconnects en stilstaan kan via de `Settings` database dynamisch worden geconfigureerd.

## Recovery

Recovery is ontworpen met betrouwbaarheid en systeemvriendelijkheid in gedachten. Het werkt als volgt:
1. De `BootReceiver` vangt de trigger op (en valideert de Intent).
2. De `TrackingRecoveryManager` checkt in de Room Database de dynamische instelling `maxRecoveryAttempts`.
3. Er wordt een exponentiële back-off worker gepland in de `WorkManager` (om te voorkomen dat de app oneindig crasht en in een boot-loop raakt als bijv. locatierechten zijn ingetrokken).
4. Zolang de limiet niet bereikt is, zal de Worker een veilige doorstart van de `TrackingService` proberen.

Dit gecentraliseerde design zorgt ervoor dat systeemfouten en appcrashes veel beter kunnen worden afgevangen en geanalyseerd, en het voorkomt "onzichtbare" falende achtergrondservices.
