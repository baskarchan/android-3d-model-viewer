package com.example.modelviewer.glb

import android.content.Context
import org.json.JSONObject
import java.nio.ByteBuffer
import java.nio.ByteOrder


data class LabelDef(
    val nodeIndex: Int,
    val nodeName: String,
    val text: String,
)


object GlbExtrasParser {

    private const val MAGIC = 0x46546C67 // "glTF"
    private const val CHUNK_TYPE_JSON = 0x4E4F534A // "JSON"

    fun parseLabelsFromAsset(context: Context, assetPath: String): List<LabelDef> {
        context.assets.open(assetPath).use { input ->
            val bytes = input.readBytes()
            return parseLabels(bytes)
        }
    }

    fun parseLabels(bytes: ByteArray): List<LabelDef> {
        val buf = ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN)
        if (buf.remaining() < 12) return emptyList()

        val magic = buf.int
        if (magic != MAGIC) return emptyList() // not a valid .glb, or already plain .gltf JSON
        buf.int // version, unused
        buf.int // total length, unused

        if (buf.remaining() < 8) return emptyList()
        val chunkLength = buf.int
        val chunkType = buf.int
        if (chunkType != CHUNK_TYPE_JSON) return emptyList()
        if (buf.remaining() < chunkLength) return emptyList()

        val jsonBytes = ByteArray(chunkLength)
        buf.get(jsonBytes)
        val json = String(jsonBytes, Charsets.UTF_8)

        return try {
            extractLabels(JSONObject(json))
        } catch (e: Exception) {
            emptyList()
        }
    }

    private fun extractLabels(root: JSONObject): List<LabelDef> {
        val nodes = root.optJSONArray("nodes") ?: return emptyList()
        val result = mutableListOf<LabelDef>()
        for (i in 0 until nodes.length()) {
            val node = nodes.optJSONObject(i) ?: continue
            val extras = node.optJSONObject("extras") ?: continue
            val prop = extras.optString("prop", "")
            if (prop.isNotBlank()) {
                val name = node.optString("name", "node_$i")
                result.add(LabelDef(nodeIndex = i, nodeName = name, text = prop))
            }
        }
        return result
    }
}
