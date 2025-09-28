package dev.davidv.translator

import android.graphics.Bitmap
import java.nio.ByteBuffer

class TesseractOCR(
  private val datapath: String,
  private val language: String = "eng",
) {
  private val binding = TesseractBinding()
  private var isInitialized = false

  fun initialize(): Boolean {
    if (isInitialized) return true

    isInitialized = binding.create(datapath, language)
    return isInitialized
  }

  fun setPageSegmentationMode(mode: PageSegMode): Boolean =
    if (isInitialized) {
      binding.setPageSegMode(mode)
      true
    } else {
      false
    }

  fun processImage(bitmap: Bitmap): List<DetectedWord>? {
    if (!isInitialized) return null

    val imageData = bitmapToByteArray(bitmap)
    val width = bitmap.width
    val height = bitmap.height
    val bytesPerPixel = 4 // RGBA
    val bytesPerLine = width * bytesPerPixel

    val success = binding.setFrame(imageData, width, height, bytesPerPixel, bytesPerLine)

    return if (success) {
      binding.getWordBoxes()
    } else {
      null
    }
  }

  fun close() {
    binding.destroy()
    isInitialized = false
  }

  private fun bitmapToByteArray(bitmap: Bitmap): ByteArray {
    val buffer = ByteBuffer.allocate(bitmap.byteCount)
    bitmap.copyPixelsToBuffer(buffer)
    return buffer.array()
  }

  protected fun finalize() {
    close()
  }
}
