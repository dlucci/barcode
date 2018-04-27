package com.barcode.dlucci.barcode

import android.Manifest
import android.content.Context
import android.support.annotation.RequiresPermission
import android.util.AttributeSet
import android.util.Log
import android.view.SurfaceHolder
import android.view.SurfaceView
import android.view.ViewGroup
import com.google.android.gms.common.images.Size
import com.google.android.gms.vision.CameraSource

/**
 * Created by derril.lucci on 4/25/18.
 */
class KameraPreview(mContext : Context, attrs : AttributeSet) : ViewGroup(mContext, attrs) {

    var surfaceView : SurfaceView
    var mCameraSource : CameraSource? = null

    init {
        surfaceView = SurfaceView(mContext)
        surfaceView.holder.addCallback(SurfaceCallback())
        addView(surfaceView)
    }

    @RequiresPermission(Manifest.permission.CAMERA)
    fun startIfReady() = mCameraSource?.start(surfaceView.holder)

    fun start(cameraSource : CameraSource?) {
        mCameraSource = cameraSource
        startIfReady()
    }

    inner class SurfaceCallback : SurfaceHolder.Callback {
        override fun surfaceDestroyed(holder: SurfaceHolder?) {
            Log.d("KOTLIN", "destroy")
        }

        override fun surfaceCreated(holder: SurfaceHolder?) {
            startIfReady()
        }

        override fun surfaceChanged(holder: SurfaceHolder?, format: Int, width: Int, height: Int) { Log.d("KOTLIN", "change") }
    }

    override fun onLayout(changed: Boolean, l: Int, t: Int, r: Int, b: Int) {
        var width  :  Int? = 320
        var height : Int? = 240
        var size : Size? = mCameraSource?.previewSize
        if(size != null) {
            width = size?.width
            height = size?.height
        }


        val layoutWidth = r - l
        val layoutHeight = b - t

        var childWidth = layoutWidth
        var childHeight = (layoutWidth.toFloat() / width!!.toFloat() * height!!).toInt()

        if(childHeight > layoutHeight){
            childHeight
        }

        for (i in 0 until childCount) {
            getChildAt(i).layout(0, 0, childWidth, childHeight)
        }

        startIfReady()
    }

    fun stop() = mCameraSource?.stop()

}

