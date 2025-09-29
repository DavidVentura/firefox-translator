package dev.davidv.translator

import android.os.Parcel
import android.os.Parcelable

data class TranslationError(
  val type: ErrorType,
  val language: String?,
  val message: String?,
) : Parcelable {
  enum class ErrorType {
    COULD_NOT_DETECT_LANGUAGE,
    DETECTED_BUT_UNAVAILABLE,
    UNEXPECTED,
  }

  constructor(parcel: Parcel) : this(
    ErrorType.valueOf(parcel.readString()!!),
    parcel.readString(),
    parcel.readString(),
  )

  override fun writeToParcel(
    parcel: Parcel,
    flags: Int,
  ) {
    parcel.writeString(type.name)
    parcel.writeString(language)
    parcel.writeString(message)
  }

  override fun describeContents(): Int = 0

  companion object CREATOR : Parcelable.Creator<TranslationError> {
    override fun createFromParcel(parcel: Parcel): TranslationError = TranslationError(parcel)

    override fun newArray(size: Int): Array<TranslationError?> = arrayOfNulls(size)

    fun couldNotDetectLanguage(): TranslationError = TranslationError(ErrorType.COULD_NOT_DETECT_LANGUAGE, null, null)

    fun detectedButUnavailable(language: String): TranslationError = TranslationError(ErrorType.DETECTED_BUT_UNAVAILABLE, language, null)

    fun unexpected(message: String): TranslationError = TranslationError(ErrorType.UNEXPECTED, null, message)
  }
}
