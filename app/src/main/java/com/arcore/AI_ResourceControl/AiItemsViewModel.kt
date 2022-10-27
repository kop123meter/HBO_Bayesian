package com.arcore.AI_ResourceControl;

/**
 * Object to hold classifier object for recyclerview content
 * */
class AiItemsViewModel {
    val models = listOf("MN V1 1.0 F 224", "MN v2 1.0 Q 224", "MN v1 1.0 Q 224", "IN V1 Q 224",
        "MN v1 0.25 Q 128", "mnasnet" ,"Inception v4 Quant", "Inception v4 Float",
        "Mobilenet v2 Float","mobilenetv1")
    val devices = listOf("CPU", "GPU", "NPU")
    var collector : BitmapCollector? = null
    var classifier: ImageClassifier? = null
    var currentDevice = 0
    var currentModel = 0
    var currentNumThreads = 1
}