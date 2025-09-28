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

import android.widget.TextView
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.text
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.viewinterop.AndroidView

@Composable
fun InteractiveText(
  text: String,
  modifier: Modifier = Modifier,
  style: TextStyle = MaterialTheme.typography.bodyMedium,
  onDictionaryLookup: (String) -> Unit,
) {
  val context = LocalContext.current
  val actionModeCallback =
    remember(onDictionaryLookup) {
      DictionaryActionModeCallback(context, onDictionaryLookup)
    }

  val textColor = MaterialTheme.colorScheme.onSurface.toArgb()
  val fontSize = style.fontSize.value

  AndroidView(
    factory = { context ->
      TextView(context).apply {
        this.text = text
        this.textSize = fontSize
        this.setTextColor(textColor)
        this.setTextIsSelectable(true)
        this.customSelectionActionModeCallback = actionModeCallback
        this.customInsertionActionModeCallback = actionModeCallback
        actionModeCallback.setTextView(this)
      }
    },
    update = { textView ->
      textView.text = text
      textView.textSize = fontSize
      textView.setTextColor(textColor)
      textView.setTextIsSelectable(true)
      textView.customSelectionActionModeCallback = actionModeCallback
      textView.customInsertionActionModeCallback = actionModeCallback
      actionModeCallback.setTextView(textView)
    },
    modifier =
      modifier.semantics {
        this.text = AnnotatedString(text)
      },
  )
}
