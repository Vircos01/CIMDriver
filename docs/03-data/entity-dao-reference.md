# Entity- en DAO-referentie

Dit document is bedoeld als verdieping op het gegevensmodel. Het beschrijft hoe entiteiten en DAO's gedocumenteerd moeten worden en waar ontwikkelaars op moeten letten bij wijzigingen.

## Per entiteit documenteren

Per entiteit hoort minimaal vastgelegd te worden:

- functioneel doel;
- primaire sleutel;
- belangrijke velden;
- verplichte en optionele waarden;
- relaties met andere entiteiten;
- bron van de data;
- mutatiepaden;
- impact op export, backup en dashboards.

## Per DAO documenteren

Per DAO hoort minimaal vastgelegd te worden:

- welke queries beschikbaar zijn;
- welke filters of sorteringen worden toegepast;
- welke methoden write-operaties doen;
- welke methoden door services, ViewModels of widgets worden gebruikt;
- welke query's kritisch zijn voor performance of consistentie.

## Wijzigingsrichtlijnen

Wijzig een entiteit of DAO nooit geïsoleerd. Controleer altijd de impact op migraties, repositories, dashboards, detailschermen, widgets, export, backup en recovery.
