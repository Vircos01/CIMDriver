package com.cimdriver.app.di

import android.content.Context
import com.cimdriver.app.data.local.AppDatabase
import com.cimdriver.app.data.local.dao.ClassificationRuleDao
import com.cimdriver.app.data.local.dao.LocationPointDao
import com.cimdriver.app.data.local.dao.SavedAddressDao
import com.cimdriver.app.data.local.dao.TripDao
import com.cimdriver.app.data.local.dao.VehicleDao
import com.cimdriver.app.data.local.dao.SettingsDao
import com.cimdriver.app.data.local.dao.WorkDayDao
import com.cimdriver.app.data.local.dao.BluetoothDeviceDao
import com.cimdriver.app.data.repository.TripRepository
import com.cimdriver.app.service.GeocoderService
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    fun provideAppDatabase(@ApplicationContext context: Context): AppDatabase {
        return AppDatabase.getDatabase(context)
    }

    @Provides
    fun provideTripDao(database: AppDatabase): TripDao {
        return database.tripDao()
    }

    @Provides
    fun provideVehicleDao(database: AppDatabase): VehicleDao {
        return database.vehicleDao()
    }

    @Provides
    fun provideClassificationRuleDao(database: AppDatabase): ClassificationRuleDao {
        return database.classificationRuleDao()
    }

    @Provides
    fun provideSavedAddressDao(database: AppDatabase): SavedAddressDao {
        return database.savedAddressDao()
    }

    @Provides
    fun provideSettingsDao(database: AppDatabase): SettingsDao {
        return database.settingsDao()
    }

    @Provides
    fun provideWorkDayDao(database: AppDatabase): WorkDayDao {
        return database.workDayDao()
    }

    @Provides
    fun provideBluetoothDeviceDao(database: AppDatabase): BluetoothDeviceDao {
        return database.bluetoothDeviceDao()
    }

    @Provides
    fun provideLocationPointDao(database: AppDatabase): LocationPointDao {
        return database.locationPointDao()
    }

    @Provides
    @Singleton
    fun provideGeocoderService(@ApplicationContext context: Context): GeocoderService {
        return GeocoderService(context)
    }

    @Provides
    @Singleton
    fun provideTripRepository(
        tripDao: TripDao,
        vehicleDao: VehicleDao,
        classificationRuleDao: ClassificationRuleDao,
        savedAddressDao: SavedAddressDao,
        geocoderService: GeocoderService
    ): TripRepository {
        return TripRepository(
            tripDao = tripDao,
            vehicleDao = vehicleDao,
            classificationRuleDao = classificationRuleDao,
            savedAddressDao = savedAddressDao,
            geocoderService = geocoderService
        )
    }
}
