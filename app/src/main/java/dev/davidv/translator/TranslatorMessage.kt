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

import android.net.Uri

sealed class TranslatorMessage {
    data class TextInput(val text: String) : TranslatorMessage()
    data class FromLang(val language: Language) : TranslatorMessage()
    data class ToLang(val language: Language) : TranslatorMessage()
    data class SetImageUri(val uri: Uri) : TranslatorMessage()
    object SwapLanguages : TranslatorMessage()
    object ClearImage : TranslatorMessage()
    data class InitializeLanguages(val from: Language, val to: Language) : TranslatorMessage()
    data class ImageTextDetected(val extractedText: String) : TranslatorMessage()
}