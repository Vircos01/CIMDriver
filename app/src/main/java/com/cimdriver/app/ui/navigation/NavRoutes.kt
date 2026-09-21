package com.cimdriver.app.ui.navigation

import kotlinx.serialization.Serializable

@Serializable
object DashboardRoute

@Serializable
object TripsRoute

@Serializable
object VehiclesRoute

@Serializable
object WorkHoursRoute

@Serializable
object SettingsRoute

@Serializable
object TripClassificationSettingsRoute

@Serializable
object TrackingPrivacySettingsRoute

@Serializable
object NotificationSettingsRoute

@Serializable
object DataManagementRoute

@Serializable
object WorkDaysEditorRoute

@Serializable
object AddressBookRoute

@Serializable
object AboutRoute

@Serializable
object HelpRoute

@Serializable
object DiagnosticsRoute

@Serializable
data class TripDetailRoute(val tripId: Long)

@Serializable
data class AddTripRoute(val tripId: Long? = null, val copyFrom: Long? = null, val isReturn: Boolean = false)

@Serializable
data class AddWorkDayRoute(val workDayId: Long? = null, val preSelectedDateMs: Long? = null)

@Serializable
data class AddVehicleRoute(val vehicleId: Long? = null)

@Serializable
data class AddAddressRoute(val addressId: Long? = null)
