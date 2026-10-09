import SwiftUI

struct HelpView: View {
    var body: some View {
        List {
            Section {
                DisclosureGroup("Klanten en projectcodes") {
                    Text("Maak klanten en projectcodes aan via Instellingen → Klanten & Projectcodes. Per projectcode kun je een vast uurtarief instellen. Je kunt ongebruikte projectcodes archiveren en herstellen.")
                }
                DisclosureGroup("Uren- en omzetdoelen") {
                    Text("Voeg via Instellingen → Werkuren → Uren- & omzetdoelen een jaardoel toe. Kies uren of omzet en stel desgewenst een klant- of projectfilter in. Goedgekeurde werkdagen tellen mee; omzet wordt berekend met het vaste tarief van de projectcode.")
                }
                DisclosureGroup("Doel naar rato bij indiensttreding") {
                    Text("Zet Datum in dienst gebruiken aan bij Instellingen → Werkuren. De maand van indiensttreding telt volledig mee: start je op 1 september, dan is je doel 4/12 van het jaardoel. Het aangepaste doel en je voortgang staan op het dashboard en in het targetoverzicht.")
                }
                DisclosureGroup("Werkuren zoeken") {
                    Text("Gebruik de zoekknop in Werkuren om werkdagen te vinden op projectcode, locatie of notitie.")
                }
            }
        }
        .navigationTitle("Help")
        .navigationBarTitleDisplayMode(.inline)
    }
}