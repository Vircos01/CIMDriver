# Schermen en navigatie

## Doel

Dit document beschrijft hoe de SwiftUI-app zijn hoofdschermen structureert en welke navigatiepatronen worden gebruikt. Het doel is een heldere, testbare en uitbreidbare flow die goed werkt op iPhone en later uitbreidbaar blijft naar widget- en CarPlay-ingangen.

## Verwachte hoofdschermen

- Dashboard
- Rittenoverzicht
- Ritdetail
- Werkurenoverzicht
- Adresboek
- Voertuigen
- Instellingen
- Onboarding en permissieflow

## Hoofdnavigatie

De voorkeursopzet is een `TabView` voor primaire productdomeinen, gecombineerd met `NavigationStack` per tab voor diepte. Dit houdt featuregrenzen duidelijk en sluit goed aan op SwiftUI-statebehoud per sectie.

## Navigatieregels

- Iedere tab beheert een eigen pad of routecollectie.
- Detailnavigatie gebruikt typed routes in plaats van losse stringgebaseerde identifiers.
- Cross-feature navigatie verloopt via een coordinator of router, niet via directe knowledge van andere views.
- Modale flows zoals filters, editors of permissie-uitleg verlopen via sheets of full-screen covers.

## Voorbeeldstructuur

```text
TabView
├── DashboardNavigationStack
├── TripsNavigationStack
├── WorkHoursNavigationStack
├── AddressesNavigationStack
└── SettingsNavigationStack
```

## Deep links en notificaties

Notificaties, widgets of toekomstige quick actions landen eerst in een appbrede router. Deze vertaalt externe triggers naar een feature-ingang, bijvoorbeeld direct naar ritdetail of een permissiescherm.

## CarPlay en alternatieve entrypoints

CarPlay en widgetextensies krijgen geen eigen businesslogica, maar hergebruiken dezelfde use-cases en repositories. Navigatie daar is beperkter en wordt daarom apart gecoördineerd buiten de reguliere iPhone-tabstructuur.
