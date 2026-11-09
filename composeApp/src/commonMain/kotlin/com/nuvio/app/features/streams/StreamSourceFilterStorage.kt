package com.nuvio.app.features.streams

/**
 * Persistence for stream source filter selections (quality/language chips).
 * The payload is a single JSON document: the last global selection plus the
 * per-title memory that auto-applies a remembered filter when a title's
 * streams are opened again.
 */
expect object StreamSourceFilterStorage {
    fun loadPayload(): String?
    fun savePayload(payload: String)
}
