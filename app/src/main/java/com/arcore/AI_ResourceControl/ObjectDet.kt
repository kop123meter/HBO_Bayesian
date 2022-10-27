/**
 * Copyright 2021 Google LLC
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package  com.arcore.AI_ResourceControl

import android.graphics.*
import android.util.Log
import org.tensorflow.lite.support.image.TensorImage
import org.tensorflow.lite.task.vision.detector.Detection
import org.tensorflow.lite.task.vision.detector.ObjectDetector
import java.io.File
import java.io.FileOutputStream
import java.io.PrintWriter
import java.text.SimpleDateFormat
import java.util.*

//class ObjectDetector : AppCompatActivity(), View.OnClickListener {
//this class called by main activity

public  class ObjectDet(mInstance: MainActivity) {

    companion object {
        const val TAG = "TFLite - ODT"
        const val REQUEST_IMAGE_CAPTURE: Int = 1
        const val MAX_FONT_SIZE = 96F
    }
    val dateFormat = SimpleDateFormat("HH:mm")
    val fileseries: String = dateFormat.format(Date())

//    private lateinit var captureImageFab: Button
//    private lateinit var inputImageView: ImageView
//    private lateinit var imgSampleOne: ImageView
//    private lateinit var imgSampleTwo: ImageView
//    private lateinit var imgSampleThree: ImageView
//    private lateinit var tvPlaceholder: TextView
//    private lateinit var currentPhotoPath: String

    private var mInstance: MainActivity? = mInstance




//    override fun onCreate(savedInstanceState: Bundle?) {
//        super.onCreate(savedInstanceState)
    // setContentView(R.layout.activity_main)

//        write() // writes model.csv
//        captureImageFab = findViewById(R.id.captureImageFab)
//        inputImageView = findViewById(R.id.imageView)
//        imgSampleOne = findViewById(R.id.imgSampleOne)
//        imgSampleTwo = findViewById(R.id.imgSampleTwo)
//        imgSampleThree = findViewById(R.id.imgSampleThree)
//        tvPlaceholder = findViewById(R.id.tvPlaceholder)
//
//        captureImageFab.setOnClickListener(this)
//        imgSampleOne.setOnClickListener(this)
//        imgSampleTwo.setOnClickListener(this)
//        imgSampleThree.setOnClickListener(this)
//    }

//     fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
//        super.onActivityResult(requestCode, resultCode, data)
//        if (requestCode == REQUEST_IMAGE_CAPTURE &&
//            resultCode == Activity.RESULT_OK
//        ) {
//            setViewAndDetect(getCapturedImage())
//        }
//    }

    /**
     * onClick(v: View?)
     *      Detect touches on the UI components
     */
//    override fun onClick(v: View?) {
//        when (v?.id) {
//            R.id.captureImageFab -> {
//                try {
//                    dispatchTakePictureIntent()
//                } catch (e: ActivityNotFoundException) {
//                    Log.e(TAG, e.message.toString())
//                }
//            }
//            R.id.imgSampleOne -> {
//                setViewAndDetect(getSampleImage(R.drawable.img_meal_one))
//            }
//            R.id.imgSampleTwo -> {
//                setViewAndDetect(getSampleImage(R.drawable.img_meal_two))
//            }
//            R.id.imgSampleThree -> {
//                setViewAndDetect(getSampleImage(R.drawable.img_meal_three))
//            }
//        }
//    }

    /**
     * runObjectDetection(bitmap: Bitmap)
     *      TFLite Object Detection function
     */
    fun runObjectDetection(bitmap: Bitmap) {
        // Step 1: Create TFLite's TensorImage object
        val image = TensorImage.fromBitmap(bitmap)

        // Step 2: Initialize the detector object
        val options = ObjectDetector.ObjectDetectorOptions.builder()
            .setMaxResults(5)
            .setScoreThreshold(0.3f)
            .build()
        val detector = ObjectDetector.createFromFileAndOptions(
            mInstance,
            "mobilenetv1.tflite",
            //"salad.tflite",
            options
        )

        // Step 3: Feed given image to the detector
        val results = detector.detect(image)

        debugPrint(results)
/*
        // Step 4: Parse the detection result and show it
        val resultToDisplay = results.map {
            // Get the top-1 category and craft the display text
            val category = it.categories.first()
            val text = "${category.label}, ${category.score.times(100).toInt()}%"

            // Create a data object to display the detection result
            DetectionResult(it.boundingBox, text)
        }
        // Draw the detection result on the bitmap and show it.
        val imgWithResult = drawDetectionResult(bitmap, resultToDisplay)
        runOnUiThread {
            inputImageView.setImageBitmap(imgWithResult)
        }*/
    }

    /**
     * debugPrint(visionObjects: List<Detection>)
     *      Print the detection result to logcat to examine
     */


//        String currentFolder = getExternalFilesDir(null).getAbsolutePath();
//        String FILEPATH = currentFolder + File.separator + "CPU_Mem_"+ fileseries+".csv";
//        try (PrintWriter writer = new PrintWriter(new FileOutputStream(FILEPATH, false))) {
//
//            StringBuilder sbb = new StringBuilder();
//
//           sbb.append( "time,7m,PID,USER,PR,NI,VIRT,[RES],SHR,S,%CPU,%MEM,TIME,ARGS");
//
//
//            sbb.append('\n');
//            writer.write(sbb.toString());
//
//            System.out.println("done!");
//
//        } catch (FileNotFoundException e) {
//            System.out.println(e.getMessage());
//        }



    fun debugPrint(results : List<Detection>) {


        var currentFolder: String? = mInstance?.getExternalFilesDir(null)!!.absolutePath
        var FILEPATH = currentFolder + File.separator + "model_" + fileseries + ".csv"

        for ((i, obj) in results.withIndex()) {

            val box = obj.boundingBox

            try {
                PrintWriter(FileOutputStream(FILEPATH, true)).use { writer ->
                    val sbb = StringBuilder()
                    sbb.append("${i}")
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

    /**
     * setViewAndDetect(bitmap: Bitmap)
     *      Set image to view and call object detection
     */


    /**
     * getCapturedImage():
     *      Decodes and crops the captured image from camera.
     */


    /**
     * getSampleImage():
     *      Get image form drawable and convert to bitmap.
     */


    /**
     * rotateImage():
     *     Decodes and crops the captured image from camera.
     */
    fun rotateImage(source: Bitmap, angle: Float): Bitmap {
        val matrix = Matrix()
        matrix.postRotate(angle)
        return Bitmap.createBitmap(
            source, 0, 0, source.width, source.height,
            matrix, true
        )
    }

    /**
     * createImageFile():
     *     Generates a temporary image file for the Camera app to write to.
     */
//    @Throws(IOException::class)
//    private fun createImageFile(): File {
//        // Create an image file name
//        val timeStamp: String = SimpleDateFormat("yyyyMMdd_HHmmss").format(Date())
//        val storageDir: File? = mInstance?.getExternalFilesDir(Environment.DIRECTORY_PICTURES)
//        return File.createTempFile(
//            "JPEG_${timeStamp}_", /* prefix */
//            ".jpg", /* suffix */
//            storageDir /* directory */
//        ).apply {
//            // Save a file: path for use with ACTION_VIEW intents
//            currentPhotoPath = absolutePath
//        }
//    }

    /**
     * dispatchTakePictureIntent():
     *     Start the Camera app to take a photo.
     */
//    private fun dispatchTakePictureIntent() {
//        Intent(MediaStore.ACTION_IMAGE_CAPTURE).also { takePictureIntent ->
//            // Ensure that there's a camera activity to handle the intent
//            mInstance?.let {
//                takePictureIntent.resolveActivity(it.packageManager)?.also {
//                    // Create the File where the photo should go
//                    val photoFile: File? = try {
//                        createImageFile()
//                    } catch (e: IOException) {
//                        Log.e(TAG, e.message.toString())
//                        null
//                    }
//                    // Continue only if the File was successfully created
//                    photoFile?.also {
//                        val photoURI: Uri = FileProvider.getUriForFile(
//                            mInstance!!,
//                            "org.tensorflow.codelabs.objectdetection.fileprovider",
//                            it
//                        )
//                        takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, photoURI)
//                        mInstance?.startActivityForResult(takePictureIntent, REQUEST_IMAGE_CAPTURE)
//                    }
//                }
//            }
//        }
//    }

    /**
     * drawDetectionResult(bitmap: Bitmap, detectionResults: List<DetectionResult>
     *      Draw a box around each objects and show the object's name.
     */
 
}

/**
 * DetectionResult
 *      A class to store the visualization info of a detected object.
 */
//data class DetectionResult(val boundingBox: RectF, val text: String)
