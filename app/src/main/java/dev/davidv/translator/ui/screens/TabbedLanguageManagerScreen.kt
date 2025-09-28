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

package dev.davidv.translator.ui.screens

import android.content.Context
import android.util.Log
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import dev.davidv.translator.AppSettings
import dev.davidv.translator.DictionaryIndex
import dev.davidv.translator.DictionaryInfo
import dev.davidv.translator.DownloadService
import dev.davidv.translator.DownloadState
import dev.davidv.translator.FilePathManager
import dev.davidv.translator.LangAvailability
import dev.davidv.translator.Language
import dev.davidv.translator.LanguageAvailabilityState
import dev.davidv.translator.LanguageManagerScreen
import dev.davidv.translator.LanguageStateManager
import dev.davidv.translator.R
import dev.davidv.translator.createPreviewStates
import dev.davidv.translator.fromEnglishFiles
import dev.davidv.translator.ui.components.LanguageEvent
import dev.davidv.translator.ui.theme.TranslatorTheme
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlin.math.roundToInt

@Composable
fun TabbedLanguageManagerScreen(
  context: Context,
  languageStateManager: LanguageStateManager,
  installedLanguages: List<Language>,
  availableLanguages: List<Language>,
  languageAvailabilityState: LanguageAvailabilityState,
  downloadStates: Map<Language, DownloadState>,
  dictionaryDownloadStates: Map<Language, DownloadState>,
  dictionaryIndex: DictionaryIndex?,
  defaultTabIndex: Int = 0,
) {
  var selectedTabIndex by remember { mutableIntStateOf(defaultTabIndex) }

  val installedDictionaries =
    (installedLanguages + Language.ENGLISH).filter { lang ->
      languageAvailabilityState.availableLanguageMap[lang]?.dictionaryFiles == true
    }
  val availableDictionaries =
    (installedLanguages + Language.ENGLISH).filter { lang ->
      languageAvailabilityState.availableLanguageMap[lang]?.dictionaryFiles == false
    }

  Scaffold(
    modifier =
      Modifier
        .fillMaxSize()
        .navigationBarsPadding()
        .imePadding(),
    topBar = {
      TabRow(selectedTabIndex = selectedTabIndex) {
        Tab(
          selected = selectedTabIndex == 0,
          onClick = { selectedTabIndex = 0 },
          text = { Text("Languages") },
        )
        Tab(
          selected = selectedTabIndex == 1,
          onClick = { selectedTabIndex = 1 },
          text = { Text("Dictionaries") },
        )
      }
    },
  ) { scaffoldPaddingValues ->
    Box(
      modifier =
        Modifier
          .fillMaxSize()
          .navigationBarsPadding()
          .imePadding()
          .padding(scaffoldPaddingValues)
          .padding(bottom = 8.dp),
    ) {
      when (selectedTabIndex) {
        0 -> {
          LanguageManagerScreen(
            embedded = true,
            title = "Language Packs",
            installedLanguages = installedLanguages,
            availableLanguages = availableLanguages,
            languageAvailabilityState = languageAvailabilityState,
            downloadStates = downloadStates,
            availabilityCheck = { it.translatorFiles },
            onEvent = { event ->
              when (event) {
                is LanguageEvent.Download -> DownloadService.startDownload(context, event.language)
                is LanguageEvent.Delete -> languageStateManager.deleteLanguage(event.language)
                is LanguageEvent.Cancel -> DownloadService.cancelDownload(context, event.language)
                is LanguageEvent.DeleteDictionary -> languageStateManager.deleteDict(event.language)
                is LanguageEvent.FetchDictionaryIndex -> {}
              }
            },
            description = { lang ->
              val size = lang.sizeBytes / (1024f * 1024f)
              if (size > 10f) {
                "${size.roundToInt()} MB"
              } else {
                String.format("%.2f MB", size)
              }
            },
            sizeBytes = { it.sizeBytes.toLong() },
          )
        }

        1 -> {
          if (installedLanguages.filterNot { it == Language.ENGLISH }.isNotEmpty()) {
            if (dictionaryIndex == null) {
              Column(
                modifier =
                  Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
              ) {
                Text(
                  text = "Download the index (~5KB) to browse available dictionaries",
                  style = MaterialTheme.typography.bodyLarge,
                  textAlign = TextAlign.Center,
                  modifier = Modifier.padding(bottom = 16.dp),
                )
                Button(
                  onClick = { DownloadService.fetchDictionaryIndex(context) },
                ) {
                  Text("Fetch Dictionary Index")
                }
              }
            } else {
              LanguageManagerScreen(
                embedded = true,
                title = "Dictionary Packs",
                installedLanguages = installedDictionaries,
                availableLanguages = availableDictionaries,
                languageAvailabilityState = languageAvailabilityState,
                downloadStates = dictionaryDownloadStates,
                availabilityCheck = { it.dictionaryFiles },
                sizeBytes = { l ->
                  val indexEntry = dictionaryIndex.dictionaries[l.code]
                  (indexEntry?.size ?: 0)
                },
                description = { l ->
                  val indexEntry = dictionaryIndex.dictionaries[l.code]
                  val size = (indexEntry?.size ?: 0) / (1024f * 1024f)
                  val entries = indexEntry?.wordCount ?: 0
                  val type = indexEntry?.type ?: "unknown"
                  val entriesStr =
                    if (entries == 0L) {
                      ""
                    } else {
                      " - ${humanCount(entries)} entries - $type"
                    }
                  if (size > 10f) {
                    "${size.roundToInt()} MB$entriesStr"
                  } else {
                    String.format("%.2f MB$entriesStr", size)
                  }
                },
                onEvent = { ev ->
                  when (ev) {
                    is LanguageEvent.Download ->
                      DownloadService.startDictDownload(
                        context,
                        ev.language,
                        dictionaryIndex.dictionaries[ev.language.code],
                      )
                    is LanguageEvent.Delete -> languageStateManager.deleteDict(ev.language)
                    is LanguageEvent.FetchDictionaryIndex -> DownloadService.fetchDictionaryIndex(context)
                    else -> {
                      Log.i("LanguageManager", "Got unexpected event $ev")
                    }
                  }
                },
              )
            }
          } else {
            Column(
              modifier =
                Modifier
                  .fillMaxSize()
                  .padding(16.dp),
              horizontalAlignment = Alignment.CenterHorizontally,
              verticalArrangement = Arrangement.Center,
            ) {
              Icon(
                painter = painterResource(id = R.drawable.question),
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                modifier = Modifier.padding(bottom = 16.dp),
              )
              Text(
                text = "To download dictionaries, you need to download translation packages first",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                textAlign = TextAlign.Center,
              )
            }
          }
        }
      }
    }
  }
}

fun humanCount(v: Long): String =
  when {
    v < 1000 -> v.toString()
    v < 1_000_000 -> "${(v / 1000.0).roundToInt()}k"
    else -> {
      val millions = v / 1_000_000.0
      if (millions >= 10) {
        "${millions.roundToInt()}m"
      } else {
        "%.2fm".format(millions)
      }
    }
  }

@Composable
@Preview
fun TabbedLanguageManagerPreview() {
  val states = createPreviewStates()
  val downloadStates by states.downloadStates.collectAsState()

  // Create mock data with some installed languages and dictionaries
  val mockLanguageState =
    LanguageAvailabilityState(
      availableLanguageMap =
        mapOf(
          Language.ENGLISH to LangAvailability(true, true, true),
          Language.FRENCH to LangAvailability(true, true, true),
          Language.SPANISH to LangAvailability(true, true, false),
          Language.GERMAN to LangAvailability(true, true, false),
        ),
    )

  val availLangs = mockLanguageState.availableLanguageMap.filterValues { it.translatorFiles }.keys
  val installedLanguages = availLangs.filter { it != Language.ENGLISH }.sortedBy { it.displayName }
  val availableLanguages =
    Language.entries
      .filter { lang ->
        fromEnglishFiles[lang] != null && !availLangs.contains(lang) && lang != Language.ENGLISH
      }.sortedBy { it.displayName }

  TranslatorTheme {
    TabbedLanguageManagerScreen(
      context = LocalContext.current,
      languageStateManager =
        LanguageStateManager(
          CoroutineScope(Dispatchers.Main),
          FilePathManager(LocalContext.current, kotlinx.coroutines.flow.MutableStateFlow(AppSettings())),
        ),
      installedLanguages = installedLanguages,
      availableLanguages = availableLanguages,
      languageAvailabilityState = mockLanguageState,
      downloadStates = downloadStates,
      dictionaryDownloadStates =
        mapOf(
          Language.SPANISH to DownloadState(isDownloading = true, totalSize = 1000000, downloaded = 500000),
        ),
      dictionaryIndex =
        DictionaryIndex(
          dictionaries =
            mapOf(
              "uk" to DictionaryInfo(1757701535, "uk.dict", 1302634, "english", 47102),
              "zh" to DictionaryInfo(1757701535, "zh.dict", 10272058, "bilingual", 443718),
            ),
          updatedAt = 1757962526,
          version = 1,
        ),
    )
  }
}

@Composable
@Preview
fun TabbedLanguageManagerDictionaryTabPreview() {
  val states = createPreviewStates()
  val downloadStates by states.downloadStates.collectAsState()

  val mockLanguageState =
    LanguageAvailabilityState(
      availableLanguageMap =
        mapOf(
          Language.ENGLISH to LangAvailability(true, true, true),
        ),
    )

  val availLangs = mockLanguageState.availableLanguageMap.filterValues { it.translatorFiles }.keys
  val installedLanguages = availLangs.filter { it != Language.ENGLISH }.sortedBy { it.displayName }
  val availableLanguages =
    Language.entries
      .filter { lang ->
        fromEnglishFiles[lang] != null && !availLangs.contains(lang) && lang != Language.ENGLISH
      }.sortedBy { it.displayName }

  TranslatorTheme {
    TabbedLanguageManagerScreen(
      context = LocalContext.current,
      languageStateManager =
        LanguageStateManager(
          CoroutineScope(Dispatchers.Main),
          FilePathManager(LocalContext.current, kotlinx.coroutines.flow.MutableStateFlow(AppSettings())),
        ),
      installedLanguages = installedLanguages,
      availableLanguages = availableLanguages,
      languageAvailabilityState = mockLanguageState,
      downloadStates = downloadStates,
      dictionaryDownloadStates = emptyMap(),
      dictionaryIndex = null,
      defaultTabIndex = 1,
    )
  }
}
