package com.nuvio.app.core.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.shrinkHorizontally
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ChevronLeft
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.painterResource

/**
 * Tablet-only navigation rail.
 *
 * Collapsed it is a narrow icon rail, expanded it grows to a stable width and
 * shows labels. The rail itself only animates its own width, so the tab content
 * next to it re-lays out without being recreated or reloaded.
 */

internal class SidebarNavigationItem(
    val label: String,
    val selected: Boolean,
    val onClick: () -> Unit,
    val icon: ImageVector? = null,
    val drawable: DrawableResource? = null,
)

internal val sidebarCollapsedWidth = 84.dp
internal val sidebarExpandedWidth = 216.dp

@Composable
internal fun NuvioSidebarNavigation(
    items: List<SidebarNavigationItem>,
    expanded: Boolean,
    onToggleExpanded: () -> Unit,
    expandLabel: String,
    collapseLabel: String,
    modifier: Modifier = Modifier,
) {
    val tokens = MaterialTheme.nuvio
    val width by animateDpAsState(
        targetValue = if (expanded) sidebarExpandedWidth else sidebarCollapsedWidth,
        animationSpec = tween(durationMillis = 180, easing = FastOutSlowInEasing),
        label = "sidebar-width",
    )

    Row(
        modifier = modifier
            .width(width)
            .fillMaxHeight()
            .background(tokens.colors.surfaceElevated),
    ) {
        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight()
                .windowInsetsPadding(WindowInsets.statusBars)
                .windowInsetsPadding(WindowInsets.navigationBars)
                .padding(horizontal = 10.dp, vertical = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            items.forEach { item ->
                SidebarNavigationRow(item = item, expanded = expanded)
            }
            Spacer(modifier = Modifier.weight(1f))
            SidebarNavigationToggle(
                expanded = expanded,
                onToggleExpanded = onToggleExpanded,
                expandLabel = expandLabel,
                collapseLabel = collapseLabel,
            )
        }
        Box(
            modifier = Modifier
                .width(1.dp)
                .fillMaxHeight()
                .background(tokens.colors.borderSubtle),
        )
    }
}

@Composable
private fun SidebarNavigationRow(
    item: SidebarNavigationItem,
    expanded: Boolean,
) {
    val tokens = MaterialTheme.nuvio
    val iconTint = if (item.selected) tokens.colors.accent else tokens.colors.textSecondary
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(52.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(if (item.selected) tokens.colors.overlaySelected else Color.Transparent)
            .clickable(role = Role.Tab, onClick = item.onClick)
            .padding(horizontal = if (expanded) 14.dp else 0.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = if (expanded) Arrangement.spacedBy(14.dp) else Arrangement.Center,
    ) {
        SidebarItemIcon(item = item, tint = iconTint)
        AnimatedVisibility(
            visible = expanded,
            enter = fadeIn(tween(durationMillis = 140)) + expandHorizontally(tween(durationMillis = 180)),
            exit = fadeOut(tween(durationMillis = 90)) + shrinkHorizontally(tween(durationMillis = 140)),
        ) {
            Text(
                text = item.label,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = if (item.selected) FontWeight.SemiBold else FontWeight.Medium,
                color = if (item.selected) tokens.colors.textPrimary else tokens.colors.textSecondary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
private fun SidebarItemIcon(
    item: SidebarNavigationItem,
    tint: Color,
) {
    val drawable = item.drawable
    val icon = item.icon
    when {
        drawable != null -> Icon(
            painter = painterResource(drawable),
            contentDescription = item.label,
            tint = tint,
            modifier = Modifier.size(24.dp),
        )

        icon != null -> Icon(
            imageVector = icon,
            contentDescription = item.label,
            tint = tint,
            modifier = Modifier.size(24.dp),
        )
    }
}

@Composable
private fun SidebarNavigationToggle(
    expanded: Boolean,
    onToggleExpanded: () -> Unit,
    expandLabel: String,
    collapseLabel: String,
) {
    val tokens = MaterialTheme.nuvio
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(44.dp)
            .clip(RoundedCornerShape(14.dp))
            .clickable(role = Role.Button, onClick = onToggleExpanded)
            .padding(horizontal = if (expanded) 14.dp else 0.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = if (expanded) Arrangement.spacedBy(14.dp) else Arrangement.Center,
    ) {
        Icon(
            imageVector = if (expanded) Icons.Rounded.ChevronLeft else Icons.Rounded.ChevronRight,
            contentDescription = if (expanded) collapseLabel else expandLabel,
            tint = tokens.colors.textMuted,
            modifier = Modifier.size(22.dp),
        )
        AnimatedVisibility(
            visible = expanded,
            enter = fadeIn(tween(durationMillis = 140)),
            exit = fadeOut(tween(durationMillis = 90)),
        ) {
            Text(
                text = collapseLabel,
                style = MaterialTheme.typography.labelMedium,
                color = tokens.colors.textMuted,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}
