package com.cimdriver.app

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import com.cimdriver.app.ui.navigation.MainScreen
import com.cimdriver.app.ui.theme.CIMDriverTheme

import androidx.compose.foundation.Image
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import kotlinx.coroutines.delay

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.ui.Alignment
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.activity.viewModels
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.collectAsState
import com.cimdriver.app.ui.viewmodel.SettingsViewModel
import com.cimdriver.app.service.OdometerCheckStore

import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    private val settingsViewModel: SettingsViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        handleIntents(intent)
        setContent {
            val settings by settingsViewModel.settings.collectAsState()
            val themeMode = settings?.themeMode ?: "SYSTEM"
            val useDarkTheme = when (themeMode) {
                "DARK" -> true
                "LIGHT" -> false
                else -> isSystemInDarkTheme()
            }

            CIMDriverTheme(useDarkTheme = useDarkTheme) {
                var showSplash by remember { mutableStateOf(true) }

                LaunchedEffect(Unit) {
                    delay(2000)
                    showSplash = false
                }

                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    if (showSplash) {
                        Box(modifier = Modifier.fillMaxSize()) {
                            Image(
                                painter = painterResource(id = R.drawable.splash_screen_full),
                                contentDescription = "CIMDriver Splash Screen",
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.matchParentSize()
                            )
                        }
                    } else {
                        MainScreen()
                    }
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleIntents(intent)
    }

    private fun handleIntents(intent: Intent?) {
        if (intent?.getBooleanExtra("SHOW_ODOMETER_CHECK", false) == true) {
            val vehicleId = intent.getLongExtra("ODOMETER_CHECK_VEHICLE_ID", -1L)
            if (vehicleId != -1L) {
                OdometerCheckStore.requestCheck(vehicleId)
            }
            // Clear the extras so they don't re-trigger
            intent.removeExtra("SHOW_ODOMETER_CHECK")
            intent.removeExtra("ODOMETER_CHECK_VEHICLE_ID")
        }

        if (intent?.hasExtra("EXTRA_VEHICLE_ID") == true) {
            val vehicleId = intent.getLongExtra("EXTRA_VEHICLE_ID", -1L)
            if (vehicleId != -1L) {
                com.cimdriver.app.service.VehicleSelectionStore.requestVehicle(vehicleId)
            }
            intent.removeExtra("EXTRA_VEHICLE_ID")
        }
    }
}
