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

import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.filter
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onChildren
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.By
import androidx.test.uiautomator.UiDevice
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class LanguageDetectionInstrumentedTest {
  @get:Rule
  val composeTestRule = createAndroidComposeRule<MainActivity>()

  @Before
  fun setUp() {
    TestUtils.setupLanguagesForApp()
  }

  @Test
  fun testSpanishLanguageDetectionToast() {
    val device = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation())

    composeTestRule.waitForIdle()
    composeTestRule.waitUntil(timeoutMillis = 1000) {
      try {
        composeTestRule.onAllNodes(hasContentDescription("Main screen")).fetchSemanticsNodes().isNotEmpty()
      } catch (_: Exception) {
        false
      }
    }
    println("Main screen loaded")
    composeTestRule.waitForIdle()

    val editText = device.findObject(By.desc("Primary input"))
    if (editText != null) {
      editText.click()
      editText.text = "Ciao, come vai?"
      println("Text input completed using UiAutomator")
    } else {
      throw AssertionError("Could not find EditText to input text")
    }

    composeTestRule.waitUntil(timeoutMillis = 1500) {
      try {
        composeTestRule.onAllNodes(hasContentDescription("Detected language toast")).fetchSemanticsNodes().isNotEmpty()
      } catch (_: Exception) {
        false
      }
    }

    val toastNode =
      composeTestRule
        .onNodeWithContentDescription("Detected language toast", useUnmergedTree = true)
        .assertIsDisplayed()

    // Check for text recursively within the toast component
    toastNode
      .onChildren()
      .filter(
        hasText("Missing language") or hasText("Italian"),
      ).assertCountEquals(2)
  }
}
