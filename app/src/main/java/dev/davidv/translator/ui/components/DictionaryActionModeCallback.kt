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

import android.content.Context
import android.content.Intent
import android.util.Log
import android.view.ActionMode
import android.view.Menu
import android.view.MenuItem
import android.widget.TextView

class DictionaryActionModeCallback(
  private val context: Context,
  private val onDictionaryLookup: (String) -> Unit,
) : ActionMode.Callback2() {
  private var currentTextView: TextView? = null

  fun setTextView(textView: TextView) {
    currentTextView = textView
  }

  override fun onCreateActionMode(
    mode: ActionMode?,
    menu: Menu?,
  ): Boolean {
    menu?.add(0, DICTIONARY_ID, 0, "Dictionary")
    return true
  }

  override fun onPrepareActionMode(
    mode: ActionMode?,
    menu: Menu?,
  ): Boolean {
    menu?.let { m ->
      val itemsToRemove = mutableListOf<Int>()
      for (i in 0 until m.size()) {
        val item = m.getItem(i)
        if (item.intent?.action == Intent.ACTION_TRANSLATE ||
          item.intent?.`package` == context.packageName ||
          item.title == "Translate"
        ) {
          itemsToRemove.add(item.itemId)
        }
      }
      itemsToRemove.forEach { m.removeItem(it) }
    }
    Log.d("DictionaryActionMode", "Show menu now")
    return false
  }

  override fun onActionItemClicked(
    mode: ActionMode?,
    item: MenuItem?,
  ): Boolean {
    Log.d("DictionaryActionMode", "Clicked '${item?.itemId}' == '${item?.title}'")
    return when (item?.itemId) {
      DICTIONARY_ID -> {
        val textView = currentTextView
        val selectedText =
          textView
            ?.text
            ?.subSequence(
              textView.selectionStart,
              textView.selectionEnd,
            )?.toString() ?: ""

        if (selectedText.isNotBlank()) {
          onDictionaryLookup(selectedText)
        }

        mode?.finish()
        true
      }

      else -> false
    }
  }

  override fun onDestroyActionMode(mode: ActionMode?) {}

  companion object {
    private const val DICTIONARY_ID = 12345
  }
}
