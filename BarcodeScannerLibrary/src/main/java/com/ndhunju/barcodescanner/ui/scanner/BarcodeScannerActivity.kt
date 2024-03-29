package com.ndhunju.barcodescanner.ui.scanner

import android.Manifest
import android.animation.ValueAnimator
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarVisuals
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.Observer
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.google.mlkit.vision.barcode.common.Barcode
import com.ndhunju.barcodescanner.R
import com.ndhunju.barcodescanner.barcodedetection.BarcodeFrameProcessor
import com.ndhunju.barcodescanner.barcodedetection.FailureBarcodeGraphic
import com.ndhunju.barcodescanner.barcodedetection.LoadingBarcodeGraphic
import com.ndhunju.barcodescanner.barcodedetection.SuccessBarcodeGraphic
import com.ndhunju.barcodescanner.camera.CameraSource
import com.ndhunju.barcodescanner.camera.GraphicOverlayView
import com.ndhunju.barcodescanner.camera.WorkflowModel
import com.ndhunju.barcodescanner.camera.WorkflowModel.WorkflowState
import com.ndhunju.barcodescanner.isPermissionGranted
import com.ndhunju.barcodescanner.requestPermission
import java.io.IOException

/**
 * Activity that lays essential elements for scanning bar codes
 */
abstract class BarcodeScannerActivity : FragmentActivity() {

    // Private Variables
    private var graphicOverlayView: GraphicOverlayView? = null
    private lateinit var workflowModel: WorkflowModel
    private var currentWorkflowState = WorkflowState.NOT_STARTED

    // Exposed Variables
    var uiState: UiState
        get() { return workflowModel.uiStateFlow.value }
        set(value) { workflowModel.updateUiState(value) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        workflowModel = viewModels<WorkflowModel>().value

        setContent {
            BarcodeScannerScreen(
                uiState = workflowModel.uiStateFlow.collectAsStateWithLifecycle(),
                onGraphicLayerInitialized = { view -> onGraphicLayerInitialized(view)},
                onClickGraphicOverlay = { /* Do nothing for now*/ },
                onClickCloseIcon = { onBackPressedDispatcher.onBackPressed() },
                onClickFlashIcon = { workflowModel.uiStateFlow.value.toggleFlash() }
            )
        }
    }

    private fun onGraphicLayerInitialized(graphicOverlayView: GraphicOverlayView) {
        this.graphicOverlayView = graphicOverlayView
        setUpWorkflowModel(graphicOverlayView)
        resumeScanner()
    }

    override fun onResume() {
        super.onResume()
        setupIfPermissionsGranted()
    }

    private fun setupIfPermissionsGranted() {

        if (isPermissionGranted(Manifest.permission.CAMERA).not()) {
            requestPermission(REQUEST_CAMERA_PERMISSION, Manifest.permission.CAMERA)
            return
        }

        workflowModel.markCameraFrozen()
        currentWorkflowState = WorkflowState.NOT_STARTED
    }

    override fun onPause() {
        super.onPause()
        currentWorkflowState = WorkflowState.NOT_STARTED
        stopCameraPreview()
    }

    override fun onDestroy() {
        super.onDestroy()
        workflowModel.releaseCameraSource()
        workflowModel.cameraSource = null
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        if (requestCode == REQUEST_CAMERA_PERMISSION) {
            if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                setupIfPermissionsGranted()
            } else {
                finish()
            }
        } else {
            super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        }
    }

    fun resumeScanner() {
        workflowModel.setWorkflowState(WorkflowState.DETECTING)
    }

    private fun startCameraPreview(graphicOverlayView: GraphicOverlayView) {
        if (workflowModel.isCameraLive.not()) {
            try {
                graphicOverlayView.clear()
                workflowModel.cameraSource = (CameraSource(graphicOverlayView).apply {
                    setFrameProcessor(BarcodeFrameProcessor(graphicOverlayView, workflowModel))
                })
                workflowModel.isCameraLive = true
                uiState.setPromptText(null)
            } catch (e: IOException) {
                Log.e(TAG, "Failed to start camera preview!", e)
                workflowModel.cameraSource?.release()
                workflowModel.cameraSource = null
                workflowModel.isCameraLive = false
            }
        }

    }

    private fun stopCameraPreview() {
        if (workflowModel.isCameraLive) {
            workflowModel.markCameraFrozen()
            workflowModel.isFlashOn = false
            workflowModel.cameraSource = null
        }
    }

    private fun setUpWorkflowModel(graphicOverlayView: GraphicOverlayView) {
        // Observes the workflow state changes, if happens,
        // update the overlay view indicators and camera preview state.
        workflowModel.workflowState.observe(this, Observer { workflowState ->
            if (workflowState == null || currentWorkflowState == workflowState) {
                return@Observer
            }

            currentWorkflowState = workflowState
            Log.d(TAG, "Current workflow state: ${currentWorkflowState.name}")

            when (workflowState) {
                WorkflowState.DETECTING -> { startCameraPreview(graphicOverlayView) }
                // It becomes CONFIRMING when Barcode is not centered
                WorkflowState.CONFIRMING -> {
                    workflowModel.uiStateFlow.value
                        .setPromptText(getString(R.string.barcode_prompt_move_camera_closer))
                    startCameraPreview(graphicOverlayView)
                }
                WorkflowState.DETECTED -> { stopCameraPreview() }
                else -> {
                    workflowModel.uiStateFlow.value
                        .setPromptText(getString(R.string.activity_barcode_workflow_unknown_state))
                }
            }
        })

        workflowModel.detectedBarcode.observe(this) { barcode ->
            onBarcodeDetected(barcode)
        }
    }

    private val loadingAnimator: ValueAnimator by lazy {
        ValueAnimator.ofFloat(0f, 1f).apply {
            repeatMode = ValueAnimator.RESTART
            repeatCount = ValueAnimator.INFINITE
            duration = 2000
            addUpdateListener {
                graphicOverlayView?.invalidate()
            }
        }
    }

    protected fun applyLoadingGraphics() {
        loadingAnimator.start()
        graphicOverlayView?.apply {
            clear()
            add(LoadingBarcodeGraphic(this, loadingAnimator))
        }
    }

    protected fun applySuccessGraphics() {
        graphicOverlayView?.apply {
            clear()
            add(SuccessBarcodeGraphic(this))
        }
    }

    protected fun applyFailureGraphics() {
        graphicOverlayView?.apply {
            clear()
            add(FailureBarcodeGraphic(this))
        }
    }

    private fun onBarcodeDetected(barcode: Barcode) {
        barcode.rawValue?.let { onBarcodeRawValue(it) }
    }

    protected open fun onBarcodeRawValue(rawValue: String) {
        // Show barcode's value on a snack bar
        showSnackBar(rawValue)
    }

    fun showSnackBar(rawValue: String, duration: SnackbarDuration = SnackbarDuration.Short) {
        workflowModel.setSnackBarVisuals(object : SnackbarVisuals {
            override val actionLabel: String?
                get() = null
            override val duration: SnackbarDuration
                get() = duration
            override val message: String
                get() = rawValue
            override val withDismissAction: Boolean
                get() = true

        })
    }

    companion object {
        private val TAG = BarcodeScannerActivity::class.simpleName
        private const val REQUEST_CAMERA_PERMISSION = 1
    }
}
