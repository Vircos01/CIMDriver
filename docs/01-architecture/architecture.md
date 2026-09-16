# Architectuur

De architectuur van CIMDriver is meerlaags opgezet met afzonderlijke verantwoordelijkheden voor applicatie-entrypoints, services, data, UI, widgets, workers en auto-integratie. Die gelaagdheid is zichtbaar in de package-structuur van de hoofdmodule.

## Hoofdstructuur

De repository bevat één primaire Android-appmodule onder `app`, aangevuld met configuratiebestanden, release-inhoud en projectdocumentatie. Binnen de hoofdmodule bevinden zich 79 Kotlin-bronbestanden en 25 XML-bestanden in de main source set.

## Packages

| Package | Verantwoordelijkheid |
|---|---|
| `com.cimdriver.app` | Applicatie-initialisatie en hoofdentrypoints. |
| `com.cimdriver.app.service` | Tracking, recovery, geocoding, geofencing, notificatie-acties en statusgerelateerde runtimeprocessen. |
| `com.cimdriver.app.data.local` | Lokale Room-database, DAO's en entiteiten. |
| `com.cimdriver.app.data.repository` | Repositorylogica voor onder meer ritgegevens. |
| `com.cimdriver.app.ui.*` | Compose-schermen, componenten, dialogs, thema, navigatie en ViewModels. |
| `com.cimdriver.app.car` | Schermen en services voor Android Auto en Automotive. |
| `com.cimdriver.app.widget` en `com.cimdriver.app.ui.widget` | Widgetproviders en widgetgerelateerde logica. |
| `com.cimdriver.app.worker` | Achtergrondtaken zoals backup en data lifecycle. |
| `com.cimdriver.app.di` | Dependency injection-configuratie. |
| `com.cimdriver.app.domain` | Domeinberekeningen zoals dashboardstatistieken. |
| `com.cimdriver.app.util` | Hulplogica voor adresmatching, export, classificatie, tijd en berekeningen. |

## Runtime-ingangen

Het manifest toont meerdere ingangen in de app: de launcher-activity, een foreground trackingservice, boot- en Bluetooth-receivers, widget-receivers, een FileProvider en een car app service. Daarmee is de app niet alleen UI-gestuurd, maar ook event- en systeemgestuurd.

## Verantwoordelijkheden per laag

De presentatielaag bestaat uit schermen, navigatie, componenten en ViewModels. De servicelaag verwerkt tracking, recovery en systeemevents. De datalaag bewaart en ontsluit informatie via Room en repositories. De integratielaag koppelt de app aan widgets, workers en car-interfaces.
