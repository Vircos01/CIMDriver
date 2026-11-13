# 🚀 What's new? (iOS Changelog)

Stay up to date with the latest updates, new features, and bug fixes for the iOS version of CIMDriver.

---

## Version 0.0.13 Alpha (Current Version)
*Release date: November 13, 2026*

!!! info "Patch: Pro Rata Fleet Card & Inbox Clarification"
    * **Pro Rata Fleet Card:** The private km limit on the Dashboard fleet card (`multiVehicleFleetCard`) now calculates pro rata limits dynamically based on in/end of service dates and configured vehicle limits, including a "(naar rato)" label and accurate remaining km guidance.
    * **Daily Inbox Empty State:** The message when all items are reviewed clarifies that no trips, work hours, or alerts require attention.

## Version 0.0.12 Alpha
*Release date: November 13, 2026*

!!! success "Phase 8: Tax Month-End Closing, Route Presets & Odometer Audit Evidence (Android Feature Sync)"
    * **Tax Month-End Closing & Local Share Notification (Privacy-First):** Monthly local notification on the 1st of each month and a discrete dashboard tile during days 1-7 to share the closed month directly with your accountant via the native iOS ShareSheet (PDF/CSV).
    * **Favorite Routes as 1-Tap Presets & Triage Match:** 1-tap presets in `ManualTripEntryView` auto-filling all trip details, plus 95% confidence triage matching in `TripClassification`.
    * **Odometer Photo Evidence & PDF Audit Appendix:** Captured dashboard photos are stored locally, viewable in the odometer check history of `OdometerGapReviewView` (with full-screen zoom), and appended as an audit evidence sheet with photos in the PDF export.
    * **Multi-Vehicle Fleet Card:** Consolidated fleet card on the Dashboard for fleets with $\ge$ 2 active vehicles with a 500 km private limit indicator.

## Version 0.0.11 Alpha
*Release date: November 13, 2026*

!!! success "Phase 7: Smart Work Hours Guidance, Triage & Target Coaching (Android Feature Sync)"
    * **Work Hours Review List & Dashboard Daily Inbox:** Workdays requiring attention (missing checkout time, unrecorded breaks, or off-schedule days) appear consolidated in the Daily Inbox on the Dashboard and in the Work Hours screen.
    * **Gentle Departure & Break Suggestions:** Automatic departure time deduction via the recorded return trip home/office or from the day-specific work schedule (`workDays`). Automatic break suggestion compliant with Dutch Working Hours Act (Arbowet: min. 45m for $\ge$ 8h, 30m for $\ge$ 4.5h).
    * **Workday Status in Plain Language:** Transparent badges such as *"Almost certainly complete (7.5h net, 30m break)"*, *"Missing end time (suggestion: 17:00)"*, and *"Registered outside regular work days"*.
    * **Target Coaching with Dynamic Daily Pace:** Target progress now dynamically computes remaining active work days in the current period based on your work schedule, with motivating pacing guidance (e.g. *"16.5h needed over 2 remaining work days (8.3h/day)"*).
    * **Single-Tap Approval with Reversible Undo Banner:** Quick approval of suggested workday corrections with an animated Undo banner to immediately revert any mistakes.

## Version 0.0.10 Alpha
*Release date: November 13, 2026*

!!! success "Phase 6: Reliability, Audit & Flawless Export (Android Feature Sync)"
    * **Pre-Export Completeness Check (ExportPreCheckSheet):** Before sharing PDF or CSV reports, CIMDriver runs an interactive completeness audit verifying unreviewed trips, odometer gaps, and route deviations without notes, complete with 1-tap bulk approval and tax compliance badge.
    * **Explainable Trip Proposals & Automatic Project Codes:** Project codes are now automatically derived from saved client addresses and suggested in the review overlay and trip edit sheets.
    * **Human-Readable Tracking Reliability in Diagnostics:** Diagnostics now features a status overview explaining iOS location services and notification permissions in plain language with direct navigation shortcuts.
    * **Reversible Changes (Undo Banner):** Batch classifications and quick approvals in the trips list show an animated `[Undo]` banner for immediate error reversal.
    * **Backup Integrity & Restorability Check:** Data Management now verifies the timestamp, file size, and validated restoration status of the latest local JSON backup stored in `Documents/CIMDriver/Backups`.

## Version 0.0.9 Alpha
*Release date: November 13, 2026*

!!! success "Phase 5: Assisted Decision-Making & Dashboard Calmness (Android Feature Sync)"
    * **Daily Exceptions Inbox:** Consolidated triage card on the Dashboard grouping pending trips to review and unconfirmed workdays into a calm, focused overview.
    * **Confidence Scoring & Gentle Review Suggestions:** Algorithmic confidence scoring (0–100%) based on route rules, commute patterns, and work hours. In Trip Details, the suggested category can be approved with a single tap.
    * **Trip Audit & Context:** Transparent context block in Trip Details indicating registration origin (GPS background tracking vs manual), vehicle association, and private detour calculations.
    * **Adaptive GPS Jitter Filter:** Suppression of stationary GPS drift (< 3m at < 0.8 m/s within 30 seconds) to prevent phantom distance and reduce battery impact.
    * **Fiscal Year-End Private Km Projection:** The private limit card now extrapolates full-year private kilometers with an early alert if projected usage risks exceeding the 500 km ceiling.

## Version 0.0.8 Alpha
*Release date: November 13, 2026*

!!! success "New features & Improvements (Android Feature Sync)"
    * **Fiscal Compliance & Private Detour Kilometers:** Full support for private detour kilometers within business and commute trips in accordance with Dutch Tax Authority (Belastingdienst) requirements. Private detours automatically count towards the 500 km yearly threshold and are deducted from business totals.
    * **Landscape A4 PDF Reports:** Overhauled PDF export in Landscape A4 format featuring vehicle metadata headers, full fiscal columns (start/end odometer, detour km, trip purpose, addresses), and a clear totals summary block.
    * **RDW License Plate Lookup:** One-tap RDW integration in vehicle management to auto-fetch make, commercial model, and powertrain type (ICE, PHEV, EV) via the official Dutch open data API.
    * **Live Trip Classification Toggle:** Switch between Business and Private on the fly directly from the dashboard status card during active tracking.
    * **Calendar Day Tap Inspection:** Tap any date in the trips calendar to instantly inspect all trips for that day and drill down into the trip details view.
    * **Periodic Work Hours Export & Share Button:** Direct monthly work hours export (PDF & CSV) via section share buttons, complete with total worked hours calculation.
    * **Quick Target Management Navigation:** Jump directly to hours and revenue target management from the dashboard via the new "Beheren" action or by tapping any target card.
    * **EV & Charging Presets:** Electric vehicle support in fuel tracking with automatic kWh units, charging type badges, and quick presets for Home (€0.28), Public (€0.45), and Fast charging (€0.69 / kWh).

## Version 0.0.7 Alpha
*Release date: October 30, 2026*

!!! success "New features"
    * **Odometer gap detection:** Automatically detect missing or overlapping kilometers between consecutive trips, start odometer discrepancies, and current vehicle odometer mismatches.
    * **Chronological odometer recalculation:** Recalculate trip odometer readings based on trip distances and verified odometer checkpoints while preserving audited anchors.
    * **Gap resolution workflow:** Resolve detected gaps with connecting trips (private, business, or commute with quick presets), synchronize current vehicle odometer, or record audit explanations (MOT/APK, maintenance, test drive).
    * **Enhanced trip editing:** Direct edit and delete actions from the trip details screen, with editable timestamps, addresses, and odometer readings.
    * **Resilient map rendering:** Safe map polyline rendering for single-coordinate or short manual trips.

## Version 0.0.6 Alpha
*Release date: October 9, 2026*

!!! success "New features"
    * **Clients and project codes:** Manage clients and project codes, configure a fixed hourly rate per project, and archive unused codes.
    * **Hours and revenue targets:** Track annual targets by hours, client, or project. Revenue uses the fixed project rate.
    * **Pro-rated targets:** Set an employment start date. Starting on September 1 counts 4/12 of the annual target; the adjusted goal appears on the dashboard.
    * **Project selection and search:** Assign project codes to addresses, trips, and workdays, and search work hours by project, location, or note.
    * **Odometer recognition:** A photo of the odometer can be read with text recognition; review the detected value before saving.
    * **Classification rules:** Enable or disable rules, set priorities, and automatically approve matching trips.
    * **In-app help:** Added guidance for project rates and prorated targets.

## Version 0.0.5 (Alpha)
*Release date: September 2026*

!!! success "New"
    * **iOS Alpha Launch:** The first closed test version for iOS is now available via TestFlight.
    * **Automatic Trip Registration:** Basic functionality for automatically detecting trips in the background.
    * **Apple CarPlay Integration (Beta):** Initial setup to start and manage trips from your dashboard.

!!! info "Known Issues"
    * In extreme power-saving mode, iOS may suspend the app after 24 hours of inactivity, meaning trips will only resume when the app is manually opened. This is being investigated.

<br>
<div align="center" markdown="1">
[:material-apple: Download iOS Alpha](../download/ios.en.md){ .md-button .md-button--primary }
</div>
