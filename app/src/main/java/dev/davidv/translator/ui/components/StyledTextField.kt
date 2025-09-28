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

import android.text.Editable
import android.text.InputType
import android.text.TextWatcher
import android.view.Gravity
import android.view.ViewGroup
import android.widget.EditText
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import dev.davidv.translator.ui.theme.TranslatorTheme

@Composable
fun StyledTextField(
  text: String,
  onValueChange: (String) -> Unit,
  onDictionaryLookup: (String) -> Unit,
  modifier: Modifier = Modifier,
  placeholder: String? = null,
  readOnly: Boolean = false,
  textStyle: TextStyle = MaterialTheme.typography.bodyLarge,
) {
  val context = LocalContext.current
  val lifecycleOwner = LocalLifecycleOwner.current
  val actionModeCallback =
    remember(onDictionaryLookup) {
      DictionaryActionModeCallback(context, onDictionaryLookup)
    }
  val textColor = MaterialTheme.colorScheme.onSurface.toArgb()
  val hintColor = MaterialTheme.colorScheme.onSurfaceVariant.toArgb()
  val fontSize = textStyle.fontSize.value

  Box(modifier = modifier.fillMaxSize()) {
    AndroidView(
      factory = { context ->
        EditText(context).apply {
          this.contentDescription = "Primary input"
          this.layoutParams =
            ViewGroup.LayoutParams(
              ViewGroup.LayoutParams.MATCH_PARENT,
              ViewGroup.LayoutParams.MATCH_PARENT,
            )
          this.gravity = Gravity.START
          this.setText(text)
          this.textSize = fontSize
          this.setTextColor(textColor)
          this.setHintTextColor(hintColor)
          this.hint = placeholder
          this.isEnabled = !readOnly
          this.isFocusable = !readOnly
          this.isFocusableInTouchMode = !readOnly
          this.setTextIsSelectable(true)
          this.customSelectionActionModeCallback = actionModeCallback
          this.customInsertionActionModeCallback = actionModeCallback
          this.inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_MULTI_LINE
          this.background = null
          actionModeCallback.setTextView(this)
          this.addTextChangedListener(
            object : TextWatcher {
              override fun beforeTextChanged(
                s: CharSequence?,
                start: Int,
                count: Int,
                after: Int,
              ) {}

              override fun onTextChanged(
                s: CharSequence?,
                start: Int,
                before: Int,
                count: Int,
              ) {}

              override fun afterTextChanged(s: Editable?) {
                val newText = s?.toString() ?: ""
                // TODO text is always ''
                if (newText != text || newText == "") {
                  onValueChange(newText)
                }
              }
            },
          )
        }
      },
      update = { editText ->
        if (editText.text.toString() != text) {
          editText.setText(text)
        }
        editText.textSize = fontSize
        editText.setTextColor(textColor)
        editText.setHintTextColor(hintColor)
        editText.hint = placeholder
        editText.isEnabled = !readOnly
        editText.isFocusable = !readOnly
        editText.isFocusableInTouchMode = !readOnly
        editText.customSelectionActionModeCallback = actionModeCallback
        actionModeCallback.setTextView(editText)
      },
    )

    DisposableEffect(lifecycleOwner) {
      val observer =
        LifecycleEventObserver { _, event ->
          when (event) {
            Lifecycle.Event.ON_PAUSE -> {
              val currentFocus = (context as? android.app.Activity)?.currentFocus
              currentFocus?.clearFocus()
            }
            else -> {}
          }
        }
      lifecycleOwner.lifecycle.addObserver(observer)
      onDispose {
        lifecycleOwner.lifecycle.removeObserver(observer)
      }
    }
  }
}

@Preview(showBackground = true)
@Composable
fun StyledTextFieldWeightedPreview() {
  TranslatorTheme {
    Box(modifier = Modifier.fillMaxSize()) {
      StyledTextField(
        text = "very long text. very long text. very long text. very long text. very long text. very long text. very long text. very long text. very long text. very long text. very long text. very long text. very long text. very long text. very long text. very long text. very long text. very long text. very long text. very long text. very long text. very long text. very long text. very long text. very long text. very long text. very long text. very long text. very long text. very long text. very long text. very long text. very long text. very long text. very long text. very long text. very long text. very long text. very long text. very long text. very long text. very long text. very long text. very long text. very long text. very long text. very long text. very long text. very long text. very long text. very long text. very long text. very long text. very long text. very long text. very long text. very long text. very long text. very long text. very long text. very long text. very long text. very long text. very long text. very long text. very long text.",
        onValueChange = {},
        onDictionaryLookup = {},
        placeholder = "Enter text",
        modifier = Modifier.fillMaxSize(),
      )
    }
  }
}

@Preview(showBackground = true)
@Composable
fun ShortText() {
  TranslatorTheme {
    Box(modifier = Modifier.fillMaxSize()) {
      StyledTextField(
        text = "very short text.",
        onValueChange = {},
        onDictionaryLookup = {},
        placeholder = "Enter text",
        modifier = Modifier.fillMaxSize(),
      )
    }
  }
}
