---
name: cimdriver-release
description: Guidelines and multi-step workflow for creating a new production release of the CIMDriver Android app.
---

# CIMDriver Release Process

When the user asks to create a new release or update the version for the CIMDriver app, follow this strict multi-step workflow.

## 1. Keystore Integrity Check
- Ensure `android/keystore.properties` exists in the `android/` directory but is ignored by `.gitignore`.
- Verify the path inside `keystore.properties`: `RELEASE_STORE_FILE` must be `release.jks` (or exactly the path relative to the android project root). NEVER use `../release.jks` if the keystore is inside the project root, as Gradle will resolve it outside the workspace, resulting in a silently unsigned APK.

## 2. Version Bump (5 Files)
Always bump the version in all 5 of these files to keep the in-app updater in sync:
1. `android/app/build.gradle.kts`: Update `versionCode` (+1) and `versionName`.
2. `version.json` (in de root) EN `android/version.json`: Update `versionCode`, `versionName`, and the filename inside the `apkUrl` string (e.g. `CIMDriver-v1.0.X.apk`) in BOTH files.
3. `README.md`: Update version text and download links to match the new version.
4. `android/CHANGELOG.md`: Add a new section at the top detailing the new changes.

## 3. Build & Extract APK
- Run a clean release build from within the android map: `cd android && ./gradlew clean assembleRelease && cd ..`
- The resulting APK is located at: `android/app/build/outputs/apk/release/app-release.apk`.
- Copy this APK to the `android/release/` directory in the project root and rename it to match the version: `android/release/CIMDriver-v1.0.X.apk`.

## 4. Git & Publish
- If there are many intermediate commits since the last release, offer to squash them down to a single clean "Release: Versie 1.0.X" commit (`git reset --soft <hash>`).
- Add the modified files (including the newly copied APK in the `android/release/` folder).
- Commit and push (use `--force` if you squashed).

## 5. Website & Documentatie (MkDocs) Updates
De publieke website en documentatie zijn een integraal onderdeel van de release. Zorg ervoor dat **zowel de Nederlandse (`.md`) als de Engelse (`.en.md`) documentatie** wordt bijgewerkt:

1. **Changelog Website**: Werk de changelog-pagina's bij voor Android en iOS, in BEIDE talen:
   - `docs/changelog/android.md` én `docs/changelog/android.en.md`
   - `docs/changelog/ios.md` én `docs/changelog/ios.en.md`
   Voeg in deze bestanden een nieuwe release sectie toe (`## Versie X.X.X` / `## Version X.X.X`) inclusief de wijzigingen.
2. **Download Pagina's (`docs/download/android.md` & `.en.md` / `ios.md` & `.en.md`)**: Controleer of versienummers (indien hardcoded in de tekst) kloppen met de nieuwe release in beide talen.
3. **Commit & Push**: Commit deze documentatie/website wijzigingen samen met de app-code in de gesquashte release-commit.
4. **Validatie**: Zodra de release-commit gepusht is (`git push`), wordt via de GitHub Actions (`.github/workflows/mkdocs-pages.yml`) automatisch de website gebouwd en live gezet. Verifieer (indien mogelijk) of deze succesvol afrondt.
