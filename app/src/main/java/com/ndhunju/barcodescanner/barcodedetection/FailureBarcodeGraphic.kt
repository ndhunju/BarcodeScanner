package com.ndhunju.barcodescanner.barcodedetection

import android.graphics.Paint
import androidx.core.graphics.toColorInt
import com.ndhunju.barcode.R
import com.ndhunju.barcode.camera.GraphicOverlay
import com.ndhunju.barcodescanner.barcodedetection.BarcodeGraphicBase

class FailureBarcodeGraphic(overlay: GraphicOverlay) : BarcodeGraphicBase(overlay) {

    override var boxBorderPaint: Paint = Paint().apply {
        color = "#FF0000".toColorInt()
        style = Paint.Style.STROKE
        strokeWidth = context.resources.getDimensionPixelOffset(
            R.dimen.barcode_reticle_stroke_width
        ).toFloat()
    }

}