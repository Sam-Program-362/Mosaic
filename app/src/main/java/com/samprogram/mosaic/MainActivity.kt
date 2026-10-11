package com.samprogram.mosaic

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import com.samprogram.mosaic.feature.folders.FolderLibraryRoute
import com.samprogram.mosaic.ui.theme.MosaicTheme

/**
 * Hosts the app. It stays deliberately thin: all folder and media work lives
 * behind the repository and the feature ViewModel.
 */
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MosaicTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background,
                ) {
                    FolderLibraryRoute()
                }
            }
        }
    }
}
