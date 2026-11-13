import SwiftUI

struct HelpView: View {
    var body: some View {
        List {
            Section(header: Text("Dashboard & Dagelijkse Inbox")) {
                DisclosureGroup("Dashboard & Statistieken") {
                    Text("Het Dashboard toont een compleet overzicht van je gereden afstanden en gewerkte uren. Gebruik de knoppen 'Week', 'Maand' en 'Jaar' om de periode aan te passen. Je kunt filteren op het actieve voertuig en ziet direct de live trackingstatus.")
                        .font(.subheadline)
                        .foregroundColor(.secondary)
                }
                
                DisclosureGroup("Dagelijkse Inbox & Uitzonderingen (Triage)") {
                    Text("Op het Dashboard verzamelt de Dagelijkse Inbox alle openstaande acties: ritten met de status 'Nog beoordelen', niet-bevestigde werkdagen, ontbrekende kilometerstanden en concept-tankbeurten.\n\nElke rit krijgt een automatische Confidence Score (0–100%) met een begrijpelijke toelichting. Ritten met een hoge betrouwbaarheid (≥ 85%) kun je met één tik in bulk goedkeuren. Per ongeluk goedgekeurd? Gebruik direct de banner 'Ongedaan maken'.")
                        .font(.subheadline)
                        .foregroundColor(.secondary)
                }
            }
            
            Section(header: Text("Ritten & Automatische Tracking")) {
                DisclosureGroup("Automatische Ritregistratie") {
                    Text("CIMDriver draait op de achtergrond. Zodra je verbinding maakt met de Bluetooth-carkit van je auto, start de rit automatisch. Bij het verbreken van de verbinding wacht de app eerst de ingestelde grace period af voordat de rit wordt afgerond.")
                        .font(.subheadline)
                        .foregroundColor(.secondary)
                }
                
                DisclosureGroup("Handmatig toevoegen & Samenvoegen") {
                    Text("Ben je met een andere auto vertrokken of stond Bluetooth uit? Voeg ritten handmatig toe via de '+' knop. Wil je opeenvolgende korte ritten bundelen? Gebruik de selectiemodus in de rittenlijst en kies 'Samenvoegen'.")
                        .font(.subheadline)
                        .foregroundColor(.secondary)
                }
                
                DisclosureGroup("Classificatieregels & Projectcodes") {
                    Text("Regels koppelen adrestypes (zoals Thuis, Werk of Klant) automatisch aan een categorie (Zakelijk, Woon-werk of Privé). Regels zijn bi-directioneel: één regel herkent zowel de heen- als de terugreis.\n\nBij ritten van en naar bekende klantadressen wordt bovendien automatisch de bijbehorende projectcode voorgesteld en vooringevuld.")
                        .font(.subheadline)
                        .foregroundColor(.secondary)
                }
            }
            
            Section(header: Text("Fiscale Compliance & Export")) {
                DisclosureGroup("Foutarme Export-voorcontrole (Belastingdienst)") {
                    Text("Tik je op PDF- of CSV-export, dan voert CIMDriver eerst een volledige voorcontrole uit via de ExportPreCheck-sheet. Er wordt gecontroleerd op:\n• Ongeclassificeerde ritten;\n• Kilometerstand-onderbrekingen of gaten;\n• Afwijkingen en privé-omrijkilometers zonder toelichting.\n\nIs alles sluitend, dan ontvang je het groene keurmerk '100% fiscaal sluitend conform Belastingdienst'.")
                        .font(.subheadline)
                        .foregroundColor(.secondary)
                }
                
                DisclosureGroup("Privé-omrijkilometers & 500 km Jaarprognose") {
                    Text("Rijd je tijdens een zakelijke rit of woon-werkverkeer een stukje om voor privédoeleinden? Vul de privé-omrijkilometers eenvoudig in bij ritdetails. Deze worden automatisch afgetrokken van het zakelijke kilometrage en meegeteld voor de fiscale 500 km-grens.\n\nOp het Dashboard toont de privégrenskaart een nauwkeurige jaarprognose op 31 december met een tijdige waarschuwing bij nadering van de grens.")
                        .font(.subheadline)
                        .foregroundColor(.secondary)
                }
                
                DisclosureGroup("Liggend A4 PDF-rapport") {
                    Text("De PDF-export is speciaal ontworpen in liggend A4 (Landscape) conform art. 3.17 Wet IB 2001. Het bevat een professionele kop met voertuig- en periodedata, begin- en eindtellerstanden, omrijkilometers, adressen, ritdoel en een sluitend totalenoverzicht.")
                        .font(.subheadline)
                        .foregroundColor(.secondary)
                }
            }
            
            Section(header: Text("Voertuigen & Brandstof / EV")) {
                DisclosureGroup("RDW Kentekencheck") {
                    Text("Voeg je een auto toe? Vul het kenteken in en tik op 'RDW Check'. Merk, handelsbenaming, bouwjaar en brandstoftype (ICE, PHEV of elektrisch) worden direct gratis opgehaald via de open data API van de RDW.")
                        .font(.subheadline)
                        .foregroundColor(.secondary)
                }
                
                DisclosureGroup("Kilometer-gatendetectie & Herberekenen") {
                    Text("CIMDriver bewaakt de chronologische aansluiting van tellerstanden. Wordt er een gat geconstateerd tussen twee ritten, dan kun je via 'Tellers herberekenen' alle ritten automatisch laten doorrekenen op basis van geregistreerde ijkpunten.")
                        .font(.subheadline)
                        .foregroundColor(.secondary)
                }
                
                DisclosureGroup("EV & Laadtarief Presets") {
                    Text("Voor elektrische voertuigen ondersteunt brandstofbeheer kWh-registraties met handige snelle presets: Thuis (€0,28), Publiek (€0,45) en Snellader (€0,69 per kWh).")
                        .font(.subheadline)
                        .foregroundColor(.secondary)
                }
            }
            
            Section(header: Text("Werkuren & Doelen")) {
                DisclosureGroup("Klanten en projectcodes") {
                    Text("Maak klanten en projectcodes aan via Instellingen → Klanten & Projectcodes. Per projectcode kun je een vast uurtarief instellen. Je kunt ongebruikte projectcodes archiveren en herstellen.")
                        .font(.subheadline)
                        .foregroundColor(.secondary)
                }
                
                DisclosureGroup("Uren- en omzetdoelen") {
                    Text("Voeg via Instellingen → Werkuren → Uren- & omzetdoelen een jaardoel toe. Kies uren of omzet en stel desgewenst een klant- of projectfilter in. Goedgekeurde werkdagen tellen mee; omzet wordt berekend met het vaste tarief van de projectcode.")
                        .font(.subheadline)
                        .foregroundColor(.secondary)
                }
                
                DisclosureGroup("Doel naar rato bij indiensttreding") {
                    Text("Zet 'Datum in dienst gebruiken' aan bij Instellingen → Werkuren. De maand van indiensttreding telt volledig mee: start je op 1 september, dan is je doel 4/12 van het jaardoel. Het aangepaste doel en je voortgang staan op het dashboard en in het targetoverzicht.")
                        .font(.subheadline)
                        .foregroundColor(.secondary)
                }
            }
            
            Section(header: Text("Systeem, Diagnostiek & Back-up")) {
                DisclosureGroup("Tracking Betrouwbaarheid & Permissies") {
                    Text("In het Diagnostiek-scherm vind je bovenaan een statuskaart die in begrijpelijke taal aangeeft of je tracking 'Optimaal' is of 'Aandacht vereist'. Voor iOS is het essentieel dat Locatievoorzieningen op 'Altijd' staat en notificaties zijn ingeschakeld.")
                        .font(.subheadline)
                        .foregroundColor(.secondary)
                }
                
                DisclosureGroup("Back-up Integriteit & Herstelcheck") {
                    Text("In Instellingen → Data Beheer kun je een complete back-up maken of herstellen. De nieuwe validatiekaart toont het tijdstip, de bestandsgrootte en de status 'Gevalideerd & Herstelbaar' van de meest recente lokale back-up.")
                        .font(.subheadline)
                        .foregroundColor(.secondary)
                }
            }
        }
        .navigationTitle("Help")
        .navigationBarTitleDisplayMode(.inline)
    }
}
