# 🚀 What's new? (iOS Changelog)

Stay up to date with the latest updates, new features, and bug fixes for the iOS version of CIMDriver.

---

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
