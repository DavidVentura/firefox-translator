package dev.davidv.translator

data class AggregatedWord(
  val word: String,
  val posGlosses: List<PosGlosses>,
  val hyphenation: List<String>?,
  val formOf: List<String>?,
  val ipaSound: List<String>?,
)

data class PosGlosses(
  val pos: String,
  val glosses: List<Gloss>,
)

data class Gloss(
  val glossLines: List<String>,
)

data class WordWithTaggedEntries(
  val word: String,
  val tag: Int,
  val entries: List<WordEntryComplete>,
  val sounds: String?,
  val hyphenations: List<String>,
  val redirects: List<String>,
) {
  enum class WordTag(
    val value: Int,
  ) {
    MONOLINGUAL(1),
    ENGLISH(2),
    BOTH(3),
    ;

    companion object {
      fun fromValue(value: Int): WordTag? = entries.find { it.value == value }
    }
  }

  val wordTag: WordTag
    get() = WordTag.fromValue(tag)!!
}

data class WordEntryComplete(
  val senses: List<Sense>,
)

data class Sense(
  val pos: String,
  val glosses: List<Gloss>,
)
