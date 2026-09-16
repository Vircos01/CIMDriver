# Codecatalogus

Dit document geeft een systematisch overzicht van de huidige hoofdbronbestanden in `app/src/main`. Het is bedoeld als naslagwerk voor ontwikkelaars die snel willen zien welke onderdelen in de codebasis aanwezig zijn.

## Kotlin-bestanden

### `app/src/main/java/com/cimdriver/app`

- `CIMDriverApplication.kt`.
- `MainActivity.kt`.

### `app/src/main/java/com/cimdriver/app/car`

- `AddressBookCarScreen.kt`.
- `CIMDriverCarAppService.kt`.
- `CIMDriverSession.kt`.
- `CarUtil.kt`.
- `MainCarScreen.kt`.
- `RecentTripsCarScreen.kt`.
- `ReviewTripScreen.kt`.
- `StartTripScreen.kt`.

### `app/src/main/java/com/cimdriver/app/data/local`

- `AppDatabase.kt`.

### `app/src/main/java/com/cimdriver/app/data/local/dao`

- `ClassificationRuleDao.kt`.
- `Daos.kt`.

### `app/src/main/java/com/cimdriver/app/data/local/entity`

- `Entities.kt`.

### `app/src/main/java/com/cimdriver/app/data/repository`

- `TripRepository.kt`.

### `app/src/main/java/com/cimdriver/app/di`

- `AppModule.kt`.

### `app/src/main/java/com/cimdriver/app/domain`

- `DashboardStatsCalculator.kt`.

### `app/src/main/java/com/cimdriver/app/service`

- `BluetoothReceiver.kt`.
- `BootReceiver.kt`.
- `GeocoderService.kt`.
- `GeofenceBroadcastReceiver.kt`.
- `GeofenceManager.kt`.
- `NotificationActionReceiver.kt`.
- `OdometerCheckStore.kt`.
- `TrackingRecoveryManager.kt`.
- `TrackingService.kt`.
- `TrackingStatus.kt`.
- `VehicleSelectionStore.kt`.

### `app/src/main/java/com/cimdriver/app/ui/components`

- `AddTripComponents.kt`.
- `Charts.kt`.
- `PermissionComponents.kt`.
- `SettingsComponents.kt`.
- `SwipeToDeleteContainer.kt`.
- `TripComponents.kt`.
- `VehicleComponents.kt`.

### `app/src/main/java/com/cimdriver/app/ui/dialogs`

- `Dialogs.kt`.

### `app/src/main/java/com/cimdriver/app/ui/navigation`

- `AppNavigation.kt`.
- `NavRoutes.kt`.
- `Navigation.kt`.

### `app/src/main/java/com/cimdriver/app/ui/screens`

- `AboutScreen.kt`.
- `AddAddressScreen.kt`.
- `AddTripScreen.kt`.
- `AddVehicleScreen.kt`.
- `AddWorkDayScreen.kt`.
- `AddressBookScreen.kt`.
- `ComposeLocale.kt`.
- `DashboardScreen.kt`.
- `DiagnosticsScreen.kt`.
- `HelpScreen.kt`.
- `Screens.kt`.
- `SettingsScreen.kt`.
- `TripDetailScreen.kt`.
- `TripsScreen.kt`.
- `VehiclesScreen.kt`.
- `WorkHoursScreen.kt`.

### `app/src/main/java/com/cimdriver/app/ui/theme`

- `Color.kt`.
- `Theme.kt`.

### `app/src/main/java/com/cimdriver/app/ui/viewmodel`

- `AddressBookViewModel.kt`.
- `SettingsViewModel.kt`.
- `TripDetailViewModel.kt`.
- `TripsUiState.kt`.
- `TripsViewModel.kt`.
- `VehiclesViewModel.kt`.
- `WorkHoursViewModel.kt`.

### `app/src/main/java/com/cimdriver/app/ui/widget`

- `CIMDriverWidget.kt`.
- `CIMDriverWidgetReceiver.kt`.

### `app/src/main/java/com/cimdriver/app/util`

- `AddressMatching.kt`.
- `BackupUtil.kt`.
- `ExportUtil.kt`.
- `TimeUtil.kt`.
- `TripClassification.kt`.
- `TripDistance.kt`.
- `TripStatus.kt`.
- `WorkHoursCalculator.kt`.
- `WorkHoursNormalizer.kt`.
- `WorkHoursUtil.kt`.

### `app/src/main/java/com/cimdriver/app/widget`

- `CIMDriverWidgetProvider.kt`.

### `app/src/main/java/com/cimdriver/app/worker`

- `BackupWorker.kt`.
- `DataLifecycleWorker.kt`.

## XML-bestanden

- `app/src/main/AndroidManifest.xml`.
- `app/src/main/res/drawable/ic_car_add.xml`.
- `app/src/main/res/drawable/ic_car_address.xml`.
- `app/src/main/res/drawable/ic_car_business.xml`.
- `app/src/main/res/drawable/ic_car_commute.xml`.
- `app/src/main/res/drawable/ic_car_history.xml`.
- `app/src/main/res/drawable/ic_car_location.xml`.
- `app/src/main/res/drawable/ic_car_navigation.xml`.
- `app/src/main/res/drawable/ic_car_person.xml`.
- `app/src/main/res/drawable/ic_car_stop.xml`.
- `app/src/main/res/drawable/widget_background.xml`.
- `app/src/main/res/layout/widget_layout.xml`.
- `app/src/main/res/mipmap-anydpi-v26/ic_cimdriver_launcher.xml`.
- `app/src/main/res/mipmap-anydpi-v26/ic_cimdriver_launcher_round.xml`.
- `app/src/main/res/values-night/themes.xml`.
- `app/src/main/res/values-nl/strings.xml`.
- `app/src/main/res/values/colors.xml`.
- `app/src/main/res/values/strings.xml`.
- `app/src/main/res/values/themes.xml`.
- `app/src/main/res/xml/automotive_app_desc.xml`.
- `app/src/main/res/xml/backup_rules.xml`.
- `app/src/main/res/xml/cimdriver_widget_info.xml`.
- `app/src/main/res/xml/data_extraction_rules.xml`.
- `app/src/main/res/xml/provider_paths.xml`.
- `app/src/main/res/xml/widget_info.xml`.