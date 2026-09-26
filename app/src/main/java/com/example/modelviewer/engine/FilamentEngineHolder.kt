package com.example.modelviewer.engine

import com.google.android.filament.Engine
import com.google.android.filament.EntityManager
import com.google.android.filament.Filament
import com.google.android.filament.gltfio.AssetLoader
import com.google.android.filament.gltfio.ResourceLoader
import com.google.android.filament.gltfio.UbershaderProvider


object FilamentEngineHolder {

    private var _engine: Engine? = null
    val engine: Engine
        get() = _engine ?: run {
            Filament.init()          // loads libfilament-jni.so — must happen before any Engine/Scene/etc. use
            Engine.create()
        }.also { _engine = it }

    private var _assetLoader: AssetLoader? = null
    val assetLoader: AssetLoader
        get() = _assetLoader ?: AssetLoader(
            engine,
            UbershaderProvider(engine),
            /* entityManager = */ EntityManager.get()
        ).also { _assetLoader = it }

    private var _resourceLoader: ResourceLoader? = null
    val resourceLoader: ResourceLoader
        get() = _resourceLoader ?: ResourceLoader(engine).also { _resourceLoader = it }

    // NOTE: lighting/environment (IBL) is created per-Scene via rememberEnvironmentLoader
    // in ModelContainer using this SAME shared `engine`, so the expensive native Engine
    // itself is still only created once; only the lightweight environment/indirect-light
    // object is per-instance. If profiling shows that's still too costly with 5 models
    // loaded, hoist a single shared Environment here as well and pass it into every Scene.

    fun destroy() {
        _resourceLoader?.destroy()
        _assetLoader?.destroy()
        _engine?.destroy()
        _resourceLoader = null
        _assetLoader = null
        _engine = null
    }
}
