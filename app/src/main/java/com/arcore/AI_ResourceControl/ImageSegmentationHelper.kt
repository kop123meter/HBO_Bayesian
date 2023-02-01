/*
 * Copyright 2022 The TensorFlow Authors. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.tensorflow.lite.examples.imagesegmentation

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Color
import android.os.SystemClock
import android.os.Build
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.core.graphics.get
import org.tensorflow.lite.gpu.CompatibilityList
import org.tensorflow.lite.support.image.ImageProcessor
import org.tensorflow.lite.support.image.TensorImage
import org.tensorflow.lite.support.image.ops.Rot90Op
import org.tensorflow.lite.task.core.BaseOptions
import org.tensorflow.lite.task.vision.segmenter.ImageSegmenter
import org.tensorflow.lite.task.vision.segmenter.OutputType
import org.tensorflow.lite.task.vision.segmenter.Segmentation
import java.io.File
import java.io.FileOutputStream
import java.io.PrintWriter
import java.text.SimpleDateFormat
import java.util.*

/**
 * Class responsible to run the Image Segmentation model. more information about the DeepLab model
 * being used can be found here:
 * https://ai.googleblog.com/2018/03/semantic-image-segmentation-with.html
 * https://github.com/tensorflow/models/tree/master/research/deeplab
 *
 * Label names: 'background', 'aeroplane', 'bicycle', 'bird', 'boat', 'bottle', 'bus', 'car', 'cat',
 * 'chair', 'cow', 'diningtable', 'dog', 'horse', 'motorbike', 'person', 'pottedplant', 'sheep',
 * 'sofa', 'train', 'tv'
 */
class ImageSegmentationHelper(
    var numThreads: Int = 1,
    var currentDelegate: Int = 0,
    val context: Context,
    val fileseries: String ,
    var modelName: String,
    //val imageSegmentationListener: SegmentationListener?
) {
    private var imageSegmenter: ImageSegmenter? = null

    init {
        setupImageSegmenter()
    }

    fun clearImageSegmenter() {
        imageSegmenter = null
    }

    private fun setupImageSegmenter() {
        // Create the base options for the segment
        val optionsBuilder =
            ImageSegmenter.ImageSegmenterOptions.builder()

        // Set general segmentation options, including number of used threads
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
//                    imageSegmentationListener?.onError("GPU is not supported on this device")
                }
            }
            DELEGATE_NNAPI -> {
                baseOptionsBuilder.useNnapi()
            }
        }

        optionsBuilder.setBaseOptions(baseOptionsBuilder.build())

        /*
        CATEGORY_MASK is being specifically used to predict the available objects
        based on individual pixels in this sample. The other option available for
        OutputType, CONFIDENCE_MAP, provides a gray scale mapping of the image
        where each pixel has a confidence score applied to it from 0.0f to 1.0f
         */
        optionsBuilder.setOutputType(OutputType.CATEGORY_MASK)
        try {
            imageSegmenter =
                ImageSegmenter.createFromFileAndOptions(
                    context,
                    modelName,
                    optionsBuilder.build()
                )
        } catch (e: IllegalStateException) {
//            imageSegmentationListener?.onError(
//                "Image segmentation failed to initialize. See error logs for details"
//            )
            Log.e(TAG, "TFLite failed to load model with error: " + e.message)
        }
    }

    fun  modelUsed() : String {
        return modelName
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

   // @RequiresApi(Build.VERSION_CODES.Q)
    fun segment(image: Bitmap, imageRotation: Int) {

        if (imageSegmenter == null) {
            setupImageSegmenter()
        }

        // Inference time is the difference between the system time at the start and finish of the
        // process
        var inferenceTime = SystemClock.uptimeMillis()

        // Create preprocessor for the image.
        // See https://www.tensorflow.org/lite/inference_with_metadata/
        //            lite_support#imageprocessor_architecture
        val imageProcessor =
            ImageProcessor.Builder()
                .add(Rot90Op(-imageRotation / 90))
                .build()

        // Preprocess the image and convert it into a TensorImage for segmentation.
        val tensorImage = imageProcessor.process(TensorImage.fromBitmap(image))

        val segmentResult = imageSegmenter?.segment(tensorImage)
        inferenceTime = SystemClock.uptimeMillis() - inferenceTime
    //   Log.d("inference_Time", " $inferenceTime" +"ms on "+ deviceUsed())

//   nil created this commented to not consider it for inference time    if (segmentResult != null) {
//           debugPrint(segmentResult)
//       }


    }


    fun debugPrint(results: MutableList<Segmentation>) {

        val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss:SSS")
        var currentFolder: String? = context.getExternalFilesDir(null)!!.absolutePath
        var FILEPATH = currentFolder + File.separator + "Image Segmentation" + fileseries + ".csv"

//        for ((i, obj) in results.withIndex()) {
//            for ((j, category) in obj.masks.withIndex()) {

//            }
//        }

        if (results.isNotEmpty()) {
            val colorLabels = results[0].coloredLabels.mapIndexed { index, coloredLabel ->
                ColorLabel(
                    index,
                    coloredLabel.getlabel(),
                    coloredLabel.argb
                )

                try {
                    PrintWriter(FileOutputStream(FILEPATH, true)).use { writer ->
                        val sbb = StringBuilder()
                        sbb.append(dateFormat.format(Date())).append(",").append(deviceUsed())
                        sbb.append(',').append(index).append(",").append(coloredLabel.getlabel()).append(",")
                            .append(   coloredLabel.argb)
                        //   sbb.append( "${category.colorSpaceType}" )
                        sbb.append('\n')
                        writer.write(sbb.toString())
                    }
                } catch (e: java.io.FileNotFoundException) {
                    println(e.message)
                }


            }



//            val maskTensor = results[0].masks[0]
//            val maskArray = maskTensor.buffer.array()
//            val pixels = IntArray(maskArray.size)
//
//            for (i in maskArray.indices) {
//                // Set isExist flag to true if any pixel contains this color.
//                val colorLabel = colorLabels[maskArray[i].toInt()].apply {
//
//                }
//                val color = colorLabel.getColor()
//                pixels[i] = color
//
//            }


        }
    }


    data class ColorLabel(
        val id: Int,
        val label: String,
        val rgbColor: Int,
        var isExist: Boolean = false

    ) {

        fun getColor(): Int {
            // Use completely transparent for the background color.
            return if (id == 0) Color.TRANSPARENT else Color.argb(
                128,
                Color.red(rgbColor),
                Color.green(rgbColor),
                Color.blue(rgbColor)
            )
        }
    }

    interface SegmentationListener {
        fun onError(error: String)
        fun onResults(
            results: List<Segmentation>?,
            inferenceTime: Long,
            imageHeight: Int,
            imageWidth: Int
        )
    }

    companion object {
        const val DELEGATE_CPU = 0
        const val DELEGATE_GPU = 1
        const val DELEGATE_NNAPI = 2
       // const val MODEL_DEEPLABV3 = "deeplabv3.tflite"

        //deeplabv3-mobilenetv2-ade20k_1_
     //   val modelName = null
           //"munet_mnv3_wm10.tflite"
           //"deeplabv3.tflite"

        private const val TAG = "Image Segmentation Helper"
    }
}
