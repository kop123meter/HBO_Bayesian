package com.arcore.AI_ResourceControl

import android.app.Activity
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import java.io.InputStream
/**
 * interface to pass static bitmap to BitmapSource
 */
interface StreamApi {
    abstract var activity : Activity
    abstract var imgFileName: String

    suspend fun fetchStream(activity : Activity): Bitmap {
        val imgStream: InputStream = activity.assets.open(imgFileName)
        return BitmapFactory.decodeStream(imgStream)
    }
}