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

import android.content.ClipboardManager
import android.content.Context
import android.content.res.Configuration
import android.graphics.Bitmap
import android.util.Log
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.core.graphics.drawable.toBitmap
import dev.davidv.translator.AppSettings
import dev.davidv.translator.DownloadService
import dev.davidv.translator.DownloadState
import dev.davidv.translator.LangAvailability
import dev.davidv.translator.Language
import dev.davidv.translator.LaunchMode
import dev.davidv.translator.R
import dev.davidv.translator.TranslatedText
import dev.davidv.translator.TranslatorMessage
import dev.davidv.translator.WordWithTaggedEntries
import dev.davidv.translator.ui.components.DetectedLanguageSection
import dev.davidv.translator.ui.components.DictionaryBottomSheet
import dev.davidv.translator.ui.components.ImageCaptureHandler
import dev.davidv.translator.ui.components.ImageDisplaySection
import dev.davidv.translator.ui.components.LanguageEvent
import dev.davidv.translator.ui.components.LanguageSelectionRow
import dev.davidv.translator.ui.components.StyledTextField
import dev.davidv.translator.ui.components.TranslationField
import dev.davidv.translator.ui.components.ZoomableImageViewer
import dev.davidv.translator.ui.theme.TranslatorTheme
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

@Composable
fun MainScreen(
  // Navigation
  onSettings: () -> Unit,
  // Current state (read-only)
  input: String,
  output: TranslatedText?,
  from: Language,
  to: Language,
  detectedLanguage: Language?,
  displayImage: Bitmap?,
  isTranslating: StateFlow<Boolean>,
  isOcrInProgress: StateFlow<Boolean>,
  dictionaryWord: WordWithTaggedEntries?,
  dictionaryStack: List<WordWithTaggedEntries>,
  dictionaryLookupLanguage: Language?,
  // Action requests
  onMessage: (TranslatorMessage) -> Unit,
  // System integration
  availableLanguages: Map<Language, LangAvailability>,
  downloadStates: Map<Language, DownloadState> = emptyMap(),
  settings: AppSettings,
  launchMode: LaunchMode,
) {
  var showFullScreenImage by remember { mutableStateOf(false) }
  var showImageSourceSheet by remember { mutableStateOf(false) }
  val extraTopPadding = if (launchMode == LaunchMode.Normal) 0.dp else 8.dp
  val context = LocalContext.current

  // Handle back button when dictionary is open
  BackHandler(enabled = dictionaryWord != null) {
    onMessage(TranslatorMessage.ClearDictionaryStack)
  }

  Scaffold(
    floatingActionButton = {
      when (launchMode) {
        LaunchMode.Normal -> {
          if (!settings.disableOcr) {
            FloatingActionButton(onClick = {
              showImageSourceSheet = true
            }) {
              Icon(
                painterResource(id = R.drawable.add_photo),
                contentDescription = "Translate image",
              )
            }
          }
        }

        LaunchMode.ReadonlyModal -> {
        }

        is LaunchMode.ReadWriteModal -> {
          if (output != null) {
            FloatingActionButton(
              onClick = {
                launchMode.reply(output.translated)
              },
              shape = FloatingActionButtonDefaults.largeShape,
            ) {
              Icon(
                painterResource(id = R.drawable.check),
                contentDescription = "Replace text",
                modifier = Modifier.size(30.dp),
              )
            }
          }
        }
      }
    },
  ) { paddingValues ->
    Box(
      modifier =
        Modifier
          .fillMaxSize()
          .navigationBarsPadding()
          .imePadding()
          .padding(top = paddingValues.calculateTopPadding() + extraTopPadding, bottom = 8.dp),
    ) {
      Column(
        modifier =
          Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
      ) {
        LanguageSelectionRow(
          from = from,
          to = to,
          availableLanguages = availableLanguages.mapValues { it.value.translatorFiles },
          onMessage = onMessage,
          onSettings = if (launchMode == LaunchMode.Normal) onSettings else null,
        )

        BoxWithConstraints(
          modifier =
            Modifier
              .fillMaxWidth()
              .weight(1f),
        ) {
          val parentHeight = maxHeight

          Column(
            modifier =
              Modifier
                .fillMaxWidth()
                .let { modifier ->
                  if (displayImage != null) {
                    modifier.verticalScroll(rememberScrollState())
                  } else {
                    modifier
                  }
                },
          ) {
            if (displayImage != null) {
              Box(
                modifier =
                  Modifier
                    .fillMaxWidth()
                    .heightIn(max = parentHeight * 0.7f),
              ) {
                ImageDisplaySection(
                  displayImage = displayImage,
                  isOcrInProgress = isOcrInProgress,
                  isTranslating = isTranslating,
                  onShowFullScreenImage = { showFullScreenImage = true },
                )
                ClearInput(onMessage)
              }
            }

            if (displayImage == null || settings.showOCRDetection) {
              Box(
                modifier =
                  Modifier
                    .fillMaxWidth()
                    .height(parentHeight * 0.5f),
              ) {
                StyledTextField(
                  text = input,
                  onValueChange = { newInput ->
                    onMessage(TranslatorMessage.TextInput(newInput))
                  },
                  onDictionaryLookup = { word ->
                    onMessage(TranslatorMessage.DictionaryLookup(word, from))
                  },
                  placeholder = if (displayImage == null) "Enter text" else null,
                  modifier =
                    Modifier
                      .fillMaxSize()
                      .padding(end = if (displayImage == null) 24.dp else 0.dp),
                  textStyle =
                    MaterialTheme.typography.bodyLarge.copy(
                      fontSize = (MaterialTheme.typography.bodyLarge.fontSize * settings.fontFactor),
                      lineHeight = (MaterialTheme.typography.bodyLarge.lineHeight * settings.fontFactor),
                    ),
                )
                if (displayImage == null) {
                  if (input.isNotEmpty()) {
                    ClearInput(onMessage)
                  } else {
                    PasteButton(onMessage)
                  }
                }
              }
            }

            DetectedLanguageSection(
              detectedLanguage = detectedLanguage,
              from = from,
              availableLanguages = availableLanguages,
              onMessage = onMessage,
              downloadStates = downloadStates,
              onEvent = { event ->
                when (event) {
                  is LanguageEvent.Download -> DownloadService.startDownload(context, event.language)
                  is LanguageEvent.Cancel -> DownloadService.cancelDownload(context, event.language)
                  else -> Log.e("MainScreen", "Got unexpected event: $event")
                }
              },
            )

            Box(
              modifier =
                Modifier
                  .fillMaxWidth()
                  .padding(vertical = 16.dp),
              contentAlignment = Alignment.Center,
            ) {
              HorizontalDivider(
                modifier = Modifier.fillMaxWidth(0.5f),
                thickness = 1.dp,
                color = MaterialTheme.colorScheme.outlineVariant,
              )
            }

            Box(
              modifier =
                Modifier
                  .fillMaxWidth()
                  .height(parentHeight * 0.5f),
            ) {
              if (output != null) {
                TranslationField(
                  text = output,
                  textStyle =
                    MaterialTheme.typography.bodyLarge.copy(
                      fontSize = (MaterialTheme.typography.bodyLarge.fontSize * settings.fontFactor),
                      lineHeight = (MaterialTheme.typography.bodyLarge.lineHeight * settings.fontFactor),
                    ),
                  onDictionaryLookup = {
                    onMessage(TranslatorMessage.DictionaryLookup(it, to))
                  },
                )
              }
            }
          }
        }
      }
    }
  }

  // Image capture handling
  ImageCaptureHandler(
    onMessage = onMessage,
    showImageSourceSheet = showImageSourceSheet,
    onDismissImageSourceSheet = { showImageSourceSheet = false },
  )

  // Full screen image viewer
  if (showFullScreenImage && displayImage != null) {
    ZoomableImageViewer(
      bitmap = displayImage,
      onDismiss = { showFullScreenImage = false },
    )
  }

  // Dictionary bottom sheet
  if (dictionaryWord != null) {
    DictionaryBottomSheet(
      dictionaryWord = dictionaryWord,
      dictionaryStack = dictionaryStack,
      // DictionaryLookupLanguage should always be set if dictionaryWord if set
      dictionaryLookupLanguage = dictionaryLookupLanguage ?: Language.ENGLISH,
      onDismiss = {
        onMessage(TranslatorMessage.ClearDictionaryStack)
      },
      onDictionaryLookup = { word ->
        onMessage(TranslatorMessage.DictionaryLookup(word, dictionaryLookupLanguage ?: Language.ENGLISH))
      },
      onBackPressed = {
        onMessage(TranslatorMessage.PopDictionary)
      },
    )
  }
}

@Composable
fun BoxScope.ClearInput(onMessage: (TranslatorMessage) -> Unit) {
  IconButton(
    onClick = { onMessage(TranslatorMessage.ClearInput) },
    modifier =
      Modifier
        .align(Alignment.TopEnd)
        .size(32.dp),
  ) {
    Icon(
      painterResource(id = R.drawable.cancel),
      contentDescription = "Clear input",
      tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f),
    )
  }
}

@Composable
fun BoxScope.PasteButton(onMessage: (TranslatorMessage) -> Unit) {
  val context = LocalContext.current

  IconButton(
    onClick = {
      val clipboardManager = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
      val clipData = clipboardManager.primaryClip
      if (clipData != null && clipData.itemCount > 0) {
        val text = clipData.getItemAt(0).text?.toString() ?: ""
        onMessage(TranslatorMessage.TextInput(text))
      }
    },
    modifier =
      Modifier
        .align(Alignment.TopEnd)
        .size(32.dp),
  ) {
    Icon(
      painterResource(id = R.drawable.paste),
      contentDescription = "Paste",
      tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f),
    )
  }
}

@Composable
fun WideDialogTheme(content: @Composable () -> Unit) {
  TranslatorTheme {
    Box(
      modifier =
        Modifier
          .fillMaxSize()
          .background(Color.Transparent),
      contentAlignment = Alignment.Center,
    ) {
      Surface(
        modifier =
          Modifier
            .fillMaxWidth(0.9f)
            .height((LocalConfiguration.current.screenHeightDp * 0.5f).dp)
            .clip(RoundedCornerShape(16.dp)),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 6.dp,
      ) {
        content()
      }
    }
  }
}

@Preview(
  showBackground = true,
  uiMode = Configuration.UI_MODE_NIGHT_YES,
)
@Composable
fun PopupMode() {
  WideDialogTheme {
    MainScreen(
      onSettings = { },
      input = "Example input",
      output = TranslatedText("Example output", null),
      from = Language.AZERBAIJANI,
      to = Language.SPANISH,
      detectedLanguage = Language.FRENCH,
      displayImage = null,
      isTranslating = MutableStateFlow(false).asStateFlow(),
      isOcrInProgress = MutableStateFlow(false).asStateFlow(),
      launchMode = LaunchMode.ReadWriteModal {},
      onMessage = {},
      availableLanguages =
        mapOf(
          Language.ENGLISH to LangAvailability(true, true, true),
          Language.SPANISH to LangAvailability(true, true, true),
          Language.FRENCH to LangAvailability(true, true, true),
        ),
      downloadStates = emptyMap(),
      settings = AppSettings(),
      dictionaryWord = null,
      dictionaryStack = emptyList(),
      dictionaryLookupLanguage = null,
    )
  }
}

@Preview(
  showBackground = true,
  uiMode = Configuration.UI_MODE_NIGHT_YES,
)
@Composable
fun MainScreenPreview() {
  TranslatorTheme {
    MainScreen(
      onSettings = { },
      input = "Example input",
      output = TranslatedText("Example output", null),
      from = Language.ENGLISH,
      to = Language.SPANISH,
      detectedLanguage = Language.FRENCH,
      displayImage = null,
      isTranslating = MutableStateFlow(false).asStateFlow(),
      isOcrInProgress = MutableStateFlow(false).asStateFlow(),
      launchMode = LaunchMode.Normal,
      onMessage = {},
      availableLanguages =
        mapOf(
          Language.ENGLISH to LangAvailability(true, true, true),
          Language.SPANISH to LangAvailability(true, true, true),
          Language.FRENCH to LangAvailability(true, true, true),
        ),
      downloadStates = emptyMap(),
      settings = AppSettings(),
      dictionaryWord = null,
      dictionaryStack = emptyList(),
      dictionaryLookupLanguage = null,
    )
  }
}

@Preview(
  showBackground = true,
  uiMode = Configuration.UI_MODE_NIGHT_YES,
)
@Composable
fun PreviewVeryLongText() {
  val vlong = "very long text. ".repeat(100)
  TranslatorTheme {
    MainScreen(
      onSettings = { },
      input = vlong,
      output =
        TranslatedText(
          vlong,
          null,
        ),
      from = Language.ENGLISH,
      to = Language.ENGLISH,
      detectedLanguage = null,
      displayImage = null,
      isTranslating = MutableStateFlow(false).asStateFlow(),
      isOcrInProgress = MutableStateFlow(false).asStateFlow(),
      launchMode = LaunchMode.Normal,
      onMessage = {},
      availableLanguages =
        mapOf(
          Language.ENGLISH to LangAvailability(true, true, true),
          Language.SPANISH to LangAvailability(true, true, true),
          Language.FRENCH to LangAvailability(true, true, true),
        ),
      downloadStates = emptyMap(),
      settings = AppSettings(),
      dictionaryWord = null,
      dictionaryStack = emptyList(),
      dictionaryLookupLanguage = null,
    )
  }
}

@Preview(
  showBackground = true,
  uiMode = Configuration.UI_MODE_NIGHT_YES,
)
@Composable
fun PreviewVeryLongTextImage() {
  val vlong = "very long text. ".repeat(100)
  val context = LocalContext.current
  val drawable = ContextCompat.getDrawable(context, R.drawable.example)
  val bitmap = drawable?.toBitmap()

  TranslatorTheme {
    MainScreen(
      onSettings = { },
      input = vlong,
      output =
        TranslatedText(
          vlong,
          null,
        ),
      from = Language.ENGLISH,
      to = Language.ENGLISH,
      detectedLanguage = null,
      displayImage = bitmap,
      isTranslating = MutableStateFlow(false).asStateFlow(),
      isOcrInProgress = MutableStateFlow(false).asStateFlow(),
      launchMode = LaunchMode.Normal,
      onMessage = {},
      availableLanguages =
        mapOf(
          Language.ENGLISH to LangAvailability(true, true, true),
          Language.SPANISH to LangAvailability(true, true, true),
          Language.FRENCH to LangAvailability(true, true, true),
        ),
      downloadStates = emptyMap(),
      settings = AppSettings(),
      dictionaryWord = null,
      dictionaryStack = emptyList(),
      dictionaryLookupLanguage = null,
    )
  }
}
