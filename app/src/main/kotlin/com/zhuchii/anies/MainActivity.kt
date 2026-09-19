package com.zhuchii.anies

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.zhuchii.anies.ui.principal.PrincipalScreen
import com.zhuchii.anies.ui.theme.AniEsTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            AniEsTheme {
                PrincipalScreen()
            }
        }
    }
}