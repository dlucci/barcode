package com.barcode.dlucci.barcode

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.SurfaceTexture
import android.hardware.camera2.*
import android.hardware.camera2.params.StreamConfigurationMap
import android.os.Bundle
import android.os.Handler
import android.os.HandlerThread
import android.support.v4.app.ActivityCompat
import android.util.Log
import android.util.Size
import android.view.Surface
import android.view.TextureView
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import butterknife.BindView
import butterknife.ButterKnife
import com.google.android.gms.vision.CameraSource
import com.google.android.gms.vision.barcode.BarcodeDetector
import java.util.*

class KameraAKtivity : Activity(), TextureView.SurfaceTextureListener, ActivityCompat.OnRequestPermissionsResultCallback {

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        setUpDetection()
    }

    private val TAG : String = "Kamera"

    @BindView(R.id.textureView)
    lateinit var textureView : TextureView

    @BindView(R.id.textView)
    lateinit var textView : TextView

    @BindView(R.id.button)
    lateinit var button : Button

    lateinit var cameraManager : CameraManager
    var cameraFacing : Int = -1
    var cameraID : String? = null

    var bgHandler : Handler? = null
    var bgThread : HandlerThread? = null

    var cameraDevice : CameraDevice? = null

    var cameraCaptureSession : CameraCaptureSession? = null

    lateinit var previewSize : Size

    lateinit  var barcodeDetector : BarcodeDetector

    lateinit var cameraSrc : CameraSource


    var stateCallback = object : CameraDevice.StateCallback(){

        override fun onOpened(camera: CameraDevice?) {
            Log.d(TAG, "open")
            this@KameraAKtivity.cameraDevice = camera
            createPreviewSesion()
        }

        override fun onDisconnected(camera: CameraDevice?) {
            cameraDevice?.close()
            this@KameraAKtivity.cameraDevice = null
        }

        override fun onError(camera: CameraDevice?, error: Int) {
            camera?.close()
            this@KameraAKtivity.cameraDevice = null
        }
    }


    private val PERMISSIONS = arrayOf(Manifest.permission.CAMERA, Manifest.permission.WRITE_EXTERNAL_STORAGE)

    override fun onSurfaceTextureSizeChanged(surface: SurfaceTexture?, width: Int, height: Int) = Unit

    override fun onSurfaceTextureUpdated(surface: SurfaceTexture?) = Unit

    override fun onSurfaceTextureDestroyed(surface: SurfaceTexture?): Boolean  = false

    override fun onSurfaceTextureAvailable(surface: SurfaceTexture?, width: Int, height: Int) {
        setUpCamera()
        openCamera()
    }

    private fun openCamera() = cameraManager.openCamera(cameraID, stateCallback, bgHandler)


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_kamera_aktivity)
        ButterKnife.bind(this)
    }

    private fun openBackgroundThread() {
        bgThread = HandlerThread("camera_bg")
        bgThread?.start()
        bgHandler = Handler(bgThread?.looper)
    }

    override fun onResume() {
        super.onResume()
        openBackgroundThread()
        if(!hasCamera()){
            Toast.makeText(this, "Camera ain't here yo!", Toast.LENGTH_SHORT).show()
        } else {
            if(!checkPermissions(PERMISSIONS)){
                ActivityCompat.requestPermissions(this, PERMISSIONS, 1)
            } else {
                cameraManager = getSystemService(Context.CAMERA_SERVICE) as CameraManager
                cameraFacing = CameraCharacteristics.LENS_FACING_BACK
                if(textureView.isAvailable) {
                    setUpCamera()
                    openCamera()
                } else {
                    textureView.surfaceTextureListener = this
                }
            }
        }
    }

    private fun setUpCamera() = try{
        cameraManager.cameraIdList.forEach { id -> setCameraId(id) }
    } catch (e : CameraAccessException){
        Log.e(TAG, e.localizedMessage)
    }

    private fun setCameraId(id : String?) {
        var characteristics : CameraCharacteristics = cameraManager.getCameraCharacteristics(id)
        if(characteristics.get(CameraCharacteristics.LENS_FACING) == cameraFacing) {
            var stream : StreamConfigurationMap = characteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP)
            //set preview size
            previewSize = stream.getOutputSizes(SurfaceTexture::class.java)[0]
            cameraID = id
        }
    }

    private fun checkPermissions(permissions: Array<String>): Boolean {
        permissions.forEach {
            u -> if (ActivityCompat.checkSelfPermission(this, u) != PackageManager.PERMISSION_GRANTED){
                return false
            }
        }
        return true
    }

    private fun hasCamera(): Boolean  =  packageManager.hasSystemFeature(PackageManager.FEATURE_CAMERA)


    override fun onStop() {
        super.onStop()
        closeCamera()
        closeBackgroundThread()

    }

    private fun closeBackgroundThread() {
        bgThread?.quitSafely()
        bgThread = null
        bgHandler = null
    }

    private fun closeCamera() {
        cameraCaptureSession?.close()
        cameraCaptureSession = null
        cameraDevice?.close()
        cameraDevice = null
    }

    fun createPreviewSesion() {

            var surfaceTexture = textureView.surfaceTexture
            surfaceTexture.setDefaultBufferSize(previewSize.width, previewSize.height)
            var previewSurface = Surface(surfaceTexture)
            var captureRequestBuilder = cameraDevice?.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW)
            captureRequestBuilder?.addTarget(previewSurface)

            cameraDevice?.createCaptureSession(Collections.singletonList(previewSurface),
                    object : CameraCaptureSession.StateCallback() {
                        override fun onConfigureFailed(session: CameraCaptureSession?) = Unit

                        override fun onConfigured(session: CameraCaptureSession?) {
                            Log.d(TAG, "here again")
                            var captureRequest = captureRequestBuilder?.build()
                            this@KameraAKtivity.cameraCaptureSession = session
                            this@KameraAKtivity.cameraCaptureSession?.setRepeatingRequest(captureRequest, null, bgHandler)
                        }

                    }, bgHandler)
    }

    private fun setUpDetection() {
        barcodeDetector = BarcodeDetector.Builder(this).build()
        cameraSrc = CameraSource.Builder(this, barcodeDetector)
                .setFacing(CameraSource.CAMERA_FACING_BACK)
                .setRequestedFps(15.0f)
                .build()
        //cameraSrc.start(textureView)
    }

}
