package com.arcore.AI_ResourceControl;

import org.tensorflow.lite.examples.imagesegmentation.ImageSegmentationHelper

/**
 * Object to hold classifier object for recyclerview content
 * */
class AiItemsViewModel {
//    fun objectDetectorHelper(context: Any, fileseries: Any) {
//
//    }

    val models = listOf("mobilenet_v1_1.0_224", "MN v2 1.0 Q 224", "MN v1 1.0 Q 224",
      // "inception_v1_224_quant",
       "efficientdet-lite0",
//        "MN v1 0.25 Q 128", "mnasnet" ,"Inception v4 Quant", "Inception v4 Float", "Mobilenet v2 Float",
        "mobilenetDetv1", "efficientclass-lite0",
        "inception_v1_224_quant", "mobilenetClassv1", "segmen_deeplabv3"
        )
    val devices = listOf("CPU", "GPU", "NPU")
    var collector : BitmapCollector? = null
    var classifier: ImageClassifier? = null
    var currentDevice = 0
    var currentModel = 0
    var currentNumThreads = 1
    var  objectDetector : ObjectDetectorHelper? = null // first add new AI here
   // var  segmentation : ObjectDetectorHelper? = null
    var newclassifier: ImageClassifierHelper? = null
    var imgSegmentation: ImageSegmentationHelper?=null
    var throughput: Double = 0.0

    var gestureClas: GestureClassifierHelper?= null// "Gesture Classifier"
}

//steps:
/**
 * 1- here
 * 2- airecyvleviewadapter in line 198 load model: itemsView.models[3]->itemsView.objectDetector=ObjectDetectorHelper( context = mainActivity, fileseries = mainActivity.fileseries)
    2-2- device setting
    2-3 thread set


3- in bitmap collector :
3-1:
line 22 initiate
3-2: run the models indiuidually

4: in AiRecyclerviewAdapter:
4-1 line itemsView.collector = BitmapCollector() add the AI model as the new argument
4-2 line 152  // Disable the AI model while updating
5-  in Activitymain check for noNullModelRunner if at least one model is not null
 6- line 126 of airecyvleviewadapter when we run updateActiveModel, add the model!=null for not running the update

 *
 *
 */
