

# CIMDriver

**Car In-route Metrics Driver**

*De moderne en slimme ritten- en werkurenregistratie app voor Android*

[![Release](https://img.shields.io/badge/release-v1.1.4-blue)](https://github.com/Vircos01/CIMDriver/releases)
[![Platform](https://img.shields.io/badge/platform-Android%2010%2B-green)](https://developer.android.com/about/versions/10)
[![License: MIT](https://img.shields.io/badge/license-MIT-yellow)](LICENSE)
[![Built with Kotlin](https://img.shields.io/badge/built%20with-Kotlin-purple)](https://kotlinlang.org/)

---


## 📱 Over de app

CIMDriver is een volledig automatische ritten- en werkurenregistratie app voor Android. De app detecteert ritten via Bluetooth-verbinding (bijv. een carkit) en classificeert ze automatisch als **Zakelijk**, **Privé**, of **Woon-werk**. Alle data blijft privé op je eigen toestel.

> [!IMPORTANT]
> **Compatibiliteit:** CIMDriver vereist **Android 10 (API 29) of nieuwer**.
> *Binnenkort:* Volledige integratie voor **Android Auto** volgt zodra de benodigde Google Play Developer licentie is afgerond!

---

## 🚀 Functionaliteiten

- **Volledig Automatische Ritregistratie** — Zodra je Bluetooth (bijv. de carkit) verbindt, begint CIMDriver de rit te loggen. Bij het verbreken van de verbinding wordt de rit netjes gestopt en geclassificeerd via slimme locatie-herkenning.
- **Geautomatiseerde Werkuren** — CIMDriver detecteert wanneer je op kantoor of bij een klant aankomt. Werkdagen worden automatisch gestart, tussendoor gepauzeerd, en gestopt zodra je thuis bent. Geen gedoe meer met handmatig inklokken!
- **Slimme Uren-tolerantie** — Vertrek je 5 minuutjes later? CIMDriver rondt werktijden binnen een (door jou instelbare) marge soepel af naar je vaste werktijden, zodat je urenoverzicht altijd netjes en werkbaar blijft voor de administratie.
- **Rijk & Modern Dashboard** — Inzichten per week, maand, en jaar in een gelikte interface.
- **Privacy First** — Alle ritten en locaties blijven uitsluitend lokaal in de Room-database op je eigen toestel, totdat jij ze als Excel (CSV) exporteert.

👉 **[Bekijk de complete lijst met alle functionaliteiten](release/FEATURES.md)**

---

| ![Dashboard](release/screenshots/Dashboard.png) | ![Rittenlijst](release/screenshots/RittenOverzicht.png) | ![Rit Details](release/screenshots/RitOverzicht.png) |
| **Werkuren** | **Adresboek** | **Voertuigen** |
| ![Werkuren](release/screenshots/Urenoverzicht.png) | ![Adresboek](release/screenshots/Adresboek.png) | ![Voertuigen](release/screenshots/AutoOverzicht.png) |

---

## 📦 Installatie

De app is momenteel niet beschikbaar via de Google Play Store. Installatie verloopt via de APK:

1. [Download `CIMDriver-v1.1.4.apk`](release/CIMDriver-v1.1.4.apk) direct vanuit de repository.
2. Open de APK op je Android-telefoon.
3. Accepteer de melding om installatie vanuit **Onbekende bronnen** toe te staan.

> **Opmerking:** Deze melding verschijnt omdat de app buiten de Play Store wordt gedistribueerd.

---

## 🆕 Wat is nieuw in v1.1.4

- **Database Migratie Fix** — Oplossing voor een crash bij het opstarten of herstellen van backups vanaf oudere versies.
- **Sneller Opstarten** — Het custom splash screen is verwijderd, waardoor de app direct start.
- **Alle wijzigingen van v1.1.3** — Buiten werktijden classificatie, interactieve notificaties, en meer.

Zie [CHANGELOG.md](CHANGELOG.md) voor de volledige versiegeschiedenis.

---

## ❤️ Support

Als je deze app handig vindt en de ontwikkeling wilt steunen, trakteer me dan op een kopje koffie!

[![ko-fi](https://ko-fi.com/img/githubbutton_sm.svg)](https://ko-fi.com/vircos01)

---

*Gebouwd met trots en Kotlin, door Antigravity & Remco.*
