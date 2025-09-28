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

import android.icu.text.Transliterator
import android.os.Build
import android.util.Log
import androidx.annotation.RequiresApi

object TransliterationService {
  private val transliterators = mutableMapOf<String, Transliterator?>()

  private val scriptComponents =
    mapOf(
      "Arabic" to listOf("Arabic"),
      "Cyrillic" to listOf("Cyrillic"),
      "Greek" to listOf("Greek"),
      "Han" to listOf("Han"),
      "Japanese" to listOf("Hiragana", "Katakana", "Han"),
      "Hangul" to listOf("Hangul"),
      "Devanagari" to listOf("Devanagari"),
      "Hebrew" to listOf("Hebrew"),
      "Bengali" to listOf("Bengali"),
      "Gujarati" to listOf("Gujarati"),
      "Kannada" to listOf("Kannada"),
      "Malayalam" to listOf("Malayalam"),
      "Tamil" to listOf("Tamil"),
      "Telugu" to listOf("Telugu"),
    )

  private fun shouldTransliterate(
    language: Language,
    targetScript: String = "Latin",
  ): Boolean = language.script != targetScript && scriptComponents.containsKey(language.script)

  private fun getTransliterationRule(
    fromScript: String,
    toScript: String = "Latin",
  ): String? {
    val components = scriptComponents[fromScript] ?: return null
    return components.joinToString("; ") { "$it-$toScript" }
  }

  fun transliterate(
    text: String,
    language: Language,
    targetScript: String = "Latin",
  ): String? {
    if (!shouldTransliterate(language, targetScript) || Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
      return null
    }

    val rule = getTransliterationRule(language.script, targetScript) ?: return null
    Log.d("Transliteration", "Using rule $rule")
    val transliterator = getTransliterator(rule) ?: return null
    return try {
      if (language == Language.JAPANESE) {
        transliterateJapanese(text, transliterator)
      } else {
        transliterator.transliterate(text)
      }
    } catch (e: Exception) {
      Log.w("TransliterationService", "Failed to transliterate text for $language", e)
      null
    }
  }

  @RequiresApi(Build.VERSION_CODES.Q)
  private fun transliterateJapanese(
    text: String,
    transliterator: Transliterator,
  ): String {
    val result = StringBuilder()
    var i = 0

    while (i < text.length) {
      val char = text[i]

      if (isKanji(char)) {
        result.append(char)
        i++
      } else {
        val segmentStart = i
        while (i < text.length && !isKanji(text[i])) {
          i++
        }
        val segment = text.substring(segmentStart, i)
        val transliterated = transliterator.transliterate(segment)
        result.append(transliterated)
      }
    }

    return result.toString()
  }

  private fun isKanji(char: Char): Boolean {
    val codePoint = char.code
    return (codePoint in 0x4E00..0x9FAF) ||
      (codePoint in 0x3400..0x4DBF) ||
      (codePoint in 0x20000..0x2A6DF) ||
      (codePoint in 0x2A700..0x2B73F) ||
      (codePoint in 0x2B740..0x2B81F) ||
      (codePoint in 0x2B820..0x2CEAF) ||
      (codePoint in 0xF900..0xFAFF) ||
      (codePoint in 0x2F800..0x2FA1F)
  }

  @RequiresApi(Build.VERSION_CODES.Q)
  private fun getTransliterator(rule: String): Transliterator? =
    transliterators.getOrPut(rule) {
      try {
        Transliterator.getInstance(rule)
      } catch (e: Exception) {
        Log.w("TransliterationService", "Failed to create transliterator for rule: $rule", e)
        null
      }
    }
}
