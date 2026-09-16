# Installatie en beheer

Volgens de bestaande projectdocumentatie vereist CIMDriver Android 10 (API 29) of nieuwer en wordt distributie momenteel beschreven via APK-installatie buiten de Play Store. De repository bevat verder de gebruikelijke Gradle-bestanden en release-inhoud voor een Android-project.

## Repository en buildcontext

De root bevat onder meer `build.gradle.kts`, `settings.gradle.kts`, de `app`-module, `gradle`-bestanden, een `release`-map en documentatiebestanden. Daarmee is de repository ingericht voor lokale ontwikkeling, builden en releasemanagement vanuit één projectstructuur.

## Beheeronderwerpen

Voor beheer zijn niet alleen build- en installatiestappen relevant, maar ook manifestconfiguratie, permissies, backupregels, workers, exportvoorzieningen en recoverygedrag. Omdat deze onderwerpen in verschillende technische lagen terugkomen, verwijst deze documentatie naar afzonderlijke themadocumenten voor verdieping.
