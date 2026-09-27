package com.hirahira.snowing

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.hirahira.snowing.feature.control.ControlRoute
import com.hirahira.snowing.ui.theme.SnowingTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            SnowingTheme {
                ControlRoute()
            }
        }
    }
}
