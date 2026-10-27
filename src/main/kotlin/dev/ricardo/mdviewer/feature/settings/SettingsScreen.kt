package dev.ricardo.mdviewer.feature.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import dev.ricardo.mdviewer.domain.model.AppTheme
import dev.ricardo.mdviewer.ui.components.AppTopBar
import dev.ricardo.mdviewer.ui.components.CategoryChip
import dev.ricardo.mdviewer.ui.components.SectionLabel
import dev.ricardo.mdviewer.ui.theme.LocalSpacing
import dev.ricardo.mdviewer.ui.theme.LocalUberColors

@Composable
fun SettingsScreen(
    onBack: () -> Unit,
    viewModel: SettingsViewModel,
) {
    val settings by viewModel.settings.collectAsState()
    val uber = LocalUberColors.current
    val spacing = LocalSpacing.current

    Column(Modifier.fillMaxSize().background(uber.canvas)) {
        AppTopBar(title = "Settings", onBack = onBack)
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(spacing.lg),
            verticalArrangement = Arrangement.spacedBy(spacing.xxl),
        ) {
            SettingGroup(label = "APPEARANCE") {
                Row(horizontalArrangement = Arrangement.spacedBy(spacing.sm)) {
                    CategoryChip("System", selected = settings.theme == AppTheme.SYSTEM, onClick = { viewModel.setTheme(AppTheme.SYSTEM) })
                    CategoryChip("Light", selected = settings.theme == AppTheme.LIGHT, onClick = { viewModel.setTheme(AppTheme.LIGHT) })
                    CategoryChip("Dark", selected = settings.theme == AppTheme.DARK, onClick = { viewModel.setTheme(AppTheme.DARK) })
                }
            }

            SettingGroup(label = "TEXT SIZE") {
                Row(horizontalArrangement = Arrangement.spacedBy(spacing.sm)) {
                    listOf(0.85f to "S", 1f to "M", 1.15f to "L", 1.3f to "XL").forEach { (scale, label) ->
                        CategoryChip(label, selected = settings.fontScale == scale, onClick = {
                            viewModel.setFontScale(scale)
                        })
                    }
                }
            }

            SettingGroup(label = "ANNOTATIONS") {
                ToggleRow(
                    title = "Accent highlights",
                    subtitle = "Use highlighter yellow. Off keeps the pure black-and-white palette.",
                    checked = settings.accentHighlight,
                    onCheckedChange = viewModel::setAccentHighlight,
                )
            }

            SettingGroup(label = "IMAGES") {
                ToggleRow(
                    title = "Load remote images",
                    subtitle = "Off by default to keep the app fully offline.",
                    checked = settings.loadRemoteImages,
                    onCheckedChange = viewModel::setLoadRemoteImages,
                )
            }

            SettingGroup(label = "ABOUT") {
                Text(
                    text = "MDViewer 1.0.1 - offline Markdown reader with portable annotations.",
                    style = MaterialTheme.typography.bodySmall,
                    color = uber.body,
                )
            }
        }
    }
}

@Composable
private fun SettingGroup(label: String, content: @Composable () -> Unit) {
    val spacing = LocalSpacing.current
    Column(verticalArrangement = Arrangement.spacedBy(spacing.md)) {
        SectionLabel(label)
        content()
    }
}

@Composable
private fun ToggleRow(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) {
    val uber = LocalUberColors.current
    val spacing = LocalSpacing.current
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(spacing.md),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(text = title, style = MaterialTheme.typography.titleMedium, color = uber.ink)
            Text(text = subtitle, style = MaterialTheme.typography.bodySmall, color = uber.body)
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = uber.canvas,
                checkedTrackColor = uber.ink,
            ),
        )
    }
}
