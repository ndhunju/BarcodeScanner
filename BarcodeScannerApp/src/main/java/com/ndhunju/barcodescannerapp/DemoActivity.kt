package com.ndhunju.barcodescannerapp

import android.app.AlertDialog
import android.content.DialogInterface
import android.os.Bundle
import androidx.activity.viewModels
import androidx.annotation.UiThread
import androidx.lifecycle.lifecycleScope
import com.ndhunju.barcodescanner.camera.WorkflowModel.WorkflowState
import com.ndhunju.barcodescanner.ui.scanner.BarcodeScannerActivity
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class DemoActivity: BarcodeScannerActivity() {

    private val viewModel: ViewModel by viewModels()

    override fun onPostCreate(savedInstanceState: Bundle?) {
        super.onPostCreate(savedInstanceState)
        setUiForInitialState()
    }

    /**
     *  Renders UI to the state where app is actively searching for barcode
     */
    private fun setUiForInitialState() {
        uiState.apply {
            setTitleText(getString(R.string.screen_demo_title))
            setDescriptionText(getString(R.string.screen_demo_description))
            setFlashOn(false)
        }
    }

    /**
     *  Renders UI to indicate that app is processing the detected a barcode
     */
    private fun setUiForScanning() {
        applyLoadingGraphics()
        uiState.setPromptText(getString(R.string.screen_demo_scanning))
    }

    /**
     *  Renders UI to indicate that app has finished processing the barcode.
     */
    @UiThread
    private fun setUiForScanComplete() {
        applySuccessGraphics()
        uiState.setPromptText(getString(R.string.screen_demo_scan_complete))
    }

    /**
     *  Renders UI to indicate that there was an error processing the barcode.
     */
    private fun showError(message: String) {
        applyFailureGraphics()
        uiState.setPromptText(message)
    }

    override fun onBarcodeRawValue(rawValue: String) {
        //super.onBarcodeRawValue(rawValue)
        /** Process the detected bar code */
        lifecycleScope.launch {
            setUiForScanning()
            viewModel.processBarcodeValue(rawValue).collect { isSuccess ->
                if (isSuccess) {
                    setWorkflowState(WorkflowState.DETECTED)
                    setUiForScanComplete()
                    showDialog(rawValue)
                } else {
                    showError(getString(R.string.screen_demo_invalid_qr_code))
                    // Delay for 3 secs before updating the UI so that
                    // user has enough time to see the error message
                    delay(3000)
                    // Set state back to DETECTING where user can continue with new barcode
                    setWorkflowState(WorkflowState.DETECTING)
                }
            }
        }
    }

    /**
     * Shows dialog with [message]
     */
    private fun showDialog(message: String) {
        AlertDialog.Builder(this)
            .setMessage(message)
            .setPositiveButton(android.R.string.ok) { dialog, _ -> dialog.dismiss() }
            .show()
    }

}