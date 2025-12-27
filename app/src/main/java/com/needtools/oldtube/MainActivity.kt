package com.needtools.oldtube

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.needtools.oldtube.ui.theme.OldTubeTheme

import androidx.navigation.compose.rememberNavController

class MainActivity : ComponentActivity() {
    private val LOG_TAG = "mainLog"
    // 1. Стан, який ми будемо змінювати для примусової перекомпозиції
    var shouldRefreshData by mutableStateOf(0)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            OldTubeTheme {
                val navController = rememberNavController()
                val refreshKey = shouldRefreshData
                NavHost(
                    navController = navController,
                    startDestination = "youtube_screen"
                ) {

                    composable("youtube_screen") {
                        YouTubeScreen(videoId = "mRLK9z5UMcE")
                    }
                }
            }
        }
    }
}


