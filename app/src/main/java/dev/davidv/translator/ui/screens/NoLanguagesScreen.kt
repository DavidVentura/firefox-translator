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

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import dev.davidv.translator.DownloadService
import dev.davidv.translator.FilePathManager
import dev.davidv.translator.Language
import dev.davidv.translator.LanguageStateManager
import dev.davidv.translator.R
import dev.davidv.translator.SettingsManager
import dev.davidv.translator.fromEnglishFiles
import dev.davidv.translator.ui.theme.TranslatorTheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NoLanguagesScreen(
  onDone: () -> Unit,
  onSettings: () -> Unit,
  languageStateManager: LanguageStateManager,
  downloadService: DownloadService?,
) {
  val state by languageStateManager.languageState.collectAsState()
  val context = LocalContext.current

  Scaffold(
    topBar = {
      TopAppBar(
        title = { Text("Language Setup") },
        actions = {
          IconButton(onClick = onSettings) {
            Icon(
              painterResource(id = R.drawable.settings),
              contentDescription = "Settings",
            )
          }
        },
      )
    },
    bottomBar = {
      Button(
        onClick = onDone,
        enabled = state.hasLanguages,
        modifier =
          Modifier
            .fillMaxWidth()
            .padding(8.dp)
            .navigationBarsPadding(),
      ) {
        Text("Done")
      }
    },
  ) { paddingValues ->
    Column(
      modifier =
        Modifier
          .fillMaxSize()
          .padding(paddingValues)
          .padding(horizontal = 16.dp),
    ) {
      Text(
        text = "Download language packs to start translating",
        style = MaterialTheme.typography.bodyLarge,
        textAlign = TextAlign.Center,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(top = 16.dp, bottom = 8.dp),
      )

      val downloadStates by downloadService?.downloadStates?.collectAsState() ?: run {
        return@run remember { mutableStateOf(emptyMap()) }
      }
      val availLangs = state.availableLanguageMap.filterValues { it.translatorFiles }.keys
      val installedLanguages = availLangs.filter { it != Language.ENGLISH }.sortedBy { it.displayName }
      val availableLanguages =
        Language.entries
          .filter { lang ->
            fromEnglishFiles[lang] != null && !availLangs.contains(lang) && lang != Language.ENGLISH
          }.sortedBy { it.displayName }

      if (downloadService != null) {
        val curDownloadService = downloadService
        val dictionaryDownloadStates by curDownloadService.dictionaryDownloadStates.collectAsState()
        val dictionaryIndex by languageStateManager.dictionaryIndex.collectAsState()

        TabbedLanguageManagerScreen(
          context = context,
          installedLanguages = installedLanguages,
          availableLanguages = availableLanguages,
          languageAvailabilityState = state,
          downloadStates = downloadStates,
          dictionaryDownloadStates = dictionaryDownloadStates,
          dictionaryIndex = dictionaryIndex,
        )
      }
    }
  }
}

@Preview(showBackground = true)
@Composable
fun NoLanguagesScreenPreview() {
  val context = LocalContext.current
  val scope = rememberCoroutineScope()
  val settingsManager = SettingsManager(context)
  val filePathManager = FilePathManager(context, settingsManager.settings)
  TranslatorTheme {
    NoLanguagesScreen(
      onDone = {},
      onSettings = {},
      languageStateManager = LanguageStateManager(scope, filePathManager, DownloadService()),
      downloadService = DownloadService(),
    )
  }
}
