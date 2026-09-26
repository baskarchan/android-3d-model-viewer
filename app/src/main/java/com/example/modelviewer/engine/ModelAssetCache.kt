package com.example.modelviewer.engine

import android.content.Context
import android.util.LruCache
import com.example.modelviewer.ModelAsset

import java.nio.ByteBuffer


object ModelAssetCache {

    private val byteCache = LruCache<String, ByteBuffer>(5)

    fun bytesFor(context: Context, asset: ModelAsset): ByteBuffer {
        byteCache.get(asset.assetPath)?.let { return it.duplicate() }

        val raw = context.assets.open(asset.assetPath).use { it.readBytes() }
        val buffer = ByteBuffer.allocateDirect(raw.size).apply {
            put(raw)
            rewind()
        }
        byteCache.put(asset.assetPath, buffer)
        return buffer.duplicate()
    }

    fun clear() = byteCache.evictAll()
}
