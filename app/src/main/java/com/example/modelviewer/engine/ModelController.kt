package com.example.modelviewer.engine



import android.graphics.PointF
import com.google.android.filament.Camera
import com.google.android.filament.Engine
import com.google.android.filament.TransformManager
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.sin


class ModelController(
    private val engine: Engine,
    private val camera: Camera,
) {
    private var yaw = 0f
    private var pitch = 0.35f
    private var distance = 3.0f

    private val minPitch = -PI.toFloat() / 2f + 0.05f
    private val maxPitch = PI.toFloat() / 2f - 0.05f
    private val minDistance = 0.6f
    private val maxDistance = 12f

    init {
        applyToCamera()
    }

    /** One-finger drag in interaction mode -> orbit rotation. */
    fun rotate(panXPx: Float, panYPx: Float, viewportWidthPx: Float) {
        val sensitivity = PI.toFloat() / max(viewportWidthPx, 1f)
        yaw -= panXPx * sensitivity
        pitch = (pitch + panYPx * sensitivity).coerceIn(minPitch, maxPitch)
        applyToCamera()
    }

    /** Two-finger pinch in interaction mode -> zoom (dolly), not container resize. */
    fun zoom(scaleFactor: Float) {
        distance = (distance / scaleFactor).coerceIn(minDistance, maxDistance)
        applyToCamera()
    }

    fun reset() {
        yaw = 0f
        pitch = 0.35f
        distance = 3.0f
        applyToCamera()
    }

    private fun applyToCamera() {
        val eyeX = distance * cos(pitch) * sin(yaw)
        val eyeY = distance * sin(pitch)
        val eyeZ = distance * cos(pitch) * cos(yaw)
        camera.lookAt(
            eyeX.toDouble(), eyeY.toDouble(), eyeZ.toDouble(),
            0.0, 0.0, 0.0,
            0.0, 1.0, 0.0,
        )
    }

    /**
     * Projects a node's current world position through this instance's camera
     * into container-local screen pixels. Returns null if behind the camera.
     */
    fun projectToScreen(
        nodeEntity: Int,
        viewportWidthPx: Float,
        viewportHeightPx: Float,
    ): PointF? {
        val tm: TransformManager = engine.transformManager
        val instance = tm.getInstance(nodeEntity)
        if (instance == 0) return null

        val worldTransform = FloatArray(16)
        tm.getWorldTransform(instance, worldTransform)
        val wx = worldTransform[12]
        val wy = worldTransform[13]
        val wz = worldTransform[14]

        val clip = multiplyViewProjection(wx, wy, wz)
        if (clip[3] <= 0f) return null

        val ndcX = clip[0] / clip[3]
        val ndcY = clip[1] / clip[3]
        val screenX = (ndcX * 0.5f + 0.5f) * viewportWidthPx
        val screenY = (1f - (ndcY * 0.5f + 0.5f)) * viewportHeightPx
        return PointF(screenX, screenY)
    }

    private fun multiplyViewProjection(wx: Float, wy: Float, wz: Float): FloatArray {
        val view = DoubleArray(16)
        val proj = DoubleArray(16)
        camera.getViewMatrix(view)
        camera.getProjectionMatrix(proj)

        val vx = FloatArray(4)
        for (row in 0 until 4) {
            vx[row] = view[row + 0].toFloat() * wx +
                    view[row + 4].toFloat() * wy +
                    view[row + 8].toFloat() * wz +
                    view[row + 12].toFloat()
        }
        val out = FloatArray(4)
        for (row in 0 until 4) {
            out[row] = proj[row + 0].toFloat() * vx[0] +
                    proj[row + 4].toFloat() * vx[1] +
                    proj[row + 8].toFloat() * vx[2] +
                    proj[row + 12].toFloat() * vx[3]
        }
        return out
    }
}