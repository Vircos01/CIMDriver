# Componenten

## Doel

Deze pagina beschrijft de herbruikbare SwiftUI-componenten die over features heen ingezet kunnen worden. Het componentniveau moet visuele consistentie én hergebruik van statepatronen ondersteunen.

## Basiscategorieën

- Dashboard cards voor samenvattingen en KPI's.
- Lijstcellen voor ritten, werkuren, adressen en voertuigen.
- Formuliervelden en selectors voor instellingen en beheer.
- Statusbanners voor permissies, tracking en fouten.
- Empty state- en diagnosecomponenten.

## Architectuurrichtlijnen

- Componenten ontvangen eenvoudige, expliciete inputmodellen.
- Componenten bevatten geen businesslogica of repository-aanroepen.
- Formattering en weergavelogica mogen in lichte mate in componenten zitten, domeinbesluiten niet.
- Toegankelijkheid en dynamische tekst zijn onderdeel van het componentcontract.

## Samenhang met design system

Plaats generieke visuele bouwstenen in `Shared/DesignSystem` of `Shared/Components` en feature-specifieke composities in de betreffende featuremap. Daarmee blijft hergebruik mogelijk zonder dat de hele app op één megacomponentbibliotheek leunt.
