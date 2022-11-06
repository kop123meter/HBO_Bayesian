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

package com.arcore.AI_ResourceControl;

import android.content.Context
import android.graphics.Bitmap
import android.os.SystemClock
import android.util.Log
import android.view.Surface
import org.tensorflow.lite.gpu.CompatibilityList
import org.tensorflow.lite.support.image.ImageProcessor
import org.tensorflow.lite.support.image.TensorImage
import org.tensorflow.lite.support.label.Category
import org.tensorflow.lite.task.core.BaseOptions
import org.tensorflow.lite.task.core.vision.ImageProcessingOptions
import org.tensorflow.lite.task.vision.classifier.Classifications
import org.tensorflow.lite.task.vision.classifier.ImageClassifier
import java.io.File
import java.io.FileOutputStream
import java.io.PrintWriter
import java.text.SimpleDateFormat
import java.util.*

class ImageClassifierHelper(
    var threshold: Float = 0.5f,
    var numThreads: Int = 2,
    var maxResults: Int = 3,
    var currentDelegate: Int = 0,
    var currentModel: Int = 1,
    val context: Context,
    var modelName: String,
    val fileseries: String ,
   // val imageClassifierListener: ClassifierListener?
) {
     var imageClassifier: ImageClassifier? = null

    init {
        setupImageClassifier()
    }

    fun clearImageClassifier() {
        imageClassifier = null
    }




     fun setupImageClassifier() {
        val optionsBuilder = ImageClassifier.ImageClassifierOptions.builder()
            .setScoreThreshold(threshold)
            .setMaxResults(maxResults)

        val baseOptionsBuilder = BaseOptions.builder().setNumThreads(numThreads)

        when (currentDelegate) {
            DELEGATE_CPU -> {
                // Default
            }
            DELEGATE_GPU -> {
                if (CompatibilityList().isDelegateSupportedOnThisDevice) {
                    baseOptionsBuilder.useGpu()
                } else {
                  //  imageClassifierListener?.onError("GPU is not supported on this device")
                }
            }
            DELEGATE_NNAPI -> {
                baseOptionsBuilder.useNnapi()
            }
        }

        optionsBuilder.setBaseOptions(baseOptionsBuilder.build())

//        val modelName =
//            when (currentModel) {
//                MODEL_MOBILENETV1 -> "mobilenetClassv1.tflite"
//                MODEL_EFFICIENTNETV0 -> "efficientclass-lite0.tflite"
//                MODEL_EFFICIENTNETV1 -> "efficientclass-lite1.tflite"
//                MODEL_EFFICIENTNETV2 -> "efficientclass-lite2.tflite"
//                Model_InceptionV1-> "inception_v1_224_quant.tflite"
//                else -> "mobilenetClassv1.tflite"
//            }

        try {
            imageClassifier =
                ImageClassifier.createFromFileAndOptions(context, modelName, optionsBuilder.build())
        } catch (e: IllegalStateException) {
//            imageClassifierListener?.onError(
//                "Image classifier failed to initialize. See error logs for details"
//            )
            Log.e(TAG, "TFLite failed to load model with error: " + e.message)
        }
    }

    fun classify(image: Bitmap, rotation: Int) {
        if (imageClassifier == null) {
            setupImageClassifier()
        }

        // Inference time is the difference between the system time at the start and finish of the
        // process
        var inferenceTime = SystemClock.uptimeMillis()

        // Create preprocessor for the image.
        // See https://www.tensorflow.org/lite/inference_with_metadata/
        //            lite_support#imageprocessor_architecture
        val imageProcessor =
            ImageProcessor.Builder()
                .build()

        // Preprocess the image and convert it into a TensorImage for classification.
        val tensorImage = imageProcessor.process(TensorImage.fromBitmap(image))

        val imageProcessingOptions = ImageProcessingOptions.builder()
            .setOrientation(getOrientationFromRotation(rotation))
            .build()

        val results = imageClassifier?.classify(tensorImage, imageProcessingOptions)
        inferenceTime = SystemClock.uptimeMillis() - inferenceTime

        if (results != null) {
            debugPrint(results)
        }


//        imageClassifierListener?.onResults(
//            results,
//            inferenceTime
//        )
    }

    // Receive the device rotation (Surface.x values range from 0->3) and return EXIF orientation
    // http://jpegclub.org/exif_orientation.html
    private fun getOrientationFromRotation(rotation: Int) : ImageProcessingOptions.Orientation {
        when (rotation) {
            Surface.ROTATION_270 ->
                return ImageProcessingOptions.Orientation.BOTTOM_RIGHT
            Surface.ROTATION_180 ->
                return ImageProcessingOptions.Orientation.RIGHT_BOTTOM
            Surface.ROTATION_90 ->
                return ImageProcessingOptions.Orientation.TOP_LEFT
            else ->
                return ImageProcessingOptions.Orientation.RIGHT_TOP
        }
    }

    fun  modelUsed() :String {
       // val modelName =

//            when (currentModel) {
//                MODEL_MOBILENETV1 -> "mobilenetClassv1.tflite"
//                MODEL_EFFICIENTNETV0 -> "efficientclass-lite0.tflite"
//                MODEL_EFFICIENTNETV1 -> "efficientclass-lite1.tflite"
//                MODEL_EFFICIENTNETV2 -> "efficientclass-lite2.tflite"
//                Model_InceptionV1-> "inception_v1_224_quant.tflite"
//                else -> "mobilenetClassv1.tflite"
//            }

        //return modelName

        return modelName
    }

    fun  deviceUsed() :String {
        if(currentDelegate==0)
            return "CPU"
        else  if(currentDelegate==1)
            return "GPU"

        else if(currentDelegate==2)
            return "NPU"

        return "CPU"
    }


    private var categories: MutableList<Category?> = mutableListOf()
    private var adapterSize: Int = 0
    fun debugPrint(results: MutableList<Classifications>) {

        val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss:SSS")
        var currentFolder: String? = context.getExternalFilesDir(null)!!.absolutePath
        var FILEPATH = currentFolder + File.separator + "NEW_Classification" + fileseries + ".csv"

        for ((i, obj) in results.withIndex()) {

           // val box = obj.boundingBox

            for ((j, category) in obj.categories.withIndex()) {

                Log.d(TAG, "    Label $j: ${category.label}")
                val confidence: Int = category.score.times(100).toInt()

                try {
                    PrintWriter(FileOutputStream(FILEPATH, true)).use { writer ->
                        val sbb = StringBuilder()
                        sbb.append(dateFormat.format(Date())).append(",").append(deviceUsed()).append(",").append("$j: ${category.label}")
                        sbb.append(',')
                        sbb.append( "${confidence}%" )
                        sbb.append('\n')
                        writer.write(sbb.toString())

                    }
                } catch (e: java.io.FileNotFoundException) {
                    println(e.message)
                }

            }




        }
    }


    interface ClassifierListener {
        fun onError(error: String)
        fun onResults(
            results: List<Classifications>?,
            inferenceTime: Long
        )
    }

    companion object {
        const val DELEGATE_CPU = 0
        const val DELEGATE_GPU = 1
        const val DELEGATE_NNAPI = 2
        const val MODEL_MOBILENETV1 = 0
        const val MODEL_EFFICIENTNETV0 = 1
        const val MODEL_EFFICIENTNETV1 = 2
        const val MODEL_EFFICIENTNETV2 = 3
        const val Model_InceptionV1=4

        private const val TAG = "ImageClassifierHelper"
    }
}
