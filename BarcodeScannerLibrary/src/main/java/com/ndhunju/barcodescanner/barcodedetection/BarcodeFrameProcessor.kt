package com.ndhunju.barcodescanner.barcodedetection

import android.graphics.Rect
import android.util.Log
import androidx.annotation.MainThread
import androidx.core.graphics.toRectF
import com.google.android.gms.tasks.Task
import com.ndhunju.barcode.InputInfo
import com.ndhunju.barcodescanner.camera.CameraReticleAnimator
import com.ndhunju.barcodescanner.camera.GraphicOverlayView
import com.ndhunju.barcodescanner.camera.WorkflowModel
import com.ndhunju.barcodescanner.camera.WorkflowModel.WorkflowState
import com.ndhunju.barcodescanner.camera.FrameProcessorBase
import com.ndhunju.barcodescanner.settings.Settings
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.common.InputImage
import java.io.IOException

/**
 * A processor to run the barcode detector.
 */
internal class BarcodeFrameProcessor(
    private val graphicOverlayView: GraphicOverlayView,
    private val workflowModel: WorkflowModel
    ) : FrameProcessorBase<List<Barcode>>() {

    private val scanner = BarcodeScanning.getClient()
    private val cameraReticleAnimator: CameraReticleAnimator = CameraReticleAnimator(
        graphicOverlayView
    )

    override fun detectInImage(image: InputImage): Task<List<Barcode>> =
        scanner.process(image)

    @MainThread
    override fun onSuccess(
        inputInfo: InputInfo,
        results: List<Barcode>,
        graphicOverlayView: GraphicOverlayView
    ) {

        if (workflowModel.uiStateFlow.value.isCameraLive.value.not()) return

        // Picks the barcode, if exists, that covers the center of graphic overlay.

        val barcodeInCenter = results.firstOrNull { barcode ->
            val boundingBox = barcode.boundingBox ?: return@firstOrNull false
            val box = graphicOverlayView.translateRect(boundingBox)
            val overlayRect = Rect()
            graphicOverlayView.getGlobalVisibleRect(overlayRect)
            box.intersect(overlayRect.toRectF())
        }

        graphicOverlayView.clear()

        if (barcodeInCenter == null) {
            graphicOverlayView.add(DetectingBarcodeGraphic(graphicOverlayView))
            cameraReticleAnimator.start()
            graphicOverlayView.add(BarcodeReticleGraphic(graphicOverlayView, cameraReticleAnimator))
            workflowModel.setWorkflowState(WorkflowState.DETECTING)
        } else {
            cameraReticleAnimator.cancel()

            val sizeProgress = Settings.getProgressToMeetBarcodeSizeRequirement(
                graphicOverlayView,
                barcodeInCenter
            )

            if (sizeProgress < 1) {
                // Barcode in the camera view is too small, so prompt user to move camera closer.
                graphicOverlayView.add(AssistConfirmBarcodeGraphic(
                    graphicOverlayView,
                    barcodeInCenter
                ))
                workflowModel.setWorkflowState(WorkflowState.CONFIRMING)
            } else {
                workflowModel.detectedBarcode.value = barcodeInCenter
                workflowModel.setWorkflowState(WorkflowState.DETECTED)
            }
        }

        graphicOverlayView.invalidate()
    }

    override fun onFailure(e: Exception) {
        Log.e(TAG, "Barcode detection failed!", e)
    }

    override fun stop() {
        super.stop()
        try {
            scanner.close()
        } catch (e: IOException) {
            Log.e(TAG, "Failed to close barcode detector!", e)
        }
    }

    companion object {
        private const val TAG = "BarcodeProcessor"
    }
}
