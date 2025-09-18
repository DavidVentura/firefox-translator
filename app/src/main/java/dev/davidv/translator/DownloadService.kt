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

import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Binder
import android.os.IBinder
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.InputStream
import java.net.URL
import java.util.zip.GZIPInputStream
import kotlin.io.path.Path
import kotlin.io.path.createDirectories
import kotlin.math.max

class TrackingInputStream(
  private val inputStream: InputStream,
  private val size: Long,
  private val onProgress: (Long) -> Unit,
) : InputStream() {
  private var totalBytesRead = 0L
  private var lastReportedBytes = 0L

  override fun read(): Int {
    val byte = inputStream.read()
    if (byte != -1) {
      totalBytesRead++
      checkProgress()
    }
    return byte
  }

  override fun read(
    b: ByteArray,
    off: Int,
    len: Int,
  ): Int {
    val bytesRead = inputStream.read(b, off, len)
    if (bytesRead > 0) {
      totalBytesRead += bytesRead
      checkProgress()
    }
    return bytesRead
  }

  private fun checkProgress() {
    if (size > 0) {
      val currentProgress = totalBytesRead
      val incrementalProgress = currentProgress - lastReportedBytes
      if (incrementalProgress > max(128 * 1024, size / 20)) { // 128KiB or 5%
        onProgress(incrementalProgress)
        lastReportedBytes = currentProgress
      }
    }
  }

  override fun close() {
    onProgress(size - lastReportedBytes)
    inputStream.close()
  }
}

class DownloadService : Service() {
  private val binder = DownloadBinder()
  private val serviceScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
  private val settingsManager by lazy { SettingsManager(this) }
  private val filePathManager by lazy { FilePathManager(this, settingsManager.settings) }

  // Track download status for each language
  private val _downloadStates = MutableStateFlow<Map<Language, DownloadState>>(emptyMap())
  val downloadStates: StateFlow<Map<Language, DownloadState>> = _downloadStates

  // Track dictionary download status for each language
  private val _dictionaryDownloadStates = MutableStateFlow<Map<Language, DownloadState>>(emptyMap())
  val dictionaryDownloadStates: StateFlow<Map<Language, DownloadState>> = _dictionaryDownloadStates

  // Event emission for download lifecycle changes
  private val _downloadEvents = MutableSharedFlow<DownloadEvent>()
  val downloadEvents: SharedFlow<DownloadEvent> = _downloadEvents.asSharedFlow()

  // Track download jobs for cancellation
  private val downloadJobs = mutableMapOf<Language, Job>()
  private val dictionaryDownloadJobs = mutableMapOf<Language, Job>()

  companion object {
    fun startDownload(
      context: Context,
      language: Language,
    ) {
      val intent =
        Intent(context, DownloadService::class.java).apply {
          action = "START_DOWNLOAD"
          putExtra("language_code", language.code)
        }
      context.startService(intent)
    }

    fun cancelDownload(
      context: Context,
      language: Language,
    ) {
      val intent =
        Intent(context, DownloadService::class.java).apply {
          action = "CANCEL_DOWNLOAD"
          putExtra("language_code", language.code)
        }
      context.startService(intent)
    }

    fun deleteLanguage(
      context: Context,
      language: Language,
    ) {
      val intent =
        Intent(context, DownloadService::class.java).apply {
          action = "DELETE_LANGUAGE"
          putExtra("language_code", language.code)
        }
      context.startService(intent)
    }

    fun startDictDownload(
      context: Context,
      language: Language,
    ) {
      Log.d("Intent", "Send START_DICT_DOWNLOAD with ${language.code}")
      val intent =
        Intent(context, DownloadService::class.java).apply {
          action = "START_DICT_DOWNLOAD"
          putExtra("language_code", language.code)
        }
      context.startService(intent)
    }

    fun cancelDictDownload(
      context: Context,
      language: Language,
    ) {
      val intent =
        Intent(context, DownloadService::class.java).apply {
          action = "CANCEL_DICT_DOWNLOAD"
          putExtra("language_code", language.code)
        }
      context.startService(intent)
    }

    fun fetchDictionaryIndex(context: Context) {
      val intent =
        Intent(context, DownloadService::class.java).apply {
          action = "FETCH_DICTIONARY_INDEX"
        }
      context.startService(intent)
    }
  }

  override fun onStartCommand(
    intent: Intent?,
    flags: Int,
    startId: Int,
  ): Int {
    when (intent?.action) {
      "START_DOWNLOAD" -> {
        val languageCode = intent.getStringExtra("language_code")
        val language = Language.entries.find { it.code == languageCode }
        if (language != null) {
          startLanguageDownload(language)
        }
      }

      "CANCEL_DOWNLOAD" -> {
        val languageCode = intent.getStringExtra("language_code")
        val language = Language.entries.find { it.code == languageCode }
        if (language != null) {
          cancelLanguageDownload(language)
        }
      }

      "DELETE_LANGUAGE" -> {
        val languageCode = intent.getStringExtra("language_code")
        val language = Language.entries.find { it.code == languageCode }
        if (language != null) {
          deleteLanguageFiles(language)
        }
      }

      "START_DICT_DOWNLOAD" -> {
        val languageCode = intent.getStringExtra("language_code")
        Log.d("onStartCommand", "Dict download for $languageCode")
        val language = Language.entries.find { it.code == languageCode }
        if (language != null) {
          startDictionaryDownload(language)
        }
      }

      "CANCEL_DICT_DOWNLOAD" -> {
        val languageCode = intent.getStringExtra("language_code")
        val language = Language.entries.find { it.code == languageCode }
        if (language != null) {
          cancelDictionaryDownload(language)
        }
      }

      "FETCH_DICTIONARY_INDEX" -> {
        fetchDictionaryIndex()
      }
    }
    return START_STICKY
  }

  override fun onBind(intent: Intent): IBinder = binder

  private fun startLanguageDownload(language: Language) {
    // Don't start if already downloading
    if (_downloadStates.value[language]?.isDownloading == true) return
    updateDownloadState(language) {
      DownloadState(
        isDownloading = true,
        isCompleted = false,
        downloaded = 1,
      )
    }
    val job =
      serviceScope.launch {
        try {
          val downloadTasks = mutableListOf<suspend () -> Boolean>()
          val dataDir = filePathManager.getDataDir()
          val tessDir = filePathManager.getTesseractDataDir()
          Path(tessDir.absolutePath).createDirectories()
          val (fromSize, missingFrom) = missingFilesFrom(dataDir, language)
          val (toSize, missingTo) = missingFilesTo(dataDir, language)
          val tessFile = File(tessDir, language.tessFilename)
          val engTessFile = File(tessDir, Language.ENGLISH.tessFilename)
          var toDownload = (fromSize + toSize).toLong()
          if (missingTo.isNotEmpty()) {
            val tasks = downloadLanguageFiles(dataDir, language, Language.ENGLISH, toEnglishFiles[language]!!.quality, missingTo, language)
            downloadTasks.addAll(tasks)
          }

          if (missingFrom.isNotEmpty()) {
            val tasks =
              downloadLanguageFiles(
                dataDir,
                Language.ENGLISH,
                language,
                fromEnglishFiles[language]!!.quality,
                missingFrom,
                language,
              )
            downloadTasks.addAll(
              tasks,
            )
          }

          if (!tessFile.exists()) {
            val task = downloadTessData(language)
            toDownload += language.tessdataSizeBytes
            downloadTasks.add(task)
          }

          // Always ensure English OCR is available
          if (!engTessFile.exists()) {
            val task = downloadTessData(Language.ENGLISH)
            toDownload += Language.ENGLISH.tessdataSizeBytes
            downloadTasks.add(task)
          }

          // Execute all downloads in parallel
          var success = true
          if (downloadTasks.isNotEmpty()) {
            updateDownloadState(language) {
              it.copy(
                isDownloading = true,
                downloaded = 1,
                totalSize = toDownload,
              )
            }
            Log.i("DownloadService", "Starting ${downloadTasks.count()} download jobs")
            val downloadJobs = downloadTasks.map { task -> async { task() } }
            success = downloadJobs.awaitAll().all { it }
          }
          updateDownloadState(language) {
            DownloadState(
              isDownloading = false,
              isCompleted = success,
            )
          }
          if (success) {
            Log.i("DownloadService", "Download complete: ${language.displayName}")
            // FIXME
            _downloadEvents.emit(DownloadEvent.NewTranslationAvailable(language))
          } else {
            _downloadEvents.emit(DownloadEvent.DownloadError("${language.displayName} download failed"))
          }
        } catch (e: Exception) {
          Log.e("DownloadService", "Download failed for ${language.displayName}", e)
          updateDownloadState(language) {
            it.copy(isDownloading = false, error = e.message)
          }
          _downloadEvents.emit(DownloadEvent.DownloadError("${language.displayName} download failed"))
        } finally {
          downloadJobs.remove(language)
        }
      }

    downloadJobs[language] = job
  }

  private fun startDictionaryDownload(language: Language) {
    if (_dictionaryDownloadStates.value[language]?.isDownloading == true) return
    Log.d("DictionaryDownload", "Starting for $language")
    updateDictionaryDownloadState(language) {
      DownloadState(
        isDownloading = true,
        isCompleted = false,
        downloaded = 1,
      )
    }
    val job =
      serviceScope.launch {
        try {
          val downloadTasks = mutableListOf<suspend () -> Boolean>()
          val dictionaryFile = filePathManager.getDictionaryFile(language)
          var toDownload = 0L

          // TODO
          if (!dictionaryFile.exists()) {
            toDownload += 1000000L // Placeholder size for dictionary file
            downloadTasks.add {
              downloadDictionaryFile(language, dictionaryFile)
            }
          }

          var success = true
          if (downloadTasks.isNotEmpty()) {
            updateDictionaryDownloadState(language) {
              it.copy(
                isDownloading = true,
                downloaded = 1,
                totalSize = toDownload,
              )
            }
            Log.i("DownloadService", "Starting dictionary download for ${language.displayName}")
            success = downloadTasks.all { task -> task() }
          }

          updateDictionaryDownloadState(language) {
            DownloadState(
              isDownloading = false,
              isCompleted = success,
            )
          }

          if (success) {
            Log.i("DownloadService", "Dictionary download complete: ${language.displayName}")
            _downloadEvents.emit(DownloadEvent.NewDictionaryAvailable(language))
          } else {
            _downloadEvents.emit(DownloadEvent.DownloadError("${language.displayName} dictionary download failed"))
          }
        } catch (e: Exception) {
          Log.e("DownloadService", "Dictionary download failed for ${language.displayName}", e)
          updateDictionaryDownloadState(language) {
            it.copy(isDownloading = false, error = e.message)
          }
          _downloadEvents.emit(DownloadEvent.DownloadError("${language.displayName} dictionary download failed"))
        } finally {
          dictionaryDownloadJobs.remove(language)
        }
      }

    dictionaryDownloadJobs[language] = job
  }

  private fun cancelDictionaryDownload(language: Language) {
    dictionaryDownloadJobs[language]?.cancel()
    dictionaryDownloadJobs.remove(language)

    updateDictionaryDownloadState(language) {
      it.copy(isDownloading = false, isCancelled = true, error = null)
    }

    Log.i("DownloadService", "Cancelled dictionary download for ${language.displayName}")
  }

  private fun cancelLanguageDownload(language: Language) {
    downloadJobs[language]?.cancel()
    downloadJobs.remove(language)

    updateDownloadState(language) {
      it.copy(isDownloading = false, isCancelled = true, error = null)
    }

    Log.i("DownloadService", "Cancelled download for ${language.displayName}")
  }

  private fun deleteLanguageFiles(language: Language) {
    serviceScope.launch {
      filePathManager.deleteLanguageFiles(language)
      _downloadEvents.emit(DownloadEvent.LanguageDeleted(language))
      Log.i(
        "DownloadService",
        "Deleted ${language.displayName}",
      )
    }
  }

  private fun updateDownloadState(
    language: Language,
    update: (DownloadState) -> DownloadState,
  ) {
    synchronized(this) {
      val currentStates = _downloadStates.value.toMutableMap()
      val currentState = currentStates[language] ?: DownloadState()
      val newState = update(currentState)
      Log.d(
        "DownloadService",
        "updateDownloadState: ${language.code} thread=${Thread.currentThread().name} before=${currentState.downloaded} after=${newState.downloaded} isDownloading=${newState.isDownloading}",
      )
      currentStates[language] = newState
      _downloadStates.value = currentStates
    }
  }

  private fun incrementDownloadBytes(
    language: Language,
    incrementalBytes: Long,
  ) {
    synchronized(this) {
      val currentStates = _downloadStates.value.toMutableMap()
      val currentState = currentStates[language] ?: DownloadState()
      val newDownloaded = currentState.downloaded + incrementalBytes
      currentStates[language] =
        currentState.copy(
          downloaded = newDownloaded,
        )
      _downloadStates.value = currentStates
    }
  }

  private fun updateDictionaryDownloadState(
    language: Language,
    update: (DownloadState) -> DownloadState,
  ) {
    synchronized(this) {
      val currentStates = _dictionaryDownloadStates.value.toMutableMap()
      val currentState = currentStates[language] ?: DownloadState()
      val newState = update(currentState)
      Log.d(
        "DownloadService",
        "updateDictionaryDownloadState: ${language.code} thread=${Thread.currentThread().name} before=${currentState.downloaded} after=${newState.downloaded} isDownloading=${newState.isDownloading}",
      )
      currentStates[language] = newState
      _dictionaryDownloadStates.value = currentStates
    }
  }

  private fun incrementDictionaryDownloadBytes(
    language: Language,
    incrementalBytes: Long,
  ) {
    synchronized(this) {
      val currentStates = _dictionaryDownloadStates.value.toMutableMap()
      val currentState = currentStates[language] ?: DownloadState()
      val newDownloaded = currentState.downloaded + incrementalBytes
      currentStates[language] =
        currentState.copy(
          downloaded = newDownloaded,
        )
      _dictionaryDownloadStates.value = currentStates
    }
  }

  private fun downloadLanguageFiles(
    dataPath: File,
    from: Language,
    to: Language,
    modelType: ModelType,
    files: List<String>,
    targetLanguage: Language,
  ): List<suspend () -> Boolean> {
    val base = settingsManager.settings.value.translationModelsBaseUrl
    val downloadJobs =
      files.mapNotNull { fileName ->
        val file = File(dataPath, fileName)
        if (!file.exists()) {
          val url = "$base/$modelType/${from.code}${to.code}/$fileName.gz"
          suspend {
            val conn = URL(url).openConnection()
            val size = conn.contentLengthLong
            val success = downloadAndDecompress(conn.getInputStream(), size, file, targetLanguage)
            Log.i("DownloadService", "Downloaded $url to $file = $success")
            success
          }
        } else {
          null
        }
      }
    return downloadJobs
  }

  private fun downloadTessData(language: Language): suspend () -> Boolean {
    val tessDataPath = filePathManager.getTesseractDataDir()
    if (!tessDataPath.isDirectory) {
      tessDataPath.mkdirs()
    }
    val tessFile = File(tessDataPath, "${language.tessName}.traineddata")
    val url = "${settingsManager.settings.value.tesseractModelsBaseUrl}/${language.tessName}.traineddata"

    if (tessFile.exists()) {
      return suspend { true }
    }

    return suspend {
      val conn = URL(url).openConnection()
      val size = conn.contentLengthLong
      val success = download(conn.getInputStream(), size, tessFile, language)
      Log.i(
        "DownloadService",
        "Downloaded tessdata for ${language.displayName} = $url to $tessFile: $success",
      )
      success
    }
  }

  private suspend fun download(
    inputStream: InputStream,
    size: Long,
    outputFile: File,
    language: Language,
    decompress: Boolean = false,
  ) = withContext(Dispatchers.IO) {
    val tempFile = File(outputFile.parentFile, "${outputFile.name}.tmp")

    try {
      outputFile.parentFile?.mkdirs()
      inputStream.use { rawInputStream ->
        val trackingStream =
          TrackingInputStream(rawInputStream, size) { incrementalProgress ->
            incrementDownloadBytes(language, incrementalProgress)
          }

        tempFile.outputStream().use { output ->
          val processedInput = if (decompress) GZIPInputStream(trackingStream) else trackingStream
          processedInput.use { stream ->
            val buffer = ByteArray(16384)
            var bytesRead: Int
            while (stream.read(buffer).also { bytesRead = it } != -1) {
              output.write(buffer, 0, bytesRead)
            }
          }
        }
      }

      if (tempFile.renameTo(outputFile)) {
        true
      } else {
        Log.e(
          "DownloadService",
          "Failed to move temp file $tempFile to final location $outputFile",
        )
        tempFile.delete()
        false
      }
    } catch (e: Exception) {
      val operation = if (decompress) "decompressing" else "downloading"
      Log.e("DownloadService", "Error $operation file", e)
      if (tempFile.exists()) {
        tempFile.delete()
      }
      if (outputFile.exists()) {
        outputFile.delete()
      }
      false
    }
  }

  private suspend fun downloadAndDecompress(
    inputStream: InputStream,
    size: Long,
    outputFile: File,
    language: Language,
  ) = download(inputStream, size, outputFile, language, decompress = true)

  private suspend fun downloadDictionaryFile(
    language: Language,
    outputFile: File,
  ): Boolean =
    withContext(Dispatchers.IO) {
      val url = "${settingsManager.settings.value.dictionaryBaseUrl}/${Constants.DICT_VERSION}/${language.code}.dict"

      return@withContext try {
        val conn = URL(url).openConnection()
        val size = conn.contentLengthLong
        val tempFile = File(outputFile.parentFile, "${outputFile.name}.tmp")

        outputFile.parentFile?.mkdirs()
        conn.getInputStream().use { rawInputStream ->
          val trackingStream =
            TrackingInputStream(rawInputStream, size) { incrementalProgress ->
              incrementDictionaryDownloadBytes(language, incrementalProgress)
            }

          tempFile.outputStream().use { output ->
            trackingStream.use { stream ->
              val buffer = ByteArray(16384)
              var bytesRead: Int
              while (stream.read(buffer).also { bytesRead = it } != -1) {
                output.write(buffer, 0, bytesRead)
              }
            }
          }
        }

        if (tempFile.renameTo(outputFile)) {
          Log.i("DownloadService", "Downloaded dictionary for ${language.displayName} from $url to $outputFile")
          true
        } else {
          Log.e("DownloadService", "Failed to move temp dictionary file $tempFile to final location $outputFile")
          tempFile.delete()
          false
        }
      } catch (e: Exception) {
        Log.e("DownloadService", "Error downloading dictionary file for ${language.displayName}", e)
        if (outputFile.exists()) {
          outputFile.delete()
        }
        false
      }
    }

  private fun fetchDictionaryIndex() {
    serviceScope.launch {
      try {
        val indexFile = filePathManager.getDictionaryIndexFile()
        val url = "${settingsManager.settings.value.dictionaryBaseUrl}/${Constants.DICT_VERSION}/index.json"

        indexFile.parentFile?.mkdirs()
        val tempFile = File(indexFile.parentFile, "${indexFile.name}.tmp")

        val conn = URL(url).openConnection()
        conn.getInputStream().use { inputStream ->
          tempFile.outputStream().use { output ->
            val buffer = ByteArray(16384)
            var bytesRead: Int
            while (inputStream.read(buffer).also { bytesRead = it } != -1) {
              output.write(buffer, 0, bytesRead)
            }
          }
        }

        if (tempFile.renameTo(indexFile)) {
          Log.i("DownloadService", "Downloaded dictionary index from $url to $indexFile")
          val index = loadDictionaryIndexFromFile(indexFile)
          if (index != null) {
            _downloadEvents.emit(DownloadEvent.DictionaryIndexLoaded(index))
          }
        } else {
          Log.e("DownloadService", "Failed to move temp index file $tempFile to final location $indexFile")
          tempFile.delete()
          _downloadEvents.emit(DownloadEvent.DownloadError("Failed to save dictionary index"))
        }
      } catch (e: Exception) {
        Log.e("DownloadService", "Error downloading dictionary index", e)
        val errorMessage = "Failed to download dictionary index: ${e.message ?: "Unknown error"}"
        _downloadEvents.emit(DownloadEvent.DownloadError(errorMessage))
      }
    }
  }

  private fun loadDictionaryIndexFromFile(file: File): DictionaryIndex? {
    if (!file.exists()) return null

    val jsonString = file.readText()
    return try {
      val jsonObject = org.json.JSONObject(jsonString)

      val dictionariesJson = jsonObject.getJSONObject("dictionaries")
      val dictionaries = mutableMapOf<String, DictionaryInfo>()

      for (key in dictionariesJson.keys()) {
        val dictJson = dictionariesJson.getJSONObject(key)
        dictionaries[key] =
          DictionaryInfo(
            date = dictJson.getLong("date"),
            filename = dictJson.getString("filename"),
            size = dictJson.getLong("size"),
            type = dictJson.getString("type"),
            wordCount = dictJson.getLong("word_count"),
          )
      }

      DictionaryIndex(
        dictionaries = dictionaries,
        updatedAt = jsonObject.getLong("updated_at"),
        version = jsonObject.getInt("version"),
      )
    } catch (e: Exception) {
      Log.e("DownloadService", "Error parsing dictionary index file; deleting it", e)
      file.delete()
      null
    }
  }

  override fun onDestroy() {
    super.onDestroy()
    serviceScope.cancel()
    cleanupTempFiles()
  }

  private fun cleanupTempFiles() {
    val binDir = filePathManager.getDataDir()
    if (binDir.exists()) {
      binDir.listFiles()?.filter { it.name.endsWith(".tmp") }?.forEach { tempFile ->
        if (tempFile.delete()) {
          Log.d("DownloadService", "Cleaned up temp file: ${tempFile.name}")
        }
      }
    }

    val tessDir = filePathManager.getTesseractDataDir()
    if (tessDir.exists()) {
      tessDir.listFiles()?.filter { it.name.endsWith(".tmp") }?.forEach { tempFile ->
        if (tempFile.delete()) {
          Log.d("DownloadService", "Cleaned up temp file: ${tempFile.name}")
        }
      }
    }
  }

  inner class DownloadBinder : Binder() {
    fun getService(): DownloadService = this@DownloadService
  }
}

data class DownloadState(
  val isDownloading: Boolean = false,
  val isCompleted: Boolean = false,
  val isCancelled: Boolean = false,
  val downloaded: Long = 0,
  val totalSize: Long = 1,
  val error: String? = null,
)
