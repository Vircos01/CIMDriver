# Gegevensmodel

## Doel

Het iOS-gegevensmodel ondersteunt ritten, werkuren, adressen, voertuigen, instellingen en exporthistorie, met lokale opslag als uitgangspunt. Het model moet SwiftUI-features voeden, runtimeherstel ondersteunen en functionele pariteit met Android mogelijk maken.

## Modellagen

### Domeinmodellen

Domeinmodellen beschrijven bedrijfsbegrippen zoals `Trip`, `WorkSession`, `AddressProfile`, `VehicleProfile` en `ExportJob`. Deze modellen zijn onafhankelijk van SwiftData of Core Data en worden gebruikt in use-cases, services en viewmodels.

### Persistente modellen

Persistente modellen beschrijven hoe data lokaal wordt opgeslagen. Wanneer SwiftData wordt gebruikt, blijven deze modellen zo dicht mogelijk bij opslagbehoeften, terwijl mapping naar domeinmodellen in repositories gebeurt.

### Presentatiemodellen

Presentatiemodellen aggregeren gegevens voor SwiftUI-schermen, bijvoorbeeld dashboardkaarten, ritlijsten of detailheaders. Ze worden niet rechtstreeks opgeslagen.

## Kernentiteiten

| Entiteit | Beschrijving | Belangrijk gebruik |
| :--- | :--- | :--- |
| Trip | Eén geregistreerde rit met tijd, afstand, classificatie en status | Rittenoverzicht, detail, export, dashboard |
| WorkSession | Werkdag of werksessie met start, pauze, eind en correcties | Werkurenoverzicht en rapportage |
| AddressProfile | Bekende locatie met type, label en herkenningsregels | Classificatie en automatische detectie |
| VehicleProfile | Voertuig of bluetoothbron gekoppeld aan ritcontext | Ritstartdetectie en voertuigbeheer |
| UserSettings | Gebruikersvoorkeuren, toleranties en featureflags | Instellingen en runtimebesluiten |
| TrackingCheckpoint | Laatste bekende voortgang voor herstel | Background recovery |
| ExportHistory | Metadata over uitgevoerde exports | Audit en gebruikersfeedback |

## Modelleringrichtlijnen

- Scheid domeinidentiteit van opslagtechnische identifiers waar nodig.
- Bewaar classificatieherkomst, zodat automatische en handmatige overrides te onderscheiden zijn.
- Modelleer tijdstippen eenduidig, inclusief tijdzone- en afrondingscontext.
- Houd trackingcheckpointdata compact maar voldoende voor herstel na onderbreking.
- Vermijd direct SwiftUI-specifieke structuren in persistente modellen.

## Relaties

- Een voertuig kan aan meerdere ritten gekoppeld zijn.
- Een adresprofiel kan zowel classificatie van ritten als start/stop van werkuren beïnvloeden.
- Een work session kan afgeleid zijn van meerdere ritten, maar blijft zelfstandig corrigeerbaar.
- Exporthistorie verwijst naar de bronselectie en uitvoerstatus, niet noodzakelijk naar alle geëxporteerde records.
