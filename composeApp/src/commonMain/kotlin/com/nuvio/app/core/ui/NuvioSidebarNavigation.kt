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
import androidx.compose.foundation.layout.offset
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
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.HazeTint
import dev.chrisbanes.haze.hazeEffect
import dev.chrisbanes.haze.hazeSource
import dev.chrisbanes.haze.rememberHazeState
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.painterResource

/**
 * Tablet-only navigation rail.
 *
 * Collapsed it is a narrow icon rail, expanded it grows to a stable width and
 * shows labels. The rail itself only animates its own width, so the tab content
 * next to it re-lays out without being recreated or reloaded.
 *
 * The rail is a floating translucent card instead of a full-height slab, and the
 * selected item is a genuinely frosted glass capsule: it blurs the rail's own
 * layered backdrop (accent bloom + colour blobs) with a light grain, so it reads
 * like glass rather than a flat grey highlight.
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
private val sidebarItemShape = RoundedCornerShape(NuvioTokens.Radius.full)
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
    // The pill frosts this state's source: the rail backdrop drawn below it.
    val railHazeState = rememberHazeState()
    val width by animateDpAsState(
        targetValue = if (expanded) sidebarExpandedWidth else sidebarCollapsedWidth,
        animationSpec = tween(durationMillis = 200, easing = FastOutSlowInEasing),
        label = "sidebar-width",
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
                .clip(sidebarRailShape),
        ) {
            SidebarGlassBackdrop(
                modifier = Modifier
                    .fillMaxSize()
                    .hazeSource(railHazeState),
            )
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .border(
                        width = NuvioTokens.Border.hairline,
                        color = tokens.colors.borderSubtle,
                        shape = sidebarRailShape,
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
                    SidebarNavigationRow(
                        item = item,
                        expanded = expanded,
                        hazeState = railHazeState,
                    )
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

/**
 * The glass scene behind the rail: a translucent vertical gradient plus soft
 * accent colour blobs. The blobs give the frosted selected pill something with
 * actual shape and colour to blend, which is what makes the blur visible.
 */
@Composable
private fun SidebarGlassBackdrop(modifier: Modifier = Modifier) {
    val tokens = MaterialTheme.nuvio
    val surface = tokens.colors.surfaceElevated
    Box(modifier = modifier) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            surface.copy(alpha = 0.80f),
                            surface.copy(alpha = 0.68f),
                            surface.copy(alpha = 0.58f),
                        ),
                    ),
                ),
        )
        Box(
            modifier = Modifier
                .align(Alignment.TopStart)
                .offset(x = (-44).dp, y = (-54).dp)
                .size(230.dp)
                .background(
                    Brush.radialGradient(
                        colors = listOf(
                            tokens.colors.accent.copy(alpha = 0.30f),
                            Color.Transparent,
                        ),
                    ),
                ),
        )
        Box(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .offset(x = 56.dp, y = 64.dp)
                .size(250.dp)
                .background(
                    Brush.radialGradient(
                        colors = listOf(
                            tokens.colors.accentStrong.copy(alpha = 0.18f),
                            Color.Transparent,
                        ),
                    ),
                ),
        )
        // A soft top sheen so the top of the card catches light like glass.
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(150.dp)
                .align(Alignment.TopCenter)
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color.White.copy(alpha = 0.05f),
                            Color.Transparent,
                        ),
                    ),
                ),
        )
    }
}

@Composable
private fun SidebarNavigationRow(
    item: SidebarNavigationItem,
    expanded: Boolean,
    hazeState: HazeState,
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
    // A light accent veil on top of the frost keeps the selection recognisable
    // without hiding the blurred backdrop underneath.
    val itemBackground by animateColorAsState(
        targetValue = if (item.selected) tokens.colors.accent.copy(alpha = 0.10f) else Color.Transparent,
        animationSpec = tween(durationMillis = 160),
        label = "sidebar-item-background",
    )

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(sidebarItemHeight)
            .clip(sidebarItemShape)
            .then(
                if (item.selected) {
                    Modifier.hazeEffect(hazeState) {
                        blurRadius = 24.dp
                        noiseFactor = 0.12f
                        tints = listOf(HazeTint(Color.White.copy(alpha = 0.08f)))
                        fallbackTint = HazeTint(tokens.colors.surfaceElevated.copy(alpha = 0.92f))
                    }
                } else {
                    Modifier
                },
            )
            .background(itemBackground)
            .then(
                if (item.selected) {
                    Modifier.border(
                        width = NuvioTokens.Border.hairline,
                        brush = Brush.verticalGradient(
                            colors = listOf(
                                tokens.colors.accent.copy(alpha = 0.55f),
                                Color.White.copy(alpha = 0.10f),
                                tokens.colors.accent.copy(alpha = 0.16f),
                            ),
                        ),
                        shape = sidebarItemShape,
                    )
                } else {
                    Modifier
                },
            )
            .clickable(role = Role.Tab, onClick = item.onClick)
            .padding(horizontal = if (expanded) 16.dp else 0.dp),
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
            .padding(horizontal = if (expanded) 16.dp else 0.dp),
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
