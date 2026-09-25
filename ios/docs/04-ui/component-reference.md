# Componentreferentie

## Doel

Leg per herbruikbaar SwiftUI-component de input, output, states en toegankelijkheidseisen vast. Deze referentie helpt bij consistente implementatie van schermen en voorkomt dat hetzelfde component per feature net anders gaat gedragen.

## Per component vastleggen

- Naam en verantwoordelijkheidsgebied.
- Inputmodel of parameters.
- Verwachte interacties.
- Variaties voor loading, leeg, fout of disabled.
- Accessibility- en dynamic type-overwegingen.
- Voorbeeld van gebruik binnen een feature.

## Voorbeelden van componenttypes

- `TripRowView`
- `DashboardSummaryCard`
- `PermissionBanner`
- `WorkSessionHeader`
- `VehicleFormSection`
- `EmptyStateView`

## Richtlijn

Documenteer componenten pas als contracten, niet alleen als visuele schets. Een component is pas herbruikbaar als duidelijk is welke state hij verwacht en welke garanties hij aan de rest van de UI geeft.
