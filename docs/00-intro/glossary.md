# Begrippenlijst

Deze begrippenlijst legt de belangrijkste termen van CIMDriver vast op basis van de huidige codebasis en de bestaande productbeschrijving. Het doel is een gedeeld vocabulaire voor ontwikkeling, onderhoud en verdere documentatiegroei.

## Kernbegrippen

- **TrackingService**: foreground Android-service voor actieve rittracking.
- **Recovery**: herstelgedrag waarmee tracking na onderbreking of herstart gecontroleerd wordt hervat of afgehandeld.
- **TripRepository**: repositorylaag voor ritgegevens.
- **AppDatabase**: lokale Room-database van de applicatie.
- **BootReceiver**: receiver voor boot- en package-replacement-events.
- **BluetoothReceiver**: receiver voor connect- en disconnect-events van Bluetooth-apparaten.
- **CarAppService**: service waarmee CIMDriver beschikbaar komt in Android Auto of Automotive-context.
- **Widget**: homescreencomponent buiten de hoofdapp.
- **Worker**: achtergrondtaak voor niet-UI-gebonden processen zoals backup of datalifecycle.
