package dev.davidv.translator

import android.app.Service
import android.content.Intent
import android.os.Bundle
import android.os.IBinder
import android.os.Message
import android.os.Messenger
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class BackgroundTranslationService : Service() {
  private val TAG = this.javaClass.name.substringAfterLast('.')
  private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

  private lateinit var translationCoordinator: TranslationCoordinator
  private lateinit var langStateManager: LanguageStateManager

  override fun onCreate() {
    super.onCreate()
    val settingsManager = SettingsManager(this)
    val filePathManager = FilePathManager(this, settingsManager.settings)
    val translationService = TranslationService(settingsManager, filePathManager)
    val languageDetector = LanguageDetector()
    val imageProcessor = ImageProcessor(this, OCRService(filePathManager))
    langStateManager = LanguageStateManager(serviceScope, filePathManager, null)
    translationCoordinator = TranslationCoordinator(this, translationService, languageDetector, imageProcessor, settingsManager, false)
  }

  override fun onStartCommand(
    intent: Intent?,
    flags: Int,
    startId: Int,
  ): Int {
    if (intent?.action == "dev.davidv.translator.action.TRANSLATE_TEXT") {
      val textToTranslate = intent.getStringExtra("text_to_translate")
      val fromLanguageStr = intent.getStringExtra("from_language")
      val toLanguageStr = intent.getStringExtra("to_language")
      val fromLanguage = fromLanguageStr?.let { lng -> Language.entries.find { it.code == lng } }
      val toLanguage = toLanguageStr?.let { lng -> Language.entries.find { it.code == lng } }
      val receiver = intent.getParcelableExtra<Messenger>("result_receiver")
      Log.d(TAG, "translate txt: $textToTranslate, $fromLanguage, $toLanguage, cb = ${receiver != null}")

      if (textToTranslate != null && receiver != null) {
        CoroutineScope(Dispatchers.IO).launch {
          langStateManager.languageState.first { !it.isChecking }
          val langs =
            langStateManager.languageState.value.availableLanguageMap.keys
              .toList()
          val from = fromLanguage ?: translationCoordinator.detectLanguageRobust(textToTranslate, null, langs)
          if (from != null) {
            val to = toLanguage ?: SettingsManager(applicationContext).settings.value.defaultTargetLanguage
            val result = translationCoordinator.translateText(from, to, textToTranslate)
            val translatedText =
              when (result) {
                is TranslationResult.Success -> result.result.translated
                is TranslationResult.Error -> "Error: " + result.message
                null -> "Error: Translation failed"
              }
            val bundleKey =
              when (result) {
                is TranslationResult.Success -> "translated_text"
                else -> "error"
              }
            Log.d(TAG, "$bundleKey: $translatedText")
            val bundle = Bundle()
            bundle.putString(bundleKey, translatedText)
            val msg = Message.obtain()
            msg.data = bundle
            receiver.send(msg)
          } else {
            val bundle = Bundle()
            bundle.putString("translated_text", "Error: Could not detect language")
            Log.d(TAG, "Error: Could not detect language")
            val msg = Message.obtain()
            msg.data = bundle
            receiver.send(msg)
          }
          stopSelf()
        }
      }
    } else {
      if (intent == null) {
        Log.d(TAG, "Intent is null")
      } else {
        Log.d(TAG, "Unknown action: ${intent?.action}")
      }
      stopSelf()
    }
    return START_NOT_STICKY
  }

  override fun onBind(intent: Intent?): IBinder? = null
}
