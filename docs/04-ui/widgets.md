# Widgets

CIMDriver bevat widgetondersteuning op zowel manifest- als broncodeniveau. Het manifest declareert twee widget-receivers en de broncode bevat meerdere widgetgerelateerde Kotlin-bestanden en XML-configuraties.

## Componenten

De widgetlaag bestaat uit `CIMDriverWidgetProvider`, `CIMDriverWidget`, `CIMDriverWidgetReceiver` en bijbehorende XML-bestanden zoals `widget_info.xml`, `cimdriver_widget_info.xml` en `widget_layout.xml`. Daardoor is duidelijk dat widgets niet alleen passief geconfigureerd zijn, maar ook eigen runtime-logica bezitten.

## Rol binnen de app

Widgets bieden homescreenfunctionaliteit buiten de hoofdapp. In een app als CIMDriver ligt het voor de hand dat widgets snelle statusinformatie, recente gegevens of directe acties rondom ritregistratie of overzichten ontsluiten, al moet de precieze inhoud per widget in verdere componentdocumentatie worden vastgelegd.
