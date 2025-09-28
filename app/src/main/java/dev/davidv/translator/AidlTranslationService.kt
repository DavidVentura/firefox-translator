package dev.davidv.translator

import android.app.Service
import android.content.Intent
import android.os.IBinder
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class AidlTranslationService : Service() {
  private val TAG = this.javaClass.name.substringAfterLast('.')

  private lateinit var translationCoordinator: TranslationCoordinator
  private lateinit var langStateManager: LanguageStateManager
  private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

  override fun onCreate() {
    super.onCreate()
    val settingsManager = SettingsManager(this)
    val filePathManager = FilePathManager(this, settingsManager.settings)
    val translationService = TranslationService(settingsManager, filePathManager)
    val languageDetector = LanguageDetector()
    val imageProcessor = ImageProcessor(this, OCRService(filePathManager))
    translationCoordinator = TranslationCoordinator(this, translationService, languageDetector, imageProcessor, settingsManager, false)
    langStateManager = LanguageStateManager(serviceScope, filePathManager, null)
    Log.d(TAG, "onCreate")
  }

  override fun onBind(intent: Intent?): IBinder {
    Log.d(TAG, "onBind")
    return binder
  }

  private val binder =
    object : ITranslationService.Stub() {
      override fun translate(
        textToTranslate: String?,
        fromLanguageStr: String?,
        toLanguageStr: String?,
        callback: ITranslationCallback?,
      ) {
        Log.d(TAG, "txt len:${textToTranslate?.length ?: -1}, from:$fromLanguageStr, to:$toLanguageStr, cb = ${callback != null}")

        if (textToTranslate == null || callback == null) {
          Log.w(TAG, "translate: textToTranslate or callback is null")
          return
        }

        val fromLanguage = fromLanguageStr?.takeIf { it.isNotEmpty() }?.let { lng -> Language.entries.find { it.code == lng } }
        val toLanguage = toLanguageStr?.takeIf { it.isNotEmpty() }?.let { lng -> Language.entries.find { it.code == lng } }

        CoroutineScope(Dispatchers.IO).launch {
          langStateManager.languageState.first { !it.isChecking }
          val langs =
            langStateManager.languageState.value.availableLanguageMap.keys
              .toList()
          while (translationCoordinator.isTranslating.value) {
            delay(100)
          }
          val from = fromLanguage ?: translationCoordinator.detectLanguageRobust(textToTranslate, null, langs)
          if (from != null) {
            val to = toLanguage ?: SettingsManager(applicationContext).settings.value.defaultTargetLanguage
            val result = translationCoordinator.translateText(from, to, textToTranslate)
            when (result) {
              is TranslationResult.Success -> {
                val translatedText = result.result.translated
                Log.d(TAG, "translated text: $translatedText")
                callback.onTranslationResult(translatedText)
              }

              is TranslationResult.Error -> {
                val errorMessage = "Error: " + result.message
                Log.d(TAG, errorMessage)
                callback.onTranslationError(errorMessage)
              }

              null -> {
                val errorMessage = "Error: Translation failed"
                Log.d(TAG, errorMessage)
                callback.onTranslationError(errorMessage)
              }
            }
          } else {
            val errorMessage = "Error: Could not detect language"
            Log.d(TAG, errorMessage)
            callback.onTranslationError(errorMessage)
          }
        }
      }
    }

  override fun onStartCommand(
    intent: Intent?,
    flags: Int,
    startId: Int,
  ): Int {
    Log.d(TAG, "onStartCommand received, but this service is meant to be bound.")
    return START_NOT_STICKY
  }

  override fun onUnbind(intent: Intent?): Boolean {
    Log.d(TAG, "onUnbind")
    return super.onUnbind(intent)
  }

  override fun onDestroy() {
    Log.d(TAG, "onDestroy")
    super.onDestroy()
  }
}
