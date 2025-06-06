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


}