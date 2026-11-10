package com.filldemaia.cronos

import android.graphics.Color as AndroidColor
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import com.filldemaia.cronos.radio.ui.RadioViewModel
import com.filldemaia.cronos.radio.ui.theme.CronosTheme

class MainActivity : ComponentActivity() {

    private val viewModel: RadioViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(AndroidColor.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(AndroidColor.TRANSPARENT)
        )
        val clockSettings = SettingsRepository(this)
        setContent {
            CronosTheme {
                CronosRoot(viewModel = viewModel, clockSettings = clockSettings)
            }
        }
    }

    override fun onResume() {
        super.onResume()
        // Els permisos es poden concedir des de la configuració del sistema.
        viewModel.refreshReliability()
    }
}
