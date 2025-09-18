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

package dev.davidv.translator.ui.components
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import dev.davidv.translator.DownloadState
import dev.davidv.translator.LangAvailability
import dev.davidv.translator.Language
import dev.davidv.translator.R
import dev.davidv.translator.ui.theme.TranslatorTheme

sealed class LanguageEvent {
  data class Download(
    val language: Language,
  ) : LanguageEvent()

  data class DownloadDictionary(
    val language: Language,
  ) : LanguageEvent()

  data class Delete(
    val language: Language,
  ) : LanguageEvent()

  data class DeleteDictionary(
    val language: Language,
  ) : LanguageEvent()

  data class Cancel(
    val language: Language,
  ) : LanguageEvent()

  data class Manage(
    val language: Language,
  ) : LanguageEvent()
}

@Composable
fun LanguageDownloadButton(
  language: Language,
  downloadState: DownloadState?,
  state: LangAvailability,
  isLanguageAvailable: Boolean,
  onEvent: (LanguageEvent) -> Unit,
  modifier: Modifier = Modifier,
) {
  val isDownloading = downloadState?.isDownloading == true
  val isCompleted = downloadState?.isCompleted == true

  if (isDownloading) {
    // Progress indicator with cancel button
    Box(
      contentAlignment = Alignment.Center,
      modifier = modifier.size(48.dp),
    ) {
      val targetProgress =
        downloadState!!.downloaded.toFloat() / downloadState.totalSize.toFloat()

      val animatedProgress by animateFloatAsState(
        targetValue = targetProgress,
        animationSpec = tween(durationMillis = 300),
        label = "progress",
      )
      CircularProgressIndicator(
        progress = { animatedProgress },
        modifier = Modifier.size(40.dp),
      )
      IconButton(
        onClick = {
          onEvent(LanguageEvent.Cancel(language))
        },
        modifier = Modifier.size(40.dp),
      ) {
        Icon(
          painterResource(id = R.drawable.cancel),
          contentDescription = "Cancel Download",
        )
      }
    }
  } else if (isLanguageAvailable || isCompleted) {
    // Delete button for available/completed languages
    IconButton(
      onClick = {
        onEvent(LanguageEvent.Delete(language))
      },
      modifier = modifier,
    ) {
      Icon(
        painterResource(id = R.drawable.delete),
        contentDescription = "Delete Language",
      )
    }
  } else {
    // Download/retry button
    IconButton(
      onClick = {
        onEvent(LanguageEvent.Download(language))
      },
      enabled = true,
      modifier = modifier,
    ) {
      when {
        downloadState?.isCancelled == true || downloadState?.error != null -> {
          Icon(
            painterResource(id = R.drawable.refresh),
            contentDescription = "Retry Download",
          )
        }
        else -> {
          Icon(
            painterResource(id = R.drawable.add),
            contentDescription = "Download",
          )
        }
      }
    }
  }
}

@Preview(showBackground = true)
@Composable
fun LanguageDownloadButtonPreview() {
  TranslatorTheme {
    Surface(
      modifier = Modifier.padding(16.dp),
      color = MaterialTheme.colorScheme.background,
    ) {
      Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
      ) {
        // DL
        LanguageDownloadButton(
          language = Language.FRENCH,
          downloadState = null,
          state = LangAvailability(false, true, true),
          isLanguageAvailable = false,
          onEvent = {},
        )

        // Prog
        LanguageDownloadButton(
          language = Language.FRENCH,
          downloadState = DownloadState(isDownloading = true, totalSize = 100, downloaded = 50),
          state = LangAvailability(false, true, true),
          isLanguageAvailable = false,
          onEvent = {},
        )

        // Complete
        LanguageDownloadButton(
          language = Language.FRENCH,
          downloadState = null,
          state = LangAvailability(true, true, true),
          isLanguageAvailable = true,
          onEvent = {},
        )
        // Failed
        LanguageDownloadButton(
          language = Language.FRENCH,
          downloadState = DownloadState(error = "Failed"),
          state = LangAvailability(false, true, true),
          isLanguageAvailable = false,
          onEvent = {},
        )
        // Missing partial
        LanguageDownloadButton(
          language = Language.FRENCH,
          downloadState = null,
          state = LangAvailability(true, true, false),
          isLanguageAvailable = true,
          onEvent = {},
        )
      }
    }
  }
}
