package com.cimdriver.app.ui.screens

import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalConfiguration
import java.util.Locale

@Composable
fun currentAppLocale(): Locale = LocalConfiguration.current.locales[0]
