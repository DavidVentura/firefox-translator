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
import androidx.core.content.FileProvider
import androidx.lifecycle.Lifecycle
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File

@RunWith(AndroidJUnit4::class)
class ImageShareIntentInstrumentedTest {
  @get:Rule
  val composeTestRule = createAndroidComposeRule<MainActivity>()

  @Before
  fun setUp() {
    TestUtils.setupLanguagesForApp()
  }

  @Test
  fun testImageShareIntentOcrTranslation() {
    val testContext = InstrumentationRegistry.getInstrumentation().context
    val appContext = InstrumentationRegistry.getInstrumentation().targetContext

    val inputStream = testContext.assets.open("example.png")
    val imageFile = File(appContext.cacheDir, "example.png")
    imageFile.outputStream().use { output ->
      inputStream.copyTo(output)
    }
    inputStream.close()

    val imageUri =
      FileProvider.getUriForFile(
        appContext,
        "${appContext.packageName}.fileprovider",
        imageFile,
      )

    val shareIntent =
      Intent(ApplicationProvider.getApplicationContext(), MainActivity::class.java).apply {
        action = Intent.ACTION_SEND
        type = "image/*"
        putExtra(Intent.EXTRA_STREAM, imageUri)
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
      }

    composeTestRule.activityRule.scenario.onActivity { activity ->
      assert(activity.lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED)) { "it resumed" }
      activity.onNewIntent(shareIntent)
    }

    println("it resumed")
    composeTestRule.waitForIdle()

    // Wait for the main screen to appear
    composeTestRule.waitUntil(timeoutMillis = 1000) {
      try {
        composeTestRule.onAllNodes(hasContentDescription("Main screen")).fetchSemanticsNodes().isNotEmpty()
      } catch (_: Exception) {
        false
      }
    }
    println("Main screen loaded")

    // Wait for OCR and translation processing using proper polling instead of sleep
    composeTestRule.waitUntil(timeoutMillis = 3000) {
      try {
        val nodes = composeTestRule.onAllNodes(hasContentDescription("Translation output")).fetchSemanticsNodes()
        val hasOutput = nodes.isNotEmpty()
        if (hasOutput) {
          println("Translation output found after processing")
        }
        hasOutput
      } catch (_: Exception) {
        false
      }
    }

    val translationOutput =
      composeTestRule
        .onNodeWithContentDescription("Translation output", useUnmergedTree = true)
        .assertIsDisplayed()

    // The example.png should contain some text that gets translated
    // We'll verify that some translation output exists (exact text may vary based on OCR)
    translationOutput.assert(hasText("For Android Studio 1.5", substring = true))
    translationOutput.assert(hasText("If you choose to use images other than icons in SVG or PNG be aware", substring = true))

    println("Translation output found - OCR and translation working")
  }
}
