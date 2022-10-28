package com.arcore.AI_ResourceControl

import android.app.Activity
import android.graphics.Bitmap
import android.text.SpannableStringBuilder
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.collect
import org.tensorflow.lite.examples.objectdetection.ObjectDetectorHelper
import java.io.File


/**
 * Collects Bitmaps from a coroutine source (BitmapSource for static JPG or DynamicBitmapSource for
 * a stream of changing files)
 */
class BitmapCollector(
    /**
     * If using static jpg, comment out DynamicBitmapSource, uncomment BitmapSource
     */
//    private val bitmapSource: BitmapSource?,
    private val bitmapSource: DynamicBitmapSource?,
    val classifier: ImageClassifier?,
    var index: Int, // to ensure unique filename.
    private val activity: Activity,
    var mInstance: MainActivity,

   // private var objectDetectorHelper: ObjectDetectorHelper


): ViewModel() {

    var start: Long = 0
    var end: Long = 0
    var overhead: Long = 0
    var classificationTime: Long = 0
    var responseTime: Long = 0
    var totalResponseTime: Long = 0
    var numOfTimesExecuted = 0
    //
    private val outputPath = activity.getExternalFilesDir(null)
    private val childDirectory = File(outputPath, "data")
    var run = false
    private var job : Job? = null
    var outputText = SpannableStringBuilder("null")

    /**
     * Resets response time collection data so changing model does not give erroneous first result
     */
    fun resetRtData() {
        totalResponseTime = 0
        numOfTimesExecuted = 0
        end = System.nanoTime()/1000000

    }

    /**
     * Stops running collector
     */
    fun pauseCollect() {
        run = false
        job?.cancel()
        Log.d("CANCEL", "Classifier $index cancelled")
    }

    /**
     * Starts collection
     * Precondition: bitmapSource must be emitting a stream to collect
     */
    fun startCollect() = runBlocking <Unit>{
        run = true
        resetRtData()
        Log.d("CANCEL", "Starting classifier $index")
        launch {
            collectStream()
        }
    }

    /**
     * launches coroutine to collect bitmap from bitmapSource, scales bitmap to
     * ImageClassifier requirements. Writes output to file.
     */
    private suspend fun collectStream() {
        childDirectory.mkdirs()
        val file = File(childDirectory,
            index.toString() + '_' +
                    classifier?.modelName + '_' +
                    classifier?.device + '_'+
                    classifier?.numThreads + "T_"+
                    classifier?.time +
                    ".csv")
//        file.appendText("timestamp,response,guess3,acc3,guess2,acc2,guess1,acc1\n")
//        file.appendText("overhead,classification Time,response time\n ")
        job = viewModelScope.launch(Dispatchers.IO) {
            bitmapSource?.bitmapStream?.collect {
                Log.d("CANCEL", "$index collected $it")
                if(it!=null && run) {
                    val bitmap = Bitmap.createScaledBitmap(
                        it,
                        classifier!!.imageSizeX,
                        classifier.imageSizeY,
                        true
                    )

                    start = System.nanoTime()/1000000
                    if(end!=0L) {
                        overhead = start-end
                    }
//object detection version simple:
                    //val odetector  = ObjectDet(mInstance)
                   // odetector.runObjectDetection(bitmap)

//object detection version complex:
                    var objectDetectorHelper: ObjectDetectorHelper
                    objectDetectorHelper = ObjectDetectorHelper(
                        context = mInstance)



                    objectDetectorHelper.detect(bitmap, 0)

//object detection

// temp comment to check GPU-CPU usage
                  //  classifier.classifyFrame(bitmap)

                    end = System.nanoTime()/1000000
                    classificationTime = end-start
                    responseTime=overhead+classificationTime
                    numOfTimesExecuted++
                    totalResponseTime+=responseTime
                    Log.d("times", "${overhead},${classificationTime},${responseTime}")
                    outputText.append("${overhead},${classificationTime},${responseTime}\n")
//                    file.appendText(outputText.toString())
                }
            }
        }
    }


    fun getThroughput(): Long {
        return if(numOfTimesExecuted>0)  {
            totalResponseTime/numOfTimesExecuted
        } else {
            0
        }
    }
}


