package com.ndhunju.barcodescanner.camera

import android.os.SystemClock
import androidx.annotation.GuardedBy
import com.ndhunju.barcode.CameraInputInfo
import com.ndhunju.barcode.InputInfo
import com.ndhunju.barcodescanner.ScopedExecutor
import com.google.android.gms.tasks.Task
import com.google.android.gms.tasks.TaskExecutors
import com.google.mlkit.vision.common.InputImage
import java.nio.ByteBuffer

/**
 * Abstract base class of [FrameProcessor].
 */
abstract class FrameProcessorBase<T> : FrameProcessor {

    // To keep the latest frame and its metadata.
    @GuardedBy("this")
    private var latestFrame: ByteBuffer? = null

    @GuardedBy("this")
    private var latestFrameMetaData: FrameMetadata? = null

    // To keep the frame and metadata in process.
    @GuardedBy("this")
    private var processingFrame: ByteBuffer? = null

    @GuardedBy("this")
    private var processingFrameMetaData: FrameMetadata? = null
    private val executor = ScopedExecutor(TaskExecutors.MAIN_THREAD)

    @Synchronized
    override fun process(
        data: ByteBuffer,
        frameMetadata: FrameMetadata,
        graphicOverlayView: GraphicOverlayView
    ) {
        latestFrame = data
        latestFrameMetaData = frameMetadata
        if (processingFrame == null && processingFrameMetaData == null) {
            processLatestFrame(graphicOverlayView)
        }
    }

    @Synchronized
    private fun processLatestFrame(graphicOverlayView: GraphicOverlayView) {
        processingFrame = latestFrame
        processingFrameMetaData = latestFrameMetaData
        latestFrame = null
        latestFrameMetaData = null
        val frame = processingFrame ?: return
        val frameMetaData = processingFrameMetaData ?: return
        val image = InputImage.fromByteBuffer(
            frame,
            frameMetaData.width,
            frameMetaData.height,
            frameMetaData.rotation,
            InputImage.IMAGE_FORMAT_NV21
        )
        val startMs = SystemClock.elapsedRealtime()
        detectInImage(image).addOnSuccessListener(executor
        ) { results: T ->
            //Log.d(TAG, "Latency is: ${SystemClock.elapsedRealtime() - startMs}")
            this@FrameProcessorBase.onSuccess(
                CameraInputInfo(frame, frameMetaData),
                results,
                graphicOverlayView
            )
            processLatestFrame(graphicOverlayView)

        }.addOnFailureListener(executor
        ) {
            this@FrameProcessorBase.onFailure(it)
        }
    }

    override fun stop() {
        executor.shutdown()
    }

    protected abstract fun detectInImage(image: InputImage): Task<T>

    /**
     * Called when the detection succeeds.
     */
    protected abstract fun onSuccess(
        inputInfo: InputInfo,
        results: T,
        graphicOverlayView: GraphicOverlayView
    )

    protected abstract fun onFailure(e: Exception)

    companion object {
        private const val TAG = "FrameProcessorBase"
    }
}
