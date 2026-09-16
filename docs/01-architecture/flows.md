# Procesflows

Dit document beschrijft de belangrijkste functionele en technische procesflows van CIMDriver in samenhang. Het doel is om ontwikkelaars snel inzicht te geven in hoe gebeurtenissen door de applicatie bewegen, inclusief de onlangs toegevoegde robuustheid en security checks.

## Bluetooth naar tracking

1. Een Bluetooth connect-event wordt ontvangen door de receiverlaag (`BluetoothReceiver`).
2. De receiver valideert expliciet of de inkomende `Intent` overeenkomt met toegestane acties ter voorkoming van spoofing.
3. De app bepaalt of de verbinding relevant is voor ritdetectie.
4. De `TrackingService` wordt gestart of hervat via de `TrackingRecoveryManager`.
5. De service gaat naar foreground-modus en start locatie-updates.
6. Een actieve rit wordt opgeslagen en bijgewerkt in de database.
7. De UI (`DiagnosticsScreen`) en de dynamische `Notification` reflecteren de nieuwe status door de `TrackingStatusStore` StateFlow uit te lezen.

## Tracking naar classificatie

1. Een rit wordt beëindigd via disconnect (na de grace period), expliciete stop of andere lifecyclegebeurtenis.
2. De service sluit de actieve rit af in de database.
3. Classificatielogica bepaalt het type rit door middel van matching-algoritmen op start- en eindlocatie.
4. Resultaten worden opgeslagen in de lokale database (Room).
5. De rit wordt zichtbaar in overzichten (`TripsScreen`), dashboards en detailschermen en krijgt in eerste instantie de status "Te beoordelen" als handmatige goedkeuring vereist is.

## Recoveryflow (Boot & Crash Recovery)

1. Een reboot, process kill of package replacement onderbreekt tracking.
2. De `BootReceiver` ontvangt het event, valideert de actie, en wekt de `TrackingRecoveryManager`.
3. De `TrackingRecoveryManager` controleert in de database de opgeslagen `maxRecoveryAttempts`.
4. Er wordt gecontroleerd of de limiet van het aantal opeenvolgende pogingen nog niet is bereikt.
5. Zo ja: Een recovery-intent wordt gepland met exponential back-off timers om systeemdrainage te voorkomen.
6. De `TrackingService` wordt met recovery-intentie gestart. De state in de store gaat naar `RECOVERING` en daarna naar `STARTING`.
7. Status en notificatie worden direct bijgewerkt om de gebruiker op de hoogte te stellen van het herstel.
8. Diagnostiek (`DiagnosticsScreen`) maakt zichtbaar wat is gebeurd, inclusief eventuele foutmeldingen (`errorCause`).

## UI-flow

1. Gebruiker opent een scherm via de navigatiestructuur in `Navigation.kt`.
2. Navigatie activeert het bijbehorende Compose screen en Hilt ViewModel.
3. ViewModel leest data via repositories (via Room of `TrackingStatusStore` StateFlow).
4. Compose hertekent (recomposition) op basis van State-veranderingen.
5. Gebruikersacties muteren state, starten onderliggende processen via coroutines of slaan nieuwe data op in de repository.
