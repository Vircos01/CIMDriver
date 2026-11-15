# 🚀 Wat is er nieuw? (iOS Changelog)

Blijf op de hoogte van de laatste updates, nieuwe functies en bugfixes voor de iOS versie van CIMDriver.

---

## Versie 0.0.15 Alpha (Huidige Versie)
*Releasedatum: 15 november 2026*

!!! success "Fase 9: Tracking-Architectuur & Stabiliteit (Android Feature Sync)"
    * **Stabiliteit & Navigatie-uitlijning:**
        * **Tracking Architectuur Synchronisatie:** Harmonisatie van de ritclassificatiemethoden en eenduidige afhandeling van ritten met status `TO_REVIEW`.
        * **Privacy-First Data-extractie:** Expliciete bescherming van de lokale datalaag conform de verscherpte privacyregels op Android.

## Versie 0.0.14 Alpha
*Releasedatum: 13 november 2026*

!!! success "UI-Consistentie & Design System Pariteit"
    * **UI-Consistentie & Design System Pariteit:**
        * **Design System Tokens & ContentUnavailableViews:** Harmonisatie van de weergave en lege statussen met de nieuwe Jetpack Compose design tokens op Android.
        * **Verbeterde Leeg-status in Dagelijkse Inbox:** Verduidelijking bij een bijgewerkte berichtenbox dat alle ritten, werkuren en controles zijn afgehandeld.

## Versie 0.0.13 Alpha
*Releasedatum: 13 november 2026*

!!! info "Patch: Wagenparkoverzicht naar rato & Inbox verduidelijking"
    * **Wagenparkoverzicht naar rato:** De privégrens op de wagenparkkaart (`multiVehicleFleetCard`) berekent de limiet nu dynamisch *naar rato* (pro rata) conform de in- en uitdiensttredingsdata en de ingestelde jaargrens per specifiek voertuig, inclusief "(naar rato)" label en accurate statusmelding.
    * **Dagelijkse Inbox Leeg-status:** De melding bij een bijgewerkte berichtenbox verduidelijkt nu dat er geen openstaande ritten, werkuren óf meldingen ter beoordeling staan.

## Versie 0.0.12 Alpha
*Releasedatum: 13 november 2026*

!!! success "Fase 8: Fiscale Maandafsluiting, Route-Presets & Odometer Audit-Bewijs (Android Feature Sync)"
    * **Fiscale Maandafsluiting & Lokale Deel-notificatie (Privacy-First):** Maandelijkse lokale herinnering op de 1e van de maand en een discrete dashboardtegel in de eerste 7 dagen om de afgesloten maand direct via het native iOS ShareSheet (PDF/CSV) te delen met je accountant.
    * **Favoriete Routes als 1-Tik Presets & Triage-Match:** 1-tik presets in `ManualTripEntryView` die direct alle ritdetails invullen, en automatische matching in `TripClassification` met 95% betrouwbaarheid.
    * **Odometer Fotobewijs & PDF Audit-Appendix:** Fysieke foto's van tellerstanden worden bewaard, zijn te bekijken in de km-controle historie van `OdometerGapReviewView` (inclusief schermvullende weergave) en worden optioneel als audit-appendix met foto's toegevoegd aan de PDF-export.
    * **Multi-Voertuig Wagenpark-kaart:** Consolidated fleet card op het Dashboard voor wagenparken met $\ge$ 2 actieve voertuigen met 500 km privégrens indicator.

## Versie 0.0.11 Alpha
*Releasedatum: 13 november 2026*

!!! success "Fase 7: Slimme Werkuren-Begeleiding, Triage & Target-Coaching (Android Feature Sync)"
    * **Werkuren-Reviewlijst & Dashboard Daily Inbox:** Werkdagen die aandacht vragen (ontbrekende uitchecktijd, ontbrekende pauzes of parttime/weekend registraties) verschijnen geconsolideerd in de dagelijkse Inbox op het Dashboard en in het Werkuren-overzicht.
    * **Slimme Voorstellen voor Vertrektijd & Pauze:** Automatische detectie van de retourrit naar huis voor de vertrektijd of toepassing van het dagspecifieke basisrooster (`workDays`). Arbowet-conforme pauzevoorstellen (minimaal 45 min bij $\ge$ 8u en 30 min bij $\ge$ 4.5u).
    * **Werkdagstatus in Begrijpelijke Mensentaal:** Transparante badges zoals *"Vrijwel zeker compleet (7.5 uur netto, 30m pauze)"*, *"Eindtijd ontbreekt (voorstel: 17:00)"* en *"Geregistreerd buiten reguliere werkdagen"*.
    * **Target-Coaching met Dynamisch Dagtempo:** Resterende actieve werkdagen worden berekend op basis van het ingestelde basisrooster, met motiverende begeleiding (bijv. *"Nog 16.5 uur nodig over 2 resterende werkdagen (8.3 u/dag)"*).
    * **1-Tik Goedkeuring met Ongedaan Maken (Undo Banner):** Snelle fiattering van voorgestelde werkdagcorrecties met een geanimeerde Undo-banner om wijzigingen direct te herstellen.

## Versie 0.0.10 Alpha
*Releasedatum: 13 november 2026*

!!! success "Fase 6: Betrouwbaarheid, Audit & Foutloze Export (Android Feature Sync)"
    * **Foutarme Export-voorcontrole (ExportPreCheckSheet):** Vóór het delen van PDF- of CSV-rapporten toont CIMDriver een interactieve sluitendheidscontrole op ongeclassificeerde ritten, kilometerstand-gaten en routeafwijkingen, compleet met directe 1-tik bulk-goedkeuring en fiscus-zegel.
    * **Uitlegbare Ritvoorstellen & Automatische Projectcodes:** Projectcodes worden nu automatisch afgeleid van gekoppelde klantadressen en voorgesteld in de reviewoverlay en het ritspecificatiescherm.
    * **Begrijpelijke Tracking-betrouwbaarheid in Diagnostiek:** Diagnostiek toont nu bovenaan een statusoverzicht met menselijke uitleg ("Optimaal" vs. "Aandacht vereist") en directe knoppen om iOS-locatievoorzieningen of notificaties te configureren.
    * **Herstelbare Wijzigingen (Undo Banner):** Batchclassificaties en snelle goedkeuringen in de rittenlijst tonen een snelle `[Ongedaan maken]` actie via een geanimeerde banner.
    * **Back-up Integriteits- & Herstelcheck:** Gegevensbeheer toont nu de recentheid, bestandsgrootte en gevalideerde herstelstatus van de laatste lokale JSON-back-up in `Documents/CIMDriver/Backups`.

## Versie 0.0.9 Alpha
*Releasedatum: 13 november 2026*

!!! success "Fase 5: Assisted Decision-Making & Dashboard Rust (Android Feature Sync)"
    * **Dagelijkse Uitzonderingen-Inbox:** Geconsolideerde triage-kaart op het Dashboard die openstaande ritten en niet-bevestigde werkdagen bundelt in één rustig overzicht.
    * **Confidence Scoring & Zachte Reviewvoorstellen:** Automatische betrouwbaarheidsscore (0–100%) op basis van classificatieregels, vaste routes en werktijden. In het ritdetailscherm kan de voorgestelde categorie direct met één tik worden overgenomen.
    * **Ritaudit & Context:** Uitgebreid contextblok in ritdetails met vermelding of de rit via de GPS-achtergrondservice of handmatig is gestart, welk voertuig gekoppeld was en of er privé-omrijden is verrekend.
    * **Adaptieve GPS-Jitter Filtering:** Onderdrukking van GPS-ruis bij stilstand (< 3m bij < 0.8 m/s binnen 30 seconden) om onnodige batterijbelasting en spookkilometers te voorkomen.
    * **Fiscale Jaarprognose voor Privékilometers:** De privégrenskaart berekent nu een betrouwbare extrapolatie naar het einde van het fiscale jaar en waarschuwt bij dreigende overschrijding van de 500 km grens.

## Versie 0.0.8 Alpha
*Releasedatum: 13 november 2026*

!!! success "Nieuwe functies & Verbeteringen (Android Feature Sync)"
    * **Fiscale compliance & Privé-omrijkilometers:** Volledige ondersteuning voor privé-omrijden binnen zakelijke en woon-werkritten conform de richtlijnen van de Belastingdienst. Privé-omrijkilometers tellen automatisch mee voor de 500 km grens en worden in mindering gebracht op zakelijke totalen.
    * **Liggend A4 PDF-rapport:** Vernieuwde PDF-export in liggend A4 formaat met sluitende ritdetails (start- en eindstand, omrijkilometers, categorie, adressen, ritdoel) en een overzichtelijk totalenblok.
    * **RDW Kentekencheck:** Eénknops RDW-koppeling bij voertuigbeheer; haalt direct merk, handelsbenaming en aandrijvingstype (ICE, PHEV, EV) op via de open data API van de RDW.
    * **Live Ritclassificatie-toggle:** Schakel tijdens een actieve rit direct tussen Zakelijk en Privé vanaf de statuskaart op het dashboard.
    * **Kalender dagtik-inspectie:** Tik op een datum in de rittenkalender om direct alle ritten van die dag te bekijken en te openen in het detailscherm.
    * **Periodieke Werkuren-export & Deelknop:** Direct werkuren per maand exporteren (PDF & CSV) via een handige deelknop per sectie, inclusief berekende totaalsom van gewerkte uren.
    * **Direct doelen beheren:** Navigeer direct naar het doelenbeheerscherm via de nieuwe "Beheren" knop of door op een doelkaart te tikken.
    * **EV & Laadpresets:** Ondersteuning voor elektrische voertuigen in brandstofbeheer met automatische kWh-weergave, laadtarief-badges en snelle presets voor Thuis (€0,28), Publiek (€0,45) en Snellader (€0,69).

## Versie 0.0.7 Alpha
*Releasedatum: 30 oktober 2026*

!!! success "Nieuwe functies"
    * **Controle op gaten in de kilometerregistratie:** Automatische detectie van ontbrekende of overlappende kilometers tussen opeenvolgende ritten, afwijkingen in de beginstand en verschillen met de actuele tellerstand van het voertuig.
    * **Tellerstanden herberekenen:** Mogelijkheid om alle kilometerstanden chronologisch opnieuw door te rekenen op basis van de ritafstanden en geregistreerde kilometerijkers (met behoud van geijkte checkpoints).
    * **Gaten oplossen:** Direct gaten oplossen via aansluitende ritten (privé, zakelijk of woon-werk met handige suggesties), de actuele tellerstand synchroniseren, of toelichtingen registreren voor de Belastingdienst (zoals APK, garagebezoek of proefrit).
    * **Verbeterde ritbewerking:** Direct bewerken en verwijderen van ritten vanuit het detailscherm, inclusief aanpasbare start- en eindtijden en kilometerstanden.
    * **Stabielere kaartweergave:** Veilige verwerking van ritten zonder routepunten of met één enkel punt.

## Versie 0.0.6 Alpha
*Releasedatum: 9 oktober 2026*

!!! success "Nieuwe functies"
    * **Klanten en projectcodes:** Beheer klanten en projectcodes, stel per project een vast uurtarief in en archiveer ongebruikte codes.
    * **Uren- en omzetdoelen:** Volg jaardoelen per uren, klant of project. Omzet wordt berekend met het vaste projecttarief.
    * **Doelen naar rato:** Stel de datum in dienst in. Bij een start op 1 september telt 4/12 van de jaartarget mee; het aangepaste doel is zichtbaar op het dashboard.
    * **Projectkeuze en zoeken:** Koppel projectcodes aan adressen, ritten en werkdagen en zoek werkuren op project, locatie of notitie.
    * **Kilometerstandherkenning:** Een foto van de kilometerstand kan met tekstherkenning worden uitgelezen; controleer de herkende waarde voor je opslaat.
    * **Classificatieregels:** Schakel regels in of uit, stel prioriteiten in en keur overeenkomende ritten automatisch goed.
    * **In-app help:** Nieuwe uitleg over projecttarieven en pro-rata targets.

## Versie 0.0.5 (Alpha)
*Release datum: September 2026*

!!! success "Nieuw"
    * **Lancering iOS Alpha:** De eerste gesloten testversie voor iOS is nu beschikbaar via TestFlight.
    * **Automatische Rittenregistratie:** Basisfunctionaliteit voor het automatisch detecteren van ritten op de achtergrond.
    * **Apple CarPlay Integratie (Beta):** Eerste opzet om ritten te starten en beheren vanaf je dashboard.

!!! info "Bekende Problemen"
    * Bij extreem stroombesparende modus kan iOS de app na 24 uur inactiviteit uitschakelen, waardoor ritten pas weer starten als de app handmatig geopend wordt. Dit wordt onderzocht.

<br>
<div align="center" markdown="1">
[:material-apple: Download iOS Alpha](../download/ios.md){ .md-button .md-button--primary }
</div>
