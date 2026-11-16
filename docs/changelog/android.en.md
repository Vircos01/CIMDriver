# 🚀 What's new? (Changelog)

Stay up to date with the latest updates, new features, and bug fixes for CIMDriver. We regularly roll out updates to make the automatic trip registration even better, more accurate, and more efficient.

---

## Version 1.9.2 (Current Version)
*Release date: November 16, 2026*

!!! info "Patch: Office Wi-Fi Work Hours Tracking & Navigation Bar Redesign"
    * **Automatic Work Hours Tracking via Office Wi-Fi:** Commuters cycling, taking public transit, or walking to work can now automatically start and finish their work hours based on office Wi-Fi presence, without creating phantom car trips.
    * **Vehicle Tracking Priority:** If the tracking service detects an active car trip, vehicle tracking immediately takes precedence over Wi-Fi presence.
    * **Smart Lunch Break Detection & Buffer:** Automatically accounts for on-site lunch breaks (> 4 hours) and off-site lunch walks (10–120 minutes) with a configurable buffer period (default 15 minutes) preventing accidental workday completion.
    * **Address Book Wi-Fi Pairing:** Easily pair an office Wi-Fi network in the Address Book with a single tap.
    * **Navigation Bar Redesign:** Bottom navigation items updated to Dashboard, Trips, Fuel, and Work Hours. Address Book is now conveniently accessed via the side drawer.

## Version 1.9.1
*Release date: November 15, 2026*

!!! info "Patch: Restorable Database Backups & RFC 4180 CSV Escaping"
    * **Restorable Database Backups (`.zip`):** `BackupWorker` now generates genuine SQLite database backups (`.zip`) that can be directly restored on new devices, with automatic retention cleanup.
    * **Clear Separation in Data Management:** The Data Management screen strictly differentiates between restorable `.zip` database backups and read-only `.csv` periodic trip exports.
    * **Full Classification Context for Exports:** CSV exports now always receive all saved addresses, classification rules, work hours settings, and linked vehicle profiles (plate and model).
    * **RFC 4180 CSV Escaping:** Special characters including semicolons (`;`), quotation marks (`"`), and line breaks are fully escaped and quoted according to standard CSV specifications.

## Version 1.9.0
*Release date: November 15, 2026*

!!! success "Phase 9: Tracking Architecture, Privacy-First Backup & Hardening"
    * **Strict Release Signing Validation:** Release builds no longer silently fallback to debug signing; missing keystore credentials now fail the build explicitly during `validateSigningRelease`.
    * **Privacy-First Backup Rules:** Local Room databases (`cimdriver_database`) and odometer photo proofs are explicitly excluded from Google Drive cloud backups, while direct device-to-device transfers remain enabled.
    * **Legacy Widget Cleanup (Glance-first):** Removed the obsolete XML-based widget provider in favor of the modern Jetpack Glance interactive widget.
    * **Unified Trip Classification:** Consolidated conflicting notification routes: all notification quick actions now run cleanly through `NotificationActionReceiver` with uniform constants (`Business Meeting`, `Home To Work`, `PRIVATE`).
    * **TrackingService Modularization:** Decoupled notification handling and channel setup into an independent `TrackingNotificationHelper`.
    * **Hamburger Menu / Drawer Back-Stack Fix:** Screens opened via the navigation drawer now clean up the navigation stack back to the start destination (`popUpTo(startDestination)`). The back button consistently navigates back to the Dashboard.
    * **Clients & Project Codes Menu Button:** The 'Clients & Project Codes' screen now displays a hamburger menu button to open the drawer directly, matching all other drawer screens.
    * **Active Menu Item Indicator:** Navigation drawer items now show an active visual `selected` state corresponding to the active destination.

## Version 1.8.2
*Release date: November 13, 2026*

!!! success "UI Consistency & Design System Harmonization"
    * **Comprehensive UI Consistency & Core Design System:**
        * **Design System Tokens (`Dimensions.kt`):** Unified tokens for spacing (`AppSpacing`), rounded corners (`AppShapes` with 16.dp cards), and iconography (`AppIconSizes`).
        * **Standardized `CimDriverTopBar`:** All sub-screens (Diagnostics, Fuel, Vehicles, Odometer Checks, Address Book, Clients & Project Codes, Hours Targets, Trip Detail, and Settings) share the same branded app bar with CIMDriver Navy background, centered titles, and auto-mirrored back buttons.
        * **Standardized `CimDriverEmptyState`:** Across all list screens (Trips, Work Hours, Fuel, Fleet, Odometer Checks, Address Book, and Clients & Project Codes), empty states are replaced by a clean, consistent view with custom icons, clear guidance, and a direct 1-tap action button.
    * **Daily Inbox Empty State:** Harmonized zero-state message on the Dashboard acknowledging trips, work hours, and fuel/charging sessions.
    * **Clients & Project Codes Restore Fix:** Reliable restore behavior for archived project codes and seamless navigation between screens without state loss.

## Version 1.8.1
*Release date: November 13, 2026*

!!! info "Patch: Pro Rata Fleet Card & Inbox Clarification"
    * **Pro Rata Fleet Card:** The private km limit on the Dashboard fleet card (`MultiVehicleFleetCard`) now calculates pro rata limits dynamically based on in/end of service dates and configured vehicle limits, including a "(naar rato)" label and accurate remaining km guidance.
    * **Daily Inbox Empty State:** The message when all items are reviewed clarifies that no trips, work hours, or fuel/charging drafts require attention.

## Version 1.8.0
*Release date: November 13, 2026*

!!! success "Phase 8: Tax Month-End Closing, Route Presets & Odometer Audit Evidence"
    * **Tax Month-End Closing & Local Share Notification (Privacy-First):** On the 1st of every month, CIMDriver sends a local notification to verify the previous month's trip log and share it directly with your accountant via the native share sheet (PDF/CSV). During the first 7 days of the month, the Dashboard also displays a discrete *"Month-end closing ready"* banner.
    * **Favorite Routes as 1-Tap Presets & Triage Match:** Manual trip entry (`AddTripScreen`) offers 1-tap preset chips for saved favorite routes, auto-filling departure, destination, category, and project code. Automatic triage matching assigns 95% confidence to trips matching a saved favorite route.
    * **Odometer Photo Evidence in History & PDF Appendix:** Dashboard photos taken during odometer readings (ML Kit OCR) are viewable as tap-to-zoom thumbnails in the check history (`OdometerCheckAnalysisScreen`). Odometer checks and dashboard photos are also included as an audit evidence appendix in the Belastingdienst PDF export.
    * **Multi-Vehicle Fleet Card:** For users with 2 or more active vehicles, the Dashboard features a consolidated fleet card showing current odometers, monthly trip stats, and 500 km private limit progress.

## Version 1.7.0
*Release date: November 13, 2026*

!!! success "Phase 7: Smart Work Hours Guidance, Triage & Target Coaching"
    * **Work Hours Review List & Daily Inbox Integration:** Unconfirmed workdays (missing end time, break discrepancies, or deviations from the work schedule) are consolidated into the Daily Inbox (`DailyInboxCard`) on the Dashboard and inside the Work Hours screen.
    * **Gentle End Time & Break Suggestions:** Automatic departure time deduction via the recorded return trip home/office or from the day-specific work schedule (`workDays`). Automatic break suggestion compliant with Dutch Working Hours Act (Arbowet: min. 45m for $\ge$ 8h, 30m for $\ge$ 4.5h).
    * **Workday Status in Plain Language:** Clear status badges per workday (e.g. *"Almost certainly complete (7.5h net, 30m break)"*, *"Missing end time (suggestion: 17:00)"*, *"Long workday (8.2h) without break"*, or *"Registered outside regular work days"*).
    * **Target Coaching with Dynamic Daily Pace:** Target progress now dynamically computes remaining active work days in the current period based on your work schedule, with motivating pacing guidance (e.g. *"16.5h needed over 2 remaining work days (8.3h/day)"*).
    * **Single-Tap Approval with Reversible Undo:** Individual days or all high-confidence workdays can be approved in one tap with an immediate Snackbar undo option.

## Version 1.6.0
*Release date: November 13, 2026*

!!! success "Phase 6: Reliability, Audit & Flawless Export"
    * **Pre-Export Completeness Check (ExportPreCheckDialog):** Before generating PDF or CSV exports, CIMDriver validates the period for unreviewed trips, odometer continuity gaps, and route deviations without explanatory notes. Features a 1-tap bulk approval action and displays the official Belastingdienst tax compliance seal when fully consistent.
    * **Explainable Trip Proposals & Smart Project Codes:** For trips departing from or arriving at saved client addresses, the associated project code is automatically suggested and prefilled, accompanied by transparent reasoning.
    * **Human-Readable Tracking Reliability in Diagnostics:** The diagnostics screen now features a prominent status card ("Optimal" vs. "Attention Required") explaining background location, battery optimization, and notification states in plain language with 1-tap repair buttons.
    * **Reversible Changes (Undo Snackbar):** Quick approvals on the Dashboard or Trips list trigger an `[Undo]` snackbar, allowing accidental actions to be immediately reverted without data loss.
    * **Backup Integrity & Restorability Check:** Data Management screen displays the timestamp, file size, and validated restorability status of the latest automated local backup.

## Version 1.5.0
*Release date: November 13, 2026*

!!! success "Phase 5: Assisted Decision-Making & Calm Autonomy"
    * **Daily Exceptions Inbox ("Daily Triage"):** Unified inbox on the Dashboard consolidating all open action items (trips to review, unconfirmed workdays, odometer gaps, and fuel drafts) with a single-tap bulk approval for high-confidence trips.
    * **Confidence Scoring & Review Suggestions:** Algorithmic confidence scoring (0–100%) for review cases with transparent reasoning ("Exact route rule matched", "Regular commute", etc.) and single-tap approval in Trip Details.
    * **Trip Audit & Contextual Diagnostics:** Clear audit card in Trip Details showing registration origin (GPS background service vs manual), connected vehicle/carkit, and private detour calculations.
    * **Permission & Battery Health Monitor:** Proactive alerts on the dashboard if battery optimization (Doze mode), location, notifications, or Bluetooth permissions risk tracking reliability, with 1-tap system setting repair intents.
    * **Hardened Network & Security Hygiene:** Replaced global cleartext traffic with strict `network_security_config.xml`. All external traffic (RDW, OSRM, Nominatim) is enforced over HTTPS; cleartext is restricted to localhost (`127.0.0.1:8765`) for the local Zepp companion server.
    * **Adaptive Tracking & Fiscal Year Projection:** Stationary GPS jitter filter suppresses drift while stopped at traffic lights. The dashboard private kilometers card displays a predictive fiscal year-end extrapolation with early warning before exceeding the 500 km limit.

## Version 1.4.1
*Release date: November 13, 2026*

!!! bug "Bug Fixes & Stability"
    * **Startup Crash Hotfix:** Fixed an immediate crash on app startup caused by a table name mismatch in the Room database migration (`fuel_fillups`). Added direct migration paths and resilient fallback handling.

## Version 1.4.0
*Release date: November 13, 2026*

!!! success "New Features & Improvements"
    * **Tax-Compliant Exports (Dutch Belastingdienst):** Start and end odometer readings are now included across CSV and PDF exports. The PDF report has been converted to Landscape A4 format featuring a professional header with vehicle details and period totals summary.
    * **Private Detour Distance Tracking:** Dedicated field for private detour kilometers on business trips. Detours are automatically deducted from business totals and counted towards the 500km private limit.
    * **RDW Open Data Vehicle Lookup:** Automatically fetches vehicle make, model, year, and fuel/engine type (EV, PHEV, ICE) from the Dutch RDW database when adding a vehicle.
    * **Live Trip Toggle while Driving:** Switch active trips seamlessly between Business and Private directly from the ongoing tracking notification or the Android Auto screen.
    * **Interactive Home Screen Widget:** Manage trips directly from your Android home screen with active trip status display, trip toggle, and quick start buttons.
    * **Work Hours Export by Period:** Export work hours filtered by individual week or month to PDF or CSV with automatic total hours summary.
    * **Calendar Day Inspection:** Tap any day in the trips calendar to review all trips for that date in an interactive pop-up with direct drill-down to trip details.
    * **Clickable Dashboard Targets:** Target progress cards on the dashboard are now clickable to directly manage work hours and revenue goals.
    * **Dutch Android Auto Localization:** Android Auto car interface localized into clean Dutch with an added option to start Private trips directly.
    * **EV Charging Presets:** Single-tap charging rate presets for electric vehicles (Home €0.28, Public €0.45, Fast charger €0.69) with automatic cost calculation.

## Version 1.3.7
*Release date: November 13, 2026*

!!! success "New Features & Improvements"
    * **Reliable Gas Station & EV Charger Detection:** Search radius doubled to 150m with native Android Geocoder fallback supporting all major brands (Shell, BP, Total, Esso, Texaco, TinQ, Tango, AVIA, Fastned, etc.).
    * **Mid-trip Refueling Detection:** When ignition/Bluetooth disconnects briefly at a station along your route, refueling is captured immediately even if the trip continues afterwards.
    * **Heads-up Notification:** Fuel alerts now use a dedicated high-priority channel (`FuelAlertChannel`). Tapping the alert directly opens registration with pre-filled station and odometer readings.
    * **Dashboard Action Card:** Pending fuel entries are prominently surfaced on the dashboard with quick "Enter Details" and "Dismiss" buttons.
    * **Auto Cost Calculation:** Automatically computes total cost or price per liter when entering amounts.
    * **Vehicle Filter for Fuel & Charging:** Multi-vehicle support with top-bar vehicle filtering on the fuel screen.

## Version 1.3.6
*Release date: November 13, 2026*

!!! success "New Features & Improvements"
    * **Smart Parking Garage Departure Detection:** Automatically determines the correct departure address when exiting an underground garage without GPS coverage, using the parked location and saved addresses within a 1500m radius.
    * **Automatic Distance Correction (OSRM):** When recorded GPS distance is shorter than the true route due to delayed satellite acquisition upon leaving the garage, the app automatically adjusts the trip distance to match the calculated route.
    * **Prevents Odometer Gaps:** The vehicle's odometer reading is updated using the corrected distance, eliminating odometer drift caused by parking structures.
    * **Seamless Map Route:** The map trace automatically connects the departure location in the garage with the first GPS fix.
    * **Pre-filled Departure Address:** When connecting to Bluetooth in a parking garage, the departure address is pre-filled from the previous trip.

## Version 1.3.5
*Release date: November 2, 2026*

!!! success "New Features & Improvements"
    * **Share Trips by Month (PDF & CSV):** Easily export and share trips filtered by individual month instead of the entire archive only.
    * **Monthly Sections with Direct Share Action:** Trip list displays clear monthly section headers with trip & distance totals, plus a direct "Share" action per month.
    * **Custom Month Titles in Reports:** PDF and CSV exports now display the specific month name in report headers and filenames.
    * **Comprehensive Top Bar Export Menu:** Quick selection between monthly exports and the full trips archive.

## Version 1.3.4
*Release date: October 30, 2026*

!!! success "Improvements & Bug Fixes"
    * **Fix Crash on Trip Detail Map:** Resolved MapLibre crash when a trip has a single geocoded point or custom address (e.g. manual entries).
    * **Direct Edit & Delete Actions:** Prominent edit button in top bar and direct delete option with confirmation in trip details.
    * **Timestamp Correction for Gap Resolution:** Ensured created correction trips enforce chronologically forward start and end times.
    * **Safe Duration Display:** Handled negative time differences for overlapping trips gracefully.

## Version 1.3.3
*Release date: October 30, 2026*

!!! success "New Features & Improvements"
    * **Recalculate Odometer Readings Chronologically:** Recompute all odometer start and end values based on trip distances and odometer checkpoints.
    * **Preserve Audit Trail Gaps:** Discrepancies identified during odometer checks remain preserved as open audit points for full tax compliance.
    * **Interactive Preview Dialog:** Preview affected trips, preserved checkpoints, and remaining gaps before applying changes.
    * **Flexible Trip Types in Gap Resolution:** Select between Personal, Business, or Commute when resolving gaps with a new trip.
    * **One-Tap Odometer Sync:** Quickly align current vehicle odometer with the end of the last recorded trip.
    * **Time Window Fix:** Prevent inverted time range display when trips overlap chronologically.

## Version 1.3.2
*Release date: October 30, 2026*

!!! success "Improvements & Bug Fixes"
    * **Fix Odometer Rounding Errors:** Resolved integer division truncation in distance calculation, ensuring meters round mathematically. Prevents gradual distance loss and spontaneous gaps during business trips.
    * **Eliminated Fictitious Distance Distribution:** Completely removed the option to artificially spread gaps across historical trips, ensuring full compliance with tax audit requirements.
    * **Customizable Addresses in Gap Review:** Start and destination addresses can now be freely edited during gap resolution.
    * **Quick Preset Chips for Short Gaps (4-5 km):** One-tap presets for common short private trips ("Groceries", "Local", "Fueling") and audited gap reasons ("Odometer calibration / rounding", "Garage / Inspection").
    * **Distance Display on Frequent Routes:** Route chips now clearly display route distance (e.g. `38 km`).
    * **Enhanced Odometer Check Alerts:** Warning for discrepancies over 30 km and consistent decimal display for trip distances.

## Version 1.3.1
*Release date: October 30, 2026*

!!! success "New Features & Improvements"
    * **Airtight Odometer Registration (Tax Authority Compliant):** Full chain integrity and gap resolution between consecutive trips and periodic odometer checks.
    * **Interactive Check Analysis:** When registering an odometer check with discrepancies, the app provides immediate root-cause analysis and realistic resolution options (assign as private trip, log audited explanation for garage/APK, or defer for manual entry). Unrealistic inflation/distribution of kilometers across existing routes has been eliminated for tax audit compliance.
    * **Retroactive Gap Detection & Review:** Automatically detects gaps and discontinuities in trip history with one-tap batch fixing.
    * **PDF Integrity Report:** Export an official integrity and audit report containing all checks, deviations, and resolutions for tax audit compliance.
    * **Thousands Separators:** Targets (revenue and hours) on the dashboard and detail screens now format numbers with readable thousands separators.

## Version 1.3.0
*Release date: October 26, 2026*

!!! success "Improvements & Bug Fixes"
    * **Dashboard Targets (Bonus/Hours/Revenue):** Worked hours are now reliably processed and displayed in dashboard target progress.
    * **Status Support:** Both approved (`APPROVED`) and completed (`DONE`) workdays count toward target progress.
    * **Project & Client Matching:** Workdays are now properly matched via both `projectCodeId` and project code string (`code`), ensuring hourly rates for revenue targets are accurately applied.
    * **Automatic & Manual Registration:** Project codes are preserved upon approval and automatically populated from saved locations.

## Version 1.2.7
*Release date: October 9, 2026*

!!! success "New Features"
    * **Hours and revenue targets:** Set targets in hours or revenue and link them to a client or project code.
    * **Fixed project rates:** Configure the hourly rate per project code; earned revenue is calculated per project.
    * **Pro-rated annual target:** The configurable employment start date determines how many calendar months count toward the annual target. Starting in September yields 4/12.
    * **Dashboard targets:** The displayed revenue or hours target is now also prorated based on the employment start date.
    * **In-app help:** Added guidance on configuring hours and revenue targets, fixed project rates, and pro-rata calculations.

## Version 1.2.6
*Release date: October 9, 2026*

!!! success "New Features"
    * **Zepp Companion Toggle:** Added a new setting to toggle the Zepp OS integration server (for smartwatches) on or off to improve battery life.
    * **Improved UI for settings:** The option to automatically archive project codes has been moved to the Work hours menu and redesigned as a user-friendly toggle switch.

## Version 1.2.5
*Release date: October 9, 2026*

!!! success "New Features"
    * **Clients & Projects:** Added the ability to create project codes and link them to clients.
    * **Archive Project Codes:** Project codes currently in use are no longer permanently deleted but archived (soft delete).
    * **Auto-Archive:** Added a setting to automatically archive unused project codes after a configurable number of days.
    * **Hours Targets:** When setting hours targets, you can now select project codes to track hours per project/client.

---

## Version 1.2.4
*Release date: October 2, 2026*

## Version 1.2.3
*Release date: October 2, 2026*

!!! bug "Bug fix"
    * **Existing vehicle:** Editing the start and end date for an existing vehicle now works correctly, even when the private-use warning is disabled.
    * **Date fields:** The date fields remain visible and can be edited or cleared directly from the vehicle settings.

---

## Version 1.1.5
*Release date: October 1, 2026*

!!! success "New"
    * **Classification Rules:** Added auto-complete and address book selection. You can now easily select addresses from your address book when setting up rules, just like with trips.

---

## Version 1.1.4
*Release date: September 24, 2026*

!!! success "New"
    * **Documentation:** Release notes have been updated with a warning about the backup functionality, and the project structure has been improved.

!!! bug "Bug fixes"
    * Fixed: Resolved an issue with the release keystore path, ensuring the signed APK is now generated correctly.

---

## Version 1.1.1 to 1.1.3
*Release dates: September 18 (1.1.1) and September 21, 2026 (1.1.2 and 1.1.3)*

!!! info "Adjustments"
    * **Help screen:** The in-app help screen has been updated with refreshed information for users.
    * Internal preparations and minor version increments for stability.

---

## Version 1.0.8 & 1.1.0 (Strict Address Matching)
*Release date: September 17, 2026*

!!! success "New"
    * **Strict Address Matching:** A new algorithm (priority-based) has been implemented to link trips to the correct locations even more accurately and prevent false connections.

!!! bug "Bug fixes"
    * Fixed: The address recognition radius has been reverted to 200 meters to prevent "false positives" (locations matched too quickly).

---

## Version 1.0.7
*Release date: September 16, 2026*

!!! info "Adjustments"
    * **UI Improvements:** Various visual improvements and fixes in address matching and keystore.

---

## Version 1.0.3 & 1.0.4
*Release date: September 16, 2026*

!!! success "New"
    * **Database Update:** Database model updated (version 30) for faster processing of entities and settings.
    * **Swiping:** Trip classification popup added; you can now swipe trips to the right to classify them quickly!

<br>
<div align="center" markdown="1">
[:material-download: Download Android APK](../download/android.en.md){ .md-button .md-button--primary }
</div>
