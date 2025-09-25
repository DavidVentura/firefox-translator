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

package dev.davidv.translator.ui.screens

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import dev.davidv.translator.AppSettings
import dev.davidv.translator.BackgroundMode
import dev.davidv.translator.Language
import dev.davidv.translator.PermissionHelper
import dev.davidv.translator.R
import dev.davidv.translator.ui.theme.TranslatorTheme
import kotlinx.coroutines.delay

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun LanguageDropdown(
  label: String,
  selectedLanguage: Language?,
  availableLanguages: List<Language>,
  fallbackLanguage: Language?,
  onLanguageSelected: (Language) -> Unit,
) {
  var expanded by remember { mutableStateOf(false) }

  Text(
    text = label,
    style = MaterialTheme.typography.bodyMedium,
    color = MaterialTheme.colorScheme.onSurface,
  )

  ExposedDropdownMenuBox(
    expanded = expanded,
    onExpandedChange = { expanded = it },
    modifier = Modifier.fillMaxWidth(),
  ) {
    OutlinedTextField(
      value = selectedLanguage?.displayName ?: fallbackLanguage?.displayName ?: "No languages available",
      onValueChange = {},
      readOnly = true,
      trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
      modifier =
        Modifier
          .menuAnchor()
          .fillMaxWidth(),
      colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors(),
    )
    ExposedDropdownMenu(
      expanded = expanded,
      onDismissRequest = { expanded = false },
    ) {
      availableLanguages.forEach { language ->
        DropdownMenuItem(
          text = { Text(language.displayName) },
          onClick = {
            onLanguageSelected(language)
            expanded = false
          },
        )
      }
    }
  }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
  settings: AppSettings,
  availableLanguages: List<Language>,
  onSettingsChange: (AppSettings) -> Unit,
  onManageLanguages: () -> Unit,
) {
  val context = LocalContext.current
  var showPermissionDialog by remember { mutableStateOf(false) }

  val permissionLauncher =
    rememberLauncherForActivityResult(
      contract = ActivityResultContracts.RequestMultiplePermissions(),
    ) { permissions ->
      val allGranted = permissions.values.all { it }
      if (allGranted) {
        onSettingsChange(settings.copy(useExternalStorage = true))
      } else {
        onSettingsChange(settings.copy(useExternalStorage = false))
      }
    }

  val manageStorageLauncher =
    rememberLauncherForActivityResult(
      contract = ActivityResultContracts.StartActivityForResult(),
    ) { _ ->
      val gotPerms = PermissionHelper.hasExternalStoragePermission(context)
      onSettingsChange(settings.copy(useExternalStorage = gotPerms))
    }
  Scaffold(
    topBar = {
      TopAppBar(
        title = { Text("Settings") },
      )
    },
  ) { paddingValues ->
    Column(
      modifier =
        Modifier
          .fillMaxSize()
          .padding(paddingValues)
          .padding(16.dp)
          .verticalScroll(rememberScrollState()),
      verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
      // Languages Section
      Card(
        modifier = Modifier.fillMaxWidth(),
        colors =
          CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
          ),
      ) {
        Column(
          modifier = Modifier.padding(16.dp),
          verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
          Text(
            text = "Languages",
            style = MaterialTheme.typography.headlineSmall,
            color = MaterialTheme.colorScheme.primary,
          )

          // Manage Languages Button
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
          ) {
            Text(
              text = "Language Packs",
              style = MaterialTheme.typography.bodyMedium,
              color = MaterialTheme.colorScheme.onSurface,
            )

            TextButton(
              onClick = onManageLanguages,
            ) {
              Text("Manage")
            }
          }
        }
      }

      // General Settings Section
      Card(
        modifier = Modifier.fillMaxWidth(),
        colors =
          CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
          ),
      ) {
        Column(
          modifier = Modifier.padding(16.dp),
          verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
          Text(
            text = "General",
            style = MaterialTheme.typography.headlineSmall,
            color = MaterialTheme.colorScheme.primary,
          )

          LanguageDropdown(
            label = "Default 'from' language",
            selectedLanguage = settings.defaultSourceLanguage,
            availableLanguages = availableLanguages,
            fallbackLanguage = availableLanguages.firstOrNull { it != settings.defaultTargetLanguage },
            onLanguageSelected = { language ->
              onSettingsChange(settings.copy(defaultSourceLanguage = language))
            },
          )

          LanguageDropdown(
            label = "Default 'to' language",
            selectedLanguage = settings.defaultTargetLanguage,
            availableLanguages = availableLanguages,
            fallbackLanguage = null,
            onLanguageSelected = { language ->
              onSettingsChange(settings.copy(defaultTargetLanguage = language))
            },
          )

          Text(
            text = "Font Size",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface,
          )

          var showExampleText by remember { mutableStateOf(false) }

          Slider(
            value = settings.fontFactor,
            onValueChange = { value ->
              onSettingsChange(settings.copy(fontFactor = value))
              showExampleText = true
            },
            valueRange = 1.0f..3.0f,
            steps = 3,
            modifier = Modifier.fillMaxWidth(),
          )

          LaunchedEffect(settings.fontFactor) {
            if (showExampleText) {
              delay(1500)
              showExampleText = false
            }
          }

          if (showExampleText) {
            Text(
              text = "This is some example text",
              style =
                MaterialTheme.typography.bodyLarge.copy(
                  fontSize = (MaterialTheme.typography.bodyLarge.fontSize * settings.fontFactor),
                  lineHeight = (MaterialTheme.typography.bodyLarge.lineHeight * settings.fontFactor),
                ),
              color = MaterialTheme.colorScheme.onSurface,
              maxLines = 1,
              overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
            )
          }
        }
      }

      // OCR Settings Section - Only show if OCR is not disabled
      if (!settings.disableOcr) {
        Card(
          modifier = Modifier.fillMaxWidth(),
          colors =
            CardDefaults.cardColors(
              containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
            ),
        ) {
          Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
          ) {
            Text(
              text = "OCR",
              style = MaterialTheme.typography.headlineSmall,
              color = MaterialTheme.colorScheme.primary,
            )

            // Background Mode
            var backgroundModeExpanded by remember { mutableStateOf(false) }

            Text(
              text = "Background Mode",
              style = MaterialTheme.typography.bodyMedium,
              color = MaterialTheme.colorScheme.onSurface,
            )

            ExposedDropdownMenuBox(
              expanded = backgroundModeExpanded,
              onExpandedChange = { backgroundModeExpanded = it },
              modifier = Modifier.fillMaxWidth(),
            ) {
              OutlinedTextField(
                value = settings.backgroundMode.displayName,
                onValueChange = {},
                readOnly = true,
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = backgroundModeExpanded) },
                modifier =
                  Modifier
                    .menuAnchor()
                    .fillMaxWidth(),
                colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors(),
              )
              ExposedDropdownMenu(
                expanded = backgroundModeExpanded,
                onDismissRequest = { backgroundModeExpanded = false },
              ) {
                BackgroundMode.entries.forEach { mode ->
                  DropdownMenuItem(
                    text = { Text(mode.displayName) },
                    onClick = {
                      onSettingsChange(settings.copy(backgroundMode = mode))
                      backgroundModeExpanded = false
                    },
                  )
                }
              }
            }

            // Min Confidence Slider
            Text(
              text = "Min Confidence: ${settings.minConfidence}%",
              style = MaterialTheme.typography.bodyMedium,
              color = MaterialTheme.colorScheme.onSurface,
            )

            Slider(
              value = settings.minConfidence.toFloat(),
              onValueChange = { value ->
                onSettingsChange(settings.copy(minConfidence = value.toInt()))
              },
              valueRange = 50f..100f,
              steps = 9,
              modifier = Modifier.fillMaxWidth(),
            )

            // Max Image Size Slider
            Text(
              text = "Max Image Size: ${settings.maxImageSize}px",
              style = MaterialTheme.typography.bodyMedium,
              color = MaterialTheme.colorScheme.onSurface,
            )

            Slider(
              value = settings.maxImageSize.toFloat(),
              onValueChange = { value ->
                onSettingsChange(settings.copy(maxImageSize = value.toInt()))
              },
              valueRange = 1500f..4000f,
              steps = 24,
              modifier = Modifier.fillMaxWidth(),
            )
          }
        }
      }
      // Advanced Settings Section
      var advancedExpanded by remember { mutableStateOf(false) }
      Card(
        modifier = Modifier.fillMaxWidth(),
        colors =
          CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
          ),
      ) {
        Column(
          modifier = Modifier.padding(16.dp),
          verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
          // Clickable header
          Row(
            modifier =
              Modifier
                .fillMaxWidth()
                .clickable { advancedExpanded = !advancedExpanded },
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
          ) {
            Text(
              text = "Advanced Settings",
              style = MaterialTheme.typography.headlineSmall,
              color = MaterialTheme.colorScheme.primary,
            )

            Icon(
              painter =
                painterResource(
                  id = if (advancedExpanded) R.drawable.expandless else R.drawable.expandmore,
                ),
              contentDescription = if (advancedExpanded) "Collapse" else "Expand",
              tint = MaterialTheme.colorScheme.primary,
            )
          }

          // Expandable content
          if (advancedExpanded) {
            // Translation Models Base URL
            Text(
              text = "Base URL for Translation Models",
              style = MaterialTheme.typography.bodyMedium,
              color = MaterialTheme.colorScheme.onSurface,
            )

            OutlinedTextField(
              value = settings.translationModelsBaseUrl,
              onValueChange = {
                onSettingsChange(settings.copy(translationModelsBaseUrl = it))
              },
              modifier = Modifier.fillMaxWidth(),
              singleLine = true,
            )

            // Tesseract Models Base URL
            Text(
              text = "Base URL for Tesseract Models",
              style = MaterialTheme.typography.bodyMedium,
              color = MaterialTheme.colorScheme.onSurface,
            )

            OutlinedTextField(
              value = settings.tesseractModelsBaseUrl,
              onValueChange = {
                onSettingsChange(settings.copy(tesseractModelsBaseUrl = it))
              },
              modifier = Modifier.fillMaxWidth(),
              singleLine = true,
            )

            // Dictionary Base URL
            Text(
              text = "Base URL for Dictionaries",
              style = MaterialTheme.typography.bodyMedium,
              color = MaterialTheme.colorScheme.onSurface,
            )

            OutlinedTextField(
              value = settings.dictionaryBaseUrl,
              onValueChange = {
                onSettingsChange(settings.copy(dictionaryBaseUrl = it))
              },
              modifier = Modifier.fillMaxWidth(),
              singleLine = true,
            )

            // Disable OCR Toggle
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.SpaceBetween,
              verticalAlignment = Alignment.CenterVertically,
            ) {
              Text(
                text = "Disable OCR",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface,
              )

              Switch(
                checked = settings.disableOcr,
                onCheckedChange = { checked ->
                  onSettingsChange(settings.copy(disableOcr = checked))
                },
              )
            }

            // Disable CLD Toggle
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.SpaceBetween,
              verticalAlignment = Alignment.CenterVertically,
            ) {
              Text(
                text = "Disable automatic language detection",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface,
              )

              Switch(
                checked = settings.disableCLD,
                onCheckedChange = { checked ->
                  onSettingsChange(settings.copy(disableCLD = checked))
                },
              )
            }

            // Disable Transliteration Toggle
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.SpaceBetween,
              verticalAlignment = Alignment.CenterVertically,
            ) {
              Text(
                text = "Disable transliteration",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface,
              )

              Switch(
                checked = settings.disableTransliteration,
                onCheckedChange = { checked ->
                  onSettingsChange(settings.copy(disableTransliteration = checked))
                },
              )
            }

            // External Storage Toggle
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.SpaceBetween,
              verticalAlignment = Alignment.CenterVertically,
            ) {
              Text(
                text = "Use external storage",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface,
              )

              Switch(
                checked = settings.useExternalStorage,
                onCheckedChange = { checked ->
                  if (checked) {
                    if (PermissionHelper.hasExternalStoragePermission(context)) {
                      onSettingsChange(settings.copy(useExternalStorage = true))
                    } else if (PermissionHelper.needsSpecialPermissionIntent()) {
                      // Android 11+ - Show dialog first, then launch Settings
                      showPermissionDialog = true
                    } else {
                      // Android 10 and below - Request runtime permissions
                      val permissions = PermissionHelper.getExternalStoragePermissions()
                      if (permissions.isNotEmpty()) {
                        permissionLauncher.launch(permissions)
                      } else {
                        onSettingsChange(settings.copy(useExternalStorage = true))
                      }
                    }
                  } else {
                    onSettingsChange(settings.copy(useExternalStorage = false))
                  }
                },
              )
            }

            // Show OCR Detection Toggle
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.SpaceBetween,
              verticalAlignment = Alignment.CenterVertically,
            ) {
              Text(
                text = "Show OCR detection",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface,
              )

              Switch(
                checked = settings.showOCRDetection,
                onCheckedChange = { checked ->
                  onSettingsChange(settings.copy(showOCRDetection = checked))
                },
              )
            }
          }
        }
      }
    }
  }

  // Permission explanation dialog
  if (showPermissionDialog) {
    AlertDialog(
      onDismissRequest = {
        showPermissionDialog = false
      },
      title = { Text("External Storage Permission") },
      text = {
        Text(
          "To store translation files in your Documents folder, " +
            "this app needs access to manage all files.\nYou'll be taken to Settings where you can grant " +
            "'Allow access to manage all files' permission.",
        )
      },
      confirmButton = {
        TextButton(
          onClick = {
            showPermissionDialog = false
            val intent = PermissionHelper.createManageStorageIntent(context)
            manageStorageLauncher.launch(intent)
          },
        ) {
          Text("OK")
        }
      },
      dismissButton = {
        TextButton(
          onClick = {
            showPermissionDialog = false
          },
        ) {
          Text("Cancel")
        }
      },
    )
  }
}

@Preview(showBackground = true)
@Composable
fun SettingsScreenPreview() {
  TranslatorTheme {
    SettingsScreen(
      settings = AppSettings(),
      availableLanguages = Language.entries,
      onSettingsChange = {},
      onManageLanguages = {},
    )
  }
}

@Preview(
  showBackground = true,
  uiMode = android.content.res.Configuration.UI_MODE_NIGHT_YES,
)
@Composable
fun SettingsScreenDarkPreview() {
  TranslatorTheme {
    SettingsScreen(
      settings = AppSettings(fontFactor = 3.0f),
      availableLanguages = Language.entries,
      onSettingsChange = {},
      onManageLanguages = {},
    )
  }
}
