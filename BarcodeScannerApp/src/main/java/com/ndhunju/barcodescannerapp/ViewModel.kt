package com.ndhunju.barcodescannerapp

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.withContext

class ViewModel: ViewModel() {

    fun processBarcodeValue(barcode: String) = flow {
        // Validated the barcode
        if (barcode.isEmpty()) {
            println("Barcode value is invalid for our use case! -> $barcode")
            emit(false)
        }

        withContext(Dispatchers.IO) {
            // Make API call to get additional info on barcode
            delay(3000)
        }

        // Return the result
        emit(true)
    }
}