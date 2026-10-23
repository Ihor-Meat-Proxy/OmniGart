package com.omnigart.shared.content

import android.content.res.AssetManager

/**
 * Reads the offline content bundle from Android / Wear `assets/`
 * (typically `generated/content-bundle` mounted at the assets root).
 */
class AssetContentStore(private val assets: AssetManager, private val rootPrefix: String = "") : ContentStore {
    private fun full(path: String): String {
        val trimmed = path.trim('/')
        val root = rootPrefix.trim('/')
        return if (root.isEmpty()) trimmed else "$root/$trimmed"
    }

    override fun exists(relativePath: String): Boolean = try {
        assets.open(full(relativePath)).close()
        true
    } catch (_: Exception) {
        false
    }

    override fun readBytes(relativePath: String): ByteArray? = try {
        assets.open(full(relativePath)).use { it.readBytes() }
    } catch (_: Exception) {
        null
    }

    override fun list(prefix: String): List<String> {
        val start = full(prefix).trim('/')
        val out = ArrayList<String>()
        fun walk(dir: String) {
            val children =
                try {
                    assets.list(dir) ?: emptyArray()
                } catch (_: Exception) {
                    emptyArray()
                }
            if (children.isEmpty()) {
                // Might be a file — callers use exists/read for files.
                return
            }
            for (name in children.sorted()) {
                val child = if (dir.isEmpty()) name else "$dir/$name"
                val grand =
                    try {
                        assets.list(child)
                    } catch (_: Exception) {
                        null
                    }
                if (grand == null || grand.isEmpty()) {
                    out.add(child.removePrefix(rootPrefix.trim('/')).trimStart('/'))
                } else {
                    walk(child)
                }
            }
        }
        walk(start)
        return out.sorted()
    }
}
