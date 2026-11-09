package com.nuvio.app.features.settings

import android.content.Context
import android.content.SharedPreferences
import android.provider.OpenableColumns
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.platform.LocalContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

internal actual object BackupRestoreStorage {
    // Preference stores included in a backup: settings, addons, plugins and
    // other configuration. Library, watch history, search history, caches,
    // auth tokens and keystore-encrypted stores are intentionally excluded —
    // library and history sync through the Nuvio account, and keystore
    // encrypted values are bound to this device and cannot restore elsewhere.
    private val backupPreferencesNames = setOf(
        "nuvio_addons",
        "nuvio_plugins",
        "nuvio_player_settings",
        "nuvio_player_track_preferences",
        "nuvio_theme_settings",
        "app_icon_settings",
        "nuvio_poster_card_style",
        "nuvio_card_depth_style",
        "nuvio_custom_poster_url",
        "nuvio_stream_badge_settings",
        "nuvio_debrid_settings",
        "nuvio_home_catalog_settings",
        "nuvio_meta_screen_settings",
        "nuvio_season_view_mode",
        "nuvio_collection_mobile_settings",
        "nuvio_library_display_settings",
        "nuvio_continue_watching_preferences",
        "torrent_settings",
        "nuvio_tmdb_settings",
        "nuvio_mdblist_settings",
        "nuvio_trakt_settings",
        "nuvio_sentry_settings",
        "nuvio_updater",
        "nuvio_stream_source_filters",
        "nuvio_episode_release_notifications",
        "nuvio_autosync_settings",
        "nuvio_discover_selection",
        "server_configuration",
    )

    private var appContext: Context? = null

    fun initialize(context: Context) {
        appContext = context.applicationContext
    }

    actual val isSupported: Boolean get() = true

    actual fun currentTimeMs(): Long = System.currentTimeMillis()

    actual fun snapshot(): Map<String, Map<String, BackupValue>> {
        val context = appContext ?: return emptyMap()
        val files = linkedMapOf<String, Map<String, BackupValue>>()
        backupPreferencesNames.forEach { name ->
            val entries = context.getSharedPreferences(name, Context.MODE_PRIVATE)
                .all
                .mapNotNull { (key, value) ->
                    value.toBackupValue()?.let { key to it }
                }
                .toMap()
            if (entries.isNotEmpty()) {
                files[name] = entries
            }
        }
        return files
    }

    actual fun restore(files: Map<String, Map<String, BackupValue>>): Int {
        val context = appContext ?: return 0
        var restoredCount = 0
        files.forEach { (name, entries) ->
            if (name !in backupPreferencesNames || entries.isEmpty()) return@forEach
            val editor = context.getSharedPreferences(name, Context.MODE_PRIVATE).edit()
            entries.forEach { (key, value) -> value.writeTo(editor, key) }
            editor.apply()
            restoredCount++
        }
        return restoredCount
    }
}

private fun Any?.toBackupValue(): BackupValue? = when (this) {
    null -> null
    is Boolean -> BackupValue.Bool(this)
    is Int -> BackupValue.IntVal(this)
    is Long -> BackupValue.LongVal(this)
    is Float -> BackupValue.FloatVal(this)
    is String -> BackupValue.StringVal(this)
    is Set<*> -> BackupValue.StringSet(mapNotNull { it as? String }.toSet())
    else -> null
}

private fun BackupValue.writeTo(editor: SharedPreferences.Editor, key: String) {
    when (this) {
        is BackupValue.Bool -> editor.putBoolean(key, value)
        is BackupValue.IntVal -> editor.putInt(key, value)
        is BackupValue.LongVal -> editor.putLong(key, value)
        is BackupValue.FloatVal -> editor.putFloat(key, value)
        is BackupValue.StringVal -> editor.putString(key, value)
        is BackupValue.StringSet -> editor.putStringSet(key, value)
    }
}

private val backupExportDateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.US)

private const val maxBackupBytes = 10L * 1024L * 1024L

@Composable
internal actual fun rememberBackupExportLauncher(onResult: (Result<Unit>) -> Unit): () -> Unit {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val latestResult = rememberUpdatedState(onResult)
    val launcher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/json"),
    ) { uri ->
        if (uri != null) scope.launch {
            val result = withContext(Dispatchers.IO) {
                runCatching {
                    val text = BackupRestoreRepository.createBackupText().getOrThrow()
                    context.contentResolver.openOutputStream(uri)?.use { output ->
                        output.write(text.toByteArray(Charsets.UTF_8))
                    } ?: error("Unable to open backup destination")
                }.map { }
            }
            latestResult.value(result)
        }
    }
    return { launcher.launch("nuvio-backup-${backupExportDateFormat.format(Date())}.json") }
}

@Composable
internal actual fun rememberBackupImportLauncher(onResult: (Result<Unit>) -> Unit): () -> Unit {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val latestResult = rememberUpdatedState(onResult)
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) scope.launch {
            val result = withContext(Dispatchers.IO) {
                runCatching {
                    val resolver = context.contentResolver
                    val size = resolver.query(uri, arrayOf(OpenableColumns.SIZE), null, null, null)?.use { cursor ->
                        if (cursor.moveToFirst()) cursor.getLong(0) else null
                    }
                    require(size == null || size <= maxBackupBytes) { "Backup file is too large" }
                    val text = resolver.openInputStream(uri)?.use { input ->
                        input.readBytes().toString(Charsets.UTF_8)
                    } ?: error("Unable to open backup file")
                    BackupRestoreRepository.applyBackupText(text).getOrThrow()
                }
            }
            latestResult.value(result)
        }
    }
    return { launcher.launch(arrayOf("*/*")) }
}
