 ///
 /**
  * This is to test if we can use weight from linear regression of model specific : thr and dis-vs tris
  This is  balancer with per AI model trained considering the latest bins
  * the measured W is omitted and instead we rely on estimated one. We hope that with parameterts that come from the newest data,
  * we'll have a correct order of impacted AI models as well as a better variation in Wi instead of stable one
  *NNNNNNOOOOOOTTTTTTEEEEe that if you use updated total tris for datacollection,
  */

/// for now it is original balance function and is not changed
package com.arcore.AI_ResourceControl;
/*This code has relation ship between AI throughput and distance and triangle count*/
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.PrintWriter;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.stream.Collectors;

import static java.lang.Math.abs;
import static java.lang.Math.max;
import static java.lang.Math.min;
import static java.lang.Math.pow;
import static java.lang.Math.sqrt;
import static java.lang.Thread.sleep;

import android.annotation.SuppressLint;
import android.os.CountDownTimer;
import android.widget.TextView;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Iterables;
import com.google.common.collect.ListMultimap;

public class bayesian implements Runnable {

    private final MainActivity mInstance;


    public bayesian(MainActivity mInstance) {

        this.mInstance = mInstance;


    }
    @SuppressLint("SuspiciousIndentation")
    @Override

    public void run() {

                if(!mInstance.stopwrite_datacollect)
                   writeRT();//starting the timer, we write the data collected for the last 10s period while all models were fixed: this is to write the results for the current combination selection


                else  {
                    mInstance.stopwrite_datacollect = false;
                }

                if(mInstance.combinations.size()==0 && mInstance.ratioArray.get(0)==0.5f &&  !mInstance.stopwrite_datacollect)/// when we checked all the combinations
                {
                    //mInstance.combinations =  mInstance.combinations_copy;// all of these assignments should be before decAll since it takes sometime and we want to have our flags true/false sooner
                    mInstance.combinations =  mInstance.combinations_copy.stream().collect(Collectors.toList());
                    mInstance.stop_thread=true;// to know the transition from 0.3->1
                    mInstance.stopwrite_datacollect=true;// to not do anything in this timer since we want to wait for all objects to be decimated
                    // mInstance.runOnUiThread(() ->
                    mInstance.decimateAll(0.1f);// reduce total triangle count

                }

               else if(mInstance.combinations.size()==0&&  !mInstance.stop_thread)/// the first simplification
                {
                    //mInstance.combinations =  mInstance.combinations_copy;
                    mInstance.combinations =  mInstance.combinations_copy.stream().collect(Collectors.toList());

                    mInstance.stopwrite_datacollect=true;// to not do anything in this timer since we want to wait for all objects to be decimated
                    //      mInstance.runOnUiThread(() ->
                    mInstance. decimateAll(0.5f);// reduce total triangle count


                }

                else  if(mInstance.combinations.size()==0&& mInstance.stop_thread)///the last condition to stop= when we checked all the combinations
                {
                    mInstance.stopTimer = true;
                    mInstance.stopwrite_datacollect=true;// to end this and avoid getting into the next if
                }

                if(!mInstance.stopwrite_datacollect){
                    List<Integer> assigned_dev=new ArrayList<>(mInstance.combinations.get(0));// get the first device

                    // this loop is to change the AI task delegate for one combination: one by one
                    for (int i=0;i<assigned_dev.size();i++){// we have each list of digits such as 000 or 003 with size of tasks, so if 00 is te digit, the task count is 2

                        AiItemsViewModel taskView = mInstance.mList.get(i);// the first to the last task
                        int model= (taskView.getCurrentModel());
                        int device=taskView.getCurrentDevice();
                        int new_device=assigned_dev.get(i);
                        if(device!=new_device)// this means that the model should be updated

                        {
                            mInstance.adapter.setMList(mInstance.mList);
                            mInstance.recyclerView_aiSettings.setAdapter(mInstance.adapter);

//                            mInstance.runOnUiThread(() -> mInstance.renderArray.get(i).decimatedModelRequest(mInstance.ratioArray.get(i), i, false));
                            int finalI = i;
                            mInstance.runOnUiThread(() ->
                                    mInstance. adapter.updateActiveModel(
                                    model,
                                    new_device,
                                    1,
                                    taskView,
                                            finalI
                            ));
                        }
                        try {
                            sleep(10);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }// this is to make sure the device is updated

                    }


                    mInstance.combinations.remove(0);
                }

        try {
            sleep(500);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        mInstance.avg_reponseT.replaceAll(s -> 0d);
        // here we've done delegate assignment and triangle change now want to restart the responseT of all tasks to start data collection

        mInstance.avgq= calculateMeanQuality();


            }



    public float calculateMeanQuality( ) {

        float sumQual=0;
        for (int ind = 0; ind < mInstance.objectCount; ind++)
        {
            int i =  mInstance.excelname.indexOf( mInstance.renderArray.get(ind).fileName);
            float gamma = mInstance.excel_gamma.get(i);
            float a = mInstance.excel_alpha.get(i);
            float b = mInstance.excel_betta.get(i);
            float c = mInstance.excel_c.get(i);
            float d = mInstance.renderArray.get(ind).return_distance();
            float curQ = mInstance.ratioArray.get(ind);

//            if (mInstance.renderArray.get(ind).fileName.contains("0.6")) // third scenario has ratio 0.6
//                curQ = 0.6f; // jsut for scenario3 objects are decimated
//            else if(mInstance.renderArray.get(ind).fileName.contains("0.3")) // sixth scenario has ratio 0.3
//                curQ=0.3f;

            float deg_error = (float) Math.round((float) (Calculate_deg_er(a, b, c, d, gamma, curQ) * 1000)) / 1000;
            float max_nrmd = mInstance.excel_maxd.get(i);

            float cur_degerror = deg_error / max_nrmd;
            float quality= 1- cur_degerror;
      //      objquality[ind]=quality;
            sumQual+=quality;


        }


        return sumQual/mInstance.objectCount;



    }



    public float Calculate_deg_er(float a,float b,float creal,float d,float gamma, float r1) {

        float error;
        if(r1==1)
            return  0f;
        error = (float) (((a * Math.pow(r1,2)) + (b * r1) + creal) / (Math.pow(d , gamma)));
        return error;
    }



    public void writeRT( ){ // this is to collect the response time of all AIS after change in the AI device
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss:SSS");
        String currentFolder = mInstance.getExternalFilesDir(null).getAbsolutePath();
        String FILEPATH = currentFolder + File.separator + "Bayesian_dataCollection"+mInstance.fileseries+".csv";

        StringBuilder sb = new StringBuilder();
        sb.append(dateFormat.format(new Date()));

        try (PrintWriter writer = new PrintWriter(new FileOutputStream(FILEPATH, true))) {
            for (int i=0;i<mInstance.mList.size();i++)
            {
                AiItemsViewModel taskView=mInstance.mList.get(i);
                sb.append(",").append(taskView.getModels().get(taskView.getCurrentModel()))
                        .append(",").append(taskView.getDevices().get(taskView.getCurrentDevice()))
                        .append(",").append( mInstance.avg_reponseT.get(i));


            }
            sb.append(",").append(mInstance.total_tris);
            sb.append(",").append(mInstance.avgq);

            sb.append('\n');
            writer.write(sb.toString());
            System.out.println("done!");
        } catch (FileNotFoundException e) {
            System.out.println(e.getMessage());
        }
    }



    }












//}



