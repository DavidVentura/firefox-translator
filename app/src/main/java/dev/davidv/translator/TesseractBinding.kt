package dev.davidv.translator

class TesseractBinding {
  companion object {
    init {
      System.loadLibrary("bindings")
    }
  }

  private var tesseractPtr: Long = 0

  fun create(
    datapath: String,
    language: String,
  ): Boolean {
    destroy()
    tesseractPtr = nativeCreate(datapath, language)
    return tesseractPtr != 0L
  }

  fun setFrame(
    frameData: ByteArray,
    width: Int,
    height: Int,
    bytesPerPixel: Int,
    bytesPerLine: Int,
  ): Boolean {
    return if (tesseractPtr != 0L) {
      nativeSetFrame(tesseractPtr, frameData, width, height, bytesPerPixel, bytesPerLine) != 0
    } else {
      false
    }
  }

  fun setPageSegMode(mode: PageSegMode) {
    if (tesseractPtr != 0L) {
      nativeSetPageSegMode(tesseractPtr, mode.value)
    }
  }

  fun getWordBoxes(): List<DetectedWord>? {
    return if (tesseractPtr != 0L) {
      nativeGetWordBoxes(tesseractPtr)
    } else {
      null
    }
  }

  fun destroy() {
    if (tesseractPtr != 0L) {
      nativeDestroy(tesseractPtr)
      tesseractPtr = 0
    }
  }

  fun isInitialized(): Boolean = tesseractPtr != 0L

  private external fun nativeCreate(
    datapath: String,
    language: String,
  ): Long

  private external fun nativeSetFrame(
    tesseractPtr: Long,
    frameData: ByteArray,
    width: Int,
    height: Int,
    bytesPerPixel: Int,
    bytesPerLine: Int,
  ): Int

  private external fun nativeSetPageSegMode(
    tesseractPtr: Long,
    mode: Int,
  )

  private external fun nativeGetWordBoxes(tesseractPtr: Long): List<DetectedWord>?

  private external fun nativeDestroy(tesseractPtr: Long)

  protected fun finalize() {
    destroy()
  }
}
