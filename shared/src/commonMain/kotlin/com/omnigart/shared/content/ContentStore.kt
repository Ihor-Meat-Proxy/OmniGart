package com.omnigart.shared.content

/**
 * Byte-oriented content store. Paths are pack-relative POSIX paths
 * (e.g. `exercises/data/back-squat.json`, `manifest.json`).
 */
interface ContentStore {
    fun exists(relativePath: String): Boolean

    fun readBytes(relativePath: String): ByteArray?

    fun readText(relativePath: String): String? =
        readBytes(relativePath)?.decodeToString()

    /** List file paths under [prefix] (inclusive of nested files). */
    fun list(prefix: String = ""): List<String>
}

/** In-memory store for tests and for verified update packs loaded into RAM/disk cache. */
class MapContentStore(
    private val files: Map<String, ByteArray>
) : ContentStore {
    override fun exists(relativePath: String): Boolean = relativePath in files

    override fun readBytes(relativePath: String): ByteArray? = files[relativePath]

    override fun list(prefix: String): List<String> {
        val norm = prefix.trim('/')
        if (norm.isEmpty()) return files.keys.sorted()
        return files.keys.filter { it == norm || it.startsWith("$norm/") }.sorted()
    }

    companion object {
        fun ofText(entries: Map<String, String>): MapContentStore =
            MapContentStore(entries.mapValues { it.value.encodeToByteArray() })
    }
}
