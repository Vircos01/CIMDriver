# Schermen en navigatie

De gebruikersinterface van CIMDriver is omvangrijk en opgebouwd uit Compose-schermen, herbruikbare componenten, dialogs, navigatiebestanden, themabestanden en ViewModels. Daarmee is de UI duidelijk gescheiden van services en opslaglagen.

## Schermen

De bronstructuur bevat onder meer de volgende schermen: dashboard, ritten, ritdetail, voertuigen, adresboek, instellingen, help, diagnostiek, werkuren en formulieren voor toevoegen van adressen, ritten, voertuigen en werkdagen. Dit wijst op een volledige operationele app in plaats van een beperkte MVP-interface.

## Componenten en navigatie

De package `ui.components` bevat onderdelen voor ritten, voertuigen, permissies, instellingen, grafieken en swipe-to-deletegedrag. Daarnaast vormen `AppNavigation.kt`, `NavRoutes.kt` en `Navigation.kt` de ruggengraat voor schermrouting binnen de app.

## ViewModels

De codebasis bevat specifieke ViewModels voor adressen, instellingen, ritdetails, ritten, voertuigen en werkuren. Deze indeling ondersteunt een scherm- of domeingerichte state-aanpak waarin presentatielogica uit de UI zelf wordt getrokken.

## Thema en lokalisatie

Met themabestanden onder `ui.theme`, Compose-lokalisatieondersteuning en resources in onder meer `values` en `values-nl` is de applicatie ingericht voor een consistente visuele stijl en Nederlandstalige gebruikersinhoud. Dat past bij de keuze om ook de technische documentatie in het Nederlands te voeren.
