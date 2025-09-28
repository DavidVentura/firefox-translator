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

import android.content.Context
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.onChildAt
import androidx.compose.ui.test.onChildren
import java.io.File

object TestUtils {
  fun copyFile(
    testContext: Context,
    appPath: File,
    filename: String,
  ) {
    val stream = testContext.assets.open(filename)
    val out = File(appPath, filename)
    out.outputStream().use { output ->
      stream.copyTo(output)
    }
    stream.close()
    println("Copied $filename to ${appPath.name}")
  }

  fun copyTessData(
    context: Context,
    tessDataPath: File,
    language: Language,
  ) {
    copyFile(context, tessDataPath, language.tessFilename)
  }

  fun copyLangData(
    context: Context,
    dataPath: File,
    language: Language,
  ) {
    val enLangFiles = fromEnglishFiles[language]
    enLangFiles?.allFiles()?.forEach {
      copyFile(context, dataPath, it)
    }

    val langEnFiles = toEnglishFiles[language]
    langEnFiles?.allFiles()?.forEach {
      copyFile(context, dataPath, it)
    }
  }

  fun setupLanguagesForApp() {
    val testContext =
      androidx.test.platform.app.InstrumentationRegistry
        .getInstrumentation()
        .context
    val appContext =
      androidx.test.platform.app.InstrumentationRegistry
        .getInstrumentation()
        .targetContext

    val settingsManager = SettingsManager(appContext)
    val filePathManager = FilePathManager(appContext, settingsManager.settings)
    val dataPath = filePathManager.getDataDir()
    dataPath.mkdirs()

    val tessDataPath = filePathManager.getTesseractDataDir()
    tessDataPath.mkdirs()

    copyLangData(testContext, dataPath, Language.SPANISH)
    copyTessData(testContext, tessDataPath, Language.ENGLISH)
    copyTessData(testContext, tessDataPath, Language.SPANISH)

    val dictionariesPath = filePathManager.getDictionariesDir()
    dictionariesPath.mkdirs()
    copyFile(testContext, dictionariesPath, "es.dict")
    copyFile(testContext, dictionariesPath, "en.dict")
    copyFile(testContext, dictionariesPath, "index.json")
  }

  fun logUiChildren(
    node: Any?,
    depth: Int = 0,
  ) {
    if (node == null) return
    val indent = "  ".repeat(depth)
    when (node) {
      is androidx.test.uiautomator.UiObject2 -> {
        println("${indent}UiObject2: text='${node.text}' desc='${node.contentDescription}' class='${node.className}'")
        try {
          node.children.forEach { child ->
            logUiChildren(child, depth + 1)
          }
        } catch (e: Exception) {
          println("${indent}Error getting children: ${e.message}")
        }
      }
    }
  }

  fun logComposeChildren(
    nodeInteraction: androidx.compose.ui.test.SemanticsNodeInteraction,
    depth: Int = 0,
  ) {
    try {
      val node = nodeInteraction.fetchSemanticsNode()
      val text = node.config.getOrNull(androidx.compose.ui.semantics.SemanticsProperties.Text)
      val contentDesc = node.config.getOrNull(androidx.compose.ui.semantics.SemanticsProperties.ContentDescription)
      val indent = "  ".repeat(depth)

      if (text?.isNotEmpty() == true || contentDesc?.isNotEmpty() == true) {
        println(
          "${indent}Node: text='${text?.joinToString("") { it.text } ?: ""}' desc='${contentDesc?.joinToString("") ?: ""}' id=${node.id}",
        )
      }

      // Recursively log children
      nodeInteraction.onChildren().fetchSemanticsNodes().forEachIndexed { index, _ ->
        try {
          logComposeChildren(nodeInteraction.onChildAt(index), depth + 1)
        } catch (e: Exception) {
          println("${indent}Error accessing child $index: ${e.message}")
        }
      }
    } catch (e: Exception) {
      println("Error logging node at depth $depth: ${e.message}")
    }
  }
}
