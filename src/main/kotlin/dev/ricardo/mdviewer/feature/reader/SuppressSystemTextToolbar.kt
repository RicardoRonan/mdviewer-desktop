package dev.ricardo.mdviewer.feature.reader

import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.platform.TextToolbar
import androidx.compose.ui.platform.TextToolbarStatus

/**
 * Suppresses the platform copy/paste toolbar so the reader can present its own pill toolbar
 * (Highlight / Note / Copy) in the Uber design language.
 */
class SuppressSystemTextToolbar : TextToolbar {

    override val status: TextToolbarStatus
        get() = TextToolbarStatus.Hidden

    override fun showMenu(
        rect: Rect,
        onCopyRequested: (() -> Unit)?,
        onPasteRequested: (() -> Unit)?,
        onCutRequested: (() -> Unit)?,
        onSelectAllRequested: (() -> Unit)?,
    ) = Unit

    override fun hide() = Unit
}
