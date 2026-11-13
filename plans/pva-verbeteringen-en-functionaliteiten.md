# 📋 Plan van Aanpak (PvA) - Verbeteringen & Functionele Uitbreidingen CIMDriver

**Documentversie:** 1.0  
**Datum:** 13 november 2026  
**Status:** In voorbereiding / Goedgekeurd voor uitvoering  
**Doelgroep:** Ontwikkelaars, Product Owner, Gebruikers  
**Toepassing:** Android & iOS (CIMDriver Ritten- en Urenregistratie)

---

## 1. Managementsamenvatting & Doelstelling

CIMDriver is in korte tijd uitgegroeid tot een volwaardige ritten- en werkurenadministratie voor zakelijke rijders, ZZP'ers en leaserijders in Nederland. Na de recente lancering van **v1.3.6** (slimme parkeergaragedetectie met segment-correctie) en **v1.3.7** (betrouwbare tankstation- en laadpaaldetectie bij tussenstops) is de app technisch en functioneel gescand.

Dit Plan van Aanpak bundelt de geconstateerde **aandachtspunten, compliance-eisen en potentiële functionaliteiten** in vier concrete fasen:
1. **Fiscale Compliance & Belastingdienst-zekerheid** (tellerstanden in export, privé-omrijkilometers).
2. **Gebruikersgemak & Automatisering** (RDW kentekencheck, live rit-toggles, interactieve widget).
3. **Rapportage & Werkurenverfijning** (uren export per periode, kalenderinteractie, dashboard targets).
4. **Android Auto & EV-optimalisaties** (Nederlandse car-interface, laadpaaldeclaraties).

---

## 2. Status Quo & Reeds Gerealiseerd

In de afgelopen releases zijn al grote stappen gezet:
- ✅ **v1.3.5:** Export van ritten per maand (PDF en CSV) inclusief maandtotalen en subsecties.
- ✅ **v1.3.6:** Detectie van vertrek uit parkeergarages zonder GPS-fix; behoud van gereden routes met OSRM-segmenttoevoeging en automatische synchronisatie van de tellerstand.
- ✅ **v1.3.7:** Herkenning van tankstations en laadpalen bij tussenstops (Bluetooth-onderbreking) én ritafronding, heads-up notificatie (`FuelAlertChannel`), actiekaart op het dashboard, voertuigkiezer in brandstofbeheer en correctie van ritstatus in Android Auto.

---

## 3. Fasering & Concrete Werkpakketten

```mermaid
flowchart TD
    subgraph Fase 1: Fiscaal Sluitend
        A1[Start/Eindteller in PDF & CSV Export] --> A2[Voertuig & Kenteken in Documentkop]
        A2 --> A3[Privé-omrijkilometers Veld]
    end

    subgraph Fase 2: Gebruikersgemak & Automatisch Invoeren
        B1[RDW Open Data Kentekencheck] --> B2[Live Rit-Toggle Notificatie/Auto]
        B2 --> B3[Interactieve Startscherm Widget]
    end

    subgraph Fase 3: Werkuren & Dashboard
        C1[Werkuren Export per Week/Maand] --> C2[Kalenderfilter op Geselecteerde Dag]
        C2 --> C3[Dashboard Targets doorklikbaar]
    end

    subgraph Fase 4: EV & Automotive
        D1[EV Specifieke Labeling kWh/Snellader] --> D2[Android Auto Volledige Lokalisatie]
    end

    Fase 1 --> Fase 2 --> Fase 3 --> Fase 4
```

---

### 🟢 Fase 1: Fiscale Compliance & Belastingdienst-zekerheid (✅ Gerealiseerd)

> [!IMPORTANT]
> Volgens art. 3.17 Wet IB 2001 en de Uitvoeringsregeling loonbelasting 2011 eist de Belastingdienst dat een sluitende rittenadministratie per rit de **begin- en eindstand van de kilometerteller** bevat, evenals duidelijke identificatie van het voertuig (kenteken, merk, type). Deze functionaliteit is volledig gerealiseerd in codebase (Room DB v49, exports en invoervelden).

#### 1.1 Start- en Eindstand in PDF & CSV Export (✅ Gerealiseerd)
- **Probleem:** In `ExportUtil.kt` ontbraken voorheen de velden `odometerStart` en `odometerEnd` in zowel `exportToCsv()` als `exportToPdf()`.
- **Oplossing & Implementatie:**
  - **CSV:** Kolommen uitgebreid: `datum;vertrektijd;aankomsttijd;kenteken;voertuig;teller_start;teller_eind;afstand_km;prive_omrij_km;karakter;vertrekadres;aankomstadres;ritdoel_project;route_toelichting`.
  - **PDF:** Volledige omschakeling naar liggend A4 (Landscape 842x595) met kolommen: Datum, Vertrek, Aankomst, Beginstand (km), Eindstand (km), Afstand (km), Privé omrijden (km), Karakter, Vertrekadres, Aankomstadres, Doel / Toelichting.
  - **Paginakop & Totalen:** Bovenin het document voertuig (merk, model, kenteken) en fiscale periode; onderaan een overzichtelijk totalenblok (totaal gereden km, zakelijk km, woon-werk km, privé km, privé-omrij km).
- **Betrokken bestanden:**
  - `android/app/src/main/java/com/cimdriver/app/util/ExportUtil.kt`
  - `android/app/src/main/java/com/cimdriver/app/ui/screens/TripsScreen.kt`

#### 1.2 Registratie van Privé-omrijkilometers (✅ Gerealiseerd)
- **Probleem:** Bij een zakelijke of woon-werk rit kan sprake zijn van een privé-tussenstop (bijv. school, supermarkt). De Belastingdienst vereist dat de extra gereden privékilometers apart vermeld worden en meetellen voor de 500km-privégrens.
- **Oplossing & Implementatie:**
  - `Trip`-entiteit uitgebreid met `@ColumnInfo(defaultValue = "0") val privateDetourMeters: Int = 0`.
  - Room database migratie `MIGRATION_48_49` gerealiseerd en geregistreerd in `AppDatabase.kt`.
  - `DashboardStatsCalculator.kt`: `privateDetourMeters` telt mee voor `ytdPriveMeters` en verplaatst de omgereden kilometers van zakelijk naar privé in de periodestatistieken en grafieken.
  - `AddTripScreen.kt` & `TripDetailScreen.kt`: Invoerveld voor *"Privé-omrijkilometers (optioneel)"* toegevoegd bij zowel handmatig toevoegen, bewerken als bij het beoordelen van afwijkende ritten (`TO_REVIEW`).
  - `TripComponents.kt`: Rittenlijst toont nu *"• waarvan X.X km privé"* indien van toepassing.
- **Betrokken bestanden:**
  - `android/app/src/main/java/com/cimdriver/app/data/local/entity/Entities.kt`
  - `android/app/src/main/java/com/cimdriver/app/data/local/AppDatabase.kt`
  - `android/app/src/main/java/com/cimdriver/app/domain/DashboardStatsCalculator.kt`
  - `android/app/src/main/java/com/cimdriver/app/ui/screens/AddTripScreen.kt`
  - `android/app/src/main/java/com/cimdriver/app/ui/screens/TripDetailScreen.kt`
  - `android/app/src/main/java/com/cimdriver/app/ui/components/TripComponents.kt`
  - `android/app/src/main/java/com/cimdriver/app/ui/viewmodel/TripsViewModel.kt`
  - `android/app/src/main/java/com/cimdriver/app/ui/viewmodel/TripDetailViewModel.kt`

---

### 🔵 Fase 2: Gebruikersgemak & Slimme Automatisering (✅ Gerealiseerd)

#### 2.1 RDW Open Data Kentekencheck (✅ Gerealiseerd)
- **Doel:** Nooit meer handmatig merk, model, bouwjaar en brandstoftype intypen bij het toevoegen van een auto.
- **Uitwerking & Implementatie:**
  - `RdwVehicleLookup.kt`: Koppeling met de gratis en openbare RDW API (`m9d7-ebf2.json`) en brandstofdataset (`8ys7-d773.json`).
  - Automatische detectie van `merk`, `handelsbenaming`, `datum_eerste_toelating` en aandrijving (`EV`, `PHEV` of `ICE`).
  - In `AddVehicleScreen.kt` voorzien van een *"RDW Ophalen"*-actie met laadindicator, foutafhandeling en automatische naamvoorstelling.
- **Betrokken bestanden:**
  - `android/app/src/main/java/com/cimdriver/app/util/RdwVehicleLookup.kt`
  - `android/app/src/main/java/com/cimdriver/app/ui/screens/AddVehicleScreen.kt`
  - `android/app/src/test/java/com/cimdriver/app/util/RdwVehicleLookupTest.kt`

#### 2.2 Live Rit-Toggle tijdens het rijden (Notificatie & Android Auto) (✅ Gerealiseerd)
- **Doel:** Tijdens een rit kunnen wisselen tussen "Zakelijk" en "Privé" zonder de rit te hoeven stoppen en een nieuwe rit te starten.
- **Uitwerking & Implementatie:**
  - `TrackingService.kt`: Ondersteunt `ACTION_TOGGLE_TRIP_TYPE`. In de actieve foreground-notificatie is een directe actieknop toegevoegd: *"🔒 Naar Privé"* (als rit zakelijk/woon-werk is) of *"➡️ Naar Zakelijk"* (als rit privé is). De statusmelding en notificatietitel updaten direct realtime.
  - `MainCarScreen.kt`: Op het Android Auto dashboard toont het actieve ritpaneel nu het huidige type rit met een wisselknop *"Naar Privé"* / *"Naar Zakelijk"*.
- **Betrokken bestanden:**
  - `android/app/src/main/java/com/cimdriver/app/service/TrackingService.kt`
  - `android/app/src/main/java/com/cimdriver/app/car/MainCarScreen.kt`
  - `android/app/src/main/java/com/cimdriver/app/service/TrackingStatus.kt`

#### 2.3 Interactieve Home Screen Widget (Quick Actions) (✅ Gerealiseerd)
- **Doel:** Direct vanaf het Android-startscherm de actieve ritstatus zien en ritten starten of stoppen.
- **Uitwerking & Implementatie:**
  - `CIMDriverWidget.kt`: Volledig interactieve Glance AppWidget:
    - **Tijdens rit:** Toont actieve ritstatus (type rit, kilometers, starttijd, vertreklocatie) met actieknoppen *"Naar Zakelijk/Privé"* en *"Stop Rit"*.
    - **In rust:** Toont de maandtotalen per categorie én directe startknoppen *"▶️ Zakelijk"* en *"▶️ Privé"*.
    - Gesynchroniseerd met `TrackingService` via `updateAll()`.
- **Betrokken bestanden:**
  - `android/app/src/main/java/com/cimdriver/app/ui/widget/CIMDriverWidget.kt`

---

### 🟡 Fase 3: Werkuren, Kalender & Dashboard (✅ Gerealiseerd)

#### 3.1 Werkuren Delen per Week / Maand (✅ Gerealiseerd)
- **Probleem:** In `WorkHoursScreen.kt` deelde de deelknop voorheen blind alle werkdagen uit de hele geschiedenis, ongeacht de geselecteerde week of maand.
- **Oplossing & Implementatie:**
  - `ExportUtil.kt`: `exportWorkHoursToPdf` ondersteunt nu een aangepaste periodetitel en berekent automatisch een totaalregel met gewerkte uren onderaan het document.
  - `WorkHoursScreen.kt`: Zowel in de topbarmenu-export als via de nieuwe *"Deel"*-knop per periodeheader (`WorkHoursHeaderItem`) kan nu direct een specifieke week of maand worden geëxporteerd naar PDF of CSV (bijv. `cimdriver_werkuren_Week_46.pdf`), inclusief fallback naar volledige export.
- **Betrokken bestanden:**
  - `android/app/src/main/java/com/cimdriver/app/ui/screens/WorkHoursScreen.kt`
  - `android/app/src/main/java/com/cimdriver/app/util/ExportUtil.kt`

#### 3.2 Kalenderweergave Dag-Klik Interactie (✅ Gerealiseerd)
- **Probleem:** In `TripsScreen.kt` schakelde het klikken op een dag in `TripsCalendarView` voorheen alleen terug naar de lijst, zonder de ritten van die dag te tonen of te inspecteren.
- **Oplossing & Implementatie:**
  - `TripsScreen.kt`: Bij het klikken op een dag in de kalender opent een dialoog met alle ritten van die dag:
    - Titel met datum, aantal ritten en totale kilometers.
    - Interactieve rittenkaarten (tijden, adressen, kilometers, categorie en eventuele privé-omrijkilometers) die direct doorklikken naar `TripDetailScreen`.
    - Actieknop *"Toon in lijst"* om direct naar de lijstweergave te schakelen.
- **Betrokken bestanden:**
  - `android/app/src/main/java/com/cimdriver/app/ui/screens/TripsScreen.kt`
  - `android/app/src/main/java/com/cimdriver/app/ui/components/TripsCalendarView.kt`

#### 3.3 Dashboard Targets direct doorklikbaar (✅ Gerealiseerd)
- **Probleem:** De voortgangsbalken van doelen op het dashboard waren statisch en niet klikbaar.
- **Oplossing & Implementatie:**
  - `DashboardScreen.kt`: De "Targets"-sectie header bevat nu een actieknop *"Beheren"*, en elke doelvoortgangskaart is direct klikbaar gemaakt.
  - `Navigation.kt`: Gekoppeld aan `HoursTargetsRoute`, zodat aantikken de gebruiker direct naar het beheer van de doelen brengt.
- **Betrokken bestanden:**
  - `android/app/src/main/java/com/cimdriver/app/ui/screens/DashboardScreen.kt`
  - `android/app/src/main/java/com/cimdriver/app/ui/navigation/Navigation.kt`

---

### 🟣 Fase 4: Android Auto & EV Verfijningen (✅ Gerealiseerd)

#### 4.1 Volledige Nederlandse Lokalisatie in Android Auto (✅ Gerealiseerd)
- **Probleem:** In `StartTripScreen.kt`, `ReviewTripScreen.kt` en `RecentTripsCarScreen.kt` stonden hardcoded Engelse categorienamen ("Home To Work", "Business Meeting", "Customer Visit", etc.) en ontbrak een directe optie voor privé-ritten.
- **Oplossing & Implementatie:**
  - `CarUtil.kt`: Centrale `getLocalizedTripType(tripType: String?): String` mapping geïntroduceerd (vertaalt "Home To Work"/"COMMUTE" naar "Woon-werk", "Business Meeting" naar "Zakelijk overleg", "Customer Visit" naar "Klantbezoek", "Customer Billable" naar "Klant declarabel", "Commissioned By CIMSOLUTIONS" naar "In opdracht CIMSOLUTIONS", "Exam Course" naar "Cursus / Examen", "Car Maintenance" naar "Auto-onderhoud", "PRIVATE"/"PERSONAL" naar "Privé").
  - `StartTripScreen.kt`: Alle keuzerijen voorzien van Nederlandse titels conform het design en uitgebreid met *"Privé"* (blauwe car-locatie icoon).
  - `ReviewTripScreen.kt`: Beoordelingsopties worden gelokaliseerd getoond aan de bestuurder.
  - `RecentTripsCarScreen.kt`: Rittenhistorie toont Nederlandse categorietitels tussen haakjes in plaats van ruwe databasetypes.
  - `CarUtilTest.kt`: JUnit tests gerealiseerd en geslaagd voor alle standaard en aangepaste categorieën.
- **Betrokken bestanden:**
  - `android/app/src/main/java/com/cimdriver/app/car/CarUtil.kt`
  - `android/app/src/main/java/com/cimdriver/app/car/StartTripScreen.kt`
  - `android/app/src/main/java/com/cimdriver/app/car/ReviewTripScreen.kt`
  - `android/app/src/main/java/com/cimdriver/app/car/RecentTripsCarScreen.kt`
  - `android/app/src/test/java/com/cimdriver/app/car/CarUtilTest.kt`

#### 4.2 EV Laadsessies & Tariefdifferentiatie (✅ Gerealiseerd)
- **Doel:** EV- en PHEV-rijders optimaal gemak bieden bij het registreren van laadkosten (thuis laden tegen energietarief versus publiek laden via laadpas of snelladen).
- **Oplossing & Implementatie:**
  - `Entities.kt`: `FuelFillUp` uitgebreid met `@ColumnInfo(defaultValue = "NULL") val chargingType: String? = null` ("HOME", "PUBLIC", "FAST", "CUSTOM").
  - `AppDatabase.kt`: Room database migratie `MIGRATION_49_50` gerealiseerd en geregistreerd (databaseversie 50).
  - `AddFuelScreen.kt`:
    - Snelle tariefkeuzechips toegevoegd voor elektrische voertuigen:
      - 🏠 **Thuis (€ 0,28/kWh)** - stelt prijs in, herberekent totale kosten en suggereert *"Thuislaadpaal"*.
      - 🔌 **Publiek (€ 0,45/kWh)** - stelt standaard publiek AC-tarief in en herberekent kosten.
      - ⚡ **Snellader (€ 0,69/kWh)** - stelt snellaadtarief (Fastned/Ionity) in en herberekent kosten.
    - Volledige bidirectionele synchronisatie: handmatige invoer van kWh of totaalprijs past automatisch het tarief/laadtype aan.
  - `FuelScreen.kt`: `FuelCard` toont nu het tarieftype (bijv. *"🏠 Thuis laden (€ 0,28/kWh)"*) en *"Laadlocatie"* voor EV-laadsessies.
- **Betrokken bestanden:**
  - `android/app/src/main/java/com/cimdriver/app/data/local/entity/Entities.kt`
  - `android/app/src/main/java/com/cimdriver/app/data/local/AppDatabase.kt`
  - `android/app/src/main/java/com/cimdriver/app/ui/screens/AddFuelScreen.kt`
  - `android/app/src/main/java/com/cimdriver/app/ui/screens/FuelScreen.kt`

---

## 4. Prioriterings- en Impactmatrix

| Taak / Verbetering | Fiscale Noodzaak | Gebruikersimpact | Complexiteit | Aanbevolen Sprint |
| :--- | :---: | :---: | :---: | :---: |
| **1.1 Belastingdienst Tellerstanden in Export (PDF/CSV)** | 🔴 Zeer hoog | 🟢 Hoog | ⚡ Laag (1-2 uur) | **Sprint 1 (Direct)** |
| **2.1 RDW Kenteken Check API** | ⚪ Laag | 🟢 Zeer hoog | ⚡ Laag (2-3 uur) | **Sprint 1** |
| **3.1 Werkuren Export per Periode (Week/Maand)** | ⚪ Laag | 🟢 Hoog | ⚡ Laag (1-2 uur) | **Sprint 1** |
| **3.3 Dashboard Targets klikbaar maken** | ⚪ Laag | 🟡 Gemiddeld | ⚡ Minimaal (15 min) | **Sprint 1** |
| **4.1 Android Auto Nederlandse teksten** | ⚪ Laag | 🟡 Gemiddeld | ⚡ Minimaal (30 min) | **Sprint 1** |
| **2.2 Live Rit-Toggle Notificatie/Car** | ⚪ Laag | 🟢 Zeer hoog | 🔶 Gemiddeld (3-4 uur) | **Sprint 2** |
| **1.2 Privé-omrijkilometers Veld & DB Migratie** | 🔴 Hoog | 🟡 Gemiddeld | 🔶 Gemiddeld (3-4 uur) | **Sprint 2** |
| **3.2 Kalenderweergave filter per dag** | ⚪ Laag | 🟡 Gemiddeld | 🔶 Gemiddeld (2-3 uur) | **Sprint 2** |
| **2.3 Interactieve Home Screen Widget (Start/Stop)** | ⚪ Laag | 🟢 Hoog | 🔶 Gemiddeld (4-5 uur) | **Sprint 3** |
| **4.2 EV Tariefdifferentiatie** | ⚪ Laag | 🟡 Gemiddeld | 🔶 Gemiddeld (3-4 uur) | **Sprint 3** |

---

## 5. Beheer & Kwaliteitsborging

Voor elke oplevering binnen dit PvA gelden de strikte CIMDriver kwaliteitsstandaarden:
1. **Unit Tests:** Alle berekeningen (afstand, tellersynchronisatie, urennormalisatie) worden gedekt door JUnit-tests via `./gradlew testDebugUnitTest`.
2. **5-Bestands Versiebeheer:** Versie-ophoging wordt synchroon doorgevoerd in `build.gradle.kts`, root `version.json`, `android/version.json`, `README.md` en `CHANGELOG.md`.
3. **Bilingual Docs:** Documentatie en downloadpagina's in `docs/` worden altijd bijgewerkt in zowel het Nederlands (`.md`) als het Engels (`.en.md`).
4. **Keystore Integrity:** Releases worden gesigneerd via `android/keystore.properties` met `release.jks`.

