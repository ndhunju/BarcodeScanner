package com.ndhunju.barcodescannerapp

import android.app.AlertDialog
import android.content.ClipData
import android.content.ClipboardManager
import android.os.Bundle
import android.widget.Toast
import androidx.activity.viewModels
import androidx.annotation.UiThread
import androidx.lifecycle.lifecycleScope
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
    private fun setUiForProcessing() {
        applyLoadingGraphics()
        uiState.setPromptText(getString(R.string.screen_demo_processing))
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

    /**
     * Barcode is detected with value, [rawValue]
     */
    override fun onBarcodeRawValue(rawValue: String) {
        //super.onBarcodeRawValue(rawValue)
        /** Process the detected bar code */
        lifecycleScope.launch {
            setUiForProcessing()
            viewModel.processBarcodeValue(rawValue).collect { isSuccess ->
                if (isSuccess) {
                    setUiForScanComplete()
                    showDialog(rawValue)
                } else {
                    showError(getString(R.string.screen_demo_invalid_qr_code))
                    // Delay for 3 secs before updating the UI so that
                    // user has enough time to see the error message
                    delay(3000)
                    // Resume scanner
                    resumeScanner()
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
            .setNegativeButton(android.R.string.ok) { _, _ -> setUiForInitialState() }
            .setPositiveButton(android.R.string.copy) { _, _ -> copyToClipboard(message) }
            .setOnDismissListener { resumeScanner() }
            .show()
    }

    /**
     * Copies [text] to the clipboard and shows a [Toast] confirming that it has been copied.
     */
    private fun copyToClipboard(text: CharSequence) {
        val clip = ClipData.newPlainText(getString(R.string.app_name), text)
        val clipboardManager = getSystemService(CLIPBOARD_SERVICE) as ClipboardManager
        clipboardManager.setPrimaryClip(clip)
        showToast(getString(R.string.screen_demo_toast_msg_copied))
    }

    /**
     * Shows a [Toast] with [message]
     */
    private fun showToast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }

}