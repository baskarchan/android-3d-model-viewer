package com.example.modelviewer.ui



import android.content.Context
import android.util.Log
import android.view.Gravity
import android.view.MotionEvent
import android.view.ScaleGestureDetector
import android.view.ViewGroup
import android.widget.Button
import android.widget.FrameLayout
import android.widget.LinearLayout
import com.example.modelviewer.ModelAsset
import com.google.android.filament.Engine
import com.google.android.filament.gltfio.FilamentAsset
import com.example.modelviewer.engine.ModelAssetCache
import  com.example.modelviewer.engine.ModelController
import  com.example.modelviewer.glb.GlbExtrasParser
import com.example.modelviewer.glb.LabelDef

import io.github.sceneview.SceneView
import io.github.sceneview.node.ModelNode


class ModelContainerView(
    context: Context,
    private val asset: ModelAsset,
    private val engine: Engine,
    private val onClose: (ModelContainerView) -> Unit,
) : FrameLayout(context) {

    enum class Mode { NORMAL, INTERACTION }

    companion object {
        const val MIN_SIZE_PX = 300
        const val MAX_SIZE_PX = 1400
        const val DEFAULT_SIZE_PX = 700
    }

    private var mode = Mode.NORMAL
    private var labelsVisible = false

    private val sceneView: SceneView
    private val labelOverlay: LabelOverlayView
    private lateinit var controller: ModelController
    private var filamentAsset: FilamentAsset? = null
    private var nodeEntities: List<Pair<LabelDef, Int>> = emptyList()

    private var lastTouchX = 0f
    private var lastTouchY = 0f
    private var activePointerId = MotionEvent.INVALID_POINTER_ID

    private val scaleDetector = ScaleGestureDetector(
        context,
        object : ScaleGestureDetector.SimpleOnScaleGestureListener() {
            override fun onScale(detector: ScaleGestureDetector): Boolean {
                val factor = detector.scaleFactor
                if (mode == Mode.NORMAL) {
                    resizeBy(factor)
                } else if (::controller.isInitialized) {
                    controller.zoom(factor)
                    if (labelsVisible) reprojectLabels()
                }
                return true
            }
        },
    )

    private lateinit var interactionBtn: Button

    init {
        layoutParams = ViewGroup.LayoutParams(DEFAULT_SIZE_PX, DEFAULT_SIZE_PX)
        setBackgroundColor(0x33FFFFFF) // faint tint so empty/loading containers are visible

        sceneView = SceneView(context, sharedEngine = engine)
        addView(sceneView, ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)

        labelOverlay = LabelOverlayView(context)
        addView(labelOverlay, ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)

        addButtonsRow()
        loadModel()
    }

    private fun addButtonsRow() {
        val row = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            setBackgroundColor(0x73000000)
        }
        buttonsRow = row
        interactionBtn = Button(context).apply {
            text = "Move"
            setOnClickListener {
                mode = if (mode == Mode.NORMAL) Mode.INTERACTION else Mode.NORMAL
                text = if (mode == Mode.INTERACTION) "Rotate" else "Move"
            }
        }
        val labelBtn = Button(context).apply {
            text = "Labels"
            setOnClickListener {
                labelsVisible = !labelsVisible
                if (labelsVisible) reprojectLabels() else labelOverlay.clear()
            }
        }
        val closeBtn = Button(context).apply {
            text = "X"
            setOnClickListener {
                dispose()
                onClose(this@ModelContainerView)
            }
        }
        row.addView(interactionBtn)
        row.addView(labelBtn)
        row.addView(closeBtn)

        val lp = LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT).apply {
            gravity = Gravity.TOP or Gravity.END
        }
        addView(row, lp)
    }

    private fun loadModel() {
        val buffer = ModelAssetCache.bytesFor(context, asset)
        val instance = sceneView.modelLoader.createModelInstance(buffer)
        if (instance == null) {
            Log.e("ModelContainerView", "Failed to parse ${asset.assetPath} — not a valid .glb")
            return
        }
        val node = ModelNode(modelInstance = instance).apply {
            // Fit into a normalized cube so any container size shows the model
            // filling its bounds (spec 1.5) without per-model scale tuning.
            scaleToUnitCube()
            centerOrigin()
        }
        sceneView.addChildNode(node)

        filamentAsset = instance.asset
        controller = ModelController(engine = engine, camera = sceneView.cameraNode.camera)

        val labelDefs = GlbExtrasParser.parseLabelsFromAsset(context, asset.assetPath)
        nodeEntities = labelDefs.mapNotNull { def ->
            val entity = instance.asset.entities.firstOrNull { e -> instance.asset.getName(e) == def.nodeName }
            entity?.let { def to it }
        }
    }
    private lateinit var buttonsRow: LinearLayout   // keep a reference to it
    private val touchRect = android.graphics.Rect()
    private fun resizeBy(factor: Float) {
        val lp = layoutParams
        val newSize = (lp.width * factor).toInt().coerceIn(MIN_SIZE_PX, MAX_SIZE_PX)
        lp.width = newSize
        lp.height = newSize
        layoutParams = lp
        if (labelsVisible) reprojectLabels()
    }

    private fun reprojectLabels() {
        if (filamentAsset == null || !::controller.isInitialized) return
        val points = nodeEntities.mapNotNull { (def, entity) ->
            controller.projectToScreen(entity, width.toFloat(), height.toFloat())?.let { def to it }
        }
        labelOverlay.setLabels(points)
    }

    fun dispose() {
        // Frees this instance's own GPU resources. The shared Engine itself
        // is only torn down once, in MainActivity.onDestroy().
        sceneView.destroy()
    }

    // Always intercept here so the child SceneView never receives raw touch
    // events — this is what keeps our own gesture handling the single source
    // of truth and sidesteps needing to know SceneView's internal camera-
    // manipulator API at all (spec 1.7: modes must never mix).
    override fun onInterceptTouchEvent(ev: MotionEvent): Boolean {
        buttonsRow.getHitRect(touchRect)
        return !touchRect.contains(ev.x.toInt(), ev.y.toInt())
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        scaleDetector.onTouchEvent(event)

        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                lastTouchX = event.x
                lastTouchY = event.y
                activePointerId = event.getPointerId(0)
            }
            MotionEvent.ACTION_MOVE -> {
                if (!scaleDetector.isInProgress && event.pointerCount == 1) {
                    val idx = event.findPointerIndex(activePointerId)
                    if (idx != -1) {
                        val x = event.getX(idx)
                        val y = event.getY(idx)
                        val dx = x - lastTouchX
                        val dy = y - lastTouchY
                        if (mode == Mode.NORMAL) {
                            translationX += dx
                            translationY += dy
                        } else if (::controller.isInitialized) {
                            controller.rotate(dx, dy, width.toFloat())
                            if (labelsVisible) reprojectLabels()
                        }
                        lastTouchX = x
                        lastTouchY = y
                    }
                }
            }
            MotionEvent.ACTION_POINTER_DOWN -> {
                // Re-anchor the drag origin so adding/removing a second finger
                // doesn't cause a position jump on the next MOVE event.
                lastTouchX = event.getX(0)
                lastTouchY = event.getY(0)
            }
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                activePointerId = MotionEvent.INVALID_POINTER_ID
            }
        }
        return true
    }
}