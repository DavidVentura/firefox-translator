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

import android.widget.TextView
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
import androidx.test.uiautomator.Until
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class DictionaryUsageInstrumentedTest {
  @get:Rule
  val composeTestRule = createAndroidComposeRule<MainActivity>()

  @Before
  fun setUp() {
    TestUtils.setupLanguagesForApp()
  }

  @Test
  fun testDictionaryUsageFromTranslationOutput() {
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

    val editText = device.findObject(By.desc("Primary input"))!!
    editText.click()
    editText.text = "Hola"
    println("Text input completed using UiAutomator")

    // wait for translation present
    composeTestRule.waitUntil(timeoutMillis = 3000) {
      try {
        val translationOutput = device.findObject(By.desc("Output textview"))
        translationOutput != null && !translationOutput.text.isNullOrEmpty()
      } catch (_: Exception) {
        false
      }
    }

    val textView: TextView =
      composeTestRule.runOnUiThread {
        val root = composeTestRule.activity.window.decorView.rootView
        root.findViewWithTag("output_textview_tag")
      }!!
    val translationOutput = device.findObject(By.desc("Output textview"))
    if (translationOutput == null || translationOutput.text.isNullOrEmpty()) {
      throw AssertionError("Translation output not found or empty")
    }
    println("Translation output found: ${translationOutput.text}")

    val layout = textView.layout
    val line = textView.layout.getLineForOffset(0)
    val x = layout.getPrimaryHorizontal(0)
    val y = (layout.getLineTop(line) + layout.getLineBottom(line)) / 2f
    val coordinates = intArrayOf(0, 0)
    textView.getLocationOnScreen(coordinates)
    val screenX = coordinates[0] + x.toInt()
    val screenY = coordinates[1] + y.toInt()

    // couldn't get textView.longClick() to work
    device.swipe(screenX, screenY, screenX, screenY, 50)
    device.waitForIdle()

    val dictionaryButton =
      device.wait(
        Until.findObject(By.text("Dictionary")),
        1000,
      )
    println("dict but $dictionaryButton")
    if (dictionaryButton == null) {
      throw AssertionError("Dictionary menu item not found after long press")
    }

    dictionaryButton.click()

    println("Dictionary menu item clicked")
    device.waitForIdle()
    composeTestRule.waitForIdle()

    val entry =
      composeTestRule
        .onNodeWithContentDescription("Dictionary Entry", useUnmergedTree = true)
        .assertIsDisplayed()
    println("Dictionary entry sheet found $entry")

    println("Dictionary entry contents:")
    TestUtils.logComposeChildren(entry)

    entry
      .onChildren()
      .filter(
        hasText("/həˈləʊ/"),
      ).assertCountEquals(1)
    entry
      .onChildren()
      .filter(
        hasText("A greeting used when answering the telephone"),
      ).assertCountEquals(1)
  }
}
