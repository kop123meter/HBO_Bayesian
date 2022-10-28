package com.arcore.AI_ResourceControl

import android.graphics.Bitmap
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn

/**
 * Emits latest bitmap from BitmapUpdaterApi
 */
class DynamicBitmapSource(val bitmapUpdaterApi : BitmapUpdaterApi) {
    var run = false
    lateinit var bitmapStream : Flow<Bitmap?>

    private fun runStream() {
        bitmapStream = flow {
            while (run) {
                // get latest bitmap from BitmapUpdaterApi
//                val bitmapStream = bitmapUpdaterApi.latestBitmap
                emit(bitmapUpdaterApi.latestBitmap)
            }
        }.flowOn(Dispatchers.Default)
    }

    fun startStream() {
        run = true
        Log.d("CANCEL", "STARTING STREAM")
        runStream()
    }

    fun pauseStream() {
        run = false
//        bitmapUpdaterApi.latestBitmap=null
        Log.d("CANCEL", "STOPPING STREAM")
    }
}