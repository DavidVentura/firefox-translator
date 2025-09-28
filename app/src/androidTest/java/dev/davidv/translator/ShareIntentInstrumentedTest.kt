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

import android.content.Intent
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ShareIntentInstrumentedTest {
  @get:Rule
  val composeTestRule = createAndroidComposeRule<MainActivity>()

  @Before
  fun setUp() {
    TestUtils.setupLanguagesForApp()
  }

  @Test
  fun testShareIntentTranslation() {
    // Create share intent with "Hello World" explicitly targeting MainActivity
    val shareIntent =
      Intent(ApplicationProvider.getApplicationContext(), MainActivity::class.java).apply {
        action = Intent.ACTION_SEND
        type = "text/plain"
        putExtra(Intent.EXTRA_TEXT, "Hello World")
      }

    ActivityScenario.launch<MainActivity>(shareIntent).use { scenario ->
      composeTestRule.waitForIdle()

      composeTestRule.waitUntil(timeoutMillis = 10000) {
        try {
          composeTestRule.onAllNodes(hasContentDescription("Main screen")).fetchSemanticsNodes().isNotEmpty()
        } catch (_: Exception) {
          false
        }
      }
      println("Main screen loaded")

      composeTestRule.waitUntil(timeoutMillis = 10000) {
        try {
          composeTestRule.onAllNodes(hasContentDescription("Translation output")).fetchSemanticsNodes().isNotEmpty()
        } catch (_: Exception) {
          false
        }
      }

      val translationOutput =
        composeTestRule
          .onNodeWithContentDescription("Translation output", useUnmergedTree = true)

      translationOutput.assertIsDisplayed()
      translationOutput.assert(hasText("Hola Mundo"))
    }
  }
}
