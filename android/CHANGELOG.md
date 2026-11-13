# Changelog

Alle noemenswaardige wijzigingen aan dit project zullen in dit bestand worden bijgehouden.

## [1.8.0] - 2026-11-13

### Toegevoegd & Verbeterd
- **Fase 8: Fiscale Maandafsluiting, Route-Presets & Odometer Audit-Bewijs:**
  - **Fiscale Maandafsluiting & Lokale Deel-notificatie (Privacy-First):** Op de 1e van elke kalendermaand ontvangt de gebruiker een lokale Android-notificatie om de rittenadministratie van de zojuist afgesloten maand fiscaal te verifiëren en direct met de boekhouder te delen via het native deelsheet (PDF/CSV). In de eerste 7 dagen van de maand toont het Dashboard tevens een discrete tegel *"Maandafsluiting gereed"*.
  - **Favoriete Routes als 1-Tik Presets & Triage-Match:** Bij handmatige invoer van ritten (`AddTripScreen`) verschijnen opgeslagen favoriete routes direct als tikbare chips die vertrek- en aankomstadres, categorie en eventuele projectcode automatisch invullen. Bij de automatische rittriage krijgen ritten die matchen met een favoriete route automatisch 95% betrouwbaarheid en de juiste fiscale categorie toegewezen.
  - **Odometer Fotobewijs in Historie & PDF-Bijlage:** Dashboardfoto's gemaakt tijdens tellerstand-verificaties (ML Kit OCR) zijn nu direct als tikbare miniatuur te bekijken in de km-controle analyse (`OdometerCheckAnalysisScreen`) met een schermvullende preview. Daarnaast worden tellerstandcontroles met bijbehorende dashboardsfoto's optioneel opgenomen als audit-appendix achterin het officiële Belastingdienst PDF-rapport.
  - **Multi-Voertuig Wagenpark-kaart:** Voor gebruikers met 2 of meer actieve voertuigen toont het Dashboard een geconsolideerde wagenparkkaart met per voertuig de actuele tellerstand, ritten en kilometers deze maand, en een visuele voortgangsindicator van de 500 km privégrens.

## [1.7.0] - 2026-11-13

### Toegevoegd & Verbeterd
- **Fase 7: Slimme Werkuren-Begeleiding, Triage & Target-Coaching:**
  - **Werkuren-Reviewlijst & Dagelijkse Inbox-integratie:** Openstaande werkdagen (ontbrekende eindtijd, twijfel over pauzes of afwijkingen t.o.v. het basisrooster) worden nu gebundeld in de Dagelijkse Uitzonderingen-Inbox (`DailyInboxCard`) op het Dashboard en direct in het Werkurenscherm.
  - **Zachte Voorstellen voor Eindtijd & Pauze:** Automatische afleiding van ontbrekende vertrektijden via de geregistreerde rit naar huis/kantoor of via het dagspecifieke basisrooster (`workDays`). Automatische pauzesuggestie conform de Nederlandse Arbeidstijdenwet (minimaal 45 min bij $\ge$ 8u en 30 min bij $\ge$ 4.5u).
  - **Werkdagstatus in Begrijpelijke Mensentaal:** Duidelijke statusbadges per werkdag (bijv. *"Vrijwel zeker compleet (7.5 uur netto, 30m pauze)"*, *"Eindtijd ontbreekt (voorstel: 17:00)"*, *"Lange werkdag (8.2u) zonder pauze"* of *"Geregistreerd buiten reguliere werkdagen"*).
  - **Target-Coaching met Dynamisch Dagtempo:** De urentargets berekenen nu dynamisch de resterende actieve werkdagen in de lopende periode op basis van het ingestelde basisrooster, met een motiverende coachingstekst (bijv. *"Nog 16.5 uur nodig over 2 resterende werkdagen (8.3 u/dag)"*).
  - **1-Tik Goedkeuring met Ongedaan Maken (Undo Snackbar):** Individuele werkdagen en alle hoog-betrouwbare werkdagen kunnen in één tik worden goedgekeurd, met een directe Snackbar-optie om de actie zonder dataverlies terug te draaien.

## [1.6.0] - 2026-11-13

### Toegevoegd & Verbeterd
- **Fase 6: Betrouwbaarheid, Audit & Foutloze Export:**
  - **Foutarme Export-voorcontrole & Registratie-compleetheid (ExportPreCheckDialog):** Vóór het genereren van een PDF- of CSV-export valideert de app de gekozen periode op openstaande beoordelingen, kilometerstand-onderbrekingen en routeafwijkingen. Biedt een directe 1-tik goedkeuringsactie en toont bij volledige sluitendheid het officiële Belastingdienst-conformiteitsstempel.
  - **Uitlegbare Ritvoorstellen & Automatische Projectcodes:** Bij ritten van en naar bekende klantlocaties wordt nu automatisch de bijbehorende projectcode voorgesteld en vooringevuld, inclusief een heldere toelichting over de herkomst van de suggestie.
  - **Begrijpelijke Tracking-betrouwbaarheid in Diagnostiek:** Het diagnostiekscherm bevat nu bovenaan een intuïtieve statuskaart ("Optimaal" vs. "Aandacht vereist") die in duidelijke mensentaal uitlegt welke achtergrondpermissies (batterij-optimalisatie, achtergrondlocatie, notificaties) actief zijn, met directe 1-tik herstelknoppen.
  - **Herstelbare Wijzigingen (Undo Snackbar):** Bij het snel goedkeuren van ritten op het Dashboard of in de rittenlijst verschijnt een `[Ongedaan maken]` actie via Snackbar, zodat vergissingen direct zonder dataverlies kunnen worden teruggedraaid.
  - **Back-up Integriteits- & Herstelcheck:** In Gegevensbeheer (`DataManagementScreen`) toont een nieuwe statuskaart direct het tijdstip, de bestandsgrootte en een gevalideerde herstelbaarheidsstatus van de meest recente automatische back-up.

## [1.5.0] - 2026-11-13

### Toegevoegd & Verbeterd
- **Fase 5: Assisted Decision-Making & Slimme Rust:**
  - **Dagelijkse Uitzonderingen-Inbox ("Daily Triage"):** Eén overzichtelijke inbox-kaart op het Dashboard die alle openstaande items bundelt (te beoordelen ritten, onbevestigde werkdagen, ontbrekende kilometerstanden en concept-tankbeurten). Inclusief een 1-tik goedkeurknop om alle ritten met hoge betrouwbaarheid in één keer af te handelen.
  - **Confidence Score & Zachte Reviewvoorstellen:** Elk reviewgeval krijgt een dynamische betrouwbaarheidsscore (0–100%) en een duidelijke menselijke redenatie. In ritdetails kan het voorstel direct met één tik worden overgenomen.
  - **Ritaudit & Contextuele Diagnostiek:** Nieuwe transparante ritauditkaart in ritdetails met vermelding of de rit via de GPS-achtergrondservice of handmatig is gestart, welke bluetooth/voertuigverbinding actief was en of er privé-omrijkilometers zijn verrekend.
  - **Permission & Battery Health Monitor:** Proactieve bewaking van essentiële achtergrondinstellingen (batterij-optimalisatie/doze-mode uitschakeling, notificaties, locatietoegang en Bluetooth) met een directe herstelknop bij potentiële problemen.
  - **Netwerk- & Securityhygiëne:** In `AndroidManifest.xml` is het permissieve `usesCleartextTraffic="true"` vervangen door een restrictieve `network_security_config.xml`. Externe API's (RDW, OSRM, Nominatim) communiceren strikt via HTTPS en onversleuteld verkeer is uitsluitend toegestaan voor de lokale Zepp-companion op localhost (`127.0.0.1:8765`).
  - **Adaptieve Tracking & Fiscale Jaarprognose:** GPS-jitter bij stilstand (bijvoorbeeld voor stoplichten of tijdens parkeren) wordt adaptief gefilterd om onnodige afstandstoename te voorkomen. Op het dashboard toont de privékilometerkaart nu een nauwkeurige extrapolatie naar het einde van het fiscale jaar met een tijdige waarschuwing bij nadering van de 500 km grens.

## [1.4.1] - 2026-11-13

### Opgelost & Verbeterd
- **Hotfix opstartcrash (Room database-migratie):** Crash opgelost die direct bij het openen van de app optrad doordat in Room-migratie 49->50 een typefout zat in de tabelnaam (`fuel_fill_ups` i.p.v. `fuel_fillups`). Tevens directe migratie 48->50 toegevoegd en migratiestatements voorzien van veilige foutafhandeling.

## [1.4.0] - 2026-11-13

### Toegevoegd & Verbeterd
- **Belastingdienst Fiscale Compliance (Begin- & Eindstand in Export):** Volledige implementatie van de eisen van de Belastingdienst (art. 3.17 Wet IB 2001). Zowel de CSV- als PDF-export bevatten nu per rit de begin- en eindtellerstand. De PDF-export is overgezet naar een strak liggend A4-formaat (Landscape 842x595) met een professionele kop (voertuiggegevens & fiscale periode) en een totalenblok onderaan (totaal gereden km, zakelijk, woon-werk, privé en privé-omrijden).
- **Registratie van Privé-omrijkilometers:** Ondersteuning voor het apart opgeven van privé-omrijkilometers bij zakelijke en woon-werk ritten. Deze kilometers worden automatisch afgetrokken van het zakelijke kilometrage en meegeteld voor de fiscale 500km-privégrens.
- **Automatische RDW Kentekencheck:** Bij het toevoegen van een voertuig worden merk, handelsbenaming, bouwjaar en aandrijving (EV, PHEV of ICE) nu met één tik direct en gratis opgehaald via de officiële RDW Open Data API.
- **Live Rit-Toggle tijdens het rijden:** Wissel direct tussen Zakelijk en Privé tijdens een actieve rit zonder deze te hoeven beëindigen. Beschikbaar via de actieve notificatie en op het dashboard van Android Auto.
- **Interactieve Home Screen Widget:** Vernieuwde Glance AppWidget voor het startscherm. Toont tijdens het rijden direct de actieve ritstatus met knoppen om te wisselen of te stoppen, en in rust de maandtotalen met snelle startknoppen voor Zakelijk en Privé.
- **Werkuren delen per Week of Maand:** Exporteer en deel werkuren direct gefilterd per individuele week of maand naar PDF of CSV, compleet met een handige totaalregel met gewerkte uren.
- **Kalenderweergave Dag-Inspectie:** Tik op een dag in de rittenkalender om alle ritten van die dag in een overzichtelijke pop-up te bekijken met directe doorklikmogelijkheid naar ritdetails.
- **Doorklikbare Doelen:** Doelvoortgangskaarten op het dashboard zijn nu direct klikbaar om uren- en omzetdoelen te beheren.
- **Nederlandse Lokalisatie Android Auto:** Vaste Engelse rittypen in het car-interface zijn vervangen door nette Nederlandse labels (*Woon-werk*, *Zakelijk overleg*, *Klantbezoek*, *Klant declarabel*, etc.) en uitgebreid met een directe optie voor *Privé*.
- **EV Laadsessies & Tariefdifferentiatie:** Handige presets voor het registreren van laadsessies bij elektrische auto's (Thuis € 0,28/kWh, Publiek € 0,45/kWh, Snellader € 0,69/kWh) met automatische kostenberekening.

## [1.3.7] - 2026-11-13

### Toegevoegd & Verbeterd
- **Betrouwbare automatische tankstation- en laadpaaldetectie:** De detectie van tankstations en laadpalen is grondig vernieuwd met een vergrote zoekstraal (150m) en een native Android Geocoder fallback die bekende merken (Shell, BP, Total, Esso, Texaco, TinQ, Tango, AVIA, Fastned, etc.) direct herkent.
- **Herkenning bij tussenstops (mid-trip):** Wanneer het contact/Bluetooth tijdens een rit tijdelijk wordt verbroken bij een pomp of laadpaal, wordt direct gedetecteerd dat er getankt wordt, zelfs als de rit na de pauze gewoon weer verdergaat.
- **Directe pop-up notificatie:** Meldingen over tank- en laadsessies gebruiken nu een eigen notificatiekanaal met hoge prioriteit (`FuelAlertChannel`). Tikken op de melding opent direct het registratiescherm met de juiste pomp, datum en tellerstand alvast ingevuld.
- **Dashboard herinneringskaart:** Openstaande concept-tankbeurten worden nu direct en prominent als actiekaart op het dashboard getoond met snelle knoppen voor "Gegevens invoeren" en "Negeren".
- **Slim rekenhulpje bij invoer:** Bij het invullen van liters en prijs per liter wordt het totaalbedrag automatisch berekend (en vice versa bij invullen van liters en totaalbedrag).
- **Voertuigfilter in brandstofoverzicht:** Gebruikers met meerdere voertuigen kunnen nu in het brandstofoverzicht direct schakelen tussen auto's.
- **Statuscorrectie in Android Auto:** Beoordeelde ritten via Android Auto worden nu correct met status `DONE` geregistreerd.

## [1.3.6] - 2026-11-13

### Toegevoegd & Verbeterd
- **Slimme vertrekdetectie bij parkeergarages:** Bepaalt automatisch het juiste vertrekadres wanneer je uit een parkeergarage vertrekt waar nog geen GPS-dekking is. Het geparkeerde adres van de vorige rit en opgeslagen locaties worden binnen een uitrijradius (1500m) nauwkeurig gekoppeld.
- **Automatische afstandscorrectie op basis van route-opvraag:** Wanneer de geregistreerde GPS-afstand korter is dan de werkelijk gereden route doordat satellieten pas buiten de garage werden ontvangen, wordt de ritafstand automatisch gecorrigeerd naar de officiële route-afstand via OSRM.
- **Kilometerteller synchronisatie:** De eindstand van de auto-kilometerteller wordt berekend op basis van de gecorrigeerde ritafstand, waardoor kilometerafwijkingen en gaten door parkeergarages structureel worden voorkomen.
- **Naadloze kaartverbinding:** Als de eerste GPS-fix buiten de garage plaatsvindt, wordt de route op de kaart automatisch aangevuld vanaf de parkeergarage/het vertrekadres.
- **Directe vertrekherkenning bij Bluetooth-verbinding:** Bij het inschakelen van het contact in een parkeergarage wordt het vertrekadres alvast vooraf ingevuld op basis van de laatste rit.

## [1.3.5] - 2026-11-02

### Toegevoegd & Verbeterd
- **Rittenoverzicht per maand delen (PDF en CSV):** Gebruikers kunnen nu ritten exporteren en delen gefilterd per specifieke maand in plaats van alleen het volledige archief.
- **Maandsecties met directe deelknop:** Het rittenoverzicht is nu gegroepeerd per maand, inclusief subtotalen (aantal ritten en gereden kilometers) en een directe "Deel"-knop per maandsectie.
- **Gepersonaliseerde documenttitels en bestandsnamen:** PDF- en CSV-rapporten bevatten nu de specifieke maandnaam in zowel de bestandsnaam (`cimdriver_ritten_Oktober_2026.pdf`) als de paginakop.
- **Uitgebreid exportmenu in de navigatiebalk:** Het exportmenu biedt direct toegang tot export per maand én export van het volledige archief.

## [1.3.4] - 2026-10-30

### Opgelost & Verbeterd
- **Oplossing voor crash bij ritdetails (MapLibre):** Crash opgelost bij het openen van ritten met een enkel geocodeerpunt of handmatige bestemming (zoals "Supermarkt / Boodschappen"). De kaart gebruikt nu een veilige camerazoom in plaats van ongeldige bounding-box berekeningen.
- **Directe bewerk- en verwijderknoppen in ritdetails:** Een prominente bewerkknop toegevoegd in de bovenbalk en een directe verwijderoptie met bevestiging in het optiemenu.
- **Tijdscorrectie bij herstel van overlappende gaten:** Aangemaakte correctieritten waarborgen nu altijd een chronologisch sluitende start- en eindtijd, zelfs wanneer eerdere ritten elkaar in tijd overlapten.
- **Veilige duurweergave:** Negatieve tijdverschillen bij overlappende ritten worden afgevangen en weergegeven als positieve reistijd.

## [1.3.3] - 2026-10-30

### Toegevoegd & Verbeterd
- **Tellerstanden chronologisch opnieuw doorberekenen (`OdometerRecalculator`):** Nieuwe functie om alle tellerstanden van een voertuig chronologisch opnieuw door te rekenen vanaf de startstand op basis van geregistreerde ritafstanden en uitgevoerde kilometercontroles.
- **Behoud van audit-trail gaten:** Gaten die zijn geconstateerd bij tussentijdse kilometercontroles blijven netjes bewaard als openstaande auditpunten zodat de administratie 100% fiscaal sluitend en controleerbaar blijft.
- **Interactieve Preview & Bevestigingsdialoog:** Direct inzicht in het aantal ritten dat wordt aangepast, behouden controlepunten en overblijvende gaten voordat de herberekening wordt toegepast.
- **Flexibele rit-typen bij gaten oplossen:** Bij het toevoegen van een aansluitende rit kan nu direct gekozen worden tussen Privé (`PERSONAL`), Zakelijk (`BUSINESS`) en Woon-werk (`COMMUTE`).
- **Directe actuele tellersynchronisatie (`SYNC_VEHICLE_ODOMETER`):** Verschillen met de actuele tellerstand aan het einde kunnen nu met één klik worden gecorrigeerd naar de eindstand van de voorgaande rit, zonder overbodige ritten aan te maken.
- **Tijdsvolgorde bij overlappende ritten:** Voorkomen van geïnverteerde datumweergaven in de gatanalyse wanneer ritten elkaar in tijd overlappen.

## [1.3.2] - 2026-10-30

### Opgelost & Verbeterd
- **Oplossen afrondingsfouten bij meter- en ritberekeningen:** Integer-afkapfouten (`distanceMeters / 1000`) in de kilometertelling gecorrigeerd naar wiskundige afronding (`roundToInt()`). Hiermee wordt voorkomen dat ritten structureel meters verliezen en er spontaan gaten ontstaan in de zakelijke ritten.
- **Geen fictieve kilometerverdeling (`DISTRIBUTE` verwijderd):** De optie om gaten kunstmatig evenredig te verspreiden over eerdere ritten is volledig uitgefaseerd. Dit voorkomt onrealistische afwijkingen op bestaande routes en waarborgt een fiscaal zuivere audit trail.
- **Flexibele adressen bij gatanalyse:** In het herstelscherm voor kilometergaten kunnen vertrek- en aankomstlocaties nu direct handmatig worden aangepast in plaats van vast te zitten aan standaard adressen.
- **Snelle suggesties voor veelvoorkomende korte gaten (4-5 km):** Handige keuzeknoppen toegevoegd voor veelvoorkomende privéritten ("Boodschappen", "Lokaal", "Tanken") en verklaarde gaten ("Tellerafwijking / kalibratie", "Garage / APK", "Proefrit").
- **Afstandsindicatie bij frequente routes:** Veelgebruikte routeknoppen tonen nu direct de berekende afstand (bijv. `(38 km)`) om vergissingen bij snelle ritinvoer te voorkomen.
- **Verbeterde waarschuwingen bij controle:** Waarschuwing bij afwijkingen groter dan 30 km en consistente weergave van decimalen bij ritten.

## [1.3.1] - 2026-10-30

### Toegevoegd & Verbeterd
- **Sluitende kilometerregistratie (Belastingdienst-proof):** Complete ketencontrole en sluitende administratie tussen opeenvolgende ritten en kilometercontroles.
- **Interactieve controle-analyse:** Bij het registreren van een kilometercontrole met afwijking wordt de stand niet meer blind overschreven. De app toont direct een analyse en biedt herstelopties:
  - Gat toewijzen als zakelijke of privé rit.
  - Gat evenredig verdelen over gereden ritten in de periode.
  - Alleen als meetcorrectie registreren met duidelijke toelichting voor de Belastingdienst.
- **Achteraf gat-detectie & review:** Nieuw overzicht "Gat-detectie & Ketencontrole" binnen de kilometercontroles. Detecteert automatisch alle gaten en discontinuïteiten in de rithistorie met de mogelijkheid tot batch-herstel.
- **PDF Integriteitsrapport:** Exporteer een officieel Belastingdienst-conform integriteitsrapport van alle controles, afwijkingen, gat-detecties en audit-resoluties.
- **Leesbaarheid Dashboard Targets:** Grote getallen (uren en omzet) op het dashboard en in het doelenoverzicht worden nu geformatteerd met duizendtallen scheidingstekens (bijv. € 85.000,00 en 1.250,0 uur).

## [1.3.0] - 2026-10-26

### Opgelost & Verbeterd
- **Doelen op het dashboard (Bonus/Uren/Omzet):** Gewerkte uren worden nu betrouwbaar verwerkt en getoond in de doelen op het dashboard.
- **Ondersteuning voor status:** Zowel goedgekeurde (`APPROVED`) als voltooide (`DONE`) werkdagen tellen mee voor de doelvoortgang.
- **Projectcode & Klant Matching:** Slimme resolutie toegevoegd waardoor werkdagen zowel via `projectCodeId` als tekstuele `projectCode` correct worden gekoppeld aan het bijbehorende project en uurtarief.
- **Automatische registratie van project-ID:** `TrackingService` slaat bij automatische werkdag- en ritregistratie nu ook direct de gekoppelde `projectCodeId` op.
- **Behoud project-ID bij goedkeuring:** Bij het goedkeuren van een werkdag in het overzicht blijft de gekoppelde projectcode behouden.

## [1.2.7] - 2026-10-09

### Toegevoegd
- **Uren- en omzetdoelen:** Targets kunnen nu zowel op uren als omzet worden ingesteld en aan een klant of projectcode worden gekoppeld.
- **Vaste projecttarieven:** Het uurtarief wordt per projectcode ingesteld en gebruikt om gerealiseerde omzet te berekenen.
- **Pro-rata jaartarget:** De instelbare datum in dienst bepaalt vanaf welke kalendermaand de jaartarget meetelt; indiensttreding in september geeft 4/12 van de jaartarget.
- **Dashboardtargets:** Het getoonde doelbedrag en doeluren zijn nu ook naar rato aangepast aan de datum in dienst.
- **In-app help:** Nieuwe uitleg toegevoegd over het instellen van uren- en omzetdoelen, projecttarieven en pro-rata berekening.

## [1.2.6] - 2026-10-09

### Toegevoegd
- **Zepp Companion Toggle:** Nieuwe instelling toegevoegd om de Zepp OS integratie server (voor smartwatches) in- of uit te schakelen ter verbetering van batterijduur.
- **Verbeterde UI voor instellingen:** De optie om projectcodes automatisch te archiveren is verplaatst naar het Werkuren-menu en opnieuw opgemaakt als een gebruiksvriendelijke toggle-schakelaar.


## [1.2.5] - 2026-10-09

### Toegevoegd
- **Klanten & Projecten:** Mogelijkheid toegevoegd om projectcodes aan te maken en te koppelen aan klanten.
- **Projectcodes Archiveren:** Projectcodes die in gebruik zijn (voor ritten of targets) worden bij verwijdering niet meer gewist, maar gearchiveerd (soft delete). Via een instelling in de app ("Toon gearchiveerde items") kunnen deze weer zichtbaar worden gemaakt en hersteld.
- **Automatisch Archiveren:** Onder Instellingen -> App Gedrag is een functie toegevoegd om ongebruikte projectcodes na een instelbaar aantal dagen automatisch te archiveren.
- **Uren Targets:** Bij het instellen van uren targets kan er nu gekozen worden uit projectcodes om de uren per project/klant bij te houden.
- **Testdata:** Het interne testscript is geüpdatet zodat klanten en projecten direct goed worden ingeregeld op basis van gekoppelde adressen van het type 'Klant'.

## [1.2.4] - 2026-10-02

### Opgelost
- **Zakelijke auto's:** De begin- en einddatum kunnen nu ook bij zakelijke voertuigen worden aangepast.
- **PHEV-registratie:** Plug-in hybriden worden niet meer per ongeluk als alleen 'laden' behandeld; standaard blijft de registratie op tanken gericht, tenzij het voertuig puur elektrisch is.
- **UI:** De voertuig- en tank-/laadregistratie zijn consistent afgestemd op het type auto.

## [1.2.3] - 2026-10-02

### Opgelost
- **Voertuigdata:** Het aanpassen van de begin- en einddatum bij een bestaande auto werkt nu weer correct, ook als de waarschuwing voor privégebruik is uitgeschakeld.
- **UI:** De datumvelden blijven zichtbaar en kunnen direct worden aangepast of gewist vanuit de voertuiginstellingen.

## [1.2.2] - 2026-10-02

### Toegevoegd
- **Kilometercontroles:** Nieuw scherm voor maandelijkse km-controles per voertuig, met een overzicht van alle historische controles.
- **OCR voor km-stand:** Fotos van de kilometerstand kunnen nu automatisch worden gelezen met ML Kit, zodat de invoer sneller en robuuster verloopt.
- **Voertuigwerkstroom:** Verbeterde navigatie en voertuigdetails voor het bekijken en beheren van odometer-controles.

## [1.2.1] - 2026-10-02

### Opgelost
- **Taal:** Ontbrekende Nederlandse vertaling ("Tanken / Laden") in het navigatiemenu toegevoegd in plaats van "Fuel".

## [1.2.0] - 2026-10-02

### Toegevoegd
- **Tankbeurten en Laadsessies:** Nieuwe functionaliteit toegevoegd voor het registreren van brandstof tankbeurten en EV laadsessies, inclusief een handig overzicht in het hoofdmenu.
- **Proximity Detectie:** Automatische herkenning en opbouw van een tankbeurt of laadsessie na langdurige stilstand bij een tankstation of laadpaal (vereist achtergrondlocatie).
- **Fiscale Kilometergrens:** Bij privéauto's kan nu de datum ingebruikname worden ingesteld zodat de maximale grens van 500 privékilometers pro-rata (naar rato) wordt berekend voor het huidige jaar.
- **Verbeterde Voertuig Selectie:** Uniforme voertuig selectie widget in alle schermen geïmplementeerd.

## [1.1.5] - 2026-10-01

### Toegevoegd
- **Classificatieregels:** Auto-aanvullen en adresboek selectie toegevoegd voor classificatieregels. Vanaf nu kun je net als bij ritten eenvoudig adressen selecteren uit je adresboek.

## [1.1.4] - 2026-09-24

### Opgelost
- **Crash bij opstarten:** Oplossing voor een `IllegalStateException` database crash (Room migratiefout) die kon optreden bij het updaten van de app of het herstellen van een backup afkomstig van een eerdere versie.

### Verwijderd
- **Splash Screen:** Het custom splash screen (met de extra 2 seconden vertraging) is verwijderd, waardoor de app nu direct opstart naar het dashboard.

## [1.1.3] - 2026-09-21

> **LET OP:** Door vernieuwde, veiligere ondertekeningscertificaten vereist versie 1.1.3 een schone installatie. Maak **eerst een backup of export** via de oude versie, de-installeer deze volledig, en installeer pas daarna versie 1.1.3 om je data niet te verliezen!

### Gewijzigd
- **UI:** Het menu-item "Mijn auto's" is verplaatst uit het Instellingen-scherm naar het hoofdmenu (hamburger menu) voor een netter overzicht.

### Toegevoegd
- **Buiten werktijden ritclassificatie:** Ritten die buiten kantooruren vallen worden nu beter ondersteund door rekening te houden met specifieke werkdagen, start- en eindtijden per dag voor geautomatiseerde privé-classificatie.
- **Interactieve Notificatie:** Tijdens het rijden toont de tracking notificatie nu een handige "Stop Rit" (Stop Trip) knop waarmee de rit direct afgesloten kan worden zonder de app te hoeven openen.

## [1.1.2] - 2026-09-21

### Gewijzigd
- **Robuuste adres-matching:** Het matchingsysteem rondom kantoor- en thuislocaties is sterk verbeterd via GPS met slimme prioriteiten, waardoor ritten veel preciezer aan je werk/woonadres gekoppeld worden (in plaats van aan andere gebouwen binnen dezelfde straal).

### Toegevoegd
- **GPS Kaartweergave in Adresboek:** Bij het beheren van adressen zie je nu exact op de kaart welke pin (met een 200m actieradius) de app hanteert.

## [1.1.1] - 2026-09-18

### Toegevoegd
- **Tijdgebonden classificatie:** Ritten buiten je ingestelde werkuren (bijv. weekend) worden nu standaard gemarkeerd als privérit (`PERSONAL`).
- **Routeherkenning:** Wanneer je 3 keer exact dezelfde route rijdt met dezelfde classificatie, zal de app automatisch suggereren hier een vaste regel voor aan te maken.
- **UI:** Het Instellingen-scherm toont nu ook expliciet de exacte start- en eindadressen bij classificatieregels in plaats van alleen het type, zodat deze beter beheerd en verwijderd kunnen worden.

## [1.1.0] - 2026-09-17

### Toegevoegd
- **Notitie bij afwijkende kilometers:** Als de gereden afstand meer dan 10% (én minimaal 2 km) afwijkt van de verwachte kortste route, wordt er tijdens de beoordeling nu verplicht om een reden (bijv. file, wegafsluiting) in te vullen in het notitieveld.

## [1.0.9] - 2026-09-17

### Toegevoegd
- **Adresnamen in Overzichten:** Als een start- of eindlocatie voorkomt in je adresboek, wordt nu netjes de opgeslagen naam (zoals "Kantoor" of "Thuis") getoond in het rittenoverzicht en de ritdetails, in plaats van alleen het straatadres.

## [1.0.8] - 2026-09-17

### Gewijzigd
- **Stricte Adres Matching:** De logica voor het koppelen van start/eind locaties aan je adresboek is volledig herschreven met een prioriteitenmodel. Het willekeurig overschrijven van adressen bij meerdere matches in de buurt is opgelost: de dichtstbijzijnde kandidaat wint nu áltijd.

## [1.0.7] - 2026-09-16

### Gewijzigd
- **Versie Bump:** Versie verhoogd naar 1.0.7 voor release.

## [1.0.6] - 2026-09-16

### Gewijzigd
- **Adresherkenning:** De adresmatching is aanzienlijk robuuster gemaakt door automatisch huisletters (zoals 'b' of 'e') te negeren wanneer deze in hetzelfde pand liggen.
- **UI:** Het toevoegen/bewerken van werkdagen gebruikt nu hetzelfde geavanceerde adres-zoekveld als bij ritten, inclusief snelle toegang tot het adresboek.
- **APK Optimalisatie:** De Release APK is weer volledig geoptimaliseerd voor ARM-processoren (~48MB).

## [1.0.5] - 2026-09-16

### Opgelost
- **Urenregistratie:** Uren worden weer correct afgerond op basis van de specifieke werktijden per dag in plaats van de globale standaard instellingen. De puntnotatie (bijv. 8.30) wordt nu ook goed ondersteund.
- **Adresherkenning:** De zoekradius voor proximity matching is verder verruimd naar 400 meter, wat de herkenning van opgeslagen adressen (zoals kantoorpanden of thuis) verder verbetert.

## [1.0.4] - 2026-09-16

### Toegevoegd
- **Instelbare Vergoeding:** De zakelijke vergoeding per kilometer is nu instelbaar via de instellingenpagina en wordt direct doorgerekend op het dashboard.
- **Update Overslaan:** Het is nu mogelijk om een specifieke app update in de banner te negeren door op "Overslaan" te drukken.

## [1.0.3] - 2026-09-16

### Opgelost
- **Swipe Acties:** Bij het naar rechts swipen van een rit in het Ritten-overzicht wordt er nu netjes een pop-up menu geopend waarin je direct een keuze kunt maken (Zakelijk, Privé of Woon-werk).

## [1.0.2] - 2026-09-16

### Toegevoegd
- **Systeemdiagnostiek Uitbreiding:** Het diagnostisch scherm is flink uitgebreid met live batterij-optimalisatie controles, app-versie, actieve werkdag-status en recovery manager failsafe status.
- **In-App Updater:** De app checkt nu automatisch via GitHub of er een nieuwere versie beschikbaar is en toont een handige download-knop op het dashboard.

## [1.0.1] - 2026-09-16

### Toegevoegd
- **Systeemdiagnostiek:** Nieuw diagnostisch scherm voor direct inzicht in de state van de background trackingservice.
- **Boot Recovery:** Robuuste automatische hervatting van tracking na reboots of crashes, inclusief veilige back-off timers.

### Gewijzigd
- **Adresherkenning:** De maximale zoekradius voor proximity matching (kantoorpanden) is vergroot van 100m naar 250m om GPS-drift binnen panden beter te ondervangen.

### Opgelost
- **Deprecation:** Oplossing voor Material 3 deprecation waarschuwingen (vervanging van `Icons.Default.ArrowBack` door `Icons.AutoMirrored.Filled.ArrowBack`).

## [1.0.0] - 2026-09-16

### Toegevoegd
- **Eerste officiële release van CIMDriver!**
- Volledig geautomatiseerde ritregistratie via Bluetooth triggers.
- Automatische urenregistratie voor kantoor- en klantbezoeken.
- Slimme adres-classificatie (Thuis, Werk, Klant) via Room-database.
- Modern Material 3 (Compose) Dashboard en overzichten.
- Volledige, gestructureerde referentie- en architectuurdocumentatie.
