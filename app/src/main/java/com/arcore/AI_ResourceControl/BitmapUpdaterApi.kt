package com.arcore.AI_ResourceControl

import android.graphics.Bitmap
import android.media.Image
import androidx.compose.ui.semantics.Role.Companion.Image
import com.google.ar.core.Frame
import com.arcore.AI_ResourceControl.MainActivity as MainActivity

/**
 * Stores latest Bitmap to pass to DynamicBitmapSource
 */
class BitmapUpdaterApi {
    var latestBitmap : Bitmap? = null

    fun updateBitmap(bitmap: Bitmap) {
        latestBitmap = bitmap
    }

//    fun convertToBitmap(frame: Frame) {
//        val mainActivity: MainActivity = MainActivity()
//        val converter: YuvToRgbConverter = YuvToRgbConverter(mainActivity)
////        YuvToRgbConverter converter = new YuvToRgbConverter(this);
//        val image: Image = frame.acquireCameraImage()
//        var bmp: Bitmap = Bitmap.createBitmap(image.width, image.height, Bitmap.Config.ARGB_8888)
//        converter.yuvToRgb(image, bmp);
//        image.close();
//    }

}