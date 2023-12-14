 ///
 /**
  *
  *
  * For HBO, this code just shows AI task's response time and calculates the average throughput
  */

// @@@ check for  "Uncomment for Bayes auto Trigger" and uncomment those codes for auto trigger in
package com.arcore.AI_ResourceControl;
/*for HBO it collects data and checks if HBO trigger is needed*/
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.PrintWriter;
import java.text.SimpleDateFormat;
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

import android.annotation.SuppressLint;
import android.widget.TextView;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Iterables;
import com.google.common.collect.ListMultimap;

public class balancer implements Runnable {

    boolean hbo_trigger=true;// this enables autonomous HBO activation , make sure it's true, I temporary made it false

    int binCap=7;
    private final MainActivity mInstance;
    float ref_ratio=0.5f;
    int objC;
    double sensitivity[] ;
    float objquality[];
    double tris_share[];
    Map <Integer, Double> candidate_obj;
    float []coarse_Ratios=new float[]{1f,0.8f, 0.6f , 0.4f, 0.2f, 0.05f};
    //ArrayList <ArrayList<Float>> F_profit= new ArrayList<>();
    double [][]fProfit;
    double [][] tRemainder;
    int [][] track_obj;
    int sleepTime=30;
    //float candidate_obj[] = new float[total_obj];
    double tMin[] ;
    int missCounter=3;//means at least 4 noises
   // int aiIndx;
   TextView posText_re,posText_thr,posText_q,posText_app_hbo;
    double reward=0;

    public balancer(MainActivity mInstance) {

        this.mInstance = mInstance;

        objC=mInstance.objectCount+1;
        sensitivity = new double[objC];
        tris_share = new double[objC];
        objquality= new float[objC];// 1- degradation-error
       // aiIndx=ai_index;
                //model_index;

        //ArrayList <ArrayList<Float>> F_profit= new ArrayList<>();
        fProfit= new double[objC][coarse_Ratios.length];
        tRemainder= new double[objC][coarse_Ratios.length];
        track_obj= new int[objC][coarse_Ratios.length];
        //float candidate_obj[] = new float[total_obj];
        tMin = new double[objC];


        posText_re= mInstance.findViewById(R.id.app_re);
        posText_q= mInstance.findViewById(R.id.app_quality);
        posText_thr= mInstance.findViewById(R.id.app_thr);
       // posText_mir= mInstance.findViewById(R.id.app_bt);
        posText_app_hbo  = mInstance. findViewById(R.id.app_bt);
    }

    @SuppressLint("SuspiciousIndentation")
    @Override


    public void run() {

        boolean accmodel = true;// all AI throughput trained models and RE are accurate
        boolean accRe = true;// this is to check if the trained model for re is accurate
        boolean trainedTris = false;
        boolean trainedRT = false;
        boolean trainedRE = false;
        double maxTrisThreshold = mInstance.orgTrisAllobj;
        double minTrisThreshold = maxTrisThreshold * mInstance.coarse_Ratios[mInstance.coarse_Ratios.length - 1];
        double meanRt = 0;
        double meanThr = 0;
        //totTris;
        double meanDk = 0; // mean current dis
        double meanDkk = 0; // mean of d in the next period-> you need to cal the average of predicted d for all the objects
        double pred_meanD = mInstance.pred_meanD_cur; // for next 1 sc
        boolean fault_thr = false;
        int tris_window = 4;

        int re_window = 12;


        double period_init_tris = mInstance.total_tris;// this is the starting triangle count


        for (int i = 0; i < mInstance.objectCount; i++) {
            double cur_dis = (double) mInstance.renderArray.get(i).return_distance();
            meanDk += cur_dis;
            meanDkk += mInstance.predicted_distances.get(i).get(1); // 0: next 1 sec, 1: next 2s :  // gets the first time, next 2s-- 3s of every object, ie. d1 of every obj

        }

        int objc = mInstance.objectCount;
        ///totTris = mInstance.total_tris;// this might change in the middle of call, so it's better to directly use total_tris , not this local variable


        if (objc > 0) {
            meanDk /= objc;
            meanDkk /= objc;

            //  meanDk = (double) Math.round((double) 1000 * meanDk) / 1000;
            // meanDkk = (double) Math.round((double) 1000 * meanDkk) / 1000;

            if (objc == 1) {
                mInstance.initial_meanD = meanDk;
                mInstance.initial_totT = mInstance.total_tris;
            }
            if (meanDkk == 0)
                meanDkk = meanDk;
            if (pred_meanD == 0)
                pred_meanD = meanDk; // for the first objects

        }


        //I got an error for regression since decimation occurs in UI thread and Modeling runs at the same time
        // solution is to start data collection after one period passes from algorithm
        // else{ // just collect data when algorithm was applied in the last period



// Train the H model for each AI task from line 127 to


        //   int variousTris = mInstance.trisMeanDisk.keySet().size();


        double avg_msred_H = 0; // is equal to sum(msr_Hi)/N -> reset every period
        double avg_est_H = 0; // is equal to sum(H^i)/N -> reset every period

        // mInstance.trisMeanDisk.put(totTris, pred_meanD); //one-time correct: should have predicted value dist => removes from the head (older data) -> to then add to the tail
// this gets above bincap sooner than the rest of lists, so, we need to keep the size not more than 5

        int Ai_count = mInstance.mList.size();
        double avg_AIlatencyPeriod=0;

        for (int aiIndx = 0; aiIndx < Ai_count; aiIndx++) {

                double[] t_h = mInstance.getResponseT(aiIndx);
                meanRt = t_h[0];
                meanThr = t_h[1];

                while (meanThr > 500 ||meanThr < 0.5) // we wanna get a correct value
                { t_h=mInstance.getResponseT(aiIndx);
                    meanThr = t_h[1];
                    meanRt = t_h[0];
                }


            AiItemsViewModel taskView=mInstance.mList.get(aiIndx);
            // first find the best offline AI response Time = EXPECTED RESPONSE TIme
            int indq = mInstance.excel_BestofflineAIname.indexOf(taskView.getModels().get(taskView.getCurrentModel()));
            double expected_time = mInstance.excel_BestofflineAIRT.get(indq);
            // find the actual response Time
            double actual_rpT=meanRt;
            avg_AIlatencyPeriod+=(actual_rpT-expected_time)/actual_rpT;


            if (aiIndx == 0) {
               // TextView posText = (TextView) mInstance.findViewById(R.id.rspT);
               posText_re.setText("RT1: " + String.valueOf(meanRt));
            } else if (aiIndx == 1) {

              //  TextView posText2 = mInstance.findViewById(R.id.rspT1);
                posText_q.setText("RT2: " + String.valueOf(meanRt));
            }
//
            else if (aiIndx == 2) {
            //    TextView posText3 = mInstance.findViewById(R.id.rspT2);
                posText_thr.setText("RT3: " + String.valueOf(meanRt));
            }



        }
    //  Uncomment for Bayes auto Trigger
        if(mInstance.curBysIters==-1)
            mInstance.afterHbo_counter++;// count consecutive HBO activation

        mInstance.avgq = calculateMeanQuality();
        double avgAIltcy= avg_AIlatencyPeriod/ mInstance.mList.size();
        reward =mInstance.avgq - (mInstance.reward_weight*avgAIltcy);
        reward=(double) (Math.round((double) (reward * 100))) / 100;

        mInstance.best_BT=(double) (Math.round((double) (mInstance.best_BT * 100))) / 100;

        posText_app_hbo.setText("B_t: "+ String.valueOf(reward));
        posText_thr.setText("best_BT: " + String.valueOf(mInstance.best_BT));



        if(hbo_trigger) {
            if (mInstance.best_BT != 0 && mInstance.curBysIters == -1) {// if it's not in the middle of another Bayesian

                if (mInstance.afterHbo_counter <= 3)// this is to adjust the reward and remove any noises for the first three data collected after HBO activation
                    mInstance.best_BT = (reward + mInstance.best_BT) / 2;

                double perc_error = (mInstance.best_BT - reward) / mInstance.best_BT;
                if (perc_error > 0.05 || perc_error < -0.1)// below is the function of server button
                // if BT gets worst by object addition, error becomes higher negative, if we farther awa, error becomes positive
                {
                    mInstance.hbo_trigger_false_counter++;
                    if (mInstance.hbo_trigger_false_counter >= 3)// we won't iimmidiately trigger HBO, we'll wait till
                    {
                        mInstance.hbo_trigger_false_counter = 0;
                        ModelRequestManager.getInstance().add(new ModelRequest(mInstance.getApplicationContext(), mInstance, mInstance.deleg_req, "delegate"), false, false);
                        mInstance.deleg_req += 1;
                    }
                } else
                    mInstance.hbo_trigger_false_counter = 0;// we want to get 5 Bts consecutively with error
            }
        }



        writequality();

    }

    @SuppressLint("SuspiciousIndentation")
    public void run_old() {

        boolean accmodel = true;// all AI throughput trained models and RE are accurate
        boolean accRe=true;// this is to check if the trained model for re is accurate
        boolean trainedTris = false;
        boolean trainedRT = false;
        boolean trainedRE = false;
        double maxTrisThreshold = mInstance.orgTrisAllobj;
        double minTrisThreshold = maxTrisThreshold * mInstance.coarse_Ratios[mInstance.coarse_Ratios.length - 1];
        double meanRt=0;
        double meanThr=0;
                //totTris;
        double meanDk = 0; // mean current dis
        double meanDkk = 0; // mean of d in the next period-> you need to cal the average of predicted d for all the objects
        double pred_meanD = mInstance.pred_meanD_cur; // for next 1 sc
        boolean fault_thr=false;
        int tris_window=4;

        int re_window=12;


        double period_init_tris=mInstance.total_tris;// this is the starting triangle count





      for (int i = 0; i < mInstance.objectCount; i++) {
            double cur_dis=(double) mInstance.renderArray.get(i).return_distance();
               meanDk += cur_dis;
                meanDkk += mInstance.predicted_distances.get(i).get(1); // 0: next 1 sec, 1: next 2s :  // gets the first time, next 2s-- 3s of every object, ie. d1 of every obj

        }

        int objc= mInstance.objectCount;
        ///totTris = mInstance.total_tris;// this might change in the middle of call, so it's better to directly use total_tris , not this local variable


        if(objc>0) {
            meanDk /= objc;
            meanDkk /= objc;

          //  meanDk = (double) Math.round((double) 1000 * meanDk) / 1000;
           // meanDkk = (double) Math.round((double) 1000 * meanDkk) / 1000;

            if(objc==1) {
                mInstance.initial_meanD = meanDk;
                mInstance.initial_totT=mInstance.total_tris;
            }
            if (meanDkk == 0)
                meanDkk = meanDk;
            if (pred_meanD == 0)
                pred_meanD = meanDk; // for the first objects

        }



        //I got an error for regression since decimation occurs in UI thread and Modeling runs at the same time
        // solution is to start data collection after one period passes from algorithm
 // else{ // just collect data when algorithm was applied in the last period



        double sum_currentWt= 0; // this is to calculate est_weights for AI models
        double sum_baseWt= 0;
        double sum_rohT_wi=0;
        double sum_rohD_wi=0;
        double sum_delta_wi=0;

// Train the H model for each AI task from line 127 to


     //   int variousTris = mInstance.trisMeanDisk.keySet().size();


        double avg_msred_H=0; // is equal to sum(msr_Hi)/N -> reset every period
        double avg_est_H=0; // is equal to sum(H^i)/N -> reset every period

       // mInstance.trisMeanDisk.put(totTris, pred_meanD); //one-time correct: should have predicted value dist => removes from the head (older data) -> to then add to the tail
// this gets above bincap sooner than the rest of lists, so, we need to keep the size not more than 5
        double aiMaxError=0.1;
        int Ai_count= mInstance.mList.size();
        for (int aiIndx=0; aiIndx<Ai_count;aiIndx++) {

            double predRt = 0;

            double[] t_h = mInstance.getResponseT(aiIndx);
            // meanRt = mInstance.getResponseT(aiIndx);// after the objects are decimated
            meanRt = t_h[0];
            meanThr = t_h[1];

            while (meanThr > 400 ||meanThr < 0.5) // we wanna get a correct value
              { t_h=mInstance.getResponseT(aiIndx);
                meanThr = t_h[1];
                meanRt = t_h[0];
               }


            double curAvg= mInstance.avg_reponseT.get(aiIndx);

            if(curAvg==0d)// this is the inital value
                mInstance.avg_reponseT.set(aiIndx, meanRt  );
            else
                mInstance.avg_reponseT.set(aiIndx,
                        //( meanRt+  curAvg)/2);
                        (double) Math.round((double) ( ((meanRt+  curAvg)/2) * 100)) / 100);


// don't use tris-dis here since->we need to remove a record of old tris for each AI model
            int variousTris = mInstance.thr_models.get(aiIndx).keySet().size();// since  from thr_models

            if (variousTris == 1) {// this is to update the baseline throughput per models
                    // we have no triangle count on screen

                    if (mInstance.baseline_AIRt.get(aiIndx) == 0)
                        mInstance.baseline_AIRt.set(aiIndx, meanRt);
                    else {
                        double curr_baseline = mInstance.baseline_AIRt.get(aiIndx);
                        mInstance.baseline_AIRt.set(aiIndx, (meanRt + curr_baseline) / 2);
                    }
                }




// this is thr calculated using the modeling
                predRt = (mInstance.rohTLRt.get(aiIndx) * mInstance.total_tris) + (mInstance.rohDLRt.get(aiIndx) * pred_meanD) + mInstance.deltaLRt.get(aiIndx);// use predicted distance for almost current period (predicted distance for next 1 sec is the closest one we have)
               // predRt = (double) Math.round((double) predRt * 1000) / 1000;

//            writeThr(meanRt, predRt, trainedRT);// for the urrent period

                int ind = -1;
//                if (variousTris < 2) { // no object on the screen
//
//                    double fixedT=mInstance.total_tris;// the location is so important since we d
//                    if (  mInstance.thr_models.get(aiIndx).containsKey(fixedT) &&   mInstance.thr_models.get(aiIndx).get(fixedT).size() == binCap)  //This is to
//                        cleanOutArraysThr(fixedT, pred_meanD, mInstance);// cleans out the closest data to the curr one
//
//                    mInstance.thr_models.get(aiIndx).put(mInstance.total_tris, meanRt); //adds to the end of list correct:  should have real throughput for the regression
//                    // uses predicted cur distance for thr modeling
//                    mInstance.thParamList.get(aiIndx).put(mInstance.total_tris, Arrays.asList(mInstance.total_tris, pred_meanD, 1.0));
//
//
//                  //correct:  should have predicted value removes from the head (older data) -> to then adds to the tail
//                  if(aiIndx==0) // this prevents a crash since we don't add to tris-meankK everytime, just for one time + also needed to be removed in clear function one time
//                     mInstance.trisMeanDisk.put(mInstance.total_tris, pred_meanD); //should be called one-time per decision period not per AI task since it is the distance of all AI tasks
//
//                }
     // starting throughput model
//                if (variousTris >= 2) {// at least two points to start modeling

// checks error of the model after new added model
                    // NEW @@@ Nil test for new window-based data collection/training -> to remove old data for training
                    if(variousTris==tris_window)// I am tying to remove the current value of hashmap
                    {
                        // cleans out the closest data to the curr one within bins @@@ NOTE THAT ONLY mInstance.thr_models has the data ordered based on insersion
                        double oldest_tris= Iterables.getFirst( mInstance.thr_models.get(aiIndx).keys(), null);//  the oldest inserted entry (the trisCount) ->
                       // Map.Entry<Double, Double> entry=mInstance.thr_models.get(aiIndx).entries().iterator().next();
                        mInstance.thr_models.get(aiIndx).removeAll(oldest_tris);
                        mInstance.thParamList.get(aiIndx).removeAll(oldest_tris);
                        mInstance.trisMeanDisk.removeAll(oldest_tris);
//                        mInstance.trisMeanDisk.removeAll(entry.getKey());

                    }// removes the set of <key.list> from the head of multimap

                    double fixedT=mInstance.total_tris;
                    // should have data collection any way
                    if ( mInstance.thr_models.get(aiIndx).containsKey(fixedT) &&
                             mInstance.thr_models.get(aiIndx).get(fixedT).size() == binCap) { //we delete from array of initial tris not new, since we have data of initial tris by 100% we keep data up to binCap per triangleCount
                        cleanOutArraysThr(fixedT, pred_meanD, mInstance);// cleans out the closest data to the curr one within bins
                    }




                    mInstance.thr_models.get(aiIndx).put(mInstance.total_tris, meanRt); // we put the latest tris data-> should have real throughput for regression and thr param should have predicted distance
                    mInstance.thParamList.get(aiIndx).put(mInstance.total_tris, Arrays.asList(mInstance.total_tris, pred_meanD, 1.0));

                    if(aiIndx==0) // this prevents a crash since we don't add to tris-meankK everytime, just for one time + also needed to be removed in clear function one time
                        mInstance.trisMeanDisk.put(mInstance.total_tris, pred_meanD); //should be called one-time per decision period not per AI task since it is the distance of all AI tasks


                    double perAI_mape = 0.0; // mean of absolute error

                    double meanH=1000/meanRt;
                    double pred_H = (mInstance.rohTL.get(aiIndx) * mInstance.total_tris) + (mInstance.rohDL.get(aiIndx) * pred_meanD) + mInstance.deltaL.get(aiIndx); // fit is predicted current throughput should be predicted distance (we have next 1 sec but ok since we don't move)  for current period
                   double AI_error=abs((meanH - pred_H)) / meanH;


                    //double fit = mInstance.thSlope * mInstance.total_tris + mInstance.thIntercept;;
            perAI_mape = abs((meanRt - predRt)) / meanRt;// this is correct to calculate error coming from real throughput vs model

            int conseq_error_counter= mInstance.conseq_error.get(aiIndx);// num of consequent noises/errors per AI model

            if(perAI_mape>aiMaxError) {// assume we have the list of H during time for AI1: [13,14,15,13]
                        //mInstance.hAI_acc.set(aiIndx, false);
                        mInstance.conseq_error.set(aiIndx,conseq_error_counter+1);// this is to avoid multiple un-necessary
                    }
            else if(conseq_error_counter>0)
                          mInstance.conseq_error.set(aiIndx,conseq_error_counter-1);// reset since the model is fine


                   // variousTris = mInstance.trisMeanDisk.keySet().size();// update various tris after data collection
                    // up to here, all data is collected, now we need to check tot_tris and see if that's different from initial tris,
                    // we'll skip the training


                    ///    to train H model removed perAI_mape > aiMaxError-> replaced by conseq_error
                    if ( ( mInstance.conseq_error.get(aiIndx) >2 || AI_error>0.1 ) && variousTris >= 3 && period_init_tris==mInstance.total_tris) {// we need points with at least two diff tris in order to generate the line

                        ListMultimap<Double, List<Double>> copyRtParamList = ArrayListMultimap.create(mInstance.thParamList.get(aiIndx));// take a copy to then fill it for training up to capacity of 10
                        ListMultimap<Double, Double> copytrisMeanRt = ArrayListMultimap.create(mInstance.thr_models.get(aiIndx));// take a copy to then fill it for training up to capacity of 10

                        // to fill un-filled bins with mean data and have fair data training
                        for (double curT : copytrisMeanRt.keySet()) {// use trisMeanDisk instead of thr_models since the former is updated slowly (for AIindex=0)
                            //   this is to calculate the mean of values in the bins

                            double mmeanTh = 0, mmeanDK = 0;

                            //if (mInstance.thr_models.get(aiIndx).get(curT).size() < binCap) //  trisMeanDisk might be different from thr_models since data collection is based on real-time tris change
                            int index1 = copyRtParamList.get(curT).size();
                            if (index1 <= binCap)
                            {   mmeanDK=   mInstance.trisMeanDisk.get(curT).stream().mapToDouble(a -> a).average().getAsDouble();
                                //int index = mInstance.trisMeanDisk.get(curT).size();
                                for (int j = index1; j < binCap; j++)
                                    copyRtParamList.put(curT, Arrays.asList(curT, mmeanDK, 1.0)); // this is to calculate the mean of values in the bins so it's correct
                            }
                            else// sometimes happens that the main thParamList has excess data
                                while(index1>binCap) {
                                    copyRtParamList.get(curT).remove(index1 - 1);
                                    index1--;
                                }
                                //cleanOutArraysThr(curT,pred_meanD,mInstance,aiIndx);// to be cautious about size of array

                            int index2 = copytrisMeanRt.get(curT).size();
                            if (index2 <= binCap)
                            {   mmeanTh= mInstance.thr_models.get(aiIndx).get(curT).stream().mapToDouble(a -> a).average().getAsDouble();
                                for (int j = index2; j < binCap; j++)
                                    copytrisMeanRt.put(curT, mmeanTh);

                            }
                            else
                                while(index2>binCap) {
                                    copytrisMeanRt.get(curT).remove(index2 - 1);
                                    index2--;
                                }
                               // cleanOutArraysThr(curT,pred_meanD,mInstance,aiIndx);// to be cautious about size of array


                        }


                        //     @@@@ here we check if we have any record for the
                        //  decimated objects, if yes, we need to check the ratio of decimated iteration over added scenarios, we define a rate of 40% to make sure that we have
                        //at least the record for decimated object equal to 40% of the added scenarios

                        int decCount = mInstance.decTris.size();// #iterations of decimated objects
                        if (decCount != 0) // means that we have at least one record of decimated objects
                        {
                            int j = 0;
                            int upCount = (int) Math.ceil(mInstance.thr_factor * mInstance.objectCount);// we had addition up to the object count
                            for (int i = decCount; i < upCount; i++) {
                                if (j > mInstance.decTris.size() - 1)
                                    j = 0;
                                double currentT = mInstance.decTris.get(j);// one of the triangles from the list of decimated-exp
                                List<Double> thrList = new LinkedList<>(copytrisMeanRt.get(currentT));// since copy list has full data
                                for (double th : thrList)// elements of re list
                                    copytrisMeanRt.put(currentT, th);

                                List<List<Double>> thpList = new LinkedList<>(copyRtParamList.get(currentT));// since copy list has full data
                                for (List<Double> thpr : thpList)// rt parameters
                                    copyRtParamList.put(currentT, thpr);
                                j += 1;

                            }

                        }
                        // to copy the decimated data into throughout modeling


                     double[][] thRegParameters = copyRtParamList.values().stream()
                                .map(l -> l.stream().mapToDouble(Double::doubleValue).toArray())
                                .toArray(double[][]::new);

                    if( mInstance.conseq_error.get(aiIndx) >2){
                        double[] rt = copytrisMeanRt.values().stream()
                                .mapToDouble(Double::doubleValue)
                                .toArray();

                        double[] y = Arrays.copyOfRange(rt, 0, rt.length); // should be real rt



                        mLinearRegression regression = new mLinearRegression(thRegParameters, y);
                        if (!Double.isNaN(regression.beta(0))) {

                            mInstance.rohTLRt.set(aiIndx,  regression.beta(0)     );
                            mInstance.rohDLRt.set(aiIndx,  regression.beta(1)   );
                            mInstance.deltaLRt.set(aiIndx, regression.beta(2)    );
                            //mInstance.thRmse = regression.rmse;
                            trainedRT = true;


                        }}


// this is per AI throughput model
                        if(AI_error>0.1) {
                            double[] thr = copytrisMeanRt.values().stream().mapToDouble(doubleValue -> 1000 / doubleValue).toArray();
                            double[] y2 = Arrays.copyOfRange(thr, 0, thr.length); // should be average measured throughput

                            mLinearRegression regression2 = new mLinearRegression(thRegParameters, y2);
                            if (!Double.isNaN(regression2.beta(0))) {
                                mInstance.rohTL.set(aiIndx,  regression2.beta(0)     );
                                mInstance.rohDL.set(aiIndx,  regression2.beta(1)   );
                                mInstance.deltaL.set(aiIndx, regression2.beta(2)    );
                                // trainedThr = true;
                            }
                        }

// this is per AI throughput model

                        copytrisMeanRt.clear();
                        copyRtParamList.clear();

                        // end of modeling rt

                    }//   to train H model


//                  specific AI throughput after remodeling
                    predRt = (mInstance.rohTLRt.get(aiIndx) * mInstance.total_tris) + (mInstance.rohDLRt.get(aiIndx) * pred_meanD) + mInstance.deltaLRt.get(aiIndx);// use predicted distance for almost current period (predicted distance for next 1 sec is the closest one we have)
                    //predRt = (double) Math.round((double) predRt * 1000) / 1000;


                   double mape = abs(meanRt - predRt) / meanRt;



                    if (mape > aiMaxError) {// train the model


                        //mInstance.hAI_acc.set(aiIndx, false);

                        if (variousTris >= 3) {
                            int cur_count = mInstance.thr_miss_counter.get(aiIndx);
                            mInstance.thr_miss_counter.set(aiIndx, cur_count + 1);
                        }


                        if (mInstance.thr_miss_counter.get(aiIndx) > 3 && mInstance.decTris.size() != 0) { // this is to regulate the factor of considering decimated collected data in the bins
                            if (meanRt > predRt) {
                                mInstance.thr_factor -= 0.1;// reduce the effect of decimation to increase predicted throughput
                                if (mInstance.thr_factor < 0)
                                    mInstance.thr_factor = 0;
                            } else
                                mInstance.thr_factor += 0.1;// increase the effect of decimation to decrease the calculated pred_re

                        }

                    } else {

                        mInstance.conseq_error.set(aiIndx,0);
                        //mInstance.hAI_acc.set(aiIndx, true);
                        mInstance.thr_miss_counter.set(aiIndx, 0);// the model works fine so we don't need to re-adjust the throughput factor for decimation data collection
                        // we st the estimated weights if trained models are accurate

                    }
                    if( mInstance.conseq_error.get(aiIndx) <=2)
                         mInstance.hAI_acc.set(aiIndx, true);
                     else
                          mInstance.hAI_acc.set(aiIndx, false);

                    // let's say we estimate even if models are not accurate-> we then want to compare it with the measured wi
            //mape<=aiMaxError replaced by mInstance.conseq_error.get(aiIndx) <=2
                if(variousTris>=2 && mape<=aiMaxError ) {// we won't update the est_w of the incorrect model but we update the rest Ws
                    double cur_est_w = sqrt(pow(mInstance.rohTLRt.get(aiIndx), 2) + pow(mInstance.rohDLRt.get(aiIndx), 2));
                   // double alpha = 0.7;
                   // cur_est_w = alpha * (cur_est_w) + ((1 - alpha) * mInstance.est_weights.get(aiIndx)); // smoothing function of past and current data

                    mInstance.est_weights.set(aiIndx, cur_est_w);// not normalized-> new(SQRT( (rohT^2+rohD^2)) -> this has not-normalized est_weights
                }

                   /* we don't use measured weight anymore since it's not accurate to use just a start and end point to measure w
                    double ai_sensitivity;
                    if(variousTris==2)
                         ai_sensitivity=   Math.sqrt(  Math.pow (((meanRt-mInstance.baseline_AIRt.get(aiIndx))/mInstance.total_tris),2)  +
                            Math.pow   ( ((meanRt-mInstance.baseline_AIRt.get(aiIndx))/meanDk ),2 ) ) ; // AI throughput sensitivity over change in tris and distance

                    else{

                        double iniTris=mInstance.initial_totT;// tris of first object in which throughput data is collected
                        double initial_thr = mInstance.thr_models.get(aiIndx).get(iniTris).get(0);// throughput data corresponds to initial tris for each AI model
                        ai_sensitivity=   Math.sqrt(  Math.pow (((meanRt-initial_thr )/(mInstance.total_tris- iniTris ) ),2)  +
                                Math.pow   ( ((meanRt- initial_thr )/(meanDk-mInstance.initial_meanD) ),2 ) ) ; // AI throughput sensitivity over change in tris and distance

                    }

                    if(ai_sensitivity==0){
                        // stop
                        int x=0;
                    }


                    mInstance.msr_weights.set(aiIndx,ai_sensitivity);
                    // old: mInstance.msr_weights.set(aiIndx, 1- (meanRt/ mInstance.baseline_AIRt.get(aiIndx)));//not normalized-> set the measured Wi = measured current throughput over baseline
*/



//                } //Specific AI  throughput model

                if(mInstance.hAI_acc.contains(false))// if atleast one model has inaccurate throughput model, we don't train RE model below
                    accmodel=false;


                avg_est_H+=pred_H;

                avg_msred_H+=meanH;

             //   writeThr(meanRt, predRt, trainedRT,aiIndx,100*mape, meanH, pred_H, 100*perAI_mape);// It has predicted and real throughput of task AI[index]


            //else
              //  fault_thr=true; // the measured throughput could be zero or a very large num--> faulty data

        }// for all AI models we train throughput
        /*   Specific AI throughput is tested and now works
        * so far, all model's est_weights are calculated  */



        avg_msred_H/=Ai_count; // this is average of all model's measured throughput
        avg_est_H/=Ai_count;//this is average of all estimated model throughput

      //  avg_est_H =  (double) Math.round((double) avg_est_H * 1000) / 1000;
      //  avg_msred_H =  (double) Math.round((double) avg_msred_H * 1000) / 1000;

        double predWeighted_thr=0;
        double msrd_Weighted_thr=0;
       int  variousTris = mInstance.trisMeanDisk.keySet().size();// update various tris after data collection

       boolean acc_throughput=true;

        if(variousTris>=2) { // at least one object on the screen
          //  double max_w = Collections.max(mInstance.est_weights);// this is max without considering magnitude, so -5<-2
            double max_w = Collections.max(  mInstance.est_weights, new Comparator<Double>() { // returns max of absolute value of weights
                        @Override
                        public int compare(Double x, Double y) {
                            return abs(x) < abs(y) ? -1 : 1;
                        }});

//          double max_msr_w = Collections.max(  mInstance.msr_weights, new Comparator<Double>() { // returns max of absolute value of weights
//                            @Override
//                            public int compare(Double x, Double y) {
//                                return Math.abs(x) < Math.abs(y) ? -1 : 1;
//                            }});

         for (int i = 0; i < Ai_count; i++) {

                double est_wi=    (mInstance.est_weights.get(i) / max_w) ;
                mInstance.nrmest_weights.set(i, est_wi);// this has Normalized est_weights
              //  double =nrm_estW;// equal to normalized (sqrt(rohT^2 + rohD^2))
//                double nrm_msrW=  (double) Math.round((double) (mInstance.msr_weights.get(i) / max_msr_w) * 100) / 100;
//                mInstance.msr_weights.set(i, nrm_msrW);

                //@@@@@@@@@@ correct this after weights are tested, since for PAI we need measured weights not estimated
//                double ms_wi = mInstance.msr_weights.get(i); // equal to normalized (cur_thr/baseline)
            //    double msr_Rt=  mInstance.thr_models.get(i).get(mInstance.total_tris).get( mInstance.thr_models.get(i).get(mInstance.total_tris).size()-1);// get the last/current value
             double last_tris= Iterables.getLast( mInstance.thr_models.get(i).keys(), null);// to get last tris of each model, each model might have different last tris

             double msr_Rt= Iterables.getLast( mInstance.thr_models.get(i).get(last_tris), null);// last added thr

                 //    mInstance.thr_models.get(i).get(last_tris).stream().mapToDouble(a -> a).average().getAsDouble(); //average of measured throughput in bin
                        //mInstance.thr_models.get(i).get(totTris).stream().mapToDouble(f -> f.doubleValue()).average().getAsDouble();// average of measured throughput in bin

               // double msr_Rt = mInstance.thr_models.get(i).get(totTris).get(0);// get the throughput of current tris for each model
                sum_currentWt += (est_wi * (1000/msr_Rt) ); //est_wi instead of ms_wi
                sum_baseWt += ((est_wi  * mInstance.des_thr_weight) * (1000/mInstance.baseline_AIRt.get(i))  );//sum(Normalized Wi * Hbase)


                sum_rohT_wi += ( est_wi * mInstance.rohTL.get(i));// .rohTL is for avg throughput model per Ai --- sigma(rohT_i * wi)
                sum_rohD_wi += (est_wi * mInstance.rohDL.get(i));//sigma(rohD_i * wi)
                sum_delta_wi += (est_wi * mInstance.deltaL.get(i));//sigma(delta_i * wi)

                writeWeights(mInstance.hAI_acc.get(i),i);// write specific  weight after normalization
            }


            // check even if AI models are not accurate, the measured weighted_H and estimated Weighted_H are close?
            double total_est_weights = mInstance.nrmest_weights.stream().mapToDouble(f -> f.doubleValue()).sum();

             msrd_Weighted_thr = sum_currentWt/total_est_weights;// this is average weighted throughput of current period
          //  msrd_Weighted_thr = (double)  msrd_Weighted_thr ; // sum of estimated weights



            mInstance.rohT= sum_rohT_wi/total_est_weights;
            mInstance.rohD=sum_rohD_wi/total_est_weights;
            mInstance.delta=sum_delta_wi/total_est_weights;

            predWeighted_thr = (mInstance.rohT * mInstance.total_tris) + (mInstance.rohD* pred_meanD) + mInstance.delta;// use predicted distance for almost current period (predicted distance for next 1 sec is the closest one we have)


            // the accuracy of average measured and estimated throughput
            double model_mape = abs(avg_msred_H - avg_est_H) / avg_msred_H;
           // model_mape=(double) Math.round((double)model_mape*100)/100 ;

            write_weightedH( avg_msred_H ,avg_est_H ,msrd_Weighted_thr,predWeighted_thr,model_mape*100, accmodel);// first two are average over all models for measured and estimated

            if(model_mape >0.1)// this is for avg throughput not perAI model throughput
                acc_throughput=false;


        }
        /*************************   calculate Normalized weighted P_AI*/
        // to normalize, we need to find the maximum weight and normalize all models over that. and then we calculate sum(Wi * Hi)
// just weights are based on response time parameters and the rest includeing measured RE and estimated RE should include throughput

        double avgq = calculateMeanQuality();
        double PRoAR =  (avgq / mInstance.des_Q);

        double PRoAI = (sum_currentWt / sum_baseWt) ;// should include throughput

        double reMsrd = PRoAR / PRoAI;
        double nextTris = mInstance.total_tris;
        double algNxtTris = mInstance.total_tris;


//        if( acc_throughput){ // we start to train RE and calculate next tris if H_i models are accurate


        //sum_baseWt*=mInstance.des_weight;// multiply by desired minimum throughput weight (0.7)
        //mInstance.des_Thr= mInstance.baseline_AIRt.get()/Ai_count;

        //@********************  calculated weighted P_AI* @/
//@ * ******************* calculate average AI throughput from weighted model -> roh'T= sigma(rohT_i * w^i)/N roh'D= sigma(rohD_i * wi)/Ndelta'= sigma(delta_i * wi)/N
        //@ ******************* calculate average AI throughput from weighted model   *********** @//

        int var_tris= mInstance.trisRe.keySet().size();// no need for RE since we have the correct data point of system balance even for past data

        //this is for data collection to use current average throughput since predicted thr for first data points is not accurate
            if ( !acc_throughput && variousTris <=3 && reMsrd!= Double.NaN) {

                predWeighted_thr= msrd_Weighted_thr;
                        //avg_msred_H;
            }


           else if(acc_throughput && reMsrd!= Double.NaN) {// we collect RE data if Average throughput model is accurate
                // NEW @@@ Enable W windows for RE training since we use est_thr with the same idea
                if (var_tris == re_window)// I am tying to remove the current value of hashmap
                {
                    // cleans out the closest data to the curr one within bins @@@ NOTE THAT ONLY mInstance.thr_models has the data ordered based on insersion
                    double oldest_tris = Iterables.getFirst(mInstance.trisRe.keys(), null);//  the oldest inserted entry (the trisCount) ->
                    mInstance.trisRe.removeAll(oldest_tris);
                    mInstance.reParamList.removeAll(oldest_tris);

                }// removes the set of <key.list> from the head of multimap
            }

            if(objc>0 && reMsrd!= Double.NaN){// not for zero tirangles
            //this is for data collection to use predicted throughput which is equal to msr_average for objCount<=3 and predictedThr for the rest data points
            double fixedT = mInstance.total_tris;
            if (mInstance.trisRe.containsKey(fixedT) && mInstance.trisRe.get(fixedT).size() == binCap)
                cleanOutArraysRE(fixedT, pred_meanD, mInstance);// check to remove extra value in the RE parameters list , substitue the newer one


            if( mInstance.trisRe.keySet().contains(mInstance.total_tris) && mInstance.trisRe.get(mInstance.total_tris).contains(reMsrd) ){
                Random r = new Random();
                double randomValue = 0.00001 + (0.0001 - 0.00001) * r.nextDouble();
                // double randomValue = rangeMin + (rangeMax - rangeMin) * r.nextDouble();
                mInstance.trisRe.put(mInstance.total_tris, reMsrd+randomValue  );//because tris-re doesn't allow for duplicated key-value but reparam does, so we want to keep their size equal
                // use predicted current dis and predicted current throughput in the modeling
                mInstance.reParamList.put(mInstance.total_tris, Arrays.asList(mInstance.total_tris, pred_meanD, predWeighted_thr, 1.0)); // the two pred are coming from prev period (that were predicted for current period)
            }

            else {
                mInstance.trisRe.put(mInstance.total_tris, reMsrd);//correct:  has real re should have real throughout
                mInstance.reParamList.put(mInstance.total_tris, Arrays.asList(mInstance.total_tris, pred_meanD, predWeighted_thr, 1.0)); // the two pred are coming from prev period (that were predicted for current period)
            }}
            //  sum of square error




            // this is for data training
//            if() {//to avoid wrong inf from tris=0 -> we won't have re or distance at this situation

                //* so far we train each task's throughput model individually, but RE should be trained generally using measured RE value of
                 //PAI that comes from sum(Wi * AI_thr)/0.7*(sum(Wi * base_Hi) and PAR as usual
                // @ ******************  RE modeling *************
                /// need to add a condition for running this periodically to make sure XMIR est_weights are stable but not for MIR maybe
                double re_mape = 0.0;


                double fit = (mInstance.alphaT * mInstance.total_tris) + (mInstance.alphaD * pred_meanD) + (mInstance.alphaH * predWeighted_thr) + mInstance.zeta;// uses predicted modeling  for current period
                re_mape = abs(reMsrd - fit) / reMsrd;

                var_tris= mInstance.trisRe.keySet().size();



                    if (re_mape > 0.10 && var_tris >= 3) {// we ignore tris=0 them we need points with at least two diff tris in order to generate the line

                        ListMultimap<Double, List<Double>> copyreParamList = ArrayListMultimap.create(mInstance.reParamList);// take a copy to then fill it for training up to capacity of 10
                        ListMultimap<Double, Double> copytrisRe = ArrayListMultimap.create(mInstance.trisRe);// take a copy to then fill it for training up to capacity of 10

                        // This is to calculate mean of bins for distance, throughput ,...
                        for (double curT : mInstance.trisRe.keySet()) {

                            if (curT != 0 && mInstance.trisRe.get(curT).size() < binCap) {

                                double meanRE = 0, mmeanDK = 0, meanPrth = 0;

                                int index1 = copytrisRe.get(curT).size();
                                if (index1 <= binCap)
                                {
                                    meanRE = mInstance.trisRe.get(curT).stream().mapToDouble(a -> a).average().getAsDouble();

                                   // mmeanDK=   mInstance.trisMeanDisk.get(curT).stream().mapToDouble(a -> a).average().getAsDouble();
                                    for (int j = index1; j < binCap; j++)
                                       // copythParamList.put(curT, Arrays.asList(curT, mmeanDK, 1.0)); // this is to calculate the mean of values in the bins so it's correct
                                          copytrisRe.put(curT, meanRE);
                                }
                                else// sometimes happens that the main thParamList has excess data
                                    while(index1>binCap) {
                                        copytrisRe.get(curT).remove(index1 - 1);
                                        index1--;
                                    }


                                int index2 = copyreParamList.get(curT).size();
                                if (index2 <= binCap)
                                {
                                    double[][] reParL =  mInstance.reParamList.get(curT).stream().map(l -> l.stream().mapToDouble(Double::doubleValue).toArray())
                                            .toArray(double[][]::new);
                                    for (int i = 0; i < index2; i++) {

                                        mmeanDK += reParL[i][1];
                                        meanPrth += reParL[i][2];
                                    }
                                    mmeanDK /= index2;
                                    meanPrth /= index2;


                                    for (int j = index2; j < binCap; j++)
                                        // copythParamList.put(curT, Arrays.asList(curT, mmeanDK, 1.0)); // this is to calculate the mean of values in the bins so it's correct
                                        copyreParamList.put(curT, Arrays.asList(curT, mmeanDK, meanPrth, 1.0));
                                }
                                else// sometimes happens that the main thParamList has excess data
                                    while(index2>binCap) {
                                        copyreParamList.get(curT).remove(index2 - 1);
                                        index2--;
                                    }



                            }// if <10
                        }// for all the current data

                        // for REEE

                        //             @@@@ here we check if we have any record for the
                        //  decimated objects, if yes, we need to check the ratio of decimated iteration over added scenarios, we define a rate of 40% to make sure that we have
                        //   at least the record for decimated object equal to 40% of the added scenarios
                        //  add the decimated data for throughput modeling too

                        int decCount = mInstance.decTris.size();// #iterations of decimated objects
                        if (decCount != 0) // means that we have at least one record of decimated objects
                        {
                            int j = 0;
                            //int upCount= (int) Math.ceil(0.5 * mInstance.objectCount);


                            int upCount = (int) Math.ceil(mInstance.re_factor * mInstance.objectCount);// we had addition up to the object count

                            for (int i = decCount; i <= upCount; i++) {
                                if (j > mInstance.decTris.size() - 1)
                                    j = 0;
                                double currentT = mInstance.decTris.get(j);// one of the triangles from the list of decimated-exp
                                List<Double> reList = new LinkedList<>(copytrisRe.get(currentT));// since copy list has full data
                                for (double re : reList)// elements of re list
                                    copytrisRe.put(currentT, re);

                                List<List<Double>> repList = new LinkedList<>(copyreParamList.get(currentT));// since copy list has full data
                                for (List<Double> repr : repList)
                                    copyreParamList.put(currentT, repr);
                                j += 1;

                            }
                        }
                        // to copy the decimated data into re modeling

                        double[] RE = copytrisRe.values().stream()
                                .mapToDouble(Double::doubleValue)
                                .toArray();


                        double[][] reRegParameters = copyreParamList.values().stream()
                                .map(l -> l.stream().mapToDouble(Double::doubleValue).toArray())
                                .toArray(double[][]::new);

                        if (var_tris >= 3) {// since it includes non-zero tris, so 3 is three objects on screen
                            mLinearRegression regression = new mLinearRegression(reRegParameters, RE);
                            if (!Double.isNaN(regression.beta(0))) {
                                mInstance.alphaT =  regression.beta(0) ;
                                mInstance.alphaD = regression.beta(1);
                                mInstance.alphaH = regression.beta(2);
                                mInstance.zeta = regression.beta(3);
                                trainedRE = true;
                            }


                        }

                        reRegParameters = null;
                        RE = null;

                        copytrisRe.clear();
                        copyreParamList.clear();

                    }
//                    }

                    //   nil   commented  sep 7

                    // current period
                    double predRE = (mInstance.alphaT * mInstance.total_tris) + (mInstance.alphaD * pred_meanD) + (mInstance.alphaH * predWeighted_thr) + mInstance.zeta; // for almost current period
                   // predRE = (double) Math.round((double) predRE * 1000) / 1000;
// nill added temp

                re_mape = abs(reMsrd - predRE) / reMsrd;// log this
                    if (re_mape > 0.1) {
                        accRe = false;// after training we check to see if the model is accurate to then cal next triangle
                        if (variousTris >= 3) // this is to regulate throughput factor for decimated values
                            mInstance.re_miss_counter += 1;

                        if (mInstance.re_miss_counter > missCounter && mInstance.decTris.size() != 0) {
                            if (reMsrd > predRE) {
                                mInstance.re_factor -= 0.1;// reduce the effect of decimation to increase re
                                if (mInstance.re_factor < 0)
                                    mInstance.re_factor = 0;

                            } else
                                mInstance.re_factor += 0.1;// increase the effect of decimation to decrease the calculated pred_re

                        }


                    } else
                        mInstance.re_miss_counter = 0; // the model works fine so we don't need to re-adjust the throughput factor for decimation data collection




                    //  if (variousTris>=3 && Math.abs(deltaRe) >= 0.2 && (PRoAR < 0.7 || PRoAI < 0.7))// test and see what is the re range


                    if (variousTris >= 3 && (reMsrd >= 1.20 || (reMsrd <= 0.8 && avgq != 1)))// if re is not balances (or pAR is not close to PAI, we will change the next tris count
                        // the last cond (reMsrd <0.8 && avgq!=1) says that if the AI is working better than AR and AI has not in original quality so that we can increase tot tris
                        mInstance.lastConscCounter++;

                    else
                        mInstance.lastConscCounter = 0;


                    long time1 = 0;
                    long time2 = 0;


               //     /* temp deactivate -> this is for next triangle count computation and odra algorithm
/*
                    if (  mInstance.activate_b && accRe  && mInstance.lastConscCounter > 2) // if both RE and all AI throughput models are accurate ,, the second condition is to skip change in nexttris for the first loop while we just had a change in tot tris
                    {

                        time1 = System.nanoTime() / 1000000; //starting first loop
                        time2 = time1;
                        double nomin = 1 - ((mInstance.alphaD + (mInstance.alphaH * mInstance.rohD)) * meanDkk)
                                - (mInstance.zeta + (mInstance.alphaH * mInstance.delta));
                        // double nomin2= 1- (mInstance.alphaD* meanDkk) -(mInstance.alphaH *)
                        double denom = mInstance.alphaT + (mInstance.rohT * mInstance.alphaH); //α + ργ
                        double tmpnextTris = (nomin / (denom));

                        if (tmpnextTris > 0 &&
                        tmpnextTris<= mInstance.orgTrisAllobj)  {

                            // temporarily inactive to not to run algo-> just wanna check nexttris values
                            if (tmpnextTris < mInstance.orgTrisAllobj && tmpnextTris >= minTrisThreshold)
                                nextTris = tmpnextTris;

                            else if (tmpnextTris < minTrisThreshold)
                                nextTris = minTrisThreshold ;

//                            else if (tmpnextTris > mInstance.orgTrisAllobj)
//                                nextTris = mInstance.orgTrisAllobj;


                           // nextTris = Math.round(nextTris * 1000) / 1000;
                            trainedTris = true;

                            try {


                                odraAlg((float) nextTris);
                                time2 = System.nanoTime() / 1000000;
                                // long t2 = System.nanoTime() / 1000000;
                                mInstance.t_loop1 = time2 - time1 - (sleepTime * (objC - 1));
                                // mInstance.t_loop2 = t2 - t1;
                                mInstance.lastConscCounter = 0;// we let the effect of change in triangle count stand for at least 4 times by reseting this counter. if you don't reset, by any chance new re might be <08 and then the orda happens again

                                if (nextTris != mInstance.total_tris && !mInstance.decTris.contains(mInstance.total_tris)) // if next tris is lower than total tris we have decimation
                                    mInstance.decTris.add(mInstance.total_tris);// add new total triangle count in the decimated list


                            } catch (InterruptedException e) {
                                e.printStackTrace();
                            }


                            algNxtTris = mInstance.total_tris;

                        }


                    }  //@ //if
*/

//                    writeRE(reMsrd, predRE, trainedRE, mInstance.total_tris, nextTris, algNxtTris, trainedTris, PRoAR, PRoAI, (accRe), mInstance.orgTrisAllobj, avgq, mInstance.t_loop1);// writes to the file


//                }           //  RE modeling and next tris

//          }// if all AI throughput models are accurate, we train RE and calculate next tris


//         }//  if( acc_throughput) // we start to train RE and calculate next tris if H_i models are accurate

         predRE = (mInstance.alphaT * mInstance.total_tris) + (mInstance.alphaD * pred_meanD) + (mInstance.alphaH * predWeighted_thr) + mInstance.zeta; // for almost current period
         re_mape = abs(reMsrd - predRE) / reMsrd;// log this
        if (re_mape > 0.1)
            accRe = false;// after training we check to see if the model is accurate to then cal next triangle

          writeRE(reMsrd, predRE, trainedRE, mInstance.total_tris, nextTris, algNxtTris, trainedTris, PRoAR, PRoAI, (accRe), mInstance.orgTrisAllobj, avgq, mInstance.t_loop1);// writes to the file
         writequality();
        //if we have objs on the screen, we start RE model & training
//
// temp deactive RE training
//


       // }   // if Nan

       // else
           // Log.d("Mean Throughput is", "not accepted");


        mInstance.pred_meanD_cur = meanDkk; // the predicted current_mean_distance for next period is saved here,
        // so we'll have the estimated current distance in next period




    }//run


    public void cleanOutArraysRE(double totTris, double predDis, MainActivity mInstance){// clear based on various distance value

        int index=0;


        double[][] reRegParameters = mInstance.reParamList.get(totTris).stream().map(l -> l.stream().mapToDouble(Double::doubleValue).toArray())
                .toArray(double[][]::new);// has four elements where distance is the second one

        index= findClosestRE(reRegParameters , predDis);// the index of value needed to be deleted

        for (int i=binCap-1; i<mInstance.trisRe.get(totTris).size();i++)
            // since we add if objcount != zero to avoid wrong inf from tris=0 -> we won't have re or distance at this situation
           if(index<mInstance.trisRe.get(totTris).size())
                {
                    double value = Iterables.get(mInstance.trisRe.get(totTris), index);// gets the value in thr_models (hashmultimap)
                    mInstance.trisRe.get(totTris).remove(value);
                 //  List<Double> value2 = Iterables.get(mInstance.reParamList.get(totTris), index);// gets the value in thr_models (hashmultimap)
                    mInstance.reParamList.get(totTris).remove(index);}
        // return index;
    }



    public void cleanOutArraysREFIFO(double totTris, double predDis, MainActivity mInstance){// clear based on FIFO

                int index=0;
                double value = Iterables.get(mInstance.trisRe.get(totTris), index);// gets the value in thr_models (hashmultimap)
                mInstance.trisRe.get(totTris).remove(value);
                List<Double> value2 = Iterables.get(mInstance.reParamList.get(totTris), index);// gets the value in thr_models (hashmultimap)
                mInstance.reParamList.get(totTris).remove(value2);
        // return index;
    }


    public int cleanOutArraysThrFIFO(double totTris, double predDis, MainActivity mInstance){// FIFO policy to get the updated data

//        while( mInstance.trisMeanDisk.get(totTris).size()>binCap) // to make sure if it doesn't have extra data
//            mInstance.trisMeanDisk.get(totTris).remove(0);

        int index=0; // all multimaps preserve value of a key, so we know any index  would apply for all lists

        int Ai_count= mInstance.mList.size();

        for (int aiIndx=0;aiIndx<Ai_count;aiIndx++)// we need to do it for all AIs here, since if we remove from general array of trisMeanDisk for AI_index=0, the next AIs won't have the information of tris-meandistance
            for (int i=binCap-1; i<mInstance.thr_models.get(aiIndx).get(totTris).size();i++) {

                double value = Iterables.get(mInstance.thr_models.get(aiIndx).get(totTris), index);// gets the value in thr_models (hashmultimap)
                mInstance.thr_models.get(aiIndx).get(totTris).remove(value); // This code removes the value not the older one that had remove(index)

                List<Double> value2 = Iterables.get(mInstance.thParamList.get(aiIndx).get(totTris), index);// gets the value in thr_models (hashmultimap)
                mInstance.thParamList.get(aiIndx).get(totTris).remove(value2);

                //mInstance.thParamList.get(aiIndx).get(totTris).remove(index);
                if (aiIndx == 0)// since we add to this just once at each period when aiindex==0
                    mInstance.trisMeanDisk.get(totTris).remove(index); //removes from the head (older data) -> to then add to the tail
            }

        return  index;// not used
    }

    public int cleanOutArraysThr(double totTris, double predDis, MainActivity mInstance){// if tris0meandisk is full, it means
        // the array of other AI tasks is full too, so we update the lists


        while( mInstance.trisMeanDisk.get(totTris).size()>binCap) // to make sure if it doesn't have extra data
            mInstance.trisMeanDisk.get(totTris).remove(0);

        int index=-1; // all multimaps preserve value of a key, so we know any index  would apply for all lists
        double[] disArray = mInstance.trisMeanDisk.get(totTris).stream()
                .mapToDouble(Double::doubleValue)
                .toArray(); // this has the array of predicted distance
        index = findClosest(disArray, predDis);// the index of value(closest to current mean dis) needed to be deleted

        int Ai_count= mInstance.mList.size();

        for (int aiIndx=0;aiIndx<Ai_count;aiIndx++)// we need to do it for all AIs here, since if we remove from general array of trisMeanDisk for AI_index=0, the next AIs won't have the information of tris-meandistance
            for (int i=binCap-1; i<mInstance.thr_models.get(aiIndx).get(totTris).size();i++) {

            double value = Iterables.get(mInstance.thr_models.get(aiIndx).get(totTris), index);// gets the value in thr_models (hashmultimap)
            mInstance.thr_models.get(aiIndx).get(totTris).remove(value); // This code removes the value not the older one that had remove(index)
              //  List<Double> value2 = Iterables.get(mInstance.thParamList.get(aiIndx).get(totTris), index);// gets the value in thr_models (hashmultimap)
                mInstance.thParamList.get(aiIndx).get(totTris).remove(index);

            //mInstance.thParamList.get(aiIndx).get(totTris).remove(index);
            if (aiIndx == 0)// since we add to this just once at each period when aiindex==0
                mInstance.trisMeanDisk.get(totTris).remove(index); //removes from the head (older data) -> to then add to the tail
        }

        return  index;// not used
    }


    public int cleanOutArraysThr_weighted(double totTris, double predDis, MainActivity mInstance){

        int index=-1;
        if ( mInstance.trisMeanDisk_Mir.get(totTris).size() >= binCap) {

            double[] disArray = mInstance.trisMeanDisk_Mir.get(totTris).stream()
                    .mapToDouble(Double::doubleValue)
                    .toArray(); // this has the array of predicted distance

            index = findClosest(disArray, predDis);// the index of value(closest to current mean dis) needed to be deleted
            mInstance.trisMeanThr.get(totTris).remove(index); // has the real throughput
            mInstance.thParamList_Mir.get(totTris).remove(index);


            mInstance.trisMeanDisk_Mir.get(totTris).remove(index); //removes from the head (older data) -> to then add to the tail
            // mInstance.trisMeanDiskk.get(totTris).remove(index);
        }

        return  index;
    }


    public static int findClosestRE(double[][] arr, double target) { // find the closest index of arr to value distance= target to then substitue that with the newer one
        int idx = 0;
        double dist = Math.abs(arr[0][1] - target);

        for (int i = 1; i< arr.length; i++) {
            double cdist = Math.abs(arr[i][1] - target);

            if (cdist < dist) {
                idx = i;
                dist = cdist;
            }
        }

        return idx;
    }


    public static int findClosest(double[] arr, double target) { // find the closest index of arr to value distance= target to then substitue that with the newer one
        int idx = 0;
        double dist = Math.abs(arr[0] - target);

        for (int i = 1; i< arr.length; i++) {
            double cdist = Math.abs(arr[i] - target);

            if (cdist < dist) {
                idx = i;
                dist = cdist;
            }
        }

        return idx;
    }

    public void writequality(){


        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        String currentFolder = mInstance.getExternalFilesDir(null).getAbsolutePath();
        String FILEPATH = currentFolder + File.separator + "Quality"+mInstance. fileseries+".csv";

        try (PrintWriter writer = new PrintWriter(new FileOutputStream(FILEPATH, true))) {
           for (int i=0; i<objC-1; i++) {
               double curtris = mInstance.renderArray.get(i).orig_tris * mInstance.ratioArray.get(i);
               float r1 = mInstance.ratioArray.get(i); // current object decimation ratio
//               if (mInstance.renderArray.get(i).fileName.contains("0.6")) // third scenario has ratio 0.6
//                   r1 = 0.6f; // jsut for scenario3 objects are decimated
//               else if(mInstance.renderArray.get(i).fileName.contains("0.3")) // sixth scenario has ratio 0.3
//                   r1=0.3f;


               float r2 = ref_ratio * r1; // wanna compare obj level of sensitivity to see if we decimate object more -> to (ref *curr) ratio, would the current object hurt more than the other ones?
               int indq = mInstance.excelname.indexOf(mInstance.renderArray.get(i).fileName);// search in excel file to find the name of current object and get access to the index of current object
               // excel file has all information for the degredation model
               float gamma = mInstance.excel_gamma.get(indq);
               float a = mInstance.excel_alpha.get(indq);
               float b = mInstance.excel_betta.get(indq);
               float c = mInstance.excel_c.get(indq);
               float d_k = mInstance.renderArray.get(i).return_distance();// current distance

               float tmper1 = Calculate_deg_er(a, b, c, d_k, gamma, r1); // deg error for current sit
               float tmper2 = Calculate_deg_er(a, b, c, d_k, gamma, r2); // deg error for more decimated obj


               float max_nrmd = mInstance.excel_maxd.get(indq);
               tmper1 = tmper1 / max_nrmd; // normalized
               tmper2= tmper2 /max_nrmd;

               if (tmper2 < 0)
                   tmper2 = 0;

               //Qi−Qi,r divided by Ti(1−Rr) = (1-er1) - (1-er2) / ....
               sensitivity[i] = (abs(tmper2 - tmper1) / (curtris - (ref_ratio * curtris)));
               tmper1 = (float) Math.round((float) (tmper1 * 1000)) / 1000;

                StringBuilder sb = new StringBuilder();
                sb.append(dateFormat.format(new Date()));
                sb.append(',');
                sb.append(mInstance.renderArray.get(i).fileName+"_n"+(i+1)+"_d"+(d_k));
                sb.append(',');
                sb.append(sensitivity[i]);
                sb.append(',');
                sb.append(r1);
                sb.append(',');
                sb.append(1-tmper1);
               sb.append(',');
               sb.append(reward);
               sb.append(',');
               sb.append(mInstance.best_BT);
               sb.append(',');
               sb.append((mInstance.best_BT-reward)/mInstance.best_BT);
               int hbo_running=0;// if hbo is not running, hence we have good data to show
               if(mInstance.curBysIters!=-1)
                   hbo_running=1;
              //  sb.append(mInstance.tasks.toString());
               sb.append(',');
               sb.append(hbo_running);


               sb.append(',');
               sb.append(mInstance.afterHbo_counter);
                sb.append('\n');
                writer.write(sb.toString());
                System.out.println("done!");
            }
        }catch (FileNotFoundException e) {
            System.out.println(e.getMessage());
        }

    }
//    public void writeThr(double realThr, double predThr, boolean trainedFlag,int aiIndx,double ai_acc,double meanH, double pred_H,double perAI_mape){ // AI throughput information for each task individually and response time for all models
//
//        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss:SSS");
//        String currentFolder = mInstance.getExternalFilesDir(null).getAbsolutePath();
//        String FILEPATH = currentFolder + File.separator + "Throughput"+mInstance. fileseries+".csv";
//
//        StringBuilder sb = new StringBuilder();
//        sb.append(dateFormat.format(new Date())); sb.append(',').append((aiIndx)).append(",");
//        sb.append(realThr);sb.append(',').append(predThr);sb.append(',').append(trainedFlag);sb.append(',').append(ai_acc).append(",");
//        sb.append(mInstance.rohTLRt.get(aiIndx));sb.append(',').append(mInstance.rohDLRt.get(aiIndx));sb.append(',').append(mInstance.deltaLRt.get(aiIndx));sb.append(',');
//        sb.append(mInstance.baseline_AIRt.get(aiIndx)* mInstance.des_Rt_weight);
//        sb.append(','); sb.append(mInstance.des_Q).append(',');
//        sb.append(mInstance.total_tris);
//      //  sb.append(',').append( meanthr);// this is measured directly
//        sb.append(','); sb.append(meanH).append(',').append(pred_H).append(',').append(perAI_mape);
//
//        try (PrintWriter writer = new PrintWriter(new FileOutputStream(FILEPATH, true))) {
//
//
//        for (int i=0;i<mInstance.mList.size();i++)
//        {
//            AiItemsViewModel taskView=mInstance.mList.get(i);
//            sb.append(",").append(taskView.getModels().get(taskView.getCurrentModel()))
//                    .append(",").append(taskView.getDevices().get(taskView.getCurrentDevice()))
//                    .append(",").append(taskView.getInferenceT()).append(",").append(taskView.getOverheadT());
//
//
//        }
//
//
//
//
//
//            sb.append('\n');
//            writer.write(sb.toString());
//            System.out.println("done!");
//        } catch (FileNotFoundException e) {
//            System.out.println(e.getMessage());
//        }
//    }


    public void writeThr(double realThr, double predThr, boolean trainedFlag,double ai_acc){


        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss:SSS");
        String currentFolder = mInstance.getExternalFilesDir(null).getAbsolutePath();
        String FILEPATH = currentFolder + File.separator + "Throughput"+mInstance. fileseries+".csv";
        StringBuilder sb = new StringBuilder();



        sb.append(dateFormat.format(new Date())); sb.append(",,");
        sb.append(realThr);sb.append(',').append(predThr);sb.append(',').append(trainedFlag);sb.append(',').append(ai_acc).append(",");
        sb.append(" , , ,");// for weights
        // sb.append(mInstance.rohTL.get(aiIndx));sb.append(',').append(mInstance.rohDL.get(aiIndx));sb.append(',').append(mInstance.deltaL.get(aiIndx));sb.append(',');
        sb.append(mInstance.des_Thr);
        sb.append(','); sb.append(mInstance.des_Q).append(',');
        sb.append(mInstance.total_tris);

        try (PrintWriter writer = new PrintWriter(new FileOutputStream(FILEPATH, true))) {
            for (AiItemsViewModel taskView :mInstance.mList) {

                sb.append(",").append(taskView.getModels().get(taskView.getCurrentModel()))
                        .append(",").append(taskView.getDevices().get(taskView.getCurrentDevice()))
                        .append(",").append(taskView.getInferenceT()).append(",").append(taskView.getOverheadT());
            }
            sb.append('\n');
            writer.write(sb.toString());
            System.out.println("done!");
        } catch (FileNotFoundException e) {
            System.out.println(e.getMessage());
        }
    }
    public void writeWeights( boolean ai_acc,int ai_indx){ // AI throughput information for each task individually and response time for all models

        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss:SSS");
        String currentFolder = mInstance.getExternalFilesDir(null).getAbsolutePath();
        String FILEPATH = currentFolder + File.separator + "Weights"+mInstance. fileseries+".csv";
        StringBuilder sb = new StringBuilder();
        sb.append(dateFormat.format(new Date())); sb.append(',').append(ai_acc);
        AiItemsViewModel taskView=mInstance.mList.get(ai_indx);
        sb .append(",").append(taskView.getModels().get(taskView.getCurrentModel()))
                .append(" ,")  .append(",").append(mInstance.nrmest_weights.get(ai_indx));
        sb.append('\n');

        try (PrintWriter writer = new PrintWriter(new FileOutputStream(FILEPATH, true))) {
            writer.write(sb.toString());
            System.out.println("done!");
        } catch (FileNotFoundException e) {
            System.out.println(e.getMessage());
        }
    }


    public void write_weightedH( double avg_msr_H, double avg_est_H, double msr, double pre, double Mape,boolean acc_model){ // AI throughput information for each task individually and response time for all models

        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss:SSS");
        String currentFolder = mInstance.getExternalFilesDir(null).getAbsolutePath();
        String FILEPATH = currentFolder + File.separator + "Weighted_throughput"+mInstance. fileseries+".csv";
        StringBuilder sb = new StringBuilder();
        sb.append(dateFormat.format(new Date())); sb.append(',').append(    avg_msr_H+","+avg_est_H+","+Mape+
                ","+msr+","+pre+","+acc_model);

        sb.append('\n');
        try (PrintWriter writer = new PrintWriter(new FileOutputStream(FILEPATH, true))) {
            writer.write(sb.toString());
            System.out.println("done!");
        } catch (FileNotFoundException e) {
            System.out.println(e.getMessage());
        }
    }


    // public void writeRE(double realRe, double predRe, boolean trainedFlag,double totT, double nextT, boolean trainedT, double pAR, double pAI){
    public void writeRE(double realRe, double predRe, boolean trainedFlag, double totT, double nextT, double algTris, boolean trainedT,
                        double pAR, double pAI, boolean accM, double totTris, double avgq, long duration){

        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss:SSS");
        String currentFolder = mInstance.getExternalFilesDir(null).getAbsolutePath();
        String FILEPATH = currentFolder + File.separator + "RE"+mInstance. fileseries+".csv";

        try (PrintWriter writer = new PrintWriter(new FileOutputStream(FILEPATH, true))) {
            StringBuilder sb = new StringBuilder();
            sb.append(dateFormat.format(new Date())); sb.append(',');
            sb.append(realRe);sb.append(',');
            sb.append(predRe);
            sb.append(',');  sb.append(trainedFlag);
            sb.append(',');  sb.append(totT);
            sb.append(',');  sb.append(nextT);
            sb.append(',');  sb.append(algTris);
            sb.append(',');  sb.append(trainedT);
            sb.append(',');  sb.append(pAR);
            sb.append(',');  sb.append(pAI);
            sb.append(',');  sb.append(accM);// if both models are accurate
            sb.append(',');  sb.append(totTris);// if both models are accurate
            sb.append(',');  sb.append(avgq);
            sb.append(',');  sb.append(duration);
            sb.append('\n');
            writer.write(sb.toString());
            System.out.println("done!");
        } catch (FileNotFoundException e) {
            System.out.println(e.getMessage());
        }
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


            float deg_error = (float) Math.round((float) (Calculate_deg_er(a, b, c, d, gamma, curQ) * 1000)) / 1000;
            float max_nrmd = mInstance.excel_maxd.get(i);

            float cur_degerror = deg_error / max_nrmd;
            float quality= 1- cur_degerror;
            objquality[ind]=quality;
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



    float odraAlg(float tUP) throws InterruptedException {


        candidate_obj = new HashMap<>();
        Map<Integer, Double> sortedcandidate_obj = new HashMap<>();
        float sum_org_tris = 0; // sum of all tris of the objects o the screen

        for (int ind = 0; ind < mInstance.objectCount; ind++) {

            sum_org_tris += mInstance.renderArray.get(ind).orig_tris;// this will ne used to cal min of tris needed at each row (object) in bellow


            double curtris = mInstance.renderArray.get(ind).orig_tris * mInstance.ratioArray.get(ind);


            tris_share[ind] = (curtris / tUP);
            candidate_obj.put(ind, sensitivity[ind] / tris_share[ind]);


        }
        sortedcandidate_obj = sortByValue(candidate_obj, false); // second arg is for order-> ascending or not? NO
        // Up to here, the candidate objects are known


        double updated_sum_org_tris = sum_org_tris; // keeps the last value which is sum_org_tris - tris1-tris2-....
        for (int i : sortedcandidate_obj.keySet()) { // check this gets the candidate object index to calculate min weight
            double sum_org_tris_minus = updated_sum_org_tris - mInstance.renderArray.get(i).orig_tris; // this is summ of tris for all the objects except the current one
            updated_sum_org_tris = sum_org_tris_minus;
            tMin[i] = coarse_Ratios[coarse_Ratios.length - 1] * sum_org_tris_minus;// minimum tris needs for object i+1 to object n
            ///@@@@ if this line works lonely, delete the extra line for the last object to zero in the alg
        }

        Map.Entry<Integer, Double> entry = sortedcandidate_obj.entrySet().iterator().next();
        int key = entry.getKey(); // get access to the first key -> to see if it is the first object for bellow code

        int prevInd = 0;
        for (int i : sortedcandidate_obj.keySet()){  // line 10 i here is equal to alphai -> the obj with largest candidacy
            // check this gets the candidate object index to maintain its quality
            for (int j = 0; j < coarse_Ratios.length; j++) {

                int indq = mInstance.excelname.indexOf(mInstance.renderArray.get(i).fileName);// search in excel file to find the name of current object and get access to the index of current object
                float gamma =mInstance. excel_gamma.get(indq);
                float a =mInstance. excel_alpha.get(indq);
                float b = mInstance.excel_betta.get(indq);
                float c =mInstance. excel_c.get(indq);
                float d_k = mInstance.renderArray.get(i).return_distance();// current distance
                float max_nrmd = mInstance.excel_maxd.get(indq);
                float quality = 1 -( Calculate_deg_er(a, b, c, d_k, gamma, coarse_Ratios[j]) / max_nrmd  ); // deg error for current sit

                if (i == key && tUP >= mInstance.renderArray.get(i).getOrg_tris() * coarse_Ratios[j]) { // the first object in the candidate list
                    fProfit[i][j] = quality;// Fα(i),j ←Qα(i),j -> i is alpha i
                    tRemainder[i][j] = tUP - (mInstance.renderArray.get(i).getOrg_tris() * coarse_Ratios[j]);
                } else //  here is the dynamic programming section
                    for (int s = 0; s < coarse_Ratios.length; s++) {

                        double f = fProfit[prevInd][s] + quality;
                        double t = tRemainder[prevInd][s] - (mInstance.renderArray.get(i).getOrg_tris() * coarse_Ratios[j]);
                        if (t >= tMin[i] && fProfit[i][j] < f) {

                            fProfit[i][j] = f;
                            tRemainder[i][j] = t;
                            track_obj[i][j] = s;
                        }

                    }//

            }//for j  up to here we reach line 25
            prevInd=i;
        }// for i
/// start with object with least priority

        sortedcandidate_obj = sortByValue(candidate_obj, true); // to iterate through the list from lowest to highest values

        int lowPobjIndx = sortedcandidate_obj.entrySet().iterator().next().getKey(); // line 26
        double tmp=fProfit[lowPobjIndx][0];
        int j=0;
        for  (int maxindex=1;maxindex<coarse_Ratios.length;maxindex++) // line 27
            if(fProfit[lowPobjIndx][maxindex]>tmp)// finds the index of coarse-grain ratio with maximum profit
            {
                tmp = fProfit[lowPobjIndx][maxindex];
                j=maxindex;
            }


        for (int i : sortedcandidate_obj.keySet())
            //if ( mInstance.renderArray.size()>i &&  mInstance.renderArray.get(i)!=null)

            {// to avoid null pointer error

            mInstance.total_tris = mInstance.total_tris - (mInstance.ratioArray.get(i) * (mInstance.o_tris.get(i)));// total =total -1*objtris
            mInstance.ratioArray.set(i, coarse_Ratios[j]);


            mInstance.runOnUiThread(() -> mInstance.renderArray.get(i).decimatedModelRequest(mInstance.ratioArray.get(i), i, false));

            mInstance.total_tris = mInstance.total_tris + (mInstance.ratioArray.get(i) *  mInstance.renderArray.get(i).orig_tris);// total = total + 0.8*objtris
           // mInstance.trisDec.put(mInstance.total_tris,true);

            j = track_obj[i][j];
            Thread.sleep(sleepTime);// added to prevent the crash happens while redrawing all the objects at the same time


        }


        return (float) mInstance.total_tris; // this returns the total algorithm triangle count



    }


    private static Map<Integer, Double> sortByValue(Map<Integer, Double> unsortMap, final boolean order)
    {
        List<Map.Entry<Integer, Double>> list = new LinkedList<>(unsortMap.entrySet());

        // Sorting the list based on values
        list.sort((o1, o2) -> order ? o1.getValue().compareTo(o2.getValue()) == 0
                ? o1.getKey().compareTo(o2.getKey())
                : o1.getValue().compareTo(o2.getValue()) : o2.getValue().compareTo(o1.getValue()) == 0
                ? o2.getKey().compareTo(o1.getKey())
                : o2.getValue().compareTo(o1.getValue()));
        return list.stream().collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue, (a, b) -> b, LinkedHashMap::new));

    }




}



