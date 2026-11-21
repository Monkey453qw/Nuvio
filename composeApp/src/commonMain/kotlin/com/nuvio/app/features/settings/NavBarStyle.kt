package com.nuvio.app.features.settings

import nuvio.composeapp.generated.resources.Res
import nuvio.composeapp.generated.resources.settings_nav_bar_style_adaptive
import nuvio.composeapp.generated.resources.settings_nav_bar_style_expanded
import nuvio.composeapp.generated.resources.settings_nav_bar_style_compact
import nuvio.composeapp.generated.resources.settings_nav_bar_style_classic
import nuvio.composeapp.generated.resources.settings_nav_bar_style_sidebar
import org.jetbrains.compose.resources.StringResource

enum class NavBarStyle(
    val key: String,
    val labelRes: StringResource,
) {
    ADAPTIVE("adaptive", Res.string.settings_nav_bar_style_adaptive),
    EXPANDED("expanded", Res.string.settings_nav_bar_style_expanded),
    COMPACT("compact", Res.string.settings_nav_bar_style_compact),
    CLASSIC("classic", Res.string.settings_nav_bar_style_classic),

    /**
     * Tablet-only side navigation rail. Phone layouts ignore it and fall back to
     * [ADAPTIVE] so the bottom bar always stays on phones.
     */
    SIDEBAR("sidebar", Res.string.settings_nav_bar_style_sidebar),
    ;

    companion object {
        fun fromKey(key: String?): NavBarStyle =
            entries.firstOrNull { it.key.equals(key, ignoreCase = true) } ?: ADAPTIVE
    }
}
