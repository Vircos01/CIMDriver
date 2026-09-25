# Startgids voor nieuwe iOS-ontwikkelaars

## Doel

Deze onboarding helpt nieuwe ontwikkelaars snel begrijpen hoe de iOS-variant van CIMDriver is opgebouwd, welke platformkeuzes gemaakt zijn en waar functionele of technische documentatie gevonden kan worden.

## Eerste leesvolgorde

1. Lees eerst [overview.md](overview.md) voor productdoel, scope en kernscenario's.
2. Bekijk daarna `../../idea_ios/FUNCTIONEEL_OVERZICHT.md` en `../../idea_ios/IOS_IMPLEMENTATIEPLAN.md` voor de functionele en technische koers.
3. Gebruik vervolgens de architectuur- en runtimehoofdstukken om implementatiebeslissingen te onderbouwen.

## Verwachte iOS-stack

- Swift en SwiftUI voor presentatielaag en schermcompositie.
- Core Location, Core Bluetooth en background execution APIs voor tracking.
- SwiftData of Core Data voor lokale opslag, afhankelijk van uiteindelijke targetversie.
- XCTest en XCUITest voor unit-, integratie- en UI-validatie.

## Werkafspraken

- Documenteer Android-pariteit expliciet wanneer gedrag op iOS afwijkt.
- Houd privacy- en batterij-impact zichtbaar bij trackinggerelateerde keuzes.
- Beschrijf iedere nieuwe capability, entitlement of achtergrondmodus in de architectuurdocumentatie.
