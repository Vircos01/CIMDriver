# Release Checklist (Stappenplan voor nieuwe versies)

Wanneer er een nieuwe versie van CIMDriver wordt uitgebracht (bijvoorbeeld van v1.0.2 naar v1.0.3), moeten de volgende stappen strikt gevolgd worden om ervoor te zorgen dat de In-App Updater goed blijft werken en dat de code base synchroon blijft.

## 1. Verhoog de Versienummers in de Code
1. Open `app/build.gradle.kts`.
2. Verhoog de `versionCode` (bijv. van 3 naar 4). Dit is een integer die **altijd** hoger moet zijn dan de vorige!
3. Verhoog de `versionName` (bijv. van `"1.0.2"` naar `"1.0.3"`). Let op dat de `-${getGitHash(providers)}` toevoeging intact blijft.

## 2. Update de Documentatie
1. **`CHANGELOG.md`**: Voeg bovenaan een nieuw blokje toe met de nieuwe versie (bijv. `## [1.0.3] - YYYY-MM-DD`) en omschrijf kort wat er is toegevoegd (`### Toegevoegd`), gewijzigd (`### Gewijzigd`), of opgelost (`### Opgelost`).
2. **`README.md`**: Update de versie bovenaan in de titel (bijv. `# CIMDriver v1.0.3`) en pas het blokje `## 🆕 Wat is nieuw in v1.0.3` aan met de highlights uit de release.
3. Indien er nieuwe features zijn: update documentatie-bestanden (zoals `docs/02-runtime/...` of `docs/04-ui/...`).

## 3. Bouw de Release APK
1. Run het Gradle release commando in de terminal (waarbij je refereert naar je eigen `.jks` sleutelbestand). Voorbeeld:
   ```bash
   ./gradlew assembleRelease -PRELEASE_STORE_FILE=../release.jks -PRELEASE_STORE_PASSWORD=<Wachtwoord> -PRELEASE_KEY_ALIAS=<Alias> -PRELEASE_KEY_PASSWORD=<Wachtwoord>
   ```
2. Verplaats en hernoem de resulterende APK vanuit `android/app/build/outputs/apk/release/app-release.apk` naar de root-map `android/release/CIMDriver-v1.X.X.apk`.
3. Verwijder indien gewenst de oude APK uit de `android/release/` map om opslagruimte in de repository te besparen.

## 4. Update de In-App Updater trigger (`version.json`)
1. Open `version.json` in de root van de repository.
2. Pas de `versionCode` en `versionName` aan naar de waardes die je in stap 1 hebt ingesteld.
3. Update de `apkUrl` zodat deze exact verwijst naar de bestandsnaam van je nieuwe APK uit stap 3. (bijv. `.../main/android/release/CIMDriver-v1.0.3.apk`).
4. Pas de `releaseNotes` tekst aan (deze tekst zien gebruikers live in de app in de blauwe banner verschijnen).

## 5. Commit en Push naar GitHub
1. Voeg alle bestanden toe via `git add`.
2. Commit de wijzigingen: `git commit -m "Release: Versie 1.0.3 release"`.
3. Push naar de main branch: `git push origin main`.
4. Tip: overweeg de Git geschiedenis plat te slaan (squash) voor of na het releasen om een schone commit-historie te behouden.

**Zodra de push voltooid is, pikken bestaande gebruikers met de In-App Updater de update onmiddellijk op bij de volgende keer dat zij de app starten.**
