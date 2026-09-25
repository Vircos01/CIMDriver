# CarPlay en voertuigervaring

## Doel

Dit document beschrijft hoe CarPlay in de SwiftUI-architectuur past zonder de iPhone-appstructuur te dupliceren. CarPlay krijgt een aparte scene en eigen templategedreven UI, maar moet dezelfde domeinservices en repositories gebruiken als de hoofdapp.

## Architectuurpositie

CarPlay gebruikt geen SwiftUI-schermhiërarchie zoals de iPhone-app, maar wel dezelfde use-cases voor ritstatus, voertuigcontext en overzichten. De integratielaag vertaalt CarPlay-events naar domeinintenties en terug naar toegestane template-updates.

## Richtlijnen

- Geen eigen businesslogica in CarPlay scenes.
- Alleen veilige, beperkte interacties beschikbaar maken tijdens rijden.
- Gedeelde state ontsluiten via read-only projecties waar mogelijk.
- Navigatie en templates expliciet scheiden van de reguliere app-router.

## Mogelijke CarPlay-scenario's

- Actuele ritstatus tonen.
- Snelle inzage in voertuig- of trackingstatus.
- Beperkte acties zoals stoppen of markeren van een rit, alleen als platformrichtlijnen dat toelaten.
