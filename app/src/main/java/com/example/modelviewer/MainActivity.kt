package com.example.modelviewer

import android.os.Bundle
import android.widget.FrameLayout
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.example.modelviewer.engine.FilamentEngineHolder
import com.example.modelviewer.ui.ModelContainerView
import com.google.android.material.floatingactionbutton.FloatingActionButton

class MainActivity : AppCompatActivity() {
    private lateinit var canvas: FrameLayout
    private var spawnIndex = 0
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)
        canvas = findViewById(R.id.canvas)
        findViewById<FloatingActionButton>(R.id.fabAdd).setOnClickListener {
            showAddModelDialog()
        }




    }

    private fun showAddModelDialog() {
        val assets = ModelAsset.entries.toTypedArray()
        val names = assets.map { it.displayName }.toTypedArray()
        AlertDialog.Builder(this)
            .setTitle("Add a model")
            .setItems(names) { _, which -> addModel(assets[which]) }
            .show()
    }

    private fun addModel(asset: ModelAsset) {
        val container = ModelContainerView(
            context = this,
            asset = asset,
            engine = FilamentEngineHolder.engine,
            onClose = { view -> canvas.removeView(view) },
        )
        // cascade spawns so repeated adds don't stack exactly on top of each other
        val offset = (spawnIndex % 5) * 60
        val lp = FrameLayout.LayoutParams(
            ModelContainerView.DEFAULT_SIZE_PX,
            ModelContainerView.DEFAULT_SIZE_PX,
        ).apply {
            leftMargin = 60 + offset
            topMargin = 200 + offset
        }
        canvas.addView(container, lp)
        spawnIndex++
    }
}