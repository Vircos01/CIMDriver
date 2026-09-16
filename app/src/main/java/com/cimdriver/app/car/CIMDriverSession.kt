package com.cimdriver.app.car

import android.content.Intent
import androidx.car.app.Screen
import androidx.car.app.Session

class CIMDriverSession : Session() {
    override fun onCreateScreen(intent: Intent): Screen {
        return MainCarScreen(carContext)
    }
}
