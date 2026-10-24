package dev.ricardo.mdviewer.ui.components

import androidx.compose.animation.Crossfade
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import dev.ricardo.mdviewer.ui.icons.Feather
import dev.ricardo.mdviewer.ui.theme.LocalSpacing
import dev.ricardo.mdviewer.ui.theme.LocalUberColors
import dev.ricardo.mdviewer.ui.theme.Motion
import dev.ricardo.mdviewer.ui.theme.PillShape
import dev.ricardo.mdviewer.ui.theme.motionTween
import dev.ricardo.mdviewer.ui.theme.pressScale
import dev.ricardo.mdviewer.ui.theme.rememberMotionEnabled
import dev.ricardo.mdviewer.ui.theme.toHexString

enum class PillStyle { Primary, Secondary, Subtle }

@Composable
fun PillButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    style: PillStyle = PillStyle.Primary,
    enabled: Boolean = true,
    compact: Boolean = false,
) {
    val uber = LocalUberColors.current
    val background = when (style) {
        PillStyle.Primary -> MaterialTheme.colorScheme.primary
        PillStyle.Secondary -> uber.canvas
        PillStyle.Subtle -> uber.canvasSoft
    }
    val foreground = when (style) {
        PillStyle.Primary -> MaterialTheme.colorScheme.onPrimary
        PillStyle.Secondary -> uber.ink
        PillStyle.Subtle -> uber.ink
    }
    val hPad = if (compact) 14.dp else 20.dp
    val vPad = if (compact) 8.dp else 12.dp
    val interaction = remember { MutableInteractionSource() }
    Box(
        modifier = modifier
            .pressScale(interaction)
            .heightIn(min = if (compact) 36.dp else 44.dp)
            .clip(PillShape)
            .background(if (enabled) background else uber.canvasSoft)
            .then(
                if (style == PillStyle.Secondary) {
                    Modifier.border(1.dp, uber.surfacePressed, PillShape)
                } else {
                    Modifier
                },
            )
            .clickable(interactionSource = interaction, indication = null, enabled = enabled, onClick = onClick)
            .padding(horizontal = hPad, vertical = vPad),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = text,
            style = if (compact) MaterialTheme.typography.labelMedium else MaterialTheme.typography.labelLarge,
            color = if (enabled) foreground else uber.mute,
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
fun CircleIconButton(
    icon: ImageVector,
    contentDescription: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val uber = LocalUberColors.current
    val interaction = remember { MutableInteractionSource() }
    Box(
        modifier = modifier
            .pressScale(interaction)
            .size(44.dp)
            .clip(CircleShape)
            .background(uber.canvasSoft)
            .clickable(interactionSource = interaction, indication = null, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = uber.ink,
            modifier = Modifier.size(20.dp),
        )
    }
}

@Composable
fun AppTopBar(
    title: String,
    modifier: Modifier = Modifier,
    onBack: (() -> Unit)? = null,
    actions: @Composable RowScope.() -> Unit = {},
) {
    val spacing = LocalSpacing.current
    val uber = LocalUberColors.current
    val motion = rememberMotionEnabled()
    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(uber.canvas)
            .padding(horizontal = spacing.lg, vertical = spacing.md),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(spacing.md),
    ) {
        if (onBack != null) {
            CircleIconButton(icon = Feather.ArrowLeft, contentDescription = "Back", onClick = onBack)
        }
        Crossfade(
            targetState = title,
            modifier = Modifier.weight(1f),
            animationSpec = motionTween(Motion.Quick, motion),
            label = "title",
        ) { label ->
            Text(
                text = label,
                style = MaterialTheme.typography.titleMedium,
                color = uber.ink,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        actions()
    }
}

@Composable
fun ContentCard(
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    content: @Composable () -> Unit,
) {
    val uber = LocalUberColors.current
    val spacing = LocalSpacing.current
    val interaction = remember { MutableInteractionSource() }
    Box(
        modifier = modifier
            .fillMaxWidth()
            .then(if (onClick != null) Modifier.pressScale(interaction) else Modifier)
            .clip(MaterialTheme.shapes.large)
            .background(uber.canvasSoft)
            .then(
                if (onClick != null) {
                    Modifier.clickable(interactionSource = interaction, indication = null, onClick = onClick)
                } else {
                    Modifier
                },
            )
            .padding(spacing.xxl),
    ) {
        content()
    }
}

@Composable
fun SectionLabel(text: String, modifier: Modifier = Modifier) {
    val uber = LocalUberColors.current
    Text(
        text = text.uppercase(),
        modifier = modifier,
        style = MaterialTheme.typography.labelSmall,
        color = uber.body,
    )
}

@Composable
fun CategoryChip(
    text: String,
    selected: Boolean = false,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val uber = LocalUberColors.current
    val spacing = LocalSpacing.current
    val interaction = remember { MutableInteractionSource() }
    Box(
        modifier = modifier
            .pressScale(interaction)
            .heightIn(min = 40.dp)
            .clip(PillShape)
            .background(if (selected) uber.ink else uber.canvasSoft)
            .clickable(interactionSource = interaction, indication = null, onClick = onClick)
            .padding(horizontal = spacing.lg, vertical = spacing.sm),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelMedium,
            color = if (selected) uber.canvas else uber.ink,
        )
    }
}

@Composable
fun EmptyState(
    title: String,
    body: String,
    modifier: Modifier = Modifier,
    action: (@Composable () -> Unit)? = null,
) {
    val uber = LocalUberColors.current
    val spacing = LocalSpacing.current
    Column(
        modifier = modifier.fillMaxWidth().padding(spacing.xxxl),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(spacing.md),
    ) {
        Text(text = title, style = MaterialTheme.typography.headlineSmall, color = uber.ink, textAlign = TextAlign.Center)
        Text(text = body, style = MaterialTheme.typography.bodyMedium, color = uber.body, textAlign = TextAlign.Center)
        if (action != null) {
            Box(Modifier.padding(top = spacing.sm)) { action() }
        }
    }
}

fun Color.asHex(): String = toHexString()
