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

data class AppSettings(
  val defaultTargetLanguage: Language = Language.ENGLISH,
  val defaultSourceLanguage: Language? = null,
  val translationModelsBaseUrl: String = Constants.DEFAULT_TRANSLATION_MODELS_BASE_URL,
  val tesseractModelsBaseUrl: String = Constants.DEFAULT_TESSERACT_MODELS_BASE_URL,
  val dictionaryBaseUrl: String = Constants.DEFAULT_DICTIONARY_BASE_URL,
  val backgroundMode: BackgroundMode = BackgroundMode.AUTO_DETECT,
  val minConfidence: Int = 75,
  val maxImageSize: Int = 1500,
  val disableOcr: Boolean = false,
  val disableCLD: Boolean = false,
  val disableTransliteration: Boolean = false,
  val useExternalStorage: Boolean = false,
  val fontFactor: Float = 1.0f,
  val showOCRDetection: Boolean = false,
)
