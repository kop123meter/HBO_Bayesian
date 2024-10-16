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
import org.tensorflow.lite.examples.imagesegmentation.ImageSegmentationHelper
import org.tensorflow.lite.gpu.CompatibilityList
import org.tensorflow.lite.support.image.ImageProcessor
import org.tensorflow.lite.support.image.TensorImage
import org.tensorflow.lite.support.image.ops.Rot90Op
import org.tensorflow.lite.support.label.Category
import org.tensorflow.lite.task.core.BaseOptions
import org.tensorflow.lite.task.vision.classifier.Classifications
import org.tensorflow.lite.task.vision.classifier.ImageClassifier
import kotlin.math.min

class GestureClassifierHelper(
    var threshold: Float = 0.5f,
    var numThreads: Int = 2,
    var maxResults: Int = 3,
    var currentDelegate: Int = 0,
    val context: Context,
    val fileseries: String ,
//    val gestureClassifierListener: ClassifierListener?
) {
    private var gestureClassifier: ImageClassifier? = null

    init {
        setupGestureClassifier()
    }

    fun clearGestureClassifier() {
        gestureClassifier = null
    }

    private fun setupGestureClassifier() {
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
//                    gestureClassifierListener?.onError(
//                        "GPU is not supported on " +
//                                "this device"
//                    )
                    Log.d("","GPU is not supported on \" +\n" + "this device")
                }
            }
            DELEGATE_NNAPI -> {
                baseOptionsBuilder.useNnapi()
            }
        }

        optionsBuilder.setBaseOptions(baseOptionsBuilder.build())

        try {
            gestureClassifier =
                ImageClassifier.createFromFileAndOptions(
                    context, MODEL_PATH,
                    optionsBuilder.build()
                )
        } catch (e: IllegalStateException) {
//            gestureClassifierListener?.onError(
//                "Gesture classifier failed to initialize. See error logs for " +
//                        "details"
//            )
            Log.e(TAG, "TFLite failed to load model with error: " + e.message)
        }
    }

    fun classify(image: Bitmap, imageRotation: Int) {
        if (gestureClassifier == null) {
            setupGestureClassifier()
        }

        // Inference time is the difference between the system time at the start and finish of the
        // process
        var inferenceTime = SystemClock.uptimeMillis()

        // Create preprocessor for the image.
        // See https://www.tensorflow.org/lite/inference_with_metadata/
        //            lite_support#imageprocessor_architecture
        val imageProcessor =
            ImageProcessor.Builder()
                .add(Rot90Op(imageRotation / 90))
                .build()

        // Preprocess the image and convert it into a TensorImage for classification.
        val tensorImage = imageProcessor.process(TensorImage.fromBitmap(image))

        val results = gestureClassifier?.classify(tensorImage)
        inferenceTime = SystemClock.uptimeMillis() - inferenceTime
       // Log.d("inference_Time", " $inferenceTime" +"ms on "+ deviceUsed())


//    nil created this commented to not consider it for inference time     var categories: MutableList<Category?> = mutableListOf()
//        categories = MutableList(2) { null }
//        results?.let { it ->
//            if (it.isNotEmpty()) {
//                val sortedCategories = it[0].categories.sortedBy { it?.index }
//                val min = min(sortedCategories.size, categories.size)
//                for (i in 0 until min) {
//                    categories[i] = sortedCategories[i]
//                    Log.d("category"," ${categories.get(i)?.label}"+ ", score= ${categories.get(i)?.score}")
//
//                }
//            }
//        }







//        gestureClassifierListener?.onResults(results, inferenceTime)
    }


    fun  modelUsed() :String {
        return MODEL_PATH
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

        private const val MODEL_PATH = "model_metadata.tflite"
        private const val TAG = "GestureClassifierHelper"
    }
}
