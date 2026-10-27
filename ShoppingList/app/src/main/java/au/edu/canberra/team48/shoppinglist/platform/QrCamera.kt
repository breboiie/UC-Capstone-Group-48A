package au.edu.canberra.team48.shoppinglist.platform

import android.content.Context
import android.util.Log
import androidx.annotation.OptIn
import androidx.camera.core.CameraSelector
import androidx.camera.core.ExperimentalGetImage
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner
import com.google.mlkit.vision.barcode.BarcodeScanner
import com.google.mlkit.vision.barcode.BarcodeScannerOptions
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.common.InputImage

/**
 * The QR scanner on the Scan screen, inside the app: CameraX shows the camera and
 * ML Kit's barcode model (bundled with the app, so nothing has to download first)
 * reads the code. Replaces Google's separate scanner, which could not open on
 * phones where its module was missing or still downloading.
 */
class QrCamera(private val context: Context) {
    private var provider: ProcessCameraProvider? = null
    private val scanner: BarcodeScanner = BarcodeScanning.getClient(
        BarcodeScannerOptions.Builder().setBarcodeFormats(Barcode.FORMAT_QR_CODE).build()
    )
    private var lastCode: String? = null

    /**
     * Starts the camera into [view]. [ready] says whether a code should be acted on now
     * (not while a pop-up is open or a code is being checked). [onCode] gets each new code.
     */
    fun start(owner: LifecycleOwner, view: PreviewView, ready: () -> Boolean, onCode: (String) -> Unit, onError: () -> Unit) {
        val future = ProcessCameraProvider.getInstance(context)
        future.addListener({
            try {
                val p = future.get()
                provider = p
                val preview = Preview.Builder().build().also { it.setSurfaceProvider(view.surfaceProvider) }
                val analysis = ImageAnalysis.Builder()
                    .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                    .build()
                analysis.setAnalyzer(ContextCompat.getMainExecutor(context)) { proxy -> analyse(proxy, ready, onCode) }
                p.unbindAll()
                p.bindToLifecycle(owner, CameraSelector.DEFAULT_BACK_CAMERA, preview, analysis)
            } catch (e: Exception) {
                Log.w("ShoppingList", "Camera could not start", e)
                onError()
            }
        }, ContextCompat.getMainExecutor(context))
    }

    @OptIn(ExperimentalGetImage::class)
    private fun analyse(proxy: ImageProxy, ready: () -> Boolean, onCode: (String) -> Unit) {
        val media = proxy.image
        if (media == null || !ready()) {
            proxy.close()
            return
        }
        scanner.process(InputImage.fromMediaImage(media, proxy.imageInfo.rotationDegrees))
            .addOnSuccessListener { codes ->
                val raw = codes.firstNotNullOfOrNull { it.rawValue } ?: return@addOnSuccessListener
                // A code stays in view: act on it once. After "Cancel" or "not a list code" the
                // same code is ignored until another one is seen or the scan screen is opened again.
                if (raw == lastCode) return@addOnSuccessListener
                lastCode = raw
                if (ready()) onCode(raw)
            }
            .addOnCompleteListener { proxy.close() }
    }

    fun stop() {
        runCatching { provider?.unbindAll() }
        runCatching { scanner.close() }
    }
}
