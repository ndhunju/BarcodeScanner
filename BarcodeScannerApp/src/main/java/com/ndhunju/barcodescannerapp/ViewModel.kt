package com.ndhunju.barcodescannerapp

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.flow

class ViewModel: ViewModel() {

    fun processBarcodeValue(barcode: String) = flow {
        // Validated the barcode
        if (barcode.isEmpty()) {
            println("Barcode value is invalid for our use case! -> $barcode")
            emit(false)
        }

        // Make API call to get additional info on barcode
        delay(3000)

        // Return the result
        emit(true)
    }
}