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

import android.graphics.BitmapFactory
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File

/**
 * Instrumented test, which will execute on an Android device.
 *
 * See [testing documentation](http://d.android.com/tools/testing).
 */
@RunWith(AndroidJUnit4::class)
class ExampleInstrumentedTest {
  @Test
  fun useAppContext() {
    // Context of the app under test.
    val appContext = InstrumentationRegistry.getInstrumentation().targetContext
    assertEquals("dev.davidv.translator", appContext.packageName)
  }

  @Test
  fun extractSentences() {
    val context = InstrumentationRegistry.getInstrumentation().context
    val appContext = InstrumentationRegistry.getInstrumentation().targetContext

    // Load PNG from assets
    val inputStream = context.assets.open("screen.png")
    val bitmap = BitmapFactory.decodeStream(inputStream)
    inputStream.close()

    val tessDir = File(appContext.dataDir, "tesseract")
    val tessdataDir = File(tessDir, "tessdata")
    tessdataDir.mkdirs()

    TestUtils.copyFile(context, tessdataDir, "eng.traineddata")

    val tessInstance = TesseractOCR(tessdataDir.absolutePath, "eng")
    assert(tessInstance.initialize())
    val blocks = getSentences(bitmap, tessInstance)
    blocks.forEachIndexed { i, b ->
      println("$i $b")
    }
    assertEquals(11, blocks.count())
    assertEquals(3, blocks[6].lines.count())
    assertEquals("Philipsen sprint naar eerste gele trui in", blocks[6].lines[0].text)
    assertEquals("Tour, Evenepoel mist slag in hectische", blocks[6].lines[1].text)
    assertEquals("finale", blocks[6].lines[2].text)
  }
}
