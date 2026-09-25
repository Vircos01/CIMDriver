# 📱 Lokale Installatie (Sideloading)

De CIMDriver iOS-app is momenteel in actieve ontwikkeling. Omdat er (nog) geen publieke TestFlight-versie beschikbaar is, kun je de app zelf via Xcode op je eigen iPhone installeren (sideloading).

## Vereisten

Om de app lokaal te kunnen bouwen en installeren heb je het volgende nodig:
- Een Mac met de nieuwste versie van **Xcode** (gratis te downloaden via de Mac App Store).
- Een **Apple ID** (een betaald Apple Developer account is *niet* vereist, een gratis account is voldoende).
- Je eigen **iPhone** en een geschikte USB- of USB-C-kabel.

## Installatiestappen

Volg deze stappen om CIMDriver op je iPhone te installeren:

### 1. Code ophalen
Kloon de iOS-repository naar je Mac:
```bash
git clone https://github.com/Vircos01/CIMDriver-iOS.git
```

### 2. Project openen in Xcode
Open de map `CIMDriver-iOS` en dubbelklik op het `.xcodeproj` of `.xcworkspace` bestand om het project in Xcode te openen.

### 3. Apple ID toevoegen
Als je dit nog niet hebt gedaan, voeg dan je Apple ID toe aan Xcode:
1. Ga in de menubalk naar **Xcode** > **Settings...** (of Preferences).
2. Klik op de tab **Accounts**.
3. Klik op de **+** (linksonder) > **Apple ID** en log in met je account.

### 4. Signing instellen
Om de app op een fysiek apparaat te mogen draaien, moet deze digitaal worden ondertekend met jouw gratis ontwikkelcertificaat:
1. Klik in de linkernavigatie (Project Navigator) op de hoofdmap van het project (`CIMDriver`).
2. Ga in het hoofdvenster naar het tabblad **Signing & Capabilities**.
3. Vink **Automatically manage signing** aan.
4. Selecteer jouw account onder het kopje **Team**. (Xcode zal nu automatisch een provisioning profile voor je aanmaken).
5. Verander eventueel het **Bundle Identifier** iets (bijvoorbeeld door je naam toe te voegen: `com.jouwnaam.cimdriver`), omdat deze uniek moet zijn in het Apple ecosysteem.

### 5. iPhone instellen (Developer Mode)
Zorg ervoor dat je iPhone klaar is voor ontwikkeling:
1. Verbind je iPhone met je Mac via de kabel. Ontgrendel je iPhone en kies **"Vertrouw deze computer"** indien gevraagd.
2. Ga op je iPhone naar **Instellingen** > **Privacy en beveiliging**.
3. Scroll helemaal naar beneden naar **Ontwikkelaarsmodus** (Developer Mode) en zet deze **AAN**. Je iPhone moet hierna opnieuw opstarten.

### 6. App Bouwen en Uitvoeren
1. Selecteer je iPhone bovenin Xcode in de lijst met apparaten (naast de Play-knop).
2. Druk op de **Play-knop** (Build and Run) of gebruik de sneltoets `Cmd + R`.
3. Xcode zal de app nu compileren en naar je iPhone kopiëren. 

### 7. App vertrouwen op de iPhone
Wanneer je de app voor het eerst probeert te openen op je iPhone, krijg je mogelijk de melding "Niet-vertrouwde ontwikkelaar".
1. Ga op je iPhone naar **Instellingen** > **Algemeen** > **VPN- en apparaatbeheer**.
2. Klik onder "Ontwikkelaarsapp" op je eigen Apple ID.
3. Kies voor **Vertrouw [Je Apple ID]**.

Je kunt CIMDriver nu openen en gebruiken! Let op: bij een gratis Apple ID verloopt de app na 7 dagen. Je moet de app dan opnieuw via Xcode installeren (zonder je data te verliezen) om hem weer 7 dagen te kunnen gebruiken.
