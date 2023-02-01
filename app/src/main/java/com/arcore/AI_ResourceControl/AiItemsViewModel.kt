package com.arcore.AI_ResourceControl;

import org.tensorflow.lite.examples.imagesegmentation.ImageSegmentationHelper

/**
 * Object to hold classifier object for recyclerview content
 * */
class AiItemsViewModel {
//    fun objectDetectorHelper(context: Any, fileseries: Any) {
//
//    }

    val models = listOf(

        ///"mobilenet_v1_1.0_224",
        "segmentor",
        "MN v2 1.0 Q 224", "MN v1 1.0 Q 224",
      // "inception_v1_224_quant",
       "efficientdet-lite0", "mobilenetDetv1",
//        "MN v1 0.25 Q 128", "mnasnet" ,"Inception v4 Quant", "Inception v4 Float", "Mobilenet v2 Float",

        "efficientclass-lite0",
        "inception_v1_224_quant", "mobilenetClassv1",
        "deeplabv3",
        //"lite-model_deeplabv3-xception65-ade20k",


        "model_metadata"
        )
    val devices = listOf("CPU", "GPU", "NPU")
    var collector : BitmapCollector? = null
    var classifier: ImageClassifier? = null
    var segm: ImageSegmentor? = null
    var currentDevice = 0
    var currentModel = 0
    var currentNumThreads = 1
    var  objectDetector : ObjectDetectorHelper? = null // first add new AI here
   // var  segmentation : ObjectDetectorHelper? = null
    var newclassifier: ImageClassifierHelper? = null
    var imgSegmentation: ImageSegmentationHelper?=null
    var throughput: Double = 0.0
    var overheadT: Double=0.0
    var inferenceT: Double=0.0
    var gestureClas: GestureClassifierHelper?= null// "Gesture Classifier"
}

//steps:
/**
 * 1- here
 * 2- AiRecyclerviewAdapter in line 198 load model: itemsView.models[3]->itemsView.objectDetector=ObjectDetectorHelper( context = mainActivity, fileseries = mainActivity.fileseries)
    2-2- device setting
    2-3 thread set
2-4 line itemsView.collector = BitmapCollector() add the AI model as the new argument
2-5line 152  // Disable the AI model while updating
2-6 line 126 of airecyvleviewadapter when we run updateActiveModel, add the model!=null for not running the update

3- in bitmap collector :
3-1:
line 22 initiate
3-2: run the models indiuidually

4-  in Activitymain check for noNullModelRunner if at least one model is not null


 *
 *
 */
