

# 🚀 Wat is er nieuw? (Changelog)

Blijf op de hoogte van de laatste updates, nieuwe functies en bugfixes voor CIMDriver. Wij rollen regelmatig updates uit om de automatische rittenregistratie nog beter, accurater en zuiniger te maken.

---

## Versie 1.9.4 (Huidige Versie)
*Releasedatum: 20 november 2026*

!!! info "Patch: Rustig Wagenpark Overzicht & Fiscale Standaardweergave"
    * **Rustig & Uniform Wagenpark Overzicht:** Verwijdering van overbodige overnamelabels (*"Vervangt..."*, *"Ingeruild voor..."*, *"Overgedragen naar..."* en dubbele uitsplitsingen) voor een rustig, professioneel en uniform dashboard.
    * **Standaard Actief Voertuig:** Het dashboard toont standaard direct de gegevens en gecombineerde fiscale grens van het actieve standaardvoertuig (166 km naar rato voor september t/m december).
    * **Robuuste Inruilkoppeling:** Gegarandeerde automatische koppeling van voorganger en opvolger via database-migratie 53 en in-memory fallback.

## Versie 1.9.3
*Releasedatum: 20 november 2026*

!!! info "Patch: Beginstand Tellerstand Corrigeren & Fiscale Autowissel / Inruil"
    * **Beginstand Tellerstand Corrigeren:** Het veld voor de beginstand is nu ontgrendeld voor bestaande voertuigen. Bij het opslaan kun je kiezen om alle ritten van die auto automatisch opnieuw door te rekenen of enkel de beginstand bij te werken.
    * **Fiscale Autowissel / Inruil (500 km-norm):** Volledige ondersteuning voor een wissel van leaseauto of inruil binnen hetzelfde belastingjaar. De pro-rata periode wordt gecombineerd over de totale periode van voorganger en opvolger, en de gereden privékilometers van de vorige auto worden overgedragen naar de opvolger conform de fiscale regels van de Belastingdienst.
    * **Wagenpark Overzicht Vernieuwd:** Duidelijke statustags (*"Vervangt..."* en *"Ingeruild voor..."*), inzicht in gecombineerde kilometers en voertuigen kunnen direct vanuit de overzichtskaart worden geopend voor bewerking.

## Versie 1.9.2
*Releasedatum: 16 november 2026*

!!! info "Patch: Kantoor-Wi-Fi Werkurenregistratie & Navigatie-Optimalisatie"
    * **Automatische Werkurenregistratie via Kantoor-Wi-Fi:** Forenzen die met de fiets, het openbaar vervoer of te voet naar kantoor reizen, kunnen werkuren nu automatisch laten vastleggen op basis van kantoor-Wi-Fi aanwezigheid, zonder dat er loze of fantoomritten in de rittenregistratie ontstaan.
    * **Voorrang voor Autotracking:** Indien de achtergronddienst een autorit detecteert, heeft die altijd voorrang en treedt de Wi-Fi werkurenmanager direct terug.
    * **Slimme Pauzedetectie & Buffer:** Automatische aftrek van pauze op kantoor (> 4 uur) en automatische registratie van pauzes buiten de deur (lunchwandeling van 10–120 minuten) met een configureerbare bufferperiode (standaard 15 minuten) tegen onbedoelde afsluiting.
    * **Adresboek Wi-Fi Koppeling:** Koppel het kantoor-Wi-Fi netwerk direct via het Adresboek met één tik op de knop.
    * **Herindeling Navigatiebalk:** De onderste menubalk is geoptimaliseerd met Dashboard, Ritten, Tanken / Laden en Werkuren. Het Adresboek is nu overzichtelijk geplaatst in het zijmenu (Drawer).

## Versie 1.9.1
*Releasedatum: 15 november 2026*

!!! info "Patch: Herstelbare Databaseback-ups & RFC 4180 CSV-Escaping"
    * **Herstelbare Databaseback-ups (`.zip`):** `BackupWorker` maakt nu periodiek een echte SQLite-databaseback-up (`.zip`) aan die daadwerkelijk gebruikt kan worden voor dataherstel bij een nieuw toestel of herinstallatie, inclusief automatische opschoning van oudere bestanden.
    * **Duidelijke Scheiding in Databeheer:** Het scherm Databeheer toont herstelbare `.zip`-back-ups en `.csv`-rittenexports nu strikt gescheiden, inclusief heldere toelichting en statuslabels.
    * **Volledige Context bij Rittenexports:** CSV-exports bevatten nu altijd alle opgeslagen adressen, classificatieregels, werkureninstellingen en gekoppelde voertuigen (inclusief kenteken en model).
    * **RFC 4180 CSV-Escaping:** Speciale tekens zoals puntkomma's (`;`), dubbele aanhalingstekens (`"`) en regeleinden worden conform de internationale CSV-standaard geëscaped en gequote.

## Versie 1.9.0
*Releasedatum: 15 november 2026*

!!! success "Fase 9: Tracking-Architectuur, Privacy-First Back-up & Hardening"
    * **Strikte Release Signing Validatie:** Release builds kunnen niet langer stilzwijgend terugvallen op debug-ondertekening; bij ontbrekende release-credentials faalt de build nu expliciet tijdens `validateSigningRelease`.
    * **Privacy-First Back-up Regels:** Lokale Room-databases (`cimdriver_database`) en tellerstand-fotobewijzen worden expliciet uitgesloten van Google Drive cloud-back-ups, terwijl lokale apparaat-naar-apparaat overdracht bij toestelwissel behouden blijft.
    * **Opschonen Oude Widget (Glance-first):** De verouderde XML/RemoteViews-widgetprovider is verwijderd ten gunste van de moderne Jetpack Glance interactieve widget.
    * **Eenduidige Ritclassificatie:** Twee conflicterende notificatieroutes zijn samengevoegd: alle notificatie-snelle acties lopen via één robuuste `NotificationActionReceiver` met uniforme constanten (`Business Meeting`, `Home To Work`, `PRIVATE`).
    * **TrackingService Modularisatie:** Notificatiebeheer en kanaalconfiguratie zijn ontkoppeld en ondergebracht in een zelfstandige `TrackingNotificationHelper`.
    * **Hamburgermenu / Drawer Back-Stack Herstel:** Schermen geopend via de navigatielade schonen de navigatiestack nu correct op tot de startbestemming (`popUpTo(startDestination)`). Terugnavigatie leidt consequent terug naar het Dashboard.
    * **Klanten & Projectcodes Menuknop:** Het scherm 'Klanten & Projectcodes' toont nu conform de overige menuschermen een hamburgermenuknop om de navigatielade direct te openen.
    * **Actieve Menu-item Indicator:** De menu-items in de navigatielade tonen direct een visuele `selected`-indicator passend bij het actieve scherm.

## Versie 1.8.2
*Releasedatum: 13 november 2026*

!!! success "UI-Consistentie & Design System Harmonisatie"
    * **Volledige UI-Consistentie & Core Design System:**
        * **Design System Tokens (`Dimensions.kt`):** Uniforme tokens voor spacing (`AppSpacing`), afgeronde hoeken (`AppShapes` met 16.dp kaarten) en iconen (`AppIconSizes`).
        * **Gestandaardiseerde `CimDriverTopBar`:** Alle sub-schermen (Diagnostiek, Brandstof, Voertuigen, Tellerstanden, Adresboek, Klanten & Projectcodes, Urentargets, Ritdetails en Instellingen) delen dezelfde elegante navigatiebalk met CIMDriver Navy achtergrond, gecentreerde titels en correcte terugknoppen en acties.
        * **Gestandaardiseerde `CimDriverEmptyState`:** Overal in de app (o.a. Ritten, Werkuren, Brandstof, Wagenpark, Tellerstanden, Adresboek en Klanten & Projecten) zijn lege lijsten vervangen door een uniforme, vriendelijke statusweergave met passend icoon, toelichting en directe 1-tik actieknop.
    * **Dagelijkse Inbox Leeg-status:** Volledige harmonisatie van de berichtenbox op het Dashboard bij 0 openstaande punten, inclusief vermelding van ritten, uren en tank-/laadsessies.
    * **Klanten & Projectcodes Herstel-fix:** Betrouwbare herstelactie voor gearchiveerde projectcodes en naadloze navigatie tussen schermen zonder statusverlies.

## Versie 1.8.1
*Releasedatum: 13 november 2026*

!!! info "Patch: Wagenparkoverzicht naar rato & Inbox verduidelijking"
    * **Wagenparkoverzicht naar rato:** De privégrens op de wagenparkkaart (`MultiVehicleFleetCard`) berekent de limiet nu dynamisch *naar rato* (pro rata) conform de in- en uitdiensttredingsdata en de ingestelde grens per voertuig, inclusief "(naar rato)" label en accurate statusmelding.
    * **Dagelijkse Inbox Leeg-status:** De melding bij een bijgewerkte berichtenbox verduidelijkt nu dat er geen openstaande ritten, werkuren óf tank-/laadsessies ter beoordeling staan.

## Versie 1.8.0
*Releasedatum: 13 november 2026*

!!! success "Fase 8: Fiscale Maandafsluiting, Route-Presets & Odometer Audit-Bewijs"
    * **Fiscale Maandafsluiting & Lokale Deel-notificatie (Privacy-First):** Op de 1e van elke nieuwe maand stuurt CIMDriver een lokale notificatie om de rittenadministratie van de afgelopen maand fiscaal te controleren en direct te delen via het native deelsheet (PDF/CSV). In de eerste 7 dagen van de maand toont het Dashboard tevens een discrete tegel *"Maandafsluiting gereed"*.
    * **Favoriete Routes als 1-Tik Presets & Triage-Match:** Bij handmatige invoer van ritten (`AddTripScreen`) verschijnen opgeslagen favoriete routes als directe presets die vertrek, bestemming, categorie en projectcode met één tik invullen. Bij triage krijgen ritten die matchen met een favoriete route automatisch 95% betrouwbaarheid.
    * **Odometer Fotobewijs in Historie & PDF-Bijlage:** Dashboardfoto's van kilometerstandmetingen zijn direct als tikbare miniatuur te bekijken in de km-controle historie (`OdometerCheckAnalysisScreen`) met schermvullende weergave. Daarnaast worden tellerstandcontroles en dashboardsfoto's als audit-appendix opgenomen in het officiële Belastingdienst PDF-rapport.
    * **Multi-Voertuig Wagenpark-kaart:** Voor gebruikers met 2 of meer actieve voertuigen toont het Dashboard een geconsolideerde wagenparkkaart met actuele tellerstanden, ritten deze maand en de status van de 500 km privégrens.

## Versie 1.7.0
*Releasedatum: 13 november 2026*

!!! success "Fase 7: Slimme Werkuren-Begeleiding, Triage & Target-Coaching"
    * **Werkuren-Reviewlijst & Dagelijkse Inbox-integratie:** Openstaande werkdagen (ontbrekende eindtijd, twijfel over pauzes of afwijkingen t.o.v. het basisrooster) worden nu gebundeld in de Dagelijkse Uitzonderingen-Inbox (`DailyInboxCard`) op het Dashboard en direct in het Werkurenscherm.
    * **Zachte Voorstellen voor Eindtijd & Pauze:** Automatische afleiding van ontbrekende vertrektijden via de geregistreerde rit naar huis/kantoor of via het dagspecifieke basisrooster (`workDays`). Automatische pauzesuggestie conform de Nederlandse Arbeidstijdenwet (minimaal 45 min bij $\ge$ 8u en 30 min bij $\ge$ 4.5u).
    * **Werkdagstatus in Begrijpelijke Mensentaal:** Duidelijke statusbadges per werkdag (bijv. *"Vrijwel zeker compleet (7.5 uur netto, 30m pauze)"*, *"Eindtijd ontbreekt (voorstel: 17:00)"*, *"Lange werkdag (8.2u) zonder pauze"* of *"Geregistreerd buiten reguliere werkdagen"*).
    * **Target-Coaching met Dynamisch Dagtempo:** De urentargets berekenen nu dynamisch de resterende actieve werkdagen in de lopende periode op basis van het ingestelde basisrooster, met een motiverende coachingstekst (bijv. *"Nog 16.5 uur nodig over 2 resterende werkdagen (8.3 u/dag)"*).
    * **1-Tik Goedkeuring met Ongedaan Maken (Undo Snackbar):** Individuele werkdagen en alle hoog-betrouwbare werkdagen kunnen in één tik worden goedgekeurd, met een directe Snackbar-optie om de actie zonder dataverlies terug te draaien.

## Versie 1.6.0
*Releasedatum: 13 november 2026*

!!! success "Fase 6: Betrouwbaarheid, Audit & Foutloze Export"
    * **Foutarme Export-voorcontrole (ExportPreCheckDialog):** Voordat een PDF- of CSV-export wordt gegenereerd, controleert CIMDriver de gekozen periode op openstaande beoordelingen, kilometerstand-onderbrekingen en routeafwijkingen zonder toelichting. Bevat een 1-tik bulk-goedkeurknop en toont bij volledige sluitendheid het officiële Belastingdienst-conformiteitsstempel.
    * **Uitlegbare Ritvoorstellen & Slimme Projectcodes:** Bij ritten van en naar bekende klantlocaties wordt nu automatisch de bijbehorende projectcode voorgesteld en vooringevuld, compleet met een transparante redenatie.
    * **Begrijpelijke Tracking-betrouwbaarheid in Diagnostiek:** Het diagnostiekscherm toont nu bovenaan een intuïtieve statuskaart ("Optimaal" vs. "Aandacht vereist") met uitleg in begrijpelijke mensentaal over achtergrondlocatie, batterijoptimalisatie en notificaties, inclusief directe actieknoppen voor herstel.
    * **Herstelbare Wijzigingen (Undo Snackbar):** Bij het snel fiatteren van ritten op het Dashboard of in de rittenlijst verschijnt een `[Ongedaan maken]` actie via Snackbar, zodat vergissingen direct zonder dataverlies kunnen worden hersteld.
    * **Back-up Integriteits- & Herstelcheck:** In Gegevensbeheer toont een nieuwe statuskaart direct het tijdstip, de bestandsgrootte en de gevalideerde herstelbaarheidsstatus van de laatste automatische back-up.

## Versie 1.5.0
*Releasedatum: 13 november 2026*

!!! success "Fase 5: Assisted Decision-Making & Slimme Rust"
    * **Dagelijkse Uitzonderingen-Inbox ("Daily Triage"):** Eén gecombineerde inbox op het Dashboard die alle openstaande acties verzamelt (te beoordelen ritten, onbevestigde werkdagen, ontbrekende kilometerstanden en concept-tankbeurten). Bevat een snelle 1-tik goedkeurknop om alle ritten met hoge betrouwbaarheid in één keer te fiatteren.
    * **Confidence Scoring & Reviewvoorstellen:** Automatische betrouwbaarheidsscore (0–100%) voor twijfelgevallen met een duidelijke uitleg ("Vaste route-regel matched exact", "Reguliere woon-werk route", etc.). In de ritdetails kan het voorstel direct met één tik worden goedgekeurd.
    * **Ritaudit & Contextuele Diagnostiek:** Nieuwe transparante ritauditkaart in ritdetails die precies toont hoe de rit is geregistreerd (GPS-achtergrondservice of handmatig), welk voertuig/carkit was verbonden en of er privé-omrijden is verrekend.
    * **Permission & Battery Health Monitor:** Proactieve waarschuwingen op het dashboard als batterij-optimalisatie, locatietoegang, notificaties of bluetooth-permissies de trackingbetrouwbaarheid bedreigen, inclusief directe herstelknoppen naar de Android-systeeminstellingen.
    * **Hardere Netwerk- & Securityhygiëne:** `usesCleartextTraffic` is verwijderd en vervangen door een strikte `network_security_config.xml`. Externe API's (RDW, OSRM, Nominatim) draaien uitsluitend over HTTPS; onversleuteld verkeer is strikt beperkt tot de lokale Zepp-companion op localhost (`127.0.0.1:8765`).
    * **Adaptieve GPS-Tracking & Fiscale Prognose:** Stilstand-jitterfilter die mini-bewegingen bij stoplichten negeert. De privékilometerkaart op het dashboard toont nu een voorspellende jaareindprognose en waarschuwt tijdig wanneer de fiscale 500 km-grens dreigt te worden overschreden.

## Versie 1.4.1
*Releasedatum: 13 november 2026*

!!! bug "Bugfixes & Stabiliteit"
    * **Hotfix opstartcrash:** Probleem verholpen waarbij de app direct bij het openen crashte door een verkeerde tabelnaam in de database-migratie (`fuel_fillups`). Directe migratiepaden en robuuste foutafhandeling toegevoegd.

## Versie 1.4.0
*Releasedatum: 13 november 2026*

!!! success "Nieuwe Functies & Verbeteringen"
    * **Fiscaal sluitende export (Belastingdienst):** Begin- en eindtellerstanden zijn nu standaard onderdeel van de CSV- en PDF-export. Het PDF-rapport is geoptimaliseerd naar liggend A4 (Landscape) met een professionele paginakop (voertuiggegevens, kenteken en periode) en een fiscaal totalenblok onderaan.
    * **Privé-omrijkilometers:** Eenvoudig registreren van privé-omrijden tijdens zakelijke ritten. Deze kilometers worden automatisch van de zakelijke afstand afgetrokken en meegeteld voor de 500km-privégrens.
    * **RDW Open Data Kentekencheck:** Automatisch ophalen van automerk, handelsbenaming, bouwjaar en brandstoftype/aandrijving (EV, PHEV of brandstof) bij het invoeren van een kenteken.
    * **Live Rit-Toggle tijdens het rijden:** Schakel een lopende rit direct om tussen "Zakelijk" en "Privé" via de actieve notificatie of op het Android Auto scherm.
    * **Interactieve Home Screen Widget:** Bedien je ritten direct vanaf je Android-startscherm (actuele ritstatus, rit-toggle en snelle startknoppen).
    * **Werkuren delen per periode:** Exporteer werkuren direct gefilterd per week of maand naar PDF of CSV met berekende urentotalen.
    * **Kalender dag-inspectie:** Tik op een dag in de rittenkalender om direct alle ritten van die dag in een overzichtelijke pop-up te bekijken en te openen.
    * **Klikbare doelen:** Tik op doelkaarten op het dashboard om direct naar het doelenbeheer te navigeren.
    * **Nederlandse Android Auto:** Volledige Nederlandse vertaling van categorieën op het autoscherm inclusief directe privé-rit optie.
    * **EV Laadpresets:** Snelle keuzeknoppen voor laadtarieven bij elektrische voertuigen (Thuis € 0,28, Publiek € 0,45, Snellader € 0,69).

## Versie 1.3.7
*Releasedatum: 13 november 2026*

!!! success "Nieuwe Functies & Verbeteringen"
    * **Betrouwbare tankstation- & laadpaaldetectie:** De zoekstraal is verdubbeld naar 150m en voorzien van een slimme fallback via de Android Geocoder voor betrouwbare herkenning van alle bekende merken (Shell, BP, Total, Esso, Texaco, TinQ, Tango, AVIA, Fastned, etc.).
    * **Herkenning bij tussenstops:** Bij een tankstop onderweg (contact uit) wordt direct geregistreerd dat er getankt wordt, ook wanneer de rit na de tankbeurt als één geheel doorloopt.
    * **Heads-up notificatie:** Meldingen over tank- en laadsessies poppen direct op (`FuelAlertChannel`). Eén tik opent het invoerscherm met station en tellerstand vooraf ingevuld.
    * **Dashboard actiekaart:** Openstaande tankbeurten zijn direct zichtbaar op het dashboard met actieknoppen voor snelle invoer of negeren.
    * **Automatisch rekenen:** Automatische berekening van het totaalbedrag of de literprijs bij invoer.
    * **Voertuigfilter:** Direct filteren op auto in het brandstof- en laadoverzicht.

## Versie 1.3.6
*Releasedatum: 13 november 2026*

!!! success "Nieuwe Functies & Verbeteringen"
    * **Slimme vertrekdetectie bij parkeergarages:** Bepaalt automatisch het juiste vertrekadres wanneer je uit een ondergrondse garage vertrekt waar nog geen GPS-dekking is, op basis van de geparkeerde positie en opgeslagen adressen binnen 1500m.
    * **Automatische afstandscorrectie (OSRM):** Wanneer de geregistreerde GPS-afstand korter is dan de werkelijke route doordat satellieten pas na het uitrijden werden vergrendeld, stelt de app de afstand automatisch bij naar de berekende route-afstand.
    * **Voorkomen van kilometerafwijkingen:** De kilometerteller van het voertuig telt met de bijgestelde afstand, waardoor er geen tellergaten meer ontstaan door ritten vanuit parkeergarages.
    * **Naadloze kaartverbinding:** De kaartroute wordt automatisch aangevuld vanaf het daadwerkelijke vertrekpunt tot aan de eerste GPS-fix.
    * **Vooraf ingevuld vertrekadres:** Bij Bluetooth-connectie in de garage wordt direct het geparkeerde adres als vertrekpunt klaargezet.

## Versie 1.3.5
*Releasedatum: 2 november 2026*

!!! success "Nieuwe Functies & Verbeteringen"
    * **Rittenoverzicht per maand delen (PDF en CSV):** Exporteer en deel eenvoudig ritten gefilterd per individuele maand in plaats van alleen het volledige overzicht.
    * **Maandsecties met directe deelknop:** Het overzicht toont duidelijke maandheaders met totalen (aantal ritten, kilometers) en een directe "Deel"-actie per maand.
    * **Maandtitels in rapporten:** PDF- en CSV-exporten bevatten nu de specifieke maandnaam in zowel de paginakop als bestandsnaam.
    * **Uitgebreid exportmenu:** Snelle keuze tussen maandelijkse exports en het volledige rittenarchief via het exportmenu in de titelbalk.

## Versie 1.3.4
*Releasedatum: 30 oktober 2026*

!!! success "Verbeteringen & Bugfixes"
    * **Oplossen crash bij openen van ritten:** Crash verholpen bij ritten waarbij de kaart een enkel punt of afwijkend adres toont (zoals handmatige bestemmingen).
    * **Directe bewerk- en verwijderknoppen:** Bewerkknop in de actiebalk en verwijderoptie met bevestiging direct beschikbaar in ritdetails.
    * **Tijdscorrectie bij gat-herstel:** Correctieritten garanderen een chronologische start- en eindtijd zonder negatieve duur.
    * **Veilige duurweergave:** Voorkomt negatieve tijdsduur bij overlappende ritten.

## Versie 1.3.3
*Releasedatum: 30 oktober 2026*

!!! success "Nieuwe Functies & Verbeteringen"
    * **Tellerstanden chronologisch opnieuw doorberekenen:** Herbereken alle tellerstanden van een voertuig chronologisch op basis van rittijden, afstanden en uitgevoerde km-controles.
    * **Behoud van geconstateerde gaten:** Gaten bij tussentijdse kilometercontroles blijven netjes bewaard voor een sluitende Belastingdienst-audit trail.
    * **Interactieve previewdialoog:** Direct overzicht van aangepaste ritten, behouden controlepunten en overblijvende gaten vóór toepassing.
    * **Flexibele rit-typen bij gaten oplossen:** Kies nu direct tussen Privé, Zakelijk of Woon-werk bij het toevoegen van een aansluitende rit.
    * **Directe tellersynchronisatie:** Zet de actuele tellerstand met één klik gelijk aan de laatste rit om eindverschillen direct op te lossen.
    * **Verbeterde tijdsvolgorde:** Correcte weergave van tijdsbereiken bij overlappende ritten.

## Versie 1.3.2
*Releasedatum: 30 oktober 2026*

!!! success "Verbeteringen & Bugfixes"
    * **Oplossen afrondingsfouten bij kilometertelling:** Foutieve integer-afkapping gecorrigeerd naar wiskundige afronding. Voorkomt dat ritten structureel meters verliezen en er spontaan gaten ontstaan in zakelijke ritten.
    * **Geen fictieve kilometerverdeling meer:** Het automatisch uitsmeren van kilometers over bestaande routes is definitief verwijderd ter waarborging van een fiscaal zuivere Belastingdienst-administratie.
    * **Flexibele adressen bij gatanalyse:** In de gat-detectie kunnen vertrek- en bestemmingsadressen nu direct handmatig worden ingegeven.
    * **Snelle suggesties voor korte gaten (4-5 km):** Snelkeuzes voor veelvoorkomende privéritten ("Boodschappen", "Lokaal", "Tanken") en verklaringen ("Tellerafwijking / kalibratie", "Garage / APK").
    * **Afstandsindicatie bij frequente routes:** Routeknoppen tonen direct de berekende afstand (bijv. `38 km`).
    * **Verbeterde controlewaarschuwingen:** Duidelijke waarschuwing bij afwijkingen boven 30 km en consistente weergave van decimalen bij ritten.

## Versie 1.3.1
*Releasedatum: 30 oktober 2026*

!!! success "Nieuwe Functies & Verbeteringen"
    * **Sluitende kilometerregistratie (Belastingdienst-proof):** Voorkomt en herstelt hiaten in de rittenregistratie tussen opeenvolgende ritten en periodieke kilometercontroles.
    * **Interactieve controle-analyse:** Bij afwijkingen tijdens een km-controle toont de app direct een analyse met fiscaal zuivere oplossingen (sluitende privérit toevoegen, verklaard gat met toelichting zoals garage/APK, of later handmatig losse ritten invoeren). Onrealistisch opblazen of uitsmeren van kilometers over bestaande routes is volledig uitgesloten.
    * **Gat-detectie & Ketencontrole:** Achteraf automatisch opsporen en batchgewijs herstellen van ontbrekende kilometers in de rithistorie.
    * **PDF Integriteitsrapport:** Exporteer een compleet integriteitsverslag voor de Belastingdienst met alle controles, afwijkingen en correcties.
    * **Duizendtallen scheidingstekens:** Verbeterde leesbaarheid van targets (omzet en uren) op het dashboard met duizendtalscheiding.

## Versie 1.3.0
*Releasedatum: 26 oktober 2026*

!!! success "Verbeteringen & Bugfixes"
    * **Dashboarddoelen (Bonus/Uren/Omzet):** Gewerkte uren worden nu direct en betrouwbaar verwerkt en getoond in de doelvoortgang op het dashboard.
    * **Statusondersteuning:** Zowel goedgekeurde (`APPROVED`) als voltooide (`DONE`) werkdagen tellen correct mee.
    * **Project- & Klantkoppeling:** Werkdagen worden nu accuraat gekoppeld via zowel `projectCodeId` als tekstuele projectcode (`code`), waardoor ook uurtarieven voor omzetdoelen correct worden toegepast.
    * **Automatische en handmatige registratie:** Projectcodes blijven behouden bij het goedkeuren en worden automatisch geregistreerd vanuit opgeslagen locaties.

## Versie 1.2.7
*Releasedatum: 9 oktober 2026*

!!! success "Nieuwe Functies"
    * **Uren- en omzetdoelen:** Stel targets in op uren of omzet en koppel ze aan een klant of projectcode.
    * **Vaste projecttarieven:** Configureer het uurtarief per projectcode; de gerealiseerde omzet wordt per project berekend.
    * **Pro-rata jaartarget:** De instelbare datum in dienst bepaalt hoeveel kalendermaanden van de jaartarget meetellen. Start je in september, dan is dit 4/12.
    * **Dashboardtargets:** Het getoonde omzet- of urendoel is nu ook naar rato aangepast aan de datum in dienst.
    * **In-app help:** Nieuwe uitleg over het instellen van uren- en omzetdoelen, vaste projecttarieven en de pro-rata berekening.

## Versie 1.2.6
*Releasedatum: 9 oktober 2026*

!!! success "Nieuwe Functies"
    * **Zepp Companion Toggle:** Nieuwe instelling toegevoegd om de Zepp OS integratie server (voor smartwatches) in- of uit te schakelen ter verbetering van batterijduur.
    * **Verbeterde UI voor instellingen:** De optie om projectcodes automatisch te archiveren is verplaatst naar het Werkuren-menu en opnieuw opgemaakt als een gebruiksvriendelijke toggle-schakelaar.

## Versie 1.2.5
*Releasedatum: 9 oktober 2026*

!!! success "Nieuwe Functies"
    * **Klanten & Projecten:** Mogelijkheid toegevoegd om projectcodes aan te maken en te koppelen aan klanten.
    * **Projectcodes Archiveren:** Projectcodes die in gebruik zijn worden bij verwijdering niet meer gewist, maar gearchiveerd (soft delete).
    * **Automatisch Archiveren:** Functie toegevoegd om ongebruikte projectcodes na een instelbaar aantal dagen automatisch te archiveren.
    * **Uren Targets:** Bij het instellen van uren targets kan er nu gekozen worden uit projectcodes om de uren per project/klant bij te houden.

---

## Versie 1.2.4
*Releasedatum: 2 oktober 2026*

## Versie 1.2.3
*Releasedatum: 2 oktober 2026*

!!! bug "Bugfix"
    * **Bestaande auto:** Het aanpassen van de begin- en einddatum bij een bestaande auto werkt nu weer correct, ook als de waarschuwing voor privégebruik is uitgeschakeld.
    * **Datumvelden:** De velden blijven zichtbaar en kunnen direct worden aangepast of gewist vanuit de voertuiginstellingen.

---

## Versie 1.1.5
*Releasedatum: 1 oktober 2026*

!!! success "Nieuw"
    * **Classificatieregels:** Auto-aanvullen en adresboek selectie toegevoegd. Vanaf nu kun je net als bij ritten eenvoudig adressen selecteren uit je adresboek bij het instellen van regels.

---

## Versie 1.1.4
*Releasedatum: 24 september 2026*

!!! success "Nieuw"
    * **Documentatie:** Release notes zijn bijgewerkt met een waarschuwing over de back-up functionaliteit, en de structuur van het project is verbeterd.

!!! bug "Bugfixes"
    * Opgelost: Probleem met het pad naar de release keystore verholpen, waardoor de signed APK nu correct wordt gegenereerd.

---

## Versie 1.1.1 t/m 1.1.3
*Releasedata: 18 september (1.1.1) en 21 september 2026 (1.1.2 en 1.1.3)*

!!! info "Aanpassingen"
    * **Help-scherm:** Het in-app helpscherm is geüpdatet met vernieuwde informatie voor gebruikers.
    * Interne voorbereidingen en kleine versie-ophogingen voor stabiliteit.

---

## Versie 1.0.8 & 1.1.0 (Stricte Adres Matching)
*Releasedatum: 17 september 2026*

!!! success "Nieuw"
    * **Stricte Adres Matching:** Een nieuw algoritme (priority-based) is geïmplementeerd om ritten nog accurater aan de juiste locaties te koppelen en foute verbindingen te voorkomen.

!!! bug "Bugfixes"
    * Opgelost: De adres-herkenningsradius is teruggezet naar 200 meter om "false positives" (locaties die te snel werden gematcht) te voorkomen.

---

## Versie 1.0.7
*Releasedatum: 16 september 2026*

!!! info "Aanpassingen"
    * **UI Verbeteringen:** Diverse visuele verbeteringen en fixes in adresmatching en keystore.

---

## Versie 1.0.3 & 1.0.4
*Releasedatum: 16 september 2026*

!!! success "Nieuw"
    * **Database Update:** Database model bijgewerkt (versie 30) voor snellere verwerking van entiteiten en instellingen.
    * **Swipen:** Rit classificatie pop-up toegevoegd; je kunt nu ritten naar rechts swipen om ze snel te classificeren!

<br>
<div align="center" markdown="1">
[:material-download: Download Android APK](../download/android.md){ .md-button .md-button--primary }
</div>
