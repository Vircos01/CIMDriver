# 💡 CIMDriver Feature Roadmap

Gebaseerd op een grondige analyse van de huidige codebase voor zowel Android als iOS.

**Laatst bijgewerkt:** 2 oktober 2026

---

## ✅ Afgerond

### Pro Rata 500km Privégrens
**Platform:** Android + iOS  
**Status:** ✅ Geïmplementeerd (v1.1.5+)

- Datum ingebruikname en datum einde gebruik per voertuig
- Automatische pro-rata berekening van de 500km privégrens
- Visuele weergave op het dashboard met aangepaste progressiebalk
- Info-tooltip met uitleg over fiscale regels (poolauto's, persoonsgebonden grens)

---

## 🟢 Quick Wins (weinig moeite, hoge impact)

### 1. Rittenkalender / Maandoverzicht
**Platform:** Android + iOS

Voeg een kalenderweergave toe aan het rittenoverzicht. Elke dag wordt gekleurd op basis van het type ritten (zakelijk = blauw, privé = groen, woon-werk = oranje). Dit geeft in één oogopslag inzicht in patronen en maakt het makkelijker om specifieke dagen terug te vinden.

> [!TIP]
> Op Android kun je `LazyVerticalGrid` gebruiken voor een snelle implementatie. Op iOS biedt SwiftUI's `Grid` of een simpele `LazyVGrid` dezelfde mogelijkheid.

### 2. Favoriete Routes
**Platform:** Android + iOS

De app herkent al herhaalde routes (via `getTripCountForRoute`). Bouw hier op voort: laat gebruikers veelgereden routes opslaan als "Favoriet" (bijv. "Thuis → Kantoor") zodat bij handmatige ritinvoer met één tik alles voorgevuld wordt.

### 3. Snelle Rit-classificatie via Notificatie
**Platform:** Android (iOS later)

Er is al een "Stop Rit" knop in de tracking-notificatie. Breid dit uit met knoppen om de rit direct als Zakelijk / Privé / Woon-werk te classificeren bij het beëindigen, zonder de app te openen.

### 4. Donker/Licht Thema Schakelaar met Vloeiende Transitie
**Platform:** Android + iOS

Het thema is al instelbaar (`SYSTEM`, `LIGHT`, `DARK`), maar voeg een vloeiende crossfade-animatie toe bij het wisselen en overweeg een "automatisch donker na zonsondergang" optie op basis van locatie.

---

## 🔵 Medium Effort (significante verbetering)

### 5. Maandrapport per E-mail (Automatisch)
**Platform:** Android + iOS

De CSV/PDF export bestaat al. Voeg een optionele **automatische maandelijkse rapportage** toe: aan het eind van elke maand genereert de app automatisch een periodeoverzicht (PDF) en biedt aan dit per e-mail te versturen naar een ingesteld adres (bijv. de boekhouder of HR).

### 6. Kilometerstand Foto-verificatie
**Platform:** Android + iOS

Bij de kilometerstandcontrole (al aanwezig met `odometerReminder`): laat de gebruiker een **foto van de teller** maken en sla deze op bij de controle. Dit is handig voor de Belastingdienst-administratie en geeft extra bewijs bij een controle.

### 7. Ritten Samenvoegen (Handmatig)
**Platform:** Android + iOS

Er is al een auto-merge functie (`autoMergeEnabled`, `mergeTimeLimitMin`). Voeg ook **handmatig samenvoegen** toe: selecteer 2+ ritten in het overzicht en combineer ze tot één enkele rit. Handig als de GPS even uitviel of je bij een klant kort gestopt bent.

### 8. Multi-Voertuig Dashboard
**Platform:** Android + iOS

Het dashboard filtert nu op één voertuig. Voeg een **"Alle voertuigen"** optie toe aan de voertuigselector zodat je een totaaloverzicht krijgt van al je ritten over alle auto's heen.

### 9. Tankbeurten & Brandstofkosten
**Platform:** Android + iOS

Voeg een nieuw scherm/tab toe voor het bijhouden van tankbeurten (datum, liters, prijs, kilometerstand). Bereken automatisch het verbruik per 100km en totale brandstofkosten per periode. Dit maakt de app een compleet voertuigkostenplaatje.

### 10. Rit-tags & Projectcodes
**Platform:** Android + iOS

Er is al een `projectCode` veld op ritten. Maak dit prominenter:
- Toon een autocomplete-lijst van eerder gebruikte projectcodes
- Filter het dashboard/export op projectcode
- Ideaal voor consultants die uren/km per klantproject moeten verantwoorden

---

## 🟣 Grotere Features (hoge waarde)

### 11. Cloud Sync & Multi-Device
**Platform:** Android + iOS

De data zit nu puur lokaal (Room / SwiftData). Overweeg een optionele cloud-synchronisatie (bijv. via Firebase of eigen backend) zodat:
- Data veilig in de cloud staat als backup
- Android- en iOS-versie dezelfde data delen
- Bij telefoonwisseling alles automatisch meeverhuist

> [!IMPORTANT]
> Dit is de meest gevraagde feature bij zakelijke apps. Begin simpel met een "push backup to cloud" knop en breid later uit naar real-time sync.

### 12. Digitale Rittenregistratie voor de Belastingdienst
**Platform:** Android + iOS

Genereer een **officieel belastingdienst-conforme** rittenadministratie:
- Automatische jaaroverzichten met alle verplichte velden (datum, vertrek, bestemming, km, route, zakelijk doel)
- Exporteer als XML in het door de Belastingdienst geaccepteerde formaat
- Toon de jaarlijkse privékilometer-teller met 500km grens-waarschuwing (deels al aanwezig)

### 13. Apple Watch / Wear OS Companion App
**Platform:** Beide ecosystemen

Een simpele companion app voor de smartwatch:
- Toon de huidige ritstatus (rijdend/gestopt)
- Start/stop rit vanuit de pols
- Snelle classificatie na het stoppen
- Complicatie voor het watchface met dagelijkse km-stand

### 14. Geofence-gebaseerde Automatische Check-in/out
**Platform:** Android (al gestart) + iOS

De `GeofenceManager` en `GeofenceBroadcastReceiver` bestaan al op Android. Bouw dit verder uit:
- Automatische check-in wanneer je aankomt op een werklocatie
- Automatische check-out wanneer je vertrekt
- Koppel dit aan de werkurenregistratie voor een volledig hands-free ervaring

### 15. Ritgeschiedenis op de Kaart
**Platform:** Android + iOS

Er worden al `LocationPoint`s opgeslagen per rit. Voeg een kaartweergave toe aan het ritdetailscherm waarop de **gereden route** zichtbaar is (polyline op de kaart). Op Android kan dit met de bestaande MapLibre SDK. Op iOS met MapKit.

---

## 📱 iOS-Specifiek

### 16. Siri Shortcuts & App Intents
Laat gebruikers via Siri commando's geven: *"Hey Siri, start een zakelijke rit"* of *"Hoeveel kilometer heb ik deze week gereden?"*

### 17. Live Activity Verbetering
Er is al een `TrackingLiveActivity`. Breid deze uit met:
- Real-time afgelegde afstand op het Dynamic Island
- Verstreken rijtijd
- Huidige snelheid

### 18. CarPlay Verbetering
De Android-versie heeft al een volledige Car App Library integratie. Breng de iOS CarPlay naar hetzelfde niveau met:
- Recente ritten overzicht
- Snelle classificatie
- Adresboek-toegang

---

## 🤖 Android-Specifiek

### 19. Android Auto Verbetering
De Car App Library integratie bestaat al. Voeg toe:
- Voice-input voor notities bij ritten
- Snellere classificatie-knoppen
- Brandstof-/laadstatus weergave

### 20. Tasteful Widget Uitbreiding
Er is al een Glance widget (`CIMDriverWidget`). Breid uit met:
- Meerdere widget-formaten (klein: alleen km vandaag, groot: volledige dagstats)
- Snelkoppeling-knoppen op de widget (start rit, stop rit)
- Voertuigselectie

---

## 📊 Prioriteitsoverzicht

| # | Feature | Impact | Moeite | Status |
|---|---------|--------|--------|--------|
| — | Pro Rata 500km Grens | ⭐⭐⭐⭐ | 🔧 | ✅ Klaar |
| 1 | Rittenkalender | ⭐⭐⭐ | 🔧 | ⬜ Open |
| 2 | Favoriete Routes | ⭐⭐⭐ | 🔧 | ⬜ Open |
| 3 | Snelle classificatie notificatie | ⭐⭐⭐ | 🔧 | ⬜ Open |
| 5 | Automatisch Maandrapport | ⭐⭐⭐⭐ | 🔧🔧 | ⬜ Open |
| 10 | Projectcode Autocomplete | ⭐⭐⭐ | 🔧 | ⬜ Open |
| 15 | Route op Kaart | ⭐⭐⭐⭐ | 🔧🔧 | ⬜ Open |
| 12 | Belastingdienst Export | ⭐⭐⭐⭐⭐ | 🔧🔧🔧 | ⬜ Open |
| 9 | Tankbeurten | ⭐⭐⭐ | 🔧🔧 | ⬜ Open |
| 11 | Cloud Sync | ⭐⭐⭐⭐⭐ | 🔧🔧🔧🔧 | 🔮 Later |
| 13 | Watch App | ⭐⭐⭐ | 🔧🔧🔧 | 🔮 Later |
