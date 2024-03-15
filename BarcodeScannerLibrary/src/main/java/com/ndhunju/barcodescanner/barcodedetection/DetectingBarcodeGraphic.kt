package com.ndhunju.barcodescanner.barcodedetection

import android.graphics.Paint
import com.ndhunju.barcodescanner.toColorIntWithOpacity
import com.ndhunju.barcodescanner.R
import com.ndhunju.barcodescanner.camera.GraphicOverlayView

class DetectingBarcodeGraphic(overlay: GraphicOverlayView) : BarcodeGraphicBase(overlay) {

    override var boxBorderPaint: Paint = Paint().apply {
        color = "#000000".toColorIntWithOpacity(50)
        style = Paint.Style.STROKE
        strokeWidth = context.resources.getDimensionPixelOffset(
            R.dimen.barcode_reticle_stroke_width
        ).toFloat()
    }

}