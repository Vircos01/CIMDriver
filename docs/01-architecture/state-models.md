# State- en lifecyclemodellen

Dit document beschrijft de belangrijkste runtime- en lifecycletoestanden van CIMDriver. Omdat CIMDriver leunt op automatische ritregistratie, achtergrondprocessen en herstel na crashes, is het correct beheren van state cruciaal voor de stabiliteit van de app.

## Trackingservice (`ServiceState`)

De kern van de trackingservice is de `TrackingStatusStore`, die functioneert als de single-source-of-truth voor de status van de app. Deze store beheert de state via de enum `ServiceState`, die de volgende specifieke statussen bevat:

- `CREATED`: De service is geïnitialiseerd maar voert nog geen tracking uit.
- `STARTING`: De service is bezig met opstarten en het claimen van resources (zoals locatie-updates).
- `RUNNING`: De service is actief en registreert momenteel een rit (bevat een actieve `tripId`).
- `STOPPING`: De service is bezig met het veilig afsluiten en opslaan van de huidige rit.
- `STOPPED`: De service is gestopt of staat stand-by te wachten op een automatische trigger (bijv. een Bluetooth-connectie).
- `FAILED`: Er is een kritieke fout opgetreden, waardoor tracking is afgebroken (bevat een `errorCause`).
- `RECOVERING`: De service is bezig met een poging om zichzelf te herstellen na een onverwachte onderbreking.

De overgangen tussen deze statussen worden strikt bewaakt. Een dubbele transitie naar `STARTING` als de state al `RUNNING` is, wordt bijvoorbeeld genegeerd om idempotente acties te garanderen.

## Recovery

Recovery is geen losse actie, maar een specifiek lifecyclepad dat door de `TrackingRecoveryManager` wordt gecoördineerd. De RecoveryManager maakt onderscheid tussen:
- **Automatisch herstel toegestaan:** De service is onverwacht gestopt maar het maximaal aantal herstelpogingen is nog niet overschreden.
- **Herstel in uitvoering:** De service state verandert naar `RECOVERING`, met exponential back-off timers via de `WorkManager`.
- **Handmatige interventie nodig:** Als het maximaal aantal herstelpogingen (`maxRecoveryAttempts`) bereikt is, faalt de automatische recovery definitief en moet de gebruiker handmatig ingrijpen.

## UI-state

De ViewModels in de applicatie (zoals `TripsViewModel` en `SettingsViewModel`) maken gebruik van Kotlin's `StateFlow` om UI-state door te geven aan de schermen (Jetpack Compose). Dit zorgt ervoor dat schermtoestanden zoals 'laden', 'leeg', of 'fout' automatisch reageren op de onderliggende data, zonder dat er callbacks nodig zijn.

## Data-state

Ritten en adressen hebben specifieke statussen in de database. Ritten die automatisch geregistreerd zijn, krijgen in eerste instantie een status om gecontroleerd te worden ("To Review"). Afhankelijk van classificatieregels (zoals 'Thuis' naar 'Werk') wordt de uiteindelijke categorie (Zakelijk, Privé, Woon-werk) direct of pas na goedkeuring toegepast.
