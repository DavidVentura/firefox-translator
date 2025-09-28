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

import org.junit.Assert.assertEquals
import org.junit.Test

class OCRServiceTest {
  @Test
  fun `mergeHyphenatedWords with Dutch text`() {
    val words =
      listOf(
        WordInfo("Baruch", 90f, Rect(0, 0, 50, 20), null, true, false, false),
        WordInfo("of", 95f, Rect(55, 0, 70, 20), null, false, false, false),
        WordInfo("Bento", 92f, Rect(75, 0, 120, 20), null, false, true, false),
        WordInfo("de", 88f, Rect(0, 25, 20, 45), null, true, false, false),
        WordInfo("Spinoza", 93f, Rect(25, 25, 85, 45), null, false, false, false),
        WordInfo("sprak", 91f, Rect(90, 25, 130, 45), null, false, true, false),
        WordInfo("zich", 89f, Rect(0, 50, 35, 70), null, true, false, false),
        WordInfo("uit", 94f, Rect(40, 50, 60, 70), null, false, false, false),
        WordInfo("voor", 96f, Rect(65, 50, 95, 70), null, false, false, false),
        WordInfo("ge-", 87f, Rect(100, 50, 120, 70), null, false, true, false),
        WordInfo("loofsvrijheid", 85f, Rect(0, 75, 110, 95), null, true, false, false),
        WordInfo("en", 98f, Rect(115, 75, 130, 95), null, false, true, false),
        WordInfo("voor", 92f, Rect(0, 100, 35, 120), null, true, false, false),
        WordInfo("de", 90f, Rect(40, 100, 55, 120), null, false, false, false),
        WordInfo("vrijheid", 88f, Rect(60, 100, 120, 120), null, false, false, false),
        WordInfo("om", 95f, Rect(125, 100, 145, 120), null, false, true, false),
        WordInfo("je", 93f, Rect(0, 125, 15, 145), null, true, false, false),
        WordInfo("mening", 91f, Rect(20, 125, 70, 145), null, false, false, false),
        WordInfo("ook", 89f, Rect(75, 125, 100, 145), null, false, false, false),
        WordInfo("op", 94f, Rect(105, 125, 120, 145), null, false, true, false),
        WordInfo("dat", 92f, Rect(0, 150, 25, 170), null, true, false, false),
        WordInfo("gebied", 87f, Rect(30, 150, 75, 170), null, false, false, false),
        WordInfo("onbe-", 86f, Rect(80, 150, 115, 170), null, false, true, false),
        WordInfo("lemmerd", 84f, Rect(0, 175, 60, 195), null, true, false, false),
        WordInfo("te", 97f, Rect(65, 175, 80, 195), null, false, false, false),
        WordInfo("uiten.", 90f, Rect(85, 175, 125, 195), null, false, true, true),
      )

    val result = mergeHyphenatedWords(words)

    val expectedTexts =
      listOf(
        "Baruch",
        "of",
        "Bento",
        "de",
        "Spinoza",
        "sprak",
        "zich",
        "uit",
        "voor",
        // vvv geloofsvrijheid is merged
        "geloofsvrijheid",
        "en",
        "voor",
        "de",
        "vrijheid",
        "om",
        "je",
        "mening",
        "ook",
        "op",
        "dat",
        "gebied",
        // vvv onbelemmerd is merged
        "onbelemmerd",
        "te",
        "uiten.",
      )

    assertEquals(expectedTexts.size, result.size)
    assertEquals(expectedTexts, result.map { it.text })

    val geloofsvrijheidWord = result.find { it.text == "geloofsvrijheid" }!!
    assertEquals(false, geloofsvrijheidWord.isFirstInLine)
    assertEquals(true, geloofsvrijheidWord.isLastInLine)
    assertEquals(false, geloofsvrijheidWord.isLastInPara)
    assertEquals(85f, geloofsvrijheidWord.confidence, 0.01f)

    val onbelemmerdWord = result.find { it.text == "onbelemmerd" }!!
    assertEquals(false, onbelemmerdWord.isFirstInLine)
    assertEquals(true, onbelemmerdWord.isLastInLine)
    assertEquals(false, onbelemmerdWord.isLastInPara)
    assertEquals(84f, onbelemmerdWord.confidence, 0.01f)
  }

  @Test
  fun `mergeHyphenatedWords does not merge non-hyphenated words`() {
    val words =
      listOf(
        WordInfo("word", 90f, Rect(0, 0, 50, 20), null, true, true, false),
        WordInfo("next", 85f, Rect(0, 25, 40, 45), null, true, true, false),
      )

    val result = mergeHyphenatedWords(words)

    println(result)
    assertEquals(2, result.size)
    assertEquals("word", result[0].text)
    assertEquals("next", result[1].text)
  }

  @Test
  fun `mergeHyphenatedWords handles empty list`() {
    val result = mergeHyphenatedWords(emptyList())
    assertEquals(0, result.size)
  }
}
