package com.hirahira.snowing.debug.lab

import android.graphics.Color
import android.os.Bundle
import androidx.activity.SystemBarStyle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.hirahira.snowing.ui.theme.SnowingTheme

/**
 * The Prototype lab on its own screen, in debug builds only: long-press the Moods tab
 * (the app sends [com.hirahira.snowing.feature.home.ACTION_OPEN_LAB]; only this build answers it).
 * Over the night sky, so the snow above it is easy to judge while tuning.
 */
class LabActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Light bar icons: the lab always sits on the night sky.
        val bars = SystemBarStyle.dark(Color.TRANSPARENT)
        enableEdgeToEdge(statusBarStyle = bars, navigationBarStyle = bars)
        setContent { SnowingTheme { LabRoute() } }
    }
}
