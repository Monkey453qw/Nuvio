package com.nuvio.app.features.settings

import androidx.compose.runtime.Composable

internal actual object BackupRestoreStorage {
    actual val isSupported: Boolean get() = false

    actual fun currentTimeMs(): Long = 0L

    actual fun snapshot(): Map<String, Map<String, BackupValue>> = emptyMap()

    actual fun restore(files: Map<String, Map<String, BackupValue>>): Int = 0
}

@Composable
internal actual fun rememberBackupExportLauncher(onResult: (Result<Unit>) -> Unit): () -> Unit = {}

@Composable
internal actual fun rememberBackupImportLauncher(onResult: (Result<Unit>) -> Unit): () -> Unit = {}
