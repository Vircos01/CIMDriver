# 📋 Plan van Aanpak (PvA) - Verbeteringen & Functionele Uitbreidingen CIMDriver

**Documentversie:** 1.0  
**Datum:** 13 november 2026  
**Status:** In voorbereiding / Goedgekeurd voor uitvoering  
**Doelgroep:** Ontwikkelaars, Product Owner, Gebruikers  
**Toepassing:** Android & iOS (CIMDriver Ritten- en Urenregistratie)

---

## 1. Managementsamenvatting & Doelstelling

CIMDriver is in korte tijd uitgegroeid tot een volwaardige ritten- en werkurenadministratie voor zakelijke rijders, ZZP'ers en leaserijders in Nederland. Na de recente lancering van **v1.3.6** (slimme parkeergaragedetectie met segment-correctie) en **v1.3.7** (betrouwbare tankstation- en laadpaaldetectie bij tussenstops) is de app technisch en functioneel gescand.

Dit Plan van Aanpak bundelt de geconstateerde **aandachtspunten, compliance-eisen en potentiële functionaliteiten** in vijf concrete fasen:
1. **Fiscale Compliance & Belastingdienst-zekerheid** (tellerstanden in export, privé-omrijkilometers) — *✅ Gerealiseerd*.
2. **Gebruikersgemak & Automatisering** (RDW kentekencheck, live rit-toggles, interactieve widget) — *✅ Gerealiseerd*.
3. **Rapportage & Werkurenverfijning** (uren export per periode, kalenderinteractie, dashboard targets) — *✅ Gerealiseerd*.
4. **Android Auto & EV-optimalisaties** (Nederlandse car-interface, laadpaaldeclaraties) — *✅ Gerealiseerd*.
5. **Assisted Decision-Making & Slimme Rust** (dagelijkse inbox voor uitzonderingen, confidence scoring, contextuele defaults, battery & permission health checks, netwerk- en securityhygiëne) — *⏳ In planning*.

---

## 2. Status Quo & Reeds Gerealiseerd

In de recente releases zijn de eerste vier fasen volledig gerealiseerd en in productie genomen:
- ✅ **v1.3.5 / v1.3.6 / v1.3.7:** Maandexport, parkeergaragedetectie met OSRM-segmenttoevoeging en tankstation-/laadpaalherkenning bij tussenstops.
- ✅ **v1.4.0 / v1.4.1 (Android) & v0.0.8 (iOS):** Volledige afronding van Fase 1 t/m 4:
  - Belastingdienst-sluitende liggende A4 PDF & CSV export met tellerstanden en totalen.
  - Registratie en fiscale toewijzing van privé-omrijkilometers (Room DB v49/v50 & SwiftData).
  - Open data RDW kentekencheck voor automatische merk-, model- en brandstofherkenning.
  - Live rit-toggle (Zakelijk ↔ Privé) tijdens tracking via notificatie, Android Auto en iOS dashboard.
  - Interactieve startschermwidget (Android Glance).
  - Kalenderweergave met dag-inspectie en ritdrilldown; urenexport per week/maand.
  - EV-specifieke laadpresets (Thuis €0,28, Publiek €0,45, Snellader €0,69) en volledige Nederlandse Android Auto lokalisatie.

---

## 3. Fasering & Concrete Werkpakketten

```mermaid
flowchart TD
    subgraph Fase 1: Fiscaal Sluitend (✅ Gerealiseerd)
        A1[Start/Eindteller in PDF & CSV Export] --> A2[Voertuig & Kenteken in Documentkop]
        A2 --> A3[Privé-omrijkilometers Veld]
    end

    subgraph Fase 2: Gebruikersgemak & Automatisering (✅ Gerealiseerd)
        B1[RDW Open Data Kentekencheck] --> B2[Live Rit-Toggle Notificatie/Auto]
        B2 --> B3[Interactieve Startscherm Widget]
    end

    subgraph Fase 3: Werkuren & Dashboard (✅ Gerealiseerd)
        C1[Werkuren Export per Week/Maand] --> C2[Kalenderfilter op Geselecteerde Dag]
        C2 --> C3[Dashboard Targets doorklikbaar]
    end

    subgraph Fase 4: EV & Automotive (✅ Gerealiseerd)
        D1[EV Specifieke Labeling kWh/Snellader] --> D2[Android Auto Volledige Lokalisatie]
    end

    subgraph Fase 5: Assisted Decision-Making & Slimme Rust (✅ Gerealiseerd)
        E1[Dagelijkse Uitzonderingen-Inbox] --> E2[Confidence Score & 1-Tap Goedkeuren]
        E2 --> E3[Contextuele Smart Defaults & Menselijke Diagnostiek]
        E3 --> E4[Permission & Battery Health Monitor]
        E4 --> E5[Netwerk- & Securityhygiëne / Cleartext Opschoning]
    end

    subgraph Fase 6: Betrouwbaarheid, Audit & Foutloze Export (🚀 In uitvoering)
        F1[Foutarme Export-voorcontrole] --> F2[Begrijpelijke Diagnostiekstatus]
        F2 --> F3[Projectcode Suggesties & Terugkerende Regels]
        F3 --> F4[Undo / Herstelbare Wijzigingen]
        F4 --> F5[Back-up Herstel- & Integriteitscheck]
    end

    Fase 1 --> Fase 2 --> Fase 3 --> Fase 4 --> Fase 5 --> Fase 6
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

### 🟢 Fase 5: Assisted Decision-Making & Slimme Rust (Roadmap v1.5) (✅ Gerealiseerd)

> [!TIP]
> Nu de functionele kern (tracking, exports, RDW, widgets en auto-integraties) staat, verschuift de meerwaarde naar **begeleiding en rust**: de app denkt proactief mee bij twijfelgevallen, lost uitzonderingen op met één tik en bewaakt op de achtergrond de technische en fiscale betrouwbaarheid.

#### 5.1 Dagelijkse Uitzonderingen-Inbox ("Daily Triage")
- **Probleem:** Actiepunten zijn momenteel verspreid over verschillende schermen (ritten ter beoordeling in Ritten, ontbrekende werkuren in Werkuren, tellergaten in Voertuigen en tankbonnen in Brandstof).
- **Oplossing & Implementatie:**
  - Eén centrale en rustige **"Dagelijkse Inbox"** op het dashboard (`DailyInboxCard` of compact triagescherm).
  - Bundelt direct alle openstaande items van vandaag en deze week:
    - 🚗 Ritten met status `TO_REVIEW` (met voorgestelde categorie).
    - ⏱️ Werkdagen zonder vertrektijd of met afwijkende pauze.
    - ⚠️ Gaten in de kilometertellerstand (`OdometerGap`).
    - ⛽ Tankstation-/laadpaal-tussenstops waar nog geen bon voor is ingevoerd.
  - Voorzien van een **"Alles goedkeuren"**-knop voor ritten met een hoge betrouwbaarheidsscore.
- **Betrokken bestanden:**
  - `android/app/src/main/java/com/cimdriver/app/ui/viewmodel/DailyInboxViewModel.kt`
  - `android/app/src/main/java/com/cimdriver/app/ui/components/DailyInboxCard.kt`
  - `android/app/src/main/java/com/cimdriver/app/ui/screens/DashboardScreen.kt`
  - `ios/CIMDriver/Views/Dashboard/DailyInboxView.swift`

#### 5.2 Confidence Score & Zachte Reviewvoorstellen
- **Doel:** Niet blind alles automatisch goedkeuren of de gebruiker overspoelen met pop-ups, maar betrouwbare suggesties doen met 1-tap acceptatie.
- **Oplossing & Implementatie:**
  - Introductie van een `confidenceScore` (0–100%) en `suggestedCategory` per rit in `TripClassification`.
  - **Scoringsfactoren:**
    - Vertrek en aankomst matchen met bekende `SavedAddress` (Thuis/Werk/Klant): +40%.
    - Rit valt binnen reguliere werktijden en werkdagen: +25%.
    - Rit is gestart via een bekende Bluetooth-carkit: +20%.
    - Historische consistentie (rit is al vaker identiek gereden): +15%.
  - Ritten met score ≥ 90% worden gemarkeerd als *"Vrijwel zeker Zakelijk"* of *"Woon-werk"*; in de inbox of notificatie volstaat één tik op `[✓ Klopt]` om de rit definitief te fiatteren.
- **Betrokken bestanden:**
  - `android/app/src/main/java/com/cimdriver/app/util/TripClassification.kt`
  - `android/app/src/main/java/com/cimdriver/app/data/local/entity/Entities.kt`
  - `android/app/src/main/java/com/cimdriver/app/ui/components/TripComponents.kt`
  - `ios/CIMDriver/Utilities/TripClassification.swift`

#### 5.3 Contextuele Smart Defaults & Menselijke Diagnostiek
- **Doel:** Minimaliseer handmatig invoerwerk en geef begrijpelijke uitleg over automatische ritbeslissingen.
- **Oplossing & Implementatie:**
  - **Smart Defaults:** Wanneer een rit eindigt bij een klantlocatie, wordt direct de gekoppelde `projectCode` en het uurtarief voorgesteld. Bij aankomst op een werklocatie worden de begintijd (afgerond) en standaard pauze direct vooringevuld.
  - **Menselijke Ritaudit:** In plaats van puur technische logs toont het ritdetailscherm begrijpelijke context:
    - *"Rit automatisch beëindigd na 3 minuten stilstand zonder carkit."*
    - *"GPS-onderbreking in parkeergarage automatisch overbrugd via routenetwerk."*
- **Betrokken bestanden:**
  - `android/app/src/main/java/com/cimdriver/app/ui/screens/AddTripScreen.kt`
  - `android/app/src/main/java/com/cimdriver/app/ui/screens/TripDetailScreen.kt`
  - `android/app/src/main/java/com/cimdriver/app/service/TrackingService.kt`

#### 5.4 Permission & Battery Health Monitor
- **Probleem:** Android-fabrikanten (Samsung, Xiaomi, OnePlus) leggen achtergrondservices agressief stil door batterijbesparingsmechanismen, wat leidt tot gemiste ritten en onbegrip bij gebruikers.
- **Oplossing & Implementatie:**
  - `HealthMonitorHelper`: Controleert periodiek en bij app-start:
    - `PowerManager.isIgnoringBatteryOptimizations(packageName)`
    - `ACCESS_BACKGROUND_LOCATION` permissie ("Altijd toestaan")
    - `POST_NOTIFICATIONS` permissie
    - GPS / Location Provider status
  - **Proactieve Waarschuwingskaart:** Toont een rustige banner op het Dashboard of in Diagnostics als een instelling de betrouwbaarheid bedreigt, inclusief een directe 1-tap knop naar het juiste Android-instellingenscherm (`ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS` of app-details).
- **Betrokken bestanden:**
  - `android/app/src/main/java/com/cimdriver/app/util/HealthMonitorHelper.kt`
  - `android/app/src/main/java/com/cimdriver/app/ui/screens/DashboardScreen.kt`
  - `android/app/src/main/java/com/cimdriver/app/ui/screens/DiagnosticsScreen.kt`

#### 5.5 Netwerk- & Securityhygiëne (Network Security Config)
- **Probleem:** In `AndroidManifest.xml` staat momenteel `android:usesCleartextTraffic="true"`. Dit stelt de gehele applicatie open voor onversleuteld HTTP-verkeer, terwijl externe services (OSRM, Nominatim, RDW) uitsluitend via HTTPS behoren te lopen.
- **Oplossing & Implementatie:**
  - Verwijder de globale `android:usesCleartextTraffic="true"` uit `AndroidManifest.xml`.
  - Voeg `android:networkSecurityConfig="@xml/network_security_config"` toe.
  - Maak `res/xml/network_security_config.xml` aan waarin cleartext HTTP uitsluitend wordt toegestaan voor `127.0.0.1` en `localhost` ten behoeve van de lokale `ZeppCompanionServer` (smartwatch companion).
  - Dwing strikte HTTPS (TLS 1.2+) af voor alle overige netwerkverbindingen.
- **Betrokken bestanden:**
  - `android/app/src/main/AndroidManifest.xml`
  - `android/app/src/main/res/xml/network_security_config.xml`

#### 5.6 Adaptieve Trackingintensiteit & Fiscale Jaarprognose
- **Adaptieve Tracking:** Dynamische schaling van de locatie-interval: energiezuinige significante bewegingsdetectie bij stilstand, opschalen naar 1-seconde GPS-polling bij rijsnelheden (>15 km/u) of actieve Bluetooth-carkitverbinding.
- **Fiscale Jaarprognose:** Voorspelling van het aantal privékilometers op 31 december op basis van het huidige seizoens- en weekpatroon (*"Bij het huidige rijgedrag kom je uit op 410 km privé — veilig onder de 500 km grens"* of tijdig waarschuwen bij overschrijdingsrisico).
- **Betrokken bestanden:**
  - `android/app/src/main/java/com/cimdriver/app/service/LocationClient.kt`
  - `android/app/src/main/java/com/cimdriver/app/domain/DashboardStatsCalculator.kt`

---

### 🟣 Fase 6: Betrouwbaarheid, Audit & Foutloze Export (Roadmap v1.6) (✅ Gerealiseerd)

> [!IMPORTANT]
> Deze fase tilt de registratie van "bruikbaar" naar "professioneel en onfeilbaar". In plaats van weer nieuwe grote modules bouwt Fase 6 voort op de bestaande kern van export, diagnostiek, backup en ritbeheer om fouten vóór te zijn en de app rustiger en vergevingsgezinder te maken.

#### 6.1 Foutarme Export-voorcontrole & Registratie-compleetheid per Periode (Prioriteit 1 & 4)
- **Concept:** Wanneer de gebruiker op "Exporteer PDF" of "Exporteer CSV" tikt, toont de app vóór het genereren van het document een transparante validatiedialoog (`ExportPreCheckDialog` / `ExportPreCheckSheet`).
- **Inhoudelijke checks:**
  1. *Tellerstanden-sluitendheid:* Controle via `OdometerGapAnalyzer` of alle kilometerstanden binnen de gekozen periode naadloos op elkaar aansluiten.
  2. *Rittenclassificatie:* Controle of er ritten met status `TO_REVIEW` in de exportperiode vallen. Biedt direct een 1-tik *"Keur alle hoge betrouwbaarheid goed"* knop binnen de dialoog!
  3. *Afwijkende routes:* Controle of er ritten zijn met afwijkende afstanden zonder notitie voor de fiscus.
  4. *Fiscale validatiestempel:* Groene badge *"100% fiscaal sluitend conform Belastingdienst"* als alles compleet is; duidelijke waarschuwingsbanner met keuze `[Eerst oplossen]` of `[Toch exporteren]`.

#### 6.2 Begrijpelijke Tracking-betrouwbaarheid in Diagnostiek (Prioriteit 3)
- **Concept:** In `DiagnosticsScreen.kt` en `DiagnosticsView.swift` wordt de technische logging aangevuld met een duidelijke, menselijke statuskaart bovenaan.
- **Status niveaus:**
  - 🟢 **Optimaal:** Alle rechten, Doze mode uitschakeling, notificaties, precieze locatie en carkitverbinding actief.
  - 🟠 **Beperkt:** Eén of meer instellingen (zoals batterij-optimalisatie of notificatietoestemming) verminderen de betrouwbaarheid.
  - 🔴 **Geblokkeerd:** Locatie uitgeschakeld of permissies geweigerd.
- **Actiegerichte checklist:** Elk punt bevat directe knoppen naar de juiste Android/iOS systeeminstelling.

#### 6.3 Projectcode-voorstellen & Regels voor Terugkerende Ritten (Prioriteit 2 & 6)
- **Concept:** Uitbreiding van het ritbeoordelings- en classificatiemodel:
  - Bij ritten naar bekende klantadressen of op specifieke dagen/tijden wordt niet alleen het rittype, maar ook direct de meest waarschijnlijke projectcode voorgesteld.
  - Bij het opslaan van een favoriete route krijgt de gebruiker een optie: *"Maak tevens automatische classificatieregel voor deze route"*.

#### 6.4 Herstelbare Wijzigingen / Undo Snackbar (Prioriteit 5)
- **Concept:** Bij snelle handelingen (zoals het 1-tik goedkeuren van een rit in de inbox, het herberekenen van tellers of handmatig corrigeren van een rit) toont de app een Snackbar met `[Ongedaan maken]`.
- **Werking:** Tikken op `Ongedaan maken` draait de update direct terug naar de vorige toestand zonder dataverlies.

#### 6.5 Back-up Herstel- en Integriteitscheck (Prioriteit 8)
- **Concept:** In `DataManagementScreen.kt` en `DataManagementView.swift` ziet de gebruiker in één oogopslag de status van automatische back-ups:
  - Tijdstip laatste back-up (bijv. *"Vandaag om 03:00"*).
  - Bestandsgrootte en inhoud (aantal ritten en instellingen).
  - Integriteitsindicator: *"Herstelbaar: Ja (Gevalideerd)"*.
  - Knop om een handmatige verificatietest uit te voeren.

---

## 4. Prioriterings- en Impactmatrix

| Taak / Verbetering | Fiscale Noodzaak | Gebruikersimpact | Complexiteit | Status / Sprint |
| :--- | :---: | :---: | :---: | :---: |
| **1.1 Belastingdienst Tellerstanden in Export (PDF/CSV)** | 🔴 Zeer hoog | 🟢 Hoog | ⚡ Laag | ✅ Gerealiseerd (v1.4.0) |
| **2.1 RDW Kenteken Check API** | ⚪ Laag | 🟢 Zeer hoog | ⚡ Laag | ✅ Gerealiseerd (v1.4.0) |
| **3.1 Werkuren Export per Periode (Week/Maand)** | ⚪ Laag | 🟢 Hoog | ⚡ Laag | ✅ Gerealiseerd (v1.4.0) |
| **3.3 Dashboard Targets klikbaar maken** | ⚪ Laag | 🟡 Gemiddeld | ⚡ Minimaal | ✅ Gerealiseerd (v1.4.0) |
| **4.1 Android Auto Nederlandse teksten** | ⚪ Laag | 🟡 Gemiddeld | ⚡ Minimaal | ✅ Gerealiseerd (v1.4.0) |
| **2.2 Live Rit-Toggle Notificatie/Car** | ⚪ Laag | 🟢 Zeer hoog | 🔶 Gemiddeld | ✅ Gerealiseerd (v1.4.0) |
| **1.2 Privé-omrijkilometers Veld & DB Migratie** | 🔴 Hoog | 🟡 Gemiddeld | 🔶 Gemiddeld | ✅ Gerealiseerd (v1.4.0) |
| **3.2 Kalenderweergave filter per dag** | ⚪ Laag | 🟡 Gemiddeld | 🔶 Gemiddeld | ✅ Gerealiseerd (v1.4.0) |
| **2.3 Interactieve Home Screen Widget (Start/Stop)** | ⚪ Laag | 🟢 Hoog | 🔶 Gemiddeld | ✅ Gerealiseerd (v1.4.0) |
| **4.2 EV Tariefdifferentiatie & Laadpresets** | ⚪ Laag | 🟡 Gemiddeld | 🔶 Gemiddeld | ✅ Gerealiseerd (v1.4.1) |
| **5.5 Netwerk- & Securityhygiëne (Network Security Config)** | ⚪ Laag | 🟢 Hoog (Beveiliging) | ⚡ Laag | ✅ Gerealiseerd (v1.5.0) |
| **5.4 Permission & Battery Health Monitor** | 🟡 Gemiddeld | 🟢 Zeer hoog | ⚡ Laag | ✅ Gerealiseerd (v1.5.0) |
| **5.1 Dagelijkse Uitzonderingen-Inbox ("Daily Triage")** | ⚪ Laag | 🟢 Zeer hoog | 🔶 Gemiddeld | ✅ Gerealiseerd (v1.5.0) |
| **5.2 Confidence Score & Zachte 1-Tap Reviews** | ⚪ Laag | 🟢 Hoog | 🔶 Gemiddeld | ✅ Gerealiseerd (v1.5.0) |
| **5.3 Contextuele Smart Defaults & Ritaudit** | ⚪ Laag | 🟡 Gemiddeld | ⚡ Laag | ✅ Gerealiseerd (v1.5.0) |
| **5.6 Adaptieve Tracking & Fiscale Jaarprognose** | 🔴 Hoog | 🟢 Hoog | 🔶 Gemiddeld | ✅ Gerealiseerd (v1.5.0) |
| **6.1 Foutarme Export-voorcontrole & Periode-compleetheid** | 🔴 Zeer hoog | 🟢 Zeer hoog | 🔶 Gemiddeld | ✅ Gerealiseerd (v1.6.0) |
| **6.2 Begrijpelijke Tracking-betrouwbaarheid in Diagnostiek** | ⚪ Laag | 🟢 Hoog | ⚡ Laag | ✅ Gerealiseerd (v1.6.0) |
| **6.3 Projectcode-voorstellen & Terugkerende Regels** | ⚪ Laag | 🟢 Hoog | ⚡ Laag | ✅ Gerealiseerd (v1.6.0) |
| **6.4 Herstelbare Wijzigingen (Undo Snackbar)** | ⚪ Laag | 🟢 Hoog | ⚡ Laag | ✅ Gerealiseerd (v1.6.0) |
| **6.5 Back-up Herstel- en Integriteitscheck** | 🟡 Gemiddeld | 🟢 Hoog | ⚡ Laag | ✅ Gerealiseerd (v1.6.0) |

---

## 5. Beheer & Kwaliteitsborging

Voor elke oplevering binnen dit PvA gelden de strikte CIMDriver kwaliteitsstandaarden:
1. **Unit Tests:** Alle berekeningen (afstand, tellersynchronisatie, urennormalisatie, confidence scoring) worden gedekt door JUnit-tests via `./gradlew testDebugUnitTest` en XCTest via `xcodebuild test`.
2. **5-Bestands Versiebeheer:** Versie-ophoging wordt synchroon doorgevoerd in `build.gradle.kts`, root `version.json`, `android/version.json`, `README.md` en `CHANGELOG.md`.
3. **Bilingual Docs:** Documentatie en downloadpagina's in `docs/` worden altijd bijgewerkt in zowel het Nederlands (`.md`) als het Engels (`.en.md`).
4. **Keystore Integrity:** Android-releases worden gesigneerd via `android/keystore.properties` met `release.jks`.
5. **Cross-Platform Pariteit:** Functionaliteiten in Fase 5 worden na Android direct gespiegeld naar iOS om beide apps gelijkwaardig te houden.

