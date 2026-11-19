package com.enmc.odoo_movil

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.media.AudioManager
import android.media.ToneGenerator
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.annotation.OptIn
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.content.ContextCompat
import com.enmc.odoo_movil.databinding.ActivityQrScannerBinding
import com.enmc.odoo_movil.network.SessionManager
import com.google.mlkit.vision.barcode.BarcodeScanner
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.common.InputImage
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

class QrScannerActivity : AppCompatActivity() {

    private lateinit var binding: ActivityQrScannerBinding
    private lateinit var cameraExecutor: ExecutorService
    private lateinit var barcodeScanner: BarcodeScanner
    private lateinit var sessionManager: SessionManager
    private var isProcessing = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityQrScannerBinding.inflate(layoutInflater)
        setContentView(binding.root)

        sessionManager = SessionManager(this)
        cameraExecutor = Executors.newSingleThreadExecutor()
        barcodeScanner = BarcodeScanning.getClient()

        startCamera()

        binding.btnCancel.setOnClickListener {
            finish()
        }
    }

    private fun startCamera() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(this)

        cameraProviderFuture.addListener({
            val cameraProvider = cameraProviderFuture.get()

            val preview = Preview.Builder().build().also {
                it.setSurfaceProvider(binding.previewView.surfaceProvider)
            }

            val imageAnalysis = ImageAnalysis.Builder()
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                .build()
                .also {
                    it.setAnalyzer(cameraExecutor) { imageProxy ->
                        processImageProxy(imageProxy)
                    }
                }

            try {
                cameraProvider.unbindAll()
                cameraProvider.bindToLifecycle(this, CameraSelector.DEFAULT_BACK_CAMERA, preview, imageAnalysis)
            } catch (e: Exception) {
                Log.e("QR_SCAN", "Error al iniciar cámara", e)
            }

        }, ContextCompat.getMainExecutor(this))
    }

    @OptIn(ExperimentalGetImage::class)
    private fun processImageProxy(imageProxy: ImageProxy) {
        if (isProcessing) {
            imageProxy.close()
            return
        }

        val mediaImage = imageProxy.image
        if (mediaImage != null) {
            val image = InputImage.fromMediaImage(mediaImage, imageProxy.imageInfo.rotationDegrees)
            barcodeScanner.process(image)
                .addOnSuccessListener { barcodes ->
                    if (barcodes.isNotEmpty()) {
                        val barcode = barcodes[0]
                        val rawValue = barcode.rawValue
                        val secret = sessionManager.getQrSecret()
                        
                        if (rawValue == secret) {
                            isProcessing = true
                            playFeedback(true)
                            val resultIntent = Intent()
                            resultIntent.putExtra("SCAN_RESULT", rawValue)
                            setResult(Activity.RESULT_OK, resultIntent)
                            
                            binding.previewView.postDelayed({
                                finish()
                            }, 500)
                        } else if (rawValue != null) {
                            isProcessing = true 
                            playFeedback(false)
                            runOnUiThread {
                                Toast.makeText(this, "QR no válido para esta empresa", Toast.LENGTH_SHORT).show()
                            }
                            binding.previewView.postDelayed({
                                isProcessing = false
                                runOnUiThread {
                                    binding.overlay.setBackgroundResource(R.drawable.qr_scanner_overlay)
                                }
                            }, 2000)
                        }
                    }
                }
                .addOnCompleteListener {
                    imageProxy.close()
                }
        }
    }

    private fun playFeedback(isSuccess: Boolean) {
        val tone = if (isSuccess) ToneGenerator.TONE_PROP_ACK else ToneGenerator.TONE_PROP_NACK
        ToneGenerator(AudioManager.STREAM_NOTIFICATION, 100).startTone(tone, 200)
        
        runOnUiThread {
            binding.overlay.setBackgroundResource(
                if (isSuccess) R.drawable.qr_scanner_overlay_green 
                else R.drawable.qr_scanner_overlay_red
            )
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        cameraExecutor.shutdown()
    }
}
