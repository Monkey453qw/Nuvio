package com.nuvio.app.features.settings

import androidx.compose.runtime.Composable
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.doubleOrNull
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.longOrNull
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.put

/** A single preference value captured by a backup, keeping its exact storage type. */
internal sealed interface BackupValue {
    data class Bool(val value: Boolean) : BackupValue
    data class IntVal(val value: Int) : BackupValue
    data class LongVal(val value: Long) : BackupValue
    data class FloatVal(val value: Float) : BackupValue
    data class StringVal(val value: String) : BackupValue
    data class StringSet(val value: Set<String>) : BackupValue
}

internal class BackupFormatException(message: String) : Exception(message)

@Serializable
private data class BackupPayload(
    val type: String,
    val version: Int,
    val createdAtEpochMs: Long,
    val preferences: Map<String, Map<String, JsonElement>>,
)

internal object BackupRestoreCodec {
    private const val payloadType = "nuvio-backup"
    private const val payloadVersion = 1

    private val json = Json {
        ignoreUnknownKeys = true
        prettyPrint = true
    }

    fun encode(files: Map<String, Map<String, BackupValue>>, createdAtEpochMs: Long): String {
        val payload = BackupPayload(
            type = payloadType,
            version = payloadVersion,
            createdAtEpochMs = createdAtEpochMs,
            preferences = files.mapValues { (_, entries) ->
                entries.mapValues { (_, value) -> encodeValue(value) }
            },
        )
        return json.encodeToString(payload)
    }

    fun decode(text: String): Map<String, Map<String, BackupValue>> {
        val payload = try {
            json.decodeFromString<BackupPayload>(text)
        } catch (_: Exception) {
            throw BackupFormatException("Not a valid Nuvio backup file")
        }
        if (payload.type != payloadType || payload.version != payloadVersion) {
            throw BackupFormatException("Unsupported backup payload")
        }

        val files = linkedMapOf<String, Map<String, BackupValue>>()
        payload.preferences.forEach { (name, entries) ->
            val values = linkedMapOf<String, BackupValue>()
            entries.forEach { (key, element) ->
                values[key] = decodeValue(element)
                    ?: throw BackupFormatException("Unsupported value in backup: $name.$key")
            }
            files[name] = values
        }
        return files
    }

    private fun encodeValue(value: BackupValue): JsonElement = buildJsonObject {
        when (value) {
            is BackupValue.Bool -> {
                put("t", "b")
                put("v", value.value)
            }
            is BackupValue.IntVal -> {
                put("t", "i")
                put("v", value.value)
            }
            is BackupValue.LongVal -> {
                put("t", "l")
                put("v", value.value)
            }
            is BackupValue.FloatVal -> {
                put("t", "f")
                put("v", value.value)
            }
            is BackupValue.StringVal -> {
                put("t", "s")
                put("v", value.value)
            }
            is BackupValue.StringSet -> {
                put("t", "set")
                put("v", JsonArray(value.value.map { JsonPrimitive(it) }))
            }
        }
    }

    private fun decodeValue(element: JsonElement): BackupValue? {
        val entry = element as? JsonObject ?: return null
        val type = entry["t"]?.jsonPrimitive?.contentOrNull ?: return null
        val raw = entry["v"] ?: return null
        val primitive = raw.jsonPrimitive
        return when (type) {
            "b" -> BackupValue.Bool(primitive.booleanOrNull ?: return null)
            "i" -> BackupValue.IntVal(primitive.intOrNull ?: return null)
            "l" -> BackupValue.LongVal(primitive.longOrNull ?: return null)
            "f" -> BackupValue.FloatVal(primitive.doubleOrNull?.toFloat() ?: return null)
            "s" -> BackupValue.StringVal(primitive.contentOrNull ?: return null)
            "set" -> BackupValue.StringSet(
                (raw as? JsonArray)
                    ?.map { item -> item.jsonPrimitive.contentOrNull ?: return null }
                    ?.toSet()
                    ?: return null,
            )
            else -> null
        }
    }
}

internal object BackupRestoreRepository {
    val isSupported: Boolean get() = BackupRestoreStorage.isSupported

    fun createBackupText(): Result<String> = runCatching {
        BackupRestoreCodec.encode(
            files = BackupRestoreStorage.snapshot(),
            createdAtEpochMs = BackupRestoreStorage.currentTimeMs(),
        )
    }

    fun applyBackupText(text: String): Result<Unit> = runCatching {
        val files = BackupRestoreCodec.decode(text)
        BackupRestoreStorage.restore(files)
    }
}

internal expect object BackupRestoreStorage {
    val isSupported: Boolean

    fun currentTimeMs(): Long

    fun snapshot(): Map<String, Map<String, BackupValue>>

    /** Restores the given preference stores, skipping unknown ones. Returns the restored count. */
    fun restore(files: Map<String, Map<String, BackupValue>>): Int
}

@Composable
internal expect fun rememberBackupExportLauncher(onResult: (Result<Unit>) -> Unit): () -> Unit

@Composable
internal expect fun rememberBackupImportLauncher(onResult: (Result<Unit>) -> Unit): () -> Unit
