# UI-referentie

## Doel

Deze referentie beschrijft per hoofdscherm welke SwiftUI-structuur, states en interacties verwacht worden. De focus ligt op informatiearchitectuur en stateweergave, niet op visuele pixeluitwerking.

## Dashboard

- Toont samenvattingen van ritten, werkuren en recente status.
- Gebruikt samengestelde presentatiemodellen uit meerdere repositories.
- Moet lege, fout- en loadingstates duidelijk afhandelen.

## Rittenoverzicht

- Lijst of gegroepeerd overzicht van ritten.
- Ondersteunt filteren, zoeken en handmatige correctie-ingangen.
- Heeft een route naar ritdetail en mogelijk snelle acties.

## Ritdetail

- Toont tijdlijn, classificatie, adressen, voertuig en correctiegeschiedenis.
- Bevat acties voor handmatige aanpassing en exportgerelateerde context.

## Werkurenoverzicht

- Toont dag-, week- of maandoverzichten.
- Laat automatische berekening en handmatige correcties naast elkaar zien.

## Adresboek en voertuigen

- Beheren herkenningsregels die de automatische flows beïnvloeden.
- Vragen formulieren, validatie en duidelijke feedback bij wijzigingen.

## Instellingen

- Bevat permissiestatus, trackingvoorkeuren, exportopties en appinformatie.
- Fungeert ook als ingang voor diagnose en fallbackscenario's.
