package dev.davidv.translator

class TarkkaBinding {
  companion object {
    init {
      // TODO
      System.loadLibrary("bindings")
    }
  }

  private var readerPtr: Long = 0

  fun open(path: String): Boolean {
    close() // Close any existing reader
    readerPtr = nativeOpen(path)
    return readerPtr != 0L
  }

  fun lookup(word: String): WordWithTaggedEntries? =
    if (readerPtr != 0L) {
      nativeLookup(readerPtr, word)
    } else {
      null
    }

  fun close() {
    if (readerPtr != 0L) {
      nativeClose(readerPtr)
      readerPtr = 0
    }
  }

  fun isOpen(): Boolean = readerPtr != 0L

  private external fun nativeOpen(path: String): Long

  private external fun nativeLookup(
    readerPtr: Long,
    word: String,
  ): WordWithTaggedEntries?

  private external fun nativeClose(readerPtr: Long)

  protected fun finalize() {
    close()
  }
}
