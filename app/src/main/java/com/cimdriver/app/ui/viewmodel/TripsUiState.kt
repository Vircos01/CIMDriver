package com.cimdriver.app.ui.viewmodel

import com.cimdriver.app.data.local.entity.Trip
import com.cimdriver.app.data.local.entity.Vehicle
import java.util.Calendar

data class TripsScreenUiState(
    val filteredTrips: List<Trip> = emptyList(),
    val vehicles: List<Vehicle> = emptyList(),
    val availableYears: List<Int> = emptyList(),
    val mergeSuggestion: Pair<Trip, Trip>? = null,
    val tripFilter: String = "ALLE",
    val selectedYear: Int = Calendar.getInstance().get(Calendar.YEAR),
    val searchQuery: String = "",
    val globalSelectedVehicleId: Long? = null
)

data class DashboardUiState(
    val dashboardStats: DashboardStats = DashboardStats(),
    val dashboardDistanceChartData: List<DistanceChartItem> = emptyList(),
    val tripsToReviewCount: Int = 0,
    val dashboardTimeFilter: TimeFilter = TimeFilter.MONTH,
    val vehicles: List<Vehicle> = emptyList(),
    val globalSelectedVehicleId: Long? = null,
    val dashboardCategoryBreakdown: List<CategoryBreakdownItem> = emptyList()
)
