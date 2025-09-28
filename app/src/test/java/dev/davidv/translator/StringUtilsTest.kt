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

import dev.davidv.translator.ui.screens.toggleFirstLetterCase
import org.junit.Assert.assertEquals
import org.junit.Test

class StringUtilsTest {
  @Test
  fun `toggleFirstLetterCase with uppercase ASCII`() {
    val result = toggleFirstLetterCase("Apple")
    assertEquals("apple", result)
  }

  @Test
  fun `toggleFirstLetterCase with lowercase ASCII`() {
    val result = toggleFirstLetterCase("apple")
    assertEquals("Apple", result)
  }

  @Test
  fun `toggleFirstLetterCase with uppercase Unicode`() {
    val result = toggleFirstLetterCase("Ápple")
    assertEquals("ápple", result)
  }

  @Test
  fun `toggleFirstLetterCase with lowercase Unicode`() {
    val result = toggleFirstLetterCase("ápple")
    assertEquals("Ápple", result)
  }

  @Test
  fun `toggleFirstLetterCase with empty string`() {
    val result = toggleFirstLetterCase("")
    assertEquals("", result)
  }

  @Test
  fun `toggleFirstLetterCase with single uppercase character`() {
    val result = toggleFirstLetterCase("A")
    assertEquals("a", result)
  }

  @Test
  fun `toggleFirstLetterCase with single lowercase character`() {
    val result = toggleFirstLetterCase("a")
    assertEquals("A", result)
  }

  @Test
  fun `toggleFirstLetterCase with single uppercase Unicode character`() {
    val result = toggleFirstLetterCase("Á")
    assertEquals("á", result)
  }

  @Test
  fun `toggleFirstLetterCase with single lowercase Unicode character`() {
    val result = toggleFirstLetterCase("á")
    assertEquals("Á", result)
  }

  @Test
  fun `toggleFirstLetterCase preserves rest of word`() {
    val result = toggleFirstLetterCase("iPhone")
    assertEquals("IPhone", result)
  }

  @Test
  fun `toggleFirstLetterCase with non-letter first character`() {
    val result = toggleFirstLetterCase("123word")
    assertEquals("123word", result)
  }
}
