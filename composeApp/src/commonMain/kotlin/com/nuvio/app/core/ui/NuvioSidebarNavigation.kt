package com.nuvio.app.core.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.shrinkHorizontally
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
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
 *
 * Visually it is a floating, translucent card rather than a full-height slab:
 * rounded corners, air around it, a soft shadow and a faint accent glow at the
 * top so it reads as part of the app instead of a layer placed on top of it.
 */

internal class SidebarNavigationItem(
    val label: String,
    val selected: Boolean,
    val onClick: () -> Unit,
    val icon: ImageVector? = null,
    val drawable: DrawableResource? = null,
)

private val sidebarRailCollapsedWidth = 72.dp
private val sidebarRailExpandedWidth = 208.dp
private val sidebarSlotHorizontalInset = 6.dp
private val sidebarSlotVerticalInset = 10.dp

internal val sidebarCollapsedWidth = sidebarRailCollapsedWidth + sidebarSlotHorizontalInset * 2
internal val sidebarExpandedWidth = sidebarRailExpandedWidth + sidebarSlotHorizontalInset * 2

private val sidebarItemHeight = 56.dp
private val sidebarIconSize = 26.dp
private val sidebarItemShape = RoundedCornerShape(NuvioTokens.Radius.xl)
private val sidebarRailShape = RoundedCornerShape(NuvioTokens.Radius.xxl)

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
        animationSpec = tween(durationMillis = 200, easing = FastOutSlowInEasing),
        label = "sidebar-width",
    )
    // A gradient instead of a flat fill: the rail fades from a more present
    // surface at the top to a lighter, more transparent one at the bottom.
    val surface = tokens.colors.surfaceElevated
    val railBrush = Brush.verticalGradient(
        colors = listOf(
            surface.copy(alpha = 0.80f),
            surface.copy(alpha = 0.68f),
            surface.copy(alpha = 0.58f),
        ),
    )

    Box(
        modifier = modifier
            .width(width)
            .fillMaxHeight()
            .windowInsetsPadding(WindowInsets.statusBars)
            .windowInsetsPadding(WindowInsets.navigationBars)
            .padding(
                horizontal = sidebarSlotHorizontalInset,
                vertical = sidebarSlotVerticalInset,
            ),
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .shadow(
                    elevation = 12.dp,
                    shape = sidebarRailShape,
                    clip = false,
                    ambientColor = Color.Black.copy(alpha = 0.20f),
                    spotColor = Color.Black.copy(alpha = 0.28f),
                )
                .clip(sidebarRailShape)
                .background(railBrush)
                .border(
                    width = NuvioTokens.Border.hairline,
                    color = tokens.colors.borderSubtle,
                    shape = sidebarRailShape,
                ),
        ) {
            // Faint accent bloom at the top so the glass picks up the app color
            // instead of sitting there as a neutral grey block.
            Box(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .fillMaxWidth()
                    .height(200.dp)
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                tokens.colors.accent.copy(alpha = 0.10f),
                                Color.Transparent,
                            ),
                        ),
                    ),
            )
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 8.dp, vertical = 12.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(4.dp),
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
        }
    }
}

@Composable
private fun SidebarNavigationRow(
    item: SidebarNavigationItem,
    expanded: Boolean,
) {
    val tokens = MaterialTheme.nuvio
    val iconTint by animateColorAsState(
        targetValue = if (item.selected) tokens.colors.accent else tokens.colors.textSecondary,
        animationSpec = tween(durationMillis = 160),
        label = "sidebar-icon-color",
    )
    val labelColor by animateColorAsState(
        targetValue = if (item.selected) tokens.colors.textPrimary else tokens.colors.textSecondary,
        animationSpec = tween(durationMillis = 160),
        label = "sidebar-label-color",
    )
    val itemBackground by animateColorAsState(
        targetValue = if (item.selected) tokens.colors.accent.copy(alpha = 0.15f) else Color.Transparent,
        animationSpec = tween(durationMillis = 160),
        label = "sidebar-item-background",
    )
    val itemBorder by animateColorAsState(
        targetValue = if (item.selected) tokens.colors.accent.copy(alpha = 0.32f) else Color.Transparent,
        animationSpec = tween(durationMillis = 160),
        label = "sidebar-item-border",
    )

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(sidebarItemHeight)
            .clip(sidebarItemShape)
            .background(itemBackground)
            .border(
                width = NuvioTokens.Border.hairline,
                color = itemBorder,
                shape = sidebarItemShape,
            )
            .clickable(role = Role.Tab, onClick = item.onClick)
            .padding(horizontal = if (expanded) 14.dp else 0.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = if (expanded) Arrangement.spacedBy(14.dp) else Arrangement.Center,
    ) {
        SidebarItemIcon(item = item, tint = iconTint)
        AnimatedVisibility(
            visible = expanded,
            enter = fadeIn(tween(durationMillis = 140)) + expandHorizontally(tween(durationMillis = 180), expandFrom = Alignment.Start),
            exit = fadeOut(tween(durationMillis = 90)) + shrinkHorizontally(tween(durationMillis = 140), shrinkTowards = Alignment.Start),
        ) {
            Text(
                text = item.label,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = if (item.selected) FontWeight.SemiBold else FontWeight.Medium,
                color = labelColor,
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
            modifier = Modifier.size(sidebarIconSize),
        )

        icon != null -> Icon(
            imageVector = icon,
            contentDescription = item.label,
            tint = tint,
            modifier = Modifier.size(sidebarIconSize),
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
    // One chevron that rotates instead of two icons swapping.
    val rotation by animateFloatAsState(
        targetValue = if (expanded) 180f else 0f,
        animationSpec = tween(durationMillis = 200, easing = FastOutSlowInEasing),
        label = "sidebar-toggle-rotation",
    )
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(44.dp)
            .clip(sidebarItemShape)
            .clickable(role = Role.Button, onClick = onToggleExpanded)
            .padding(horizontal = if (expanded) 14.dp else 0.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = if (expanded) Arrangement.spacedBy(14.dp) else Arrangement.Center,
    ) {
        Icon(
            imageVector = Icons.Rounded.ChevronRight,
            contentDescription = if (expanded) collapseLabel else expandLabel,
            tint = tokens.colors.textMuted,
            modifier = Modifier
                .size(22.dp)
                .rotate(rotation),
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
