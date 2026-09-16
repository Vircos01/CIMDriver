# CIMDriver Release Checklist & Lessons Learned

Dit document bevat de harde lessen en de vaste checklist voor het succesvol uitbrengen van een nieuwe CIMDriver release, inclusief een werkende in-app updater en correct gesigneerde APK.

## ⚠️ Cruciale "Lessons Learned"

1. **Keystore Pad (`keystore.properties`)**
   - **FOUT:** `RELEASE_STORE_FILE=../release.jks`
   - **GOED:** `RELEASE_STORE_FILE=release.jks`
   - *Waarom?* `rootProject.file()` in `build.gradle.kts` evalueert het pad al vanuit de root. Een `../` wijst naar buiten de workspace, waardoor de build stilletjes **ongesigneerd** faalt. Een ongesigneerde APK kan niet als update worden geïnstalleerd (geeft `INSTALL_FAILED_UPDATE_INCOMPATIBLE`).

2. **Versie Consistentie**
   - Als versies niet in sync zijn, merkt de In-App Updater de update niet op, óf downloadt hij een verouderd of niet-bestaand bestand.

## 📝 De Vaste Release Checklist

Volg bij elke nieuwe release stipt deze stappen:

### 1. Versienummers Ophogen (4 plekken!)
- [ ] **`app/build.gradle.kts`**: Verhoog `versionCode` (bijv. 7 -> 8) en pas `versionName` aan (bijv. "1.0.6" -> "1.0.7").
- [ ] **`version.json`**: Werk `versionCode` en `versionName` bij. Pas ook de bestandsnaam in de `apkUrl` URL aan (bijv. `CIMDriver-v1.0.7.apk`).
- [ ] **`README.md`**: Werk de versienummers bij in de tekst en de download-link.
- [ ] **`CHANGELOG.md`**: Voeg een nieuwe sectie toe voor de nieuwe versie en documenteer de wijzigingen.

### 2. Voorbereiden & Bouwen
- [ ] Zorg dat `keystore.properties` aanwezig is in de root, lokaal goed is ingevuld, én is genegeerd in `.gitignore`.
- [ ] Draai een **schone** build om caching problemen te voorkomen:
  ```bash
  ./gradlew clean assembleRelease
  ```
- [ ] Verifieer dat de build succesvol is afgerond.

### 3. In-App Updater Voorbereiden (GitHub Release folder)
- [ ] Kopieer de zojuist gegenereerde APK naar de `release/` map in de repository en geef hem de naam die overeenkomt met `version.json`:
  ```bash
  cp app/build/outputs/apk/release/app-release.apk release/CIMDriver-v1.0.X.apk
  ```
- [ ] (Optioneel maar netjes) Verwijder oudere `.apk` bestanden uit de `release/` map om de repo klein te houden.

### 4. Git History (Squash & Push)
- [ ] Als je veel iteratieve commits hebt gedaan (bijv. fix build, fix typo), squash deze dan tot één schone commit:
  ```bash
  git reset --soft [COMMIT_HASH_VAN_VORIGE_RELEASE]
  git commit -m "Release: Versie 1.0.X (Beschrijving van features)"
  ```
- [ ] Voeg de nieuwe/gewijzigde bestanden toe (inclusief de APK in de `release/` map):
  ```bash
  git add release/CIMDriver-v1.0.X.apk app/build.gradle.kts version.json README.md CHANGELOG.md
  ```
- [ ] Push naar de main branch:
  ```bash
  git push --force origin main
  ```
*(Let op: `--force` is nodig als je commits hebt gesquashed die al gepusht waren).*

Zodra de push op GitHub staat, zal de In-App Updater op de telefoons de nieuwe `version.json` uitlezen en de verse APK uit de `release/` map downloaden!
