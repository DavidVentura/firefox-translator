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

import android.content.res.Configuration
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import dev.davidv.translator.Language
import dev.davidv.translator.R
import dev.davidv.translator.TranslatorMessage
import dev.davidv.translator.ui.theme.TranslatorTheme

@Composable
fun LanguageSelectionRow(
  from: Language,
  to: Language,
  availableLanguages: Map<Language, Boolean>,
  onMessage: (TranslatorMessage) -> Unit,
  onSettings: (() -> Unit)?,
  drawable: Pair<String, Int>,
) {
  Row(
    modifier = Modifier.fillMaxWidth(),
    horizontalArrangement = Arrangement.spacedBy(8.dp),
    verticalAlignment = Alignment.CenterVertically,
  ) {
    val fromLanguages = Language.entries.filter { x -> x != to && x != from && availableLanguages[x] == true }
    val toLanguages = Language.entries.filter { x -> x != from && x != to && availableLanguages[x] == true }

    LanguageSelector(
      selectedLanguage = from,
      availableLanguages = fromLanguages,
      onLanguageSelected = { language ->
        onMessage(TranslatorMessage.FromLang(language))
      },
      modifier = Modifier.weight(1f),
    )

    IconButton(onClick = {
      onMessage(TranslatorMessage.SwapLanguages)
    }) {
      Icon(
        painterResource(id = R.drawable.compare),
        contentDescription = "Reverse translation direction",
      )
    }

    LanguageSelector(
      selectedLanguage = to,
      availableLanguages = toLanguages,
      onLanguageSelected = { language ->
        onMessage(TranslatorMessage.ToLang(language))
      },
      modifier = Modifier.weight(1f),
    )

    if (onSettings != null) {
      IconButton(onClick = onSettings) {
        Icon(
          painterResource(id = drawable.second),
          contentDescription = drawable.first,
        )
      }
    }
  }
}

@Preview(showBackground = true)
@Composable
fun LanguageSelectionRowPreview() {
  TranslatorTheme {
    LanguageSelectionRow(
      from = Language.ENGLISH,
      to = Language.SPANISH,
      availableLanguages =
        mapOf(
          Language.ENGLISH to true,
          Language.SPANISH to true,
          Language.FRENCH to true,
          Language.GERMAN to true,
        ),
      onMessage = {},
      onSettings = {},
      drawable = Pair("Settings", R.drawable.settings),
    )
  }
}

@Preview(
  showBackground = true,
  uiMode = Configuration.UI_MODE_NIGHT_YES,
)
@Composable
fun LanguageSelectionRowDarkPreview() {
  TranslatorTheme {
    LanguageSelectionRow(
      from = Language.FRENCH,
      to = Language.GERMAN,
      availableLanguages =
        mapOf(
          Language.ENGLISH to true,
          Language.SPANISH to true,
          Language.FRENCH to true,
          Language.GERMAN to true,
        ),
      onMessage = {},
      onSettings = {},
      drawable = Pair("Settings", R.drawable.settings),
    )
  }
}
