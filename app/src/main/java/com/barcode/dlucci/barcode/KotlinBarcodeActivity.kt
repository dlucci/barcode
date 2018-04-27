package com.barcode.dlucci.barcode

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.pm.PackageManager
import android.os.Bundle
import android.support.annotation.UiThread
import android.support.v4.app.ActivityCompat
import android.util.Log
import butterknife.BindView
import butterknife.ButterKnife
import com.google.android.gms.vision.CameraSource
import com.google.android.gms.vision.MultiProcessor
import com.google.android.gms.vision.Tracker
import com.google.android.gms.vision.barcode.Barcode
import com.google.android.gms.vision.barcode.BarcodeDetector

class KotlinBarcodeActivity : Activity(), ActivityCompat.OnRequestPermissionsResultCallback, BarcodeGraphicTracker.BarcodeUpdateListener {

    val TAG = "KotlinBarcodeActivity"

    override fun onBarcodeDetected(barcode: Barcode?) {
        Log.d(TAG, barcode?.displayValue)
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {

        createCamera()
        startCamera()
    }

    private val PERMISSIONS = arrayOf(Manifest.permission.CAMERA)

    @BindView(R.id.kamera)
    lateinit var preview : KameraPreview

    var cameraSource : CameraSource? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        ButterKnife.bind(this)
        if(!checkPermissions(PERMISSIONS)){
            ActivityCompat.requestPermissions(this, PERMISSIONS, 1)
        } else {
            createCamera()
        }
    }

    override fun onResume() {
        super.onResume()
        var rc = ActivityCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
        if(rc == PackageManager.PERMISSION_GRANTED) {
            startCamera()
        }
    }

    private fun startCamera() = preview.start(cameraSource)


    private fun checkPermissions(permissions: Array<String>): Boolean {
        permissions.forEach {
                u -> if (ActivityCompat.checkSelfPermission(this, u) != PackageManager.PERMISSION_GRANTED){
                    return false
                }
            }
        return true
    }

    fun createCamera() {
        var barcodeDetector = BarcodeDetector.Builder(this).build()
        var barcodeFactory = BarcodeTrackerFactory(this)
        barcodeDetector.setProcessor(MultiProcessor.Builder<Barcode>(barcodeFactory).build())

        var builder = CameraSource.Builder(this, barcodeDetector)
                .setFacing(CameraSource.CAMERA_FACING_BACK)
                .setRequestedPreviewSize(1600, 1024)
                .setRequestedFps(15.0f)
                .setAutoFocusEnabled(true)

        cameraSource = builder.build()
    }

    override fun onPause() {
        super.onPause()
        preview?.stop()
    }
}

class BarcodeTrackerFactory(var context: Context) : MultiProcessor.Factory<Barcode> {
    override fun create(p0: Barcode?): Tracker<Barcode> = BarcodeGraphicTracker(context)

}

class BarcodeGraphicTracker(context: Context) : Tracker<Barcode>() {

    lateinit var barcodeUpdateListener : BarcodeUpdateListener

    interface BarcodeUpdateListener {

        @UiThread
        fun onBarcodeDetected(barcode : Barcode?)
    }

    init {
        if(context is BarcodeUpdateListener)
            barcodeUpdateListener = context
    }

    override fun onNewItem(p0: Int, p1: Barcode?) {
        super.onNewItem(p0, p1)
        barcodeUpdateListener.onBarcodeDetected(p1)
    }

}
