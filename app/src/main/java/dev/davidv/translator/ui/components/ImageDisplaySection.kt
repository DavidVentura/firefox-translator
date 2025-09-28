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

import android.graphics.Bitmap
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import kotlinx.coroutines.flow.StateFlow

@Composable
fun ImageDisplaySection(
  displayImage: Bitmap,
  isOcrInProgress: StateFlow<Boolean>,
  isTranslating: StateFlow<Boolean>,
  onShowFullScreenImage: () -> Unit,
) {
  val isOcrInProgressState by isOcrInProgress.collectAsState()
  val isTranslatingState by isTranslating.collectAsState()
  val isProcessing = isOcrInProgressState || isTranslatingState

  Box {
    Image(
      bitmap = displayImage.asImageBitmap(),
      contentDescription = "Image to translate",
      modifier =
        Modifier
          .fillMaxWidth()
          .clickable { onShowFullScreenImage() },
    )

    if (isProcessing) {
      LinearProgressIndicator(
        modifier =
          Modifier
            .fillMaxWidth()
            .align(Alignment.TopCenter),
      )
    }
  }
}
