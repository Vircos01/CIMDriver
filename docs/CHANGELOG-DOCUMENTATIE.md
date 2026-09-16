# Changelog documentatiestructurering

Dit document beschrijft de wijzigingen die zijn doorgevoerd tijdens de opbouw en herstructurering van de Nederlandstalige documentatie van CIMDriver. De changelog richt zich specifiek op structuur, inhoud, navigatie en onderhoudbaarheid van de documentatieset.

## Versie 1.0 — Eerste volledige documentatiestructuur

### Toegevoegd

- Een volledige Nederlandstalige documentatieset voor CIMDriver, gericht op ontwikkelaars en technisch beheer.
- Een centrale documentatie-index in `docs/README.md`.
- Thematische documenten voor overzicht, architectuur, manifest/permissies, tracking, data, UI, widgets, Android Auto/Automotive, achtergrondtaken, diagnostiek, installatie en begrippenlijst.
- Referentiedocumenten voor core, services, data, UI, ViewModels, car, utilities, workers en widgets.
- Een codecatalogus voor de hoofdbronset van de applicatie.
- Een onboardingdocument voor nieuwe ontwikkelaars.
- Aanvullende documenten voor teststrategie, procesflows, runbooks, state- en lifecyclemodellen en entity-/DAO-referentie.
- Een afzonderlijk architectuurdiagram in Mermaid-formaat.

### Resultaat

De documentatie groeide van een beperkte en niet-gestructureerde situatie naar een brede documentatieset die functionele uitleg, technische context, onboarding en referentie combineert. De documentatie ondersteunt nu zowel kennismaking met het project als gerichte verdieping per domein.

## Versie 1.1 — Herstructurering naar submappen

### Gewijzigd

- De platte `docs/`-map is opgesplitst in functionele submappen.
- De documentatie is heringedeeld naar zeven hoofdgebieden: `00-intro`, `01-architecture`, `02-runtime`, `03-data`, `04-ui`, `05-integrations` en `06-reference`.
- `docs/README.md` is herschreven als centrale index op basis van deze nieuwe mapstructuur.

### Doel van de wijziging

De herstructurering maakt de documentatie beter schaalbaar en vergemakkelijkt navigatie voor nieuwe ontwikkelaars, beheerders en toekomstige uitbreidingen. Door onboarding, architectuur, runtimegedrag, data, UI, integraties en referentie van elkaar te scheiden, is de documentatieset overzichtelijker geworden.

## Versie 1.2 — Correctie en validatie van interne links

### Gewijzigd

- Interne markdown-links zijn aangepast aan de nieuwe documentlocaties.
- Ongeldige relatieve links in het onboardingdocument zijn hersteld na validatie van de nieuwe structuur.
- Een validatierapport voor interne markdown-links is gegenereerd als controlemechanisme voor de herstructurering.

### Resultaat

De documentatie is na de herstructurering niet alleen logisch ingedeeld, maar ook intern beter navigeerbaar gemaakt. Hierdoor wordt het risico op kapotte verwijzingen bij onboarding en dagelijks gebruik van de documentatie beperkt.

## Impact op ontwikkelaars

Voor nieuwe ontwikkelaars is het nu eenvoudiger om te starten vanuit onboarding en daarna gericht te verdiepen naar architectuur, runtime, data of UI. Voor bestaande ontwikkelaars en beheerders is de documentatie bruikbaarder geworden als technisch naslagwerk en onderhoudshandleiding.
