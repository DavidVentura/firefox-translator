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
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.rememberTransformableState
import androidx.compose.foundation.gestures.transformable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import dev.davidv.translator.R
import kotlin.math.max
import kotlin.math.roundToInt

@Composable
fun ZoomableImageViewer(
  bitmap: Bitmap,
  onDismiss: () -> Unit,
  onShare: () -> Unit,
) {
  Dialog(
    onDismissRequest = onDismiss,
    properties =
      DialogProperties(
        dismissOnBackPress = true,
        dismissOnClickOutside = false,
        usePlatformDefaultWidth = false,
      ),
  ) {
    var scale by remember { mutableStateOf(1f) }
    var offset by remember { mutableStateOf(Offset.Zero) }

    val configuration = LocalConfiguration.current
    val density = LocalDensity.current

    val screenWidth = with(density) { configuration.screenWidthDp.dp.toPx() }
    val screenHeight = with(density) { configuration.screenHeightDp.dp.toPx() }

    val state =
      rememberTransformableState { zoomChange, offsetChange, _ ->
        val newScale = (scale * zoomChange).coerceIn(1f, 3f)

        // offset is also scaled, otherwise dragging is weird
        val proposedOffset = offset + offsetChange * newScale

        val newOffset =
          if (newScale <= 1f) {
            Offset.Zero
          } else {
            // Calculate the image display size (how it's actually shown on screen)
            val imageAspectRatio = bitmap.width.toFloat() / bitmap.height.toFloat()
            val screenAspectRatio = screenWidth / screenHeight

            val (displayWidth, displayHeight) =
              if (imageAspectRatio > screenAspectRatio) {
                screenWidth to screenWidth / imageAspectRatio
              } else {
                screenHeight * imageAspectRatio to screenHeight
              }

            val scaledImageWidth = displayWidth * newScale
            val scaledImageHeight = displayHeight * newScale

            // Calculate how much of the image extends beyond the screen on each side
            val overflowX = max(0f, scaledImageWidth - screenWidth)
            val overflowY = max(0f, scaledImageHeight - screenHeight)

            // Maximum offset is half the overflow (can pan that much in each direction)
            val maxOffsetX = overflowX / 2f
            val maxOffsetY = overflowY / 2f

            Offset(
              x = proposedOffset.x.coerceIn(-maxOffsetX, maxOffsetX),
              y = proposedOffset.y.coerceIn(-maxOffsetY, maxOffsetY),
            )
          }

        scale = newScale
        offset = newOffset
      }

    Box(
      modifier =
        Modifier
          .fillMaxSize()
          .background(Color.Black),
    ) {
      Image(
        bitmap = bitmap.asImageBitmap(),
        contentDescription = "Zoomable translated image",
        modifier =
          Modifier
            .fillMaxSize()
            .clip(RectangleShape)
            .offset { IntOffset(offset.x.roundToInt(), offset.y.roundToInt()) }
            .graphicsLayer(
              scaleX = scale,
              scaleY = scale,
            ).transformable(state = state),
      )

      // Share button
      IconButton(
        onClick = onShare,
        modifier =
          Modifier
            .align(Alignment.TopStart)
            .size(48.dp),
      ) {
        Icon(
          painterResource(id = R.drawable.share),
          contentDescription = "Share image",
          tint = Color.White,
        )
      }

      // Close button
      IconButton(
        onClick = onDismiss,
        modifier =
          Modifier
            .align(Alignment.TopEnd)
            .size(48.dp),
      ) {
        Icon(
          painterResource(id = R.drawable.cancel),
          contentDescription = "Close full screen view",
          tint = Color.White,
        )
      }
    }
  }
}
