package com.nuvio.app.features.streams

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import nuvio.composeapp.generated.resources.Res
import nuvio.composeapp.generated.resources.collections_tab_all
import org.jetbrains.compose.resources.stringResource

/**
 * Quality/language filters for stream lists. Streams carry no structured
 * quality or language metadata, so both are derived from the stream text
 * (label, description, badges) and offered as filter chips.
 */

enum class StreamQualityBucket(val storageKey: String, val label: String, val priority: Int) {
    UHD_4K("uhd_4k", "4K", 0),
    FHD_1080("fhd_1080", "1080p", 1),
    HD_720("hd_720", "720p", 2),
    SD("sd", "SD", 3),
    ;

    companion object {
        fun fromStorageKey(key: String?): StreamQualityBucket? =
            entries.firstOrNull { it.storageKey == key }
    }
}

data class StreamSourceFilterSelection(
    val qualityKey: String?,
    val languageKey: String?,
) {
    val isNone: Boolean get() = qualityKey == null && languageKey == null

    companion object {
        val NONE = StreamSourceFilterSelection(null, null)
    }
}

object StreamSourceFilters {

    private const val MAX_LANGUAGE_OPTIONS = 12

    private val qualityPatterns = listOf(
        Regex("\\b2160p\\b|\\b4k\\b|\\buhd\\b") to StreamQualityBucket.UHD_4K,
        Regex("\\b1080[pi]\\b|\\bfhd\\b") to StreamQualityBucket.FHD_1080,
        Regex("\\b720p\\b|\\bhd720\\b") to StreamQualityBucket.HD_720,
        Regex("\\b480p\\b|\\b360p\\b|\\b240p\\b|\\bdvdrip\\b|\\bdvdscr\\b|\\bcamrip\\b|\\bhdts\\b|\\bsd\\b") to StreamQualityBucket.SD,
    )

    private val languageKeywords: Map<String, List<Regex>> = mapOf(
        "English" to listOf(Regex("\\benglish\\b"), Regex("\\beng\\b")),
        "Hindi" to listOf(Regex("\\bhindi\\b")),
        "Tamil" to listOf(Regex("\\btamil\\b")),
        "Telugu" to listOf(Regex("\\btelugu\\b")),
        "Malayalam" to listOf(Regex("\\bmalayalam\\b")),
        "Kannada" to listOf(Regex("\\bkannada\\b")),
        "Bengali" to listOf(Regex("\\bbengali\\b")),
        "Punjabi" to listOf(Regex("\\bpunjabi\\b")),
        "Marathi" to listOf(Regex("\\bmarathi\\b")),
        "Urdu" to listOf(Regex("\\burdu\\b")),
        "Spanish" to listOf(Regex("\\bspanish\\b"), Regex("\\bespanol\\b")),
        "French" to listOf(Regex("\\bfrench\\b"), Regex("\\bfrancais\\b")),
        "German" to listOf(Regex("\\bgerman\\b"), Regex("\\bdeutsch\\b")),
        "Italian" to listOf(Regex("\\bitalian\\b"), Regex("\\bitaliano\\b")),
        "Japanese" to listOf(Regex("\\bjapanese\\b"), Regex("\\bjpn\\b")),
        "Korean" to listOf(Regex("\\bkorean\\b")),
        "Chinese" to listOf(Regex("\\bchinese\\b"), Regex("\\bmandarin\\b")),
        "Russian" to listOf(Regex("\\brussian\\b")),
        "Turkish" to listOf(Regex("\\bturkish\\b")),
        "Arabic" to listOf(Regex("\\barabic\\b")),
        "Portuguese" to listOf(Regex("\\bportuguese\\b")),
        "Polish" to listOf(Regex("\\bpolish\\b")),
        "Dutch" to listOf(Regex("\\bdutch\\b")),
        "Thai" to listOf(Regex("\\bthai\\b")),
        "Vietnamese" to listOf(Regex("\\bvietnamese\\b")),
        "Indonesian" to listOf(Regex("\\bindonesian\\b")),
        "Filipino" to listOf(Regex("\\bfilipino\\b"), Regex("\\btagalog\\b")),
        "Hebrew" to listOf(Regex("\\bhebrew\\b")),
        "Persian" to listOf(Regex("\\bpersian\\b"), Regex("\\bfarsi\\b")),
        "Ukrainian" to listOf(Regex("\\bukrainian\\b")),
    )

    private fun StreamItem.filterableText(): String = buildString {
        append(streamLabel.lowercase())
        append(' ')
        append(streamSubtitle?.lowercase())
        append(' ')
        badges.forEach { badge ->
            append(badge.name.lowercase())
            append(' ')
        }
    }

    fun deriveQualityBucket(stream: StreamItem): StreamQualityBucket? {
        val text = stream.filterableText()
        return qualityPatterns.firstOrNull { (pattern, _) -> pattern.containsMatchIn(text) }?.second
    }

    fun deriveLanguages(stream: StreamItem): List<String> {
        val text = " ${stream.filterableText()} "
        return languageKeywords.mapNotNull { (label, patterns) ->
            if (patterns.any { it.containsMatchIn(text) }) label else null
        }
    }

    fun availableQualityBuckets(streams: List<StreamItem>): List<StreamQualityBucket> =
        streams.asSequence()
            .mapNotNull { deriveQualityBucket(it) }
            .distinct()
            .sortedBy { it.priority }
            .toList()

    fun availableLanguages(streams: List<StreamItem>): List<String> =
        streams.asSequence()
            .flatMap { deriveLanguages(it).asSequence() }
            .groupingBy { it }
            .eachCount()
            .toList()
            .sortedWith(compareByDescending<Pair<String, Int>> { it.second }.thenBy { it.first })
            .take(MAX_LANGUAGE_OPTIONS)
            .map { it.first }

    fun matches(stream: StreamItem, selection: StreamSourceFilterSelection): Boolean {
        if (selection.qualityKey != null) {
            val bucket = deriveQualityBucket(stream)
            if (bucket?.storageKey != selection.qualityKey) return false
        }
        if (selection.languageKey != null) {
            if (selection.languageKey !in deriveLanguages(stream)) return false
        }
        return true
    }

    /**
     * Applies the selection on top of the addon/source filtered groups.
     * Loading groups are always kept so arrivals stay visible; groups with an
     * error are kept so the error stays readable; groups whose streams all get
     * filtered out disappear.
     */
    fun applyTo(
        groups: List<AddonStreamGroup>,
        selection: StreamSourceFilterSelection,
    ): List<AddonStreamGroup> {
        if (selection.isNone) return groups
        return groups.mapNotNull { group ->
            val filtered = group.streams.filter { matches(it, selection) }
            if (filtered.isEmpty() && !group.isLoading && group.error == null) {
                null
            } else {
                group.copy(streams = filtered)
            }
        }
    }
}

@Serializable
private data class StoredStreamSourceFilter(
    val q: String? = null,
    val l: String? = null,
) {
    fun toSelection() = StreamSourceFilterSelection(q, l)

    companion object {
        fun of(selection: StreamSourceFilterSelection) = StoredStreamSourceFilter(selection.qualityKey, selection.languageKey)
    }
}

@Serializable
private data class StreamSourceFilterPrefs(
    val global: StoredStreamSourceFilter? = null,
    val titles: Map<String, StoredStreamSourceFilter> = emptyMap(),
)

/**
 * Holds the live filter selection shared by the streams screen and the
 * player's source/episode panels, and persists it: the global last-used
 * selection plus a per-title memory that re-applies a remembered pick when
 * the same title is opened again.
 */
object StreamSourceFilterMemory {

    private const val MAX_TITLES = 200

    private val json = Json { ignoreUnknownKeys = true }

    private var prefs: StreamSourceFilterPrefs = runCatching {
        StreamSourceFilterStorage.loadPayload()
            ?.let { json.decodeFromString(StreamSourceFilterPrefs.serializer(), it) }
    }.getOrNull() ?: StreamSourceFilterPrefs()

    private val _selection = MutableStateFlow(prefs.global?.toSelection() ?: StreamSourceFilterSelection.NONE)
    val selection: StateFlow<StreamSourceFilterSelection> = _selection.asStateFlow()

    private var activeTitleKey: String? = null

    /**
     * Called when a title's streams are requested: activates the title key and
     * restores its remembered selection, falling back to the global one.
     */
    fun seedFor(titleKey: String?) {
        activeTitleKey = titleKey
        val remembered = titleKey?.let { key -> prefs.titles[key]?.toSelection() }
            ?: prefs.global?.toSelection()
            ?: StreamSourceFilterSelection.NONE
        _selection.value = remembered
    }

    fun select(qualityKey: String?, languageKey: String?) {
        val value = StreamSourceFilterSelection(qualityKey, languageKey)
        _selection.value = value
        prefs = prefs.copy(
            global = StoredStreamSourceFilter.of(value),
            titles = activeTitleKey?.let { key -> prefs.titles + (key to StoredStreamSourceFilter.of(value)) } ?: prefs.titles,
        )
        trimTitles()
        runCatching {
            StreamSourceFilterStorage.savePayload(json.encodeToString(StreamSourceFilterPrefs.serializer(), prefs))
        }
    }

    private fun trimTitles() {
        if (prefs.titles.size <= MAX_TITLES) return
        val keep = prefs.titles.entries.drop(prefs.titles.size - MAX_TITLES)
        prefs = prefs.copy(titles = keep.associate { it.key to it.value })
    }
}

@Composable
internal fun StreamSourceFilterRow(
    availableStreams: List<StreamItem>,
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
) {
    val selection by StreamSourceFilterMemory.selection.collectAsStateWithLifecycle()
    val qualityOptions = remember(availableStreams) { StreamSourceFilters.availableQualityBuckets(availableStreams) }
    val languageOptions = remember(availableStreams) { StreamSourceFilters.availableLanguages(availableStreams) }
    if (qualityOptions.isEmpty() && languageOptions.isEmpty()) return

    val allLabel = stringResource(Res.string.collections_tab_all)

    Column(modifier = modifier.fillMaxWidth()) {
        if (qualityOptions.isNotEmpty()) {
            FilterChipRow(
                options = qualityOptions.map { it.label },
                storageKeys = qualityOptions.map { it.storageKey },
                selectedKey = selection.qualityKey,
                onSelect = { key -> StreamSourceFilterMemory.select(key, selection.languageKey) },
                allLabel = allLabel,
                contentPadding = contentPadding,
            )
        }
        if (languageOptions.isNotEmpty()) {
            FilterChipRow(
                options = languageOptions,
                storageKeys = languageOptions,
                selectedKey = selection.languageKey,
                onSelect = { key -> StreamSourceFilterMemory.select(selection.qualityKey, key) },
                allLabel = allLabel,
                contentPadding = contentPadding,
            )
        }
    }
}

@Composable
private fun FilterChipRow(
    options: List<String>,
    storageKeys: List<String>,
    selectedKey: String?,
    onSelect: (String?) -> Unit,
    allLabel: String,
    contentPadding: PaddingValues,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .padding(contentPadding),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        FilterChip(
            label = allLabel,
            isSelected = selectedKey == null,
            onClick = { onSelect(null) },
        )
        options.forEachIndexed { index, label ->
            FilterChip(
                label = label,
                isSelected = storageKeys[index] == selectedKey,
                onClick = { onSelect(storageKeys[index]) },
            )
        }
    }
}
