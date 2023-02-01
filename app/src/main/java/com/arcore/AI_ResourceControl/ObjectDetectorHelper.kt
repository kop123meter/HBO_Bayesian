/*
 * Copyright 2022 The TensorFlow Authors. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *             http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package  com.arcore.AI_ResourceControl

import android.content.Context
import android.graphics.Bitmap
import android.os.SystemClock
import android.util.Log
import org.tensorflow.lite.gpu.CompatibilityList
import org.tensorflow.lite.support.image.ImageProcessor
import org.tensorflow.lite.support.image.TensorImage
import org.tensorflow.lite.support.image.ops.Rot90Op
import org.tensorflow.lite.task.core.BaseOptions
import org.tensorflow.lite.task.vision.detector.Detection
import org.tensorflow.lite.task.vision.detector.ObjectDetector
import java.io.File
import java.io.FileOutputStream
import java.io.PrintWriter
import java.text.SimpleDateFormat
import java.util.*

class ObjectDetectorHelper(
    var threshold: Float = 0.5f,
    var numThreads: Int = 2,
    var maxResults: Int = 3,
    var currentDelegate: Int = 0,
    var currentModel: Int = 0,
    var context: Context,
    var modelName: String,

    val fileseries: String ,

    //val objectDetectorListener: DetectorListener?
) {

    // For this example this needs to be a var so it can be reset on changes. If the ObjectDetector
    // will not change, a lazy val would be preferable.
     var objectDetector: ObjectDetector? = null

    init {
        setupObjectDetector()
    }

    fun clearObjectDetector() {
        objectDetector = null
    }


    fun  deviceUsed() :String {
        if(currentDelegate==0)
            return "CPU"
        else  if(currentDelegate==1)
            return "GPU"

        else if(currentDelegate==2)
            return "NNAPI"

        return "CPU"
    }


    fun  modelUsed() :String {
//        val modelName =
//            when (currentModel) {
//                MODEL_MOBILENETV1 -> "mobilenetDetv1.tflite"
//                MODEL_EFFICIENTDETV0 -> "efficientdet-lite0.tflite"
//                MODEL_EFFICIENTDETV1 -> "efficientdet-lite1.tflite"
//                MODEL_EFFICIENTDETV2 -> "efficientdet-lite2.tflite"
//                else -> "mobilenetDetv1.tflite"
//            }

            return modelName
    }

    // Initialize the object detector using current settings on the
    // thread that is using it. CPU and NNAPI delegates can be used with detectors
    // that are created on the main thread and used on a background thread, but
    // the GPU delegate needs to be used on the thread that initialized the detector
     fun setupObjectDetector() {
        // Create the base options for the detector using specifies max results and score threshold
        val optionsBuilder =
            ObjectDetector.ObjectDetectorOptions.builder()
                .setScoreThreshold(threshold)
                .setMaxResults(maxResults)

        // Set general detection options, including number of used threads
        val baseOptionsBuilder = BaseOptions.builder().setNumThreads(numThreads)

        // Use the specified hardware for running the model. Default to CPU
        when (currentDelegate) {
            DELEGATE_CPU -> {
                // Default
            }
            DELEGATE_GPU -> {
                if (CompatibilityList().isDelegateSupportedOnThisDevice) {
                    baseOptionsBuilder.useGpu()
                } else {
               //     objectDetectorListener?.onError("GPU is not supported on this device")
                }
            }
            DELEGATE_NNAPI -> {
                baseOptionsBuilder.useNnapi()
            }
        }

        optionsBuilder.setBaseOptions(baseOptionsBuilder.build())

//        val modelName =
//            when (currentModel) {
//                MODEL_MOBILENETV1 -> "mobilenetDetv1.tflite"
//                MODEL_EFFICIENTDETV0 -> "efficientdet-lite0.tflite"
//                MODEL_EFFICIENTDETV1 -> "efficientdet-lite1.tflite"
//                MODEL_EFFICIENTDETV2 -> "efficientdet-lite2.tflite"
//                else -> "mobilenetDetv1.tflite"
//            }

        try {
            objectDetector =
                ObjectDetector.createFromFileAndOptions(context, modelName, optionsBuilder.build())
        } catch (e: IllegalStateException) {
//            objectDetectorListener?.onError(
//                "Object detector failed to initialize. See error logs for details"
//            )
            Log.e("Test", "TFLite failed to load model with error: " + e.message)
        }
    }

   public  fun detect(image: Bitmap, imageRotation: Int) {
        if (objectDetector == null) {
            setupObjectDetector()
        }

        // Inference time is the difference between the system time at the start and finish of the
        // process
        var inferenceTime = SystemClock.uptimeMillis()


        val imageProcessor =
            ImageProcessor.Builder()
                .add(Rot90Op(-imageRotation / 90))
                .build()

        // Preprocess the image and convert it into a TensorImage for detection.
        val tensorImage = imageProcessor.process(TensorImage.fromBitmap(image))

        val results = objectDetector?.detect(tensorImage)
//    nil created this commented to not consider it for inference time     if (results != null) {
//            debugPrint(results)
//        }

        inferenceTime = SystemClock.uptimeMillis() - inferenceTime


    }


// added to write the results by nil
    fun debugPrint(results : List<Detection>) {

        var currentFolder: String? = context.getExternalFilesDir(null)!!.absolutePath
        var FILEPATH = currentFolder + File.separator + "objDetectorModel_" + fileseries + ".csv"

        for ((i, obj) in results.withIndex()) {

            val box = obj.boundingBox
            val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss:SSS")
            try {
              PrintWriter(FileOutputStream(FILEPATH, true)).use { writer ->
                    val sbb = StringBuilder()
                    sbb.append(dateFormat.format(Date())).append(",").append(deviceUsed()).append(",").append("${i}")

                    sbb.append(',')
                    sbb.append( "(${box.left}, ${box.top}) - (${box.right},${box.bottom})" )
                    sbb.append(',')
                    writer.write(sbb.toString())

                }
            } catch (e: java.io.FileNotFoundException) {
                println(e.message)
            }

            Log.d(TAG, "Detected object: ${i} ")
            Log.d(TAG, "  boundingBox: (${box.left}, ${box.top}) - (${box.right},${box.bottom})")

            for ((j, category) in obj.categories.withIndex()) {

                Log.d(TAG, "    Label $j: ${category.label}")
                val confidence: Int = category.score.times(100).toInt()

                try {
                    PrintWriter(FileOutputStream(FILEPATH, true)).use { writer ->
                        val sbb = StringBuilder()
                        sbb.append("$j: ${category.label}")
                        sbb.append(',')
                        sbb.append( "${confidence}%" )
                        sbb.append(',')
                        writer.write(sbb.toString())

                    }
                } catch (e: java.io.FileNotFoundException) {
                    println(e.message)
                }



                Log.d(TAG, "    Confidence: ${confidence}%")
            }

            try {
                PrintWriter(FileOutputStream(FILEPATH, true)).use { writer ->
                    writer.write("\n")

                }
            } catch (e: java.io.FileNotFoundException) {
                println(e.message)
            }


        }
    }



    interface DetectorListener {
        fun onError(error: String)
        fun onResults(
          results: MutableList<Detection>?,
          inferenceTime: Long,
          imageHeight: Int,
          imageWidth: Int
        )
    }

    companion object {
        const val DELEGATE_CPU = 0
        const val DELEGATE_GPU = 1
        const val DELEGATE_NNAPI = 2
        const val MODEL_MOBILENETV1 = 0
        const val MODEL_EFFICIENTDETV0 = 1
        const val MODEL_EFFICIENTDETV1 = 2
        const val MODEL_EFFICIENTDETV2 = 3
        const val TAG = "TFLite - ODT"
    }
}
