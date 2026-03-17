package com.needtools.oldtube

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.needtools.oldtube.ui.theme.OldTubeTheme

import androidx.navigation.compose.rememberNavController

class MainActivity : ComponentActivity() {


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            OldTubeTheme {
                val navController = rememberNavController()

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


