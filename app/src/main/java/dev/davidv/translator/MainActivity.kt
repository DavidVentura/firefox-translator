/*
 * Copyright (C) 2024 David V
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <https://www.gnu.org/licenses/>.
 */

package dev.davidv.translator

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.IBinder
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.mutableStateOf
import dev.davidv.translator.ui.screens.TranslatorApp
import dev.davidv.translator.ui.theme.TranslatorTheme
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class MainActivity : ComponentActivity() {
  private var textToTranslate: String = ""
  private var launchMode: LaunchMode = LaunchMode.Normal
  private var sharedImageUri = mutableStateOf<Uri?>(null)
  private lateinit var ocrService: OCRService
  private lateinit var translationCoordinator: TranslationCoordinator
  private var downloadService: DownloadService? = null
  private lateinit var serviceConnection: ServiceConnection
  private val _downloadServiceState = MutableStateFlow<DownloadService?>(null)
  private val downloadServiceState: StateFlow<DownloadService?> = _downloadServiceState.asStateFlow()

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    enableEdgeToEdge()
    handleIntent(intent)

    val settingsManager = SettingsManager(this) // 8ms
    val filePathManager = FilePathManager(this, settingsManager.settings)
    ocrService = OCRService(filePathManager)
    val imageProcessor = ImageProcessor(this, ocrService)
    val ctx = this

    Log.d("MainActivity", "Initializing translation service")
    val translationService = TranslationService(settingsManager, filePathManager) // 40ms
    val languageDetector = LanguageDetector()
    translationCoordinator = TranslationCoordinator(ctx, translationService, languageDetector, imageProcessor, settingsManager)

    setContent {
      TranslatorTheme {
        TranslatorApp(
          initialText = textToTranslate,
          sharedImageUri = sharedImageUri,
          translationCoordinator = translationCoordinator,
          settingsManager = settingsManager,
          filePathManager = filePathManager,
          downloadServiceState = downloadServiceState,
          initialLaunchMode = launchMode,
        )
      }
    }

    // Create service connection for download service
    serviceConnection =
      object : ServiceConnection {
        override fun onServiceConnected(
          name: ComponentName?,
          service: IBinder?,
        ) {
          Log.d("ServiceConnection", "download service done")
          val binder = service as DownloadService.DownloadBinder
          downloadService = binder.getService()
          _downloadServiceState.value = downloadService
        }

        override fun onServiceDisconnected(name: ComponentName?) {
          downloadService = null
          _downloadServiceState.value = null
        }
      }

    // Bind to download service
    val intent = Intent(this, DownloadService::class.java)
    bindService(intent, serviceConnection, Context.BIND_AUTO_CREATE)
  }

  override fun onDestroy() {
    super.onDestroy()
    ocrService.cleanup()
    TranslationService.cleanup()
    if (::serviceConnection.isInitialized) {
      unbindService(serviceConnection)
    }
    Log.i("MainActivity", "cleaning up")
  }

  public override fun onNewIntent(intent: Intent?) {
    super.onNewIntent(intent)
    handleIntent(intent)
  }

  private fun handleIntent(intent: Intent?) {
    Log.d("MainActivity", "Got intent $intent")
    when (intent?.action) {
      Intent.ACTION_SEND -> {
        // Check if it's text or image
        val text = intent.getStringExtra(Intent.EXTRA_TEXT)
        val imageUri =
          if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            intent.getParcelableExtra(Intent.EXTRA_STREAM, Uri::class.java)
          } else {
            @Suppress("DEPRECATION")
            intent.getParcelableExtra(Intent.EXTRA_STREAM)
          }

        if (text != null) {
          textToTranslate = text
        } else if (imageUri != null) {
          sharedImageUri.value = imageUri
          textToTranslate = "" // Clear any existing text
        }
      }
    }
  }
}
