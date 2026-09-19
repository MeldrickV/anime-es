package com.zhuchii.anies

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.zhuchii.anies.ui.busqueda.InicioScreen
import com.zhuchii.anies.ui.theme.AniEsTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            AniEsTheme {
                InicioScreen()
            }
        }
    }
}