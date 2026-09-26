package com.example.modelviewer

import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

import java.util.UUID

enum class Mode { NORMAL, INTERACTION }

const val MIN_CONTAINER_SIZE_DP = 90
const val MAX_CONTAINER_SIZE_DP = 420
const val DEFAULT_CONTAINER_SIZE_DP = 220

/**
 * One model instance placed on the canvas. Position/size are plain mutableState
 * so dragging/resizing only triggers layout, not a full screen recomposition.
 */
class PlacedModel(
    val asset: ModelAsset,
    startX: Float,
    startY: Float,
) {
    val id: String = UUID.randomUUID().toString()

    val offsetX = mutableFloatStateOf(startX)
    val offsetY = mutableFloatStateOf(startY)
    val sizeDp = mutableStateOf<Dp>(DEFAULT_CONTAINER_SIZE_DP.dp)

    val mode = mutableStateOf(Mode.NORMAL)
    val labelsVisible = mutableStateOf(false) // hidden on load, per spec 1.6/2.2

    /** Bumped whenever the camera changes (interaction mode) so the label overlay
     *  knows to recompute projected positions. Avoids a free-running per-frame loop
     *  for containers that are idle. */
    val cameraRevision = mutableFloatStateOf(0f)
}
