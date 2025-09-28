package dev.davidv.translator

import android.content.Context
import android.os.Environment
import android.util.Log
import kotlinx.coroutines.flow.StateFlow
import java.io.File

class FilePathManager(
  private val context: Context,
  private val settingsFlow: StateFlow<AppSettings>,
) {
  private val baseDir: File
    get() =
      if (settingsFlow.value.useExternalStorage) {
        val documentsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS)
        File(documentsDir, "dev.davidv.translator").also { dir ->
          if (!dir.exists()) {
            dir.mkdirs()
          }
        }
      } else {
        context.filesDir
      }

  fun getDataDir(): File = File(baseDir, "bin")

  fun getTesseractDataDir(): File = File(baseDir, "tesseract/tessdata")

  fun getTesseractDir(): File = File(baseDir, "tesseract")

  fun getDictionariesDir(): File = File(baseDir, "dictionaries")

  fun getDictionaryFile(language: Language): File = File(getDictionariesDir(), "${language.code}.dict")

  fun getDictionaryIndexFile(): File = File(baseDir, "dictionaries/index.json")

  fun deleteLanguageFiles(language: Language) {
    val dataPath = getDataDir()

    // Delete to English files
    val toEnglishFiles = toEnglishFiles[language]
    toEnglishFiles?.allFiles()?.forEach { fileName ->
      val file = File(dataPath, fileName)
      if (file.exists() && file.delete()) {
        Log.i("FilePathManager", "Deleted: $fileName")
      }
    }

    // Delete from English files
    val fromEnglishFiles = fromEnglishFiles[language]
    fromEnglishFiles?.allFiles()?.forEach { fileName ->
      val file = File(dataPath, fileName)
      if (file.exists() && file.delete()) {
        Log.i("FilePathManager", "Deleted: $fileName")
      }
    }

    // Delete tessdata file
    val tessDataPath = getTesseractDataDir()
    val tessFile = File(tessDataPath, language.tessFilename)
    if (tessFile.exists() && tessFile.delete()) {
      Log.i("FilePathManager", "Deleted: ${tessFile.name}")
    }

    // Delete dictionary file
    val dictionaryFile = getDictionaryFile(language)
    if (dictionaryFile.exists() && dictionaryFile.delete()) {
      Log.i("FilePathManager", "Deleted: ${dictionaryFile.name}")
    }
  }

  fun loadDictionaryIndexFromFile(): DictionaryIndex? {
    return try {
      val indexFile = getDictionaryIndexFile()
      if (!indexFile.exists()) return null

      val jsonString = indexFile.readText()
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
      Log.e("FilePathManager", "Error parsing dictionary index file", e)
      null
    }
  }
}
