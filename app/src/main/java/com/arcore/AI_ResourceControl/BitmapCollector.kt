package com.arcore.AI_ResourceControl

import android.app.Activity
import android.graphics.Bitmap
import android.text.SpannableStringBuilder
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.collect
import org.tensorflow.lite.examples.digitclassification.DigitClassifierHelper
import org.tensorflow.lite.examples.imagesegmentation.ImageSegmentationHelper
import java.io.File

import java.io.ByteArrayOutputStream
import java.net.Socket
import android.util.Base64


/**
 * Code to run inferences on the frames taken from the dispatcher and calculates response time
 * Collects Bitmaps from a coroutine source (BitmapSource for static JPG or DynamicBitmapSource for
 * a stream of changing files)
 */
class BitmapCollector(
    /**
     * If using static jpg, comment out DynamicBitmapSource, uncomment BitmapSource
     */

    private val bitmapSource: DynamicBitmapSource?,
    val classifier: ImageClassifier?,
    val segm: ImageSegmentor?,
    var index: Int, // to ensure unique filename.
    private val activity: Activity,
    var mInstance: MainActivity,
    var objectDetector: ObjectDetectorHelper?,
    var newClassifier: ImageClassifierHelper?,
    var imgSegmentation: ImageSegmentationHelper?,
    var gestureClas: GestureClassifierHelper?,
    var digitClas: DigitClassifierHelper?,
    var device: Int,
    var model: Int

): ViewModel() {

    var start: Long = 0
    var pureInfT: Long = 0
    var infAcc: Float = 0f
    var end: Long = 0
    var overhead: Long = 0
    var InferenceTime: Long = 0
    var responseTime: Long = 0
    var totalResponseTime: Long = 0
    var totalInferenceTime: Long = 0
    var totalOverhead: Long = 0
    var totalPureInf: Long=0
    var numOfTimesExecuted = 0
  // first worked: var  objectDetectorHelper = ObjectDetectorHelper( context = mInstance, fileseries = mInstance.fileseries)


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
        totalOverhead=0
        totalInferenceTime=0
        totalResponseTime = 0
        numOfTimesExecuted = 0
        end = System.nanoTime()/1000000
        pureInfT=0
        overhead = 0
        InferenceTime = 0
        responseTime = 0
        totalPureInf=0
        infAcc=0f
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


/****** the problem was with the dispatcher--- it should be set to Default NOT IO */////////
        job = viewModelScope.launch(Dispatchers.Default) {
            bitmapSource?.bitmapStream?.collect {
                Log.d("CANCEL", "$index collected $it")

              //  nill added to get the latest bitmap for inference -> nut no needed now since we get
              //  bitmap=  bitmapSource?.bitmapUpdaterApi?.latestBitmap

                if( run) { // resize it for image classification

                    var  bitmap= it
                    if(classifier!= null )
                    {
                        val bitmap2 = Bitmap.createScaledBitmap(
                            // it,
                            bitmap!!,
                            classifier!!.imageSizeX,
                            classifier.imageSizeY,
                            true
                        )
                        bitmap=bitmap2// resized value for classification
                    }

                    else if(segm!= null )
                    {
                        val bitmap2 = Bitmap.createScaledBitmap(
                            // it,
                            bitmap!!,
                            segm!!.imageSizeX,
                            segm.imageSizeY,
                            true
                        )
                        bitmap=bitmap2// resized value for classification
                    }


                    if (newClassifier?.modelUsed()  =="inception_v1_224_quant.tflite"  )
                    {
                        bitmap=Bitmap.createScaledBitmap(bitmap!!, 229, 229, true) //229
                    }

//                    else if (imgSegmentation?.modelUsed()  =="deeplabv3.tflite" )
//                    {
//                        bitmap=Bitmap.createScaledBitmap(bitmap!!, 257, 257, true) //229
//                    }
// minist = 1024x1024, 640x480 for gesture
//                    else if (gestureClas?.modelUsed()  =="model_metadata.tflite" )
//                    {
//                        bitmap=Bitmap.createScaledBitmap(bitmap!!, 224, 224, true) //229
//                    }

                    start = System.nanoTime()/1000000
                    if(end!=0L) {
                        if(!mInstance.sleepmode)
                            overhead = start-end
                        else
                            overhead = start-end-1000

                    }

//object detection version complex: this is the main
                   if(device == 3){
                        val(total_latency ,serverResponse) = sendBitmapToServer(bitmap!!, model)
                        if (serverResponse != null) {
                            Log.d("Server Response", serverResponse)
                        }
                        else {
                            Log.e("Server Response", "Error communicating with server")
                        }
                        //System.out.println("Time:" + total_latency)
                   }
                   else if(objectDetector!= null){
                        objectDetector!!.detect(bitmap!!,0)
                   }

                   else if(classifier!= null)
                        classifier.classifyFrame(bitmap)

                   else if(segm!= null)
                      { segm.segmentFrame(bitmap)
                      bitmap!!.recycle()
                      Log.d(
                        "segmentation"
                            , "    Frame Rate: " + 1000 / segm.duration
                      )
                      }

                    else if (newClassifier!=null)
                         newClassifier?.classify(bitmap!!,0)

                    else if (imgSegmentation!=null)
                        imgSegmentation?.segment(bitmap!!,0)

                    else if (gestureClas!=null)
                        gestureClas?.classify(bitmap!!,90)

                   else if (digitClas!=null)
                       digitClas?.classify(bitmap!!)

                    end = System.nanoTime()/1000000
                    InferenceTime = end-start
                    totalOverhead+=overhead
                    responseTime=overhead+InferenceTime
                    numOfTimesExecuted++
                    totalResponseTime+=responseTime

                    totalInferenceTime+=InferenceTime
                    Log.d("times", "${overhead},${InferenceTime},${responseTime}")
                    //System.out.println("Time:" + totalResponseTime)
                    outputText.append("${overhead},${InferenceTime},${responseTime}\n")
//                    file.appendText(outputText.toString())
                }
            }
        }
    }


}

/**
    * Sends a Bitmap to a server and returns the server's response.
 */

fun sendBitmapToServer(bitmap: Bitmap, model: Int): Pair<Long, String?> {
    val tempAdd = serverAddress();

    val serverIP = "192.168.1.2"  // IP address of the server
    val serverPort = 4545        // Port number of the server
    var retryCount = 3               // Number of times to retry sending the image
    var networkLatency: Long = 0     // Network latency in milliseconds
    System.out.println(model)
    
    while (retryCount > 0) {
        try {
            // Connect to the server
            val socket = Socket(serverIP, serverPort)

            // Since the server expects a base64 encoded image, convert the bitmap to a base64 string
            val outputStream = ByteArrayOutputStream()
            bitmap.compress(Bitmap.CompressFormat.JPEG, 60, outputStream)
            var base64Image = Base64.encodeToString(outputStream.toByteArray(), Base64.DEFAULT)
            val missingPadding = base64Image.length % 4
            if(missingPadding > 0){
                base64Image += "=".repeat(4 - missingPadding)
            }
            var image_data_length = base64Image.length

            val out = socket.getOutputStream()
            
            // record the time before sending the image
            val startNetworkTime = System.nanoTime()


            // Send the model index to the server
            out.write(model.toString().toByteArray(Charsets.UTF_8))
            out.flush()

            // Data format: modelIndex:base64Image:finish
            out.write(":".toByteArray(Charsets.UTF_8))
            out.flush()

            // Get Data Length
            out.write(image_data_length.toString().toByteArray(Charsets.UTF_8))
            out.flush()

            out.write(":".toByteArray(Charsets.UTF_8))
            out.flush()

            
            // Send the image data
            out.write(base64Image.toByteArray(Charsets.UTF_8))

            out.flush()
            out.write("finish".toByteArray(Charsets.UTF_8))
            out.flush()
            Log.d("BitmapCollector", "Sent image to server")
            System.out.println("Send Image")

            // Wait for the server to acknowledge the receipt of data
            val input = socket.getInputStream()
            val ackBuffer = ByteArray(1024)
            val ackLength = input.read(ackBuffer)
            val ackMessage = String(ackBuffer, 0, ackLength)
            if (ackMessage != "RECEIVED") {
                Log.e("BitmapCollector", "Server did not acknowledge the receipt of data")
                return Pair(-1, null)
            }
            Log.d("BitmapCollector", "Server acknowledged receipt of data")
            //System.out.println("Server acknowledged receipt of data!")
//            if (input == null) {
//                Log.e("BitmapCollector", "Error getting input stream from server")
//                return Pair(-1, null)
//            }


            // read the response from the server
            val resultBuffer = ByteArray(4096)  // 4KB buffer to store the server response locally
            val length = input.read(resultBuffer)
            var serverResponse: String? = null
            if (length >= 0 && length <= resultBuffer.size) {
                serverResponse = String(resultBuffer, 0, length)
            } else {
                // Handle error case here
                println("Invalid length: $length")
            }

            // record the time after receiving the response
            val endNetworkTime = System.nanoTime()
            networkLatency = (endNetworkTime - startNetworkTime) / 1_000_000  // convert to milliseconds

            // close the input and output streams
            input.close()
            out.close()
            socket.close()



            val parts = serverResponse?.split(":") ?: emptyList()
            if (parts.size < 2) {
                Log.e("BitmapCollector", "Invalid response format")
                return Pair(-1, null)
            }
            //System.out.println("PARTS:      " + parts[0])
            val serverLatency = parts[0].toFloatOrNull()
            val result = parts.getOrNull(1)

            Log.d("OFFLOAD_MSG", "Server Processing Latency: $serverLatency ms")
            Log.d("OFFLOAD_MSG", "Total Latency: $networkLatency ms")
            // return the network latency and the server response
            return Pair(networkLatency, result)

        } catch (e: Exception) {
            Log.e("OFFLOAD_MSG", "Error communicating with server", e)
            retryCount--
        }
    }

    // return null if the server did not respond
    return Pair(-1, null)
}
