package com.arcore.AI_ResourceControl;

import android.app.Activity
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import android.widget.AbsListView.CHOICE_MODE_SINGLE
import androidx.recyclerview.widget.RecyclerView
import kotlinx.coroutines.delay
import org.tensorflow.lite.examples.imagesegmentation.ImageSegmentationHelper
import java.io.IOException
import java.lang.Thread.sleep

/**
 * Adapter for RecyclerView to populate ai_settings_card_view_design.xml
 * Heavy lifting for instantiating the producer:consumer (BitmapSource:BitmapCollector) relationship
 *
 * Holds the BitmapCollector, changes to the ImageClassifier model happen here
 */
class AiRecyclerviewAdapter(
    var mList: MutableList<AiItemsViewModel>,
    val streamSource: DynamicBitmapSource/*BitmapSource*/,
    val mainActivity: MainActivity,
    val activity: Activity
) : RecyclerView.Adapter<AiRecyclerviewAdapter.ViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        // inflates the ai_settings_card_view_design view
        // that is used to hold list item
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.ai_settings_card_view_design, parent, false)

        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        // binds the list items to a view
        val itemsViewModel = mList[position]
        holder.modelListView.choiceMode = CHOICE_MODE_SINGLE
        holder.deviceListView.choiceMode = CHOICE_MODE_SINGLE
        val modelAdapter = ArrayAdapter(
            holder.modelListView.context,
            R.layout.ai_settings_listview_row,
            R.id.listview_row_text,
            itemsViewModel.models
        )
        val deviceAdapter = ArrayAdapter(
            holder.modelListView.context,
            R.layout.ai_settings_listview_row,
            R.id.listview_row_text,
            itemsViewModel.devices
        )

        holder.modelListView.adapter = modelAdapter
        holder.deviceListView.adapter = deviceAdapter
        holder.numberPicker.minValue = 1
        holder.numberPicker.maxValue = 10
        holder.numberPicker.wrapSelectorWheel = true
        holder.numberPicker.value = itemsViewModel.currentNumThreads
        holder.modelListView.setItemChecked(itemsViewModel.currentModel, true)
        holder.deviceListView.setItemChecked(itemsViewModel.currentDevice, true) // 0 = gpu


        // set current consumer to device[0] and model[0] from ItemsViewModel
//        initializeActiveModel(itemsViewModel)
//        itemsViewModel.classifier?.numThreads = holder.numberPicker.value

        // update consumer when new options are selected
        holder.numberPicker.setOnValueChangedListener {
                picker, oldVal, newVal -> updateActiveModel(holder, itemsViewModel, position)
        }
        holder.modelListView.setOnItemClickListener {
                parent, view, pos, id -> updateActiveModel(holder, itemsViewModel, position)
        }
        holder.deviceListView.setOnItemClickListener {
                parent, view, pos, id -> updateActiveModel(holder, itemsViewModel, position)
        }
//        holder.toggle.setOnCheckedChangeListener { buttonView, isChecked ->
//            itemsViewModel.runCollection = isChecked
//        }

        holder.textAiInfo.text = "Threads: ${itemsViewModel.classifier?.numThreads}\n" +
                "Model: ${itemsViewModel.classifier?.modelName}\n" +
                "Device: ${itemsViewModel.classifier?.device}"
    }

    /**
     * return amount of items in the recyclerview list
     */
    override fun getItemCount(): Int {
        return mList.size
    }

    /**
     * Holds the views for adding it to image and text
     */
    class ViewHolder(ItemView: View) : RecyclerView.ViewHolder(ItemView) {
        var modelListView: ListView = itemView.findViewById(R.id.model)
        val deviceListView: ListView = itemView.findViewById(R.id.device)
        val numberPicker: NumberPicker= itemView.findViewById(R.id.numberPicker_aiThreadCount)
        val textAiInfo : TextView = itemView.findViewById(R.id.textView_aiModelInfo)
      //  val toggle : Switch = itemView.findViewById(R.id.switchToggleCollection)
    }

    /**
     * Initializes model to default parameters
     */
//    fun initializeActiveModel(itemsView: AiItemsViewModel, position: Int) {
//        itemsView.currentModel = 0
//        itemsView.currentDevice = 0
//        itemsView.currentNumThreads = 1
//        itemsView.classifier=ImageClassifierFloatMobileNet(activity)
//        itemsView.classifier?.numThreads = 1
//        itemsView.collector = BitmapCollector(streamSource, itemsView.classifier, position, activity, mainActivity, itemsView.objectDetector )
//        itemsView.classifier?.useCPU()
//    }

    /**
     * Updates model to parameters chosen by pressing ai_settings_card_view_design
     * Stops collector, stops bitmap stream
     */



    fun updateActiveModel(modelIndex: Int, deviceIndex: Int, numThreads: Int, itemsView : AiItemsViewModel, position: Int) {
        val switchToggleStream = activity.findViewById<Switch>(R.id.switch_streamToggle)

//        if(itemsView.collector?.run == true) {
//            itemsView.collector?.pauseCollect()
        if (switchToggleStream.isChecked) {
//            switchToggleStream.isChecked = false
            itemsView.collector?.pauseCollect()
            sleep(50)
        }
//        }

        // Do not update if there is no change /
        if (modelIndex == itemsView.currentModel
            && deviceIndex == itemsView.currentDevice
            && numThreads == itemsView.currentNumThreads
            && ( itemsView.classifier != null   || itemsView.newclassifier != null  || itemsView.objectDetector != null|| itemsView.imgSegmentation != null  )

        ) {
            return
        }
        itemsView.currentModel = modelIndex
        itemsView.currentDevice = deviceIndex
        itemsView.currentNumThreads = numThreads


        // Disable classifier while updating  ... ? should I add this for detector and newClassifier? yes
        if (itemsView.classifier != null) {
            itemsView.classifier?.close()
            itemsView.classifier = null
        }

        else if (itemsView.newclassifier != null) {
//            itemsView.newclassifier?.close()
            itemsView.newclassifier = null
        }

        else if (itemsView.objectDetector != null) {
//            itemsView.objectDetector?.close()
            itemsView.objectDetector = null
        }

        else if (itemsView.imgSegmentation != null) {
//            itemsView.objectDetector?.close()
            itemsView.imgSegmentation = null
        }


        // Lookup names of parameters.
        val model: String = itemsView.models[itemsView.currentModel]
        val device: String = itemsView.devices[itemsView.currentDevice]
        val threads = itemsView.currentNumThreads


        // Try to load model.
        try {
            when(model) {
                itemsView.models[0]->itemsView.classifier=ImageClassifierFloatMobileNet(activity)
                itemsView.models[1]->itemsView.classifier=ImageClassifierQuantizedMobileNetV2_1_0_224(activity)
                itemsView.models[2]->itemsView.classifier=ImageClassifierQuantizedMobileNet(activity)
               // itemsView.models[3]->itemsView.classifier=ImageClassifier_Inception_V1_Quantized_224(activity)
//                itemsView.models[4]->itemsView.classifier=ImageClassifierQuantizedMobileNetV1_25_0_128(activity)
//                itemsView.models[5]->itemsView.classifier=ImageClassifier_mnasnet_05_224(activity)
//                itemsView.models[6]->itemsView.classifier=ImageClassifier_Inception_V4_Quantized_299(activity)
//                itemsView.models[7]->itemsView.classifier=ImageClassifier_Inception_v4_Float_299(activity)
//                itemsView.models[8]->itemsView.classifier=ImageClassifier_MobileNet_V2_Float_224(activity)
//                itemsView.models[3]->itemsView.objectDetector=ObjectDetectorHelper( context = mainActivity, fileseries = mainActivity.fileseries)

            }
        } catch (e: IOException) {
            Log.d(
                "Custom Adapter",
                "Failed to load",
                e
            )
            itemsView.classifier = null
        }
//***************** for object detector
        try {
            when(model) {
                 itemsView.models[4]-> {
                     itemsView.objectDetector = ObjectDetectorHelper(
                         context = mainActivity, fileseries = mainActivity.fileseries,modelName =itemsView.models[4]+".tflite"  )
                    // itemsView.objectDetector!!.currentModel=0 // need to update the model too
                    //itemsView.objectDetector?.clearObjectDetector()
                 }


                itemsView.models[3]-> {
                    itemsView.objectDetector = ObjectDetectorHelper(
                        context = mainActivity, fileseries = mainActivity.fileseries,modelName =itemsView.models[3]+".tflite"  )
                    // itemsView.objectDetector!!.currentModel=0 // need to update the model too
                    //itemsView.objectDetector?.clearObjectDetector()
                }

            }
        } catch (e: IOException) {
            Log.d(
                "Custom Adapter",
                "Failed to load",
                e
            )
            itemsView.objectDetector = null
        }

//***************** for object detector

//***************** for new object CLASSIFICATION
        try {
            when(model) {
                itemsView.models[7]->
                {itemsView.newclassifier=ImageClassifierHelper( context = mainActivity,fileseries = mainActivity.fileseries, modelName =itemsView.models[7]+".tflite" )
                    //itemsView.newclassifier!!.currentModel=1 // need to update the model too
                    itemsView.newclassifier?.clearImageClassifier()
                }

                itemsView.models[5]->
                {itemsView.newclassifier=ImageClassifierHelper( context = mainActivity,fileseries = mainActivity.fileseries,modelName =itemsView.models[5]+".tflite")
                    //itemsView.newclassifier!!.currentModel=1 // need to update the model too
                    itemsView.newclassifier?.clearImageClassifier()
                }
                itemsView.models[6]->{itemsView.newclassifier=ImageClassifierHelper( context = mainActivity,fileseries = mainActivity.fileseries,modelName =itemsView.models[6]+".tflite")
                    itemsView.newclassifier?.clearImageClassifier()
                }

            }
        } catch (e: IOException) {
            Log.d(
                "Custom Adapter",
                "Failed to load",
                e
            )
            itemsView.newclassifier = null
        }

        try {
            when(model) {
                itemsView.models[8]->
                {itemsView.imgSegmentation=ImageSegmentationHelper( context = mainActivity,fileseries = mainActivity.fileseries )
                    itemsView.imgSegmentation?.clearImageSegmenter()
                }
            }
        } catch (e: IOException) {
            Log.d(
                "Custom Adapter",
                "Failed to load",
                e
            )
            itemsView.imgSegmentation = null
        }


//***************** for new object CLASSIFICATION

        if (itemsView.classifier != null) {
            itemsView.classifier?.fileseries = mainActivity.fileseries
            itemsView.classifier?.instance = mainActivity
        }

        when(device) {
            itemsView.devices[0]-> { itemsView.classifier?.useCPU()
                itemsView.objectDetector?.currentDelegate = 0
                // Needs to be cleared instead of reinitialized because the GPU delegate needs to be initialized on the thread using it when applicable
                itemsView.objectDetector?.clearObjectDetector()

                itemsView.newclassifier?.currentDelegate = 0
                // Needs to be cleared instead of reinitialized because the GPU delegate needs to be initialized on the thread using it when applicable
                itemsView.newclassifier?.clearImageClassifier()

                itemsView.imgSegmentation?.currentDelegate = 0
                // Needs to be cleared instead of reinitialized because the GPU delegate needs to be initialized on the thread using it when applicable
                itemsView.imgSegmentation?.clearImageSegmenter()




            }
            itemsView.devices[1]-> {itemsView.classifier?.useGpu()
                itemsView.objectDetector?.currentDelegate = 1
                itemsView.objectDetector?.clearObjectDetector()

                itemsView.newclassifier?.currentDelegate = 1
                // Needs to be cleared instead of reinitialized because the GPU delegate needs to be initialized on the thread using it when applicable
                itemsView.newclassifier?.clearImageClassifier()

                itemsView.imgSegmentation?.currentDelegate = 1
                // Needs to be cleared instead of reinitialized because the GPU delegate needs to be initialized on the thread using it when applicable
                itemsView.imgSegmentation?.clearImageSegmenter()


            }
            itemsView.devices[2]-> {
                itemsView.classifier?.useNNAPI()
                itemsView.objectDetector?.currentDelegate =2
                itemsView.objectDetector?.clearObjectDetector()

                itemsView.newclassifier?.currentDelegate = 2
                // Needs to be cleared instead of reinitialized because the GPU delegate needs to be initialized on the thread using it when applicable
                itemsView.newclassifier?.clearImageClassifier()

                itemsView.imgSegmentation?.currentDelegate = 2
                // Needs to be cleared instead of reinitialized because the GPU delegate needs to be initialized on the thread using it when applicable
                itemsView.imgSegmentation?.clearImageSegmenter()



            }


        }

        itemsView.classifier?.numThreads = threads
        itemsView.objectDetector?.numThreads= threads // object detector
        itemsView.objectDetector?.clearObjectDetector()
        itemsView.newclassifier?.numThreads= threads // NEW object classification
        itemsView.newclassifier?.clearImageClassifier()
        itemsView.imgSegmentation?.numThreads= threads // NEW object classification
        itemsView.imgSegmentation?.clearImageSegmenter()



        // the collector generally runs for all AI models but inside it we have a condition to run just the models we have
        itemsView.collector = BitmapCollector(streamSource, itemsView.classifier, position, activity, mainActivity,itemsView.objectDetector,itemsView.newclassifier, itemsView.imgSegmentation)
        if (switchToggleStream.isChecked) {
            itemsView.collector!!.startCollect()
        }
    }

    private fun updateActiveModel(holder: ViewHolder, itemsView : AiItemsViewModel, position: Int) { // update changes in model's setting- add for each model
        updateActiveModel(holder.modelListView.checkedItemPosition, holder.deviceListView.checkedItemPosition, holder.numberPicker.value, itemsView, position)

       if(itemsView.classifier!=null)
        holder.textAiInfo.text = "Object Classificatin\n"+
            "Threads: ${itemsView.classifier?.numThreads}\n" +
                "Model: ${itemsView.classifier?.modelName}\n" +
                "Device: ${itemsView.classifier?.device}\n"

      else if(itemsView.objectDetector!=null)
          holder.textAiInfo.text = "Object detection\n"+ "Threads: ${itemsView.objectDetector?.numThreads}\n" +
                  "Model: ${itemsView.objectDetector?.modelUsed()}\n" +
                  "Device: ${itemsView.objectDetector?.deviceUsed()}"

       else if(itemsView.newclassifier!=null)
           holder.textAiInfo.text = "NEW Image classification\n"+ "Threads: ${itemsView.newclassifier?.numThreads}\n" +
                   "Model: ${itemsView.newclassifier?.modelUsed()}\n" +
                   "Device: ${itemsView.newclassifier?.deviceUsed()}"
       else if(itemsView.imgSegmentation!=null)
           holder.textAiInfo.text = "Image Segmentation \n"+ "Threads: ${itemsView.imgSegmentation?.numThreads}\n" +
                   "Model: ${itemsView.imgSegmentation?.modelUsed()}\n" +
                   "Device: ${itemsView.imgSegmentation?.deviceUsed()}"


    }
}
