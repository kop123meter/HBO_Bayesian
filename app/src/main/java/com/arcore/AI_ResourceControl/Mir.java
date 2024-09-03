/**********This was for MIR data collection. noy used for XMIR*/
/* collects data per model per tris for binsize=10
        * periodically runs the prediction and tests if model error>10 -> Yes: retrains the model and then calculates miss-count
        * has THE POLICY TO REGULATE the DECIMATION bins if model gives us more than 5 times noise
        *At the end, calls writethr to write mean response time of measured and calculated per model in each line starting from the first model
        */
package com.arcore.AI_ResourceControl;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.ListMultimap;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.PrintWriter;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static java.lang.Math.abs;

import android.annotation.SuppressLint;
import android.graphics.Color;
import android.widget.TextView;

public class Mir implements Runnable {

    int binCap=6;
    private final MainActivity mInstance;
    float ref_ratio=0.5f;
    int objC;
   // double sensitivity[] ;
    float objquality[];
    //double tris_share[];
    Map <Integer, Double> candidate_obj;
    float []coarse_Ratios=new float[]{1f,0.8f, 0.7f,0.6f, 0.4f, 0.2f, 0.1f};
    //ArrayList <ArrayList<Float>> F_profit= new ArrayList<>();
   // double [][]fProfit;
  //  double [][] tRemainder;
  //THESE all should be defined dynamically  int [][] track_obj;
    int sleepTime=30;
    //float candidate_obj[] = new float[total_obj];
   // double tMin[] ;
    int missCounter=3;//means at least 4 noises
    double [][]fProfit;
    double [][] tRemainder;
    int [][] track_obj;
    TextView posText_re,posText_thr,posText_q,posText_mir;



    public Mir(MainActivity mInstance) {

        this.mInstance = mInstance;

        objC=mInstance.objectCount+1;
       // sensitivity = new double[objC];
      //  tris_share = new double[objC];
        objquality= new float[objC];// 1- degradation-error
        posText_re= mInstance.findViewById(R.id.app_re);
        posText_q= mInstance.findViewById(R.id.app_quality);
        posText_thr= mInstance.findViewById(R.id.app_thr);
        posText_mir= mInstance.findViewById(R.id.app_bt);
        //ArrayList <ArrayList<Float>> F_profit= new ArrayList<>();
       // fProfit= new double[objC][coarse_Ratios.length];
      //  tRemainder= new double[objC][coarse_Ratios.length];
      //  track_obj= new int[objC][coarse_Ratios.length];
        //float candidate_obj[] = new float[total_obj];
     //   tMin = new double[objC];


    }

    @SuppressLint("SuspiciousIndentation")
    @Override
    public void run() {

        boolean accmodel = true;// this is to check if the trained model for thr is accurate
        boolean accRe=true;// this is to check if the trained model for re is accurate
        boolean trainedTris = false;
        boolean trainedThr = false;
        boolean trainedRE = false;
        double maxTrisThreshold = mInstance.orgTrisAllobj;
        double minTrisThreshold = maxTrisThreshold * mInstance.coarse_Ratios[mInstance.coarse_Ratios.length - 1];
        double meanThr;
                //totTris;
        double meanDk = 0; // mean current dis
        double meanDkk = 0; // mean of d in the next period-> you need to cal the average of predicted d for all the objects
        double pred_meanD = mInstance.pred_meanD_cur; // for next 1 sc
        boolean fault_thr=false;
        int tris_window=4;

        double period_init_tris=mInstance.total_tris;// this is the starting triangle count


        //int count=0;
        for (int i = 0; i < mInstance.objectCount; i++) {
            double cur_dis=(double) mInstance.renderArray.get(i).return_distance();
           // if(cur_dis!=0) {
                meanDk += cur_dis;
                meanDkk += mInstance.predicted_distances.get(i).get(1); // 0: next 1 sec, 1: next 2s :  // gets the first time, next 2s-- 3s of every object, ie. d1 of every obj
              //  count+=1;
           // }
        }

        int objc= mInstance.objectCount;
        if(objc>0) {
            meanDk /= objc;
            meanDkk /= objc;

            meanDk = (double) (Math.round((double) (100 * meanDk))) / 100;
            meanDkk = (double) (Math.round((double) (100 * meanDkk))) / 100;


            if (meanDkk == 0)
                meanDkk = meanDk;
            if (pred_meanD == 0)
                pred_meanD = meanDk; // for the first objects

            //totTris = mInstance.total_tris; // hold tris before running the algorithm
        }



        //I got an error for regression since decimation occurs in UI thread and Modeling runs at the same time
        // solution is to start data collection after one period passes from algorithm
 // else{ // just collect data when algorithm was applied in the last period

        //totTris = mInstance.total_tris;

        meanThr = mInstance.getThroughput();// after the objects are decimated
        while((meanThr) > 200 || (meanThr) <1) // we wanna get a correct value
            meanThr = mInstance.getThroughput();

        meanThr = (double) (Math.round((double) (100 * meanThr))) / 100;

           // meanThr = (double) Math.round(meanThr * 100) / 100;
//        TextView posText = (TextView) mInstance.findViewById(R.id.rspT);
//        posText.setText( "Thr: " +String.valueOf( meanThr));

            int variousTris = mInstance.trisMeanThr.keySet().size();


            if( variousTris==1 &&  mInstance.basethr_tag==false )// just for zero triangle count
            {

                if ( mInstance.des_Thr == 0)
                    mInstance.des_Thr= meanThr*mInstance.des_thr_weight;
                else {
                    double curr_baseline =  mInstance.des_Thr ;
                    mInstance.des_Thr=( (meanThr*mInstance.des_thr_weight) + curr_baseline) / 2;
                }

            }

                //mInstance.des_Thr=mInstance.des_thr_weight*meanThr;// baseline *0.6 for instance


// this is thr calculated using the modeling
//            double predThr = (mInstance.rohT *  totTris) + (mInstance.rohD * pred_meanD) + mInstance.delta;// use predicted distance for almost current period (predicted distance for next 1 sec is the closest one we have)
//            predThr = (double) Math.round((double) predThr * 100) / 100;
            double predThr = 0;
// nill added 8 april
//            int ind = -1;
           // if (variousTris < 3) { // one object on the screen
        double fixedT=mInstance.total_tris;

        if ( mInstance.trisMeanThr.containsKey(fixedT) &&
                mInstance.trisMeanThr.get(fixedT).size() == binCap) { //we delete from array of initial tris not new, since we have data of initial tris by 100% we keep data up to binCap per triangleCount
            cleanOutArraysThr(fixedT, pred_meanD, mInstance);// cleans out the closest data to the curr one within bins
        }

        mInstance.trisMeanThr.put(mInstance.total_tris, meanThr); //correct:  should have real throughput for the regression
        // uses predicted cur distance for thr modeling
        mInstance.thParamList_Mir.put(mInstance.total_tris, Arrays.asList(mInstance.total_tris, pred_meanD, 1.0));

        mInstance.trisMeanDisk.put(mInstance.total_tris, pred_meanD); //correct:  should have predicted value removes from the head (older data) -> to then add to the tail

        boolean acc_throughput=true;

//            if (mInstance.objectCount != 0) {//to avoid wrong inf from tris=0 -> we won't have re or distance at this situation

               if (variousTris >= 2) {// at least two points to start modeling

// checks error of the model after new added model
                    double mape = 0.0; // mean of absolute error
                    double fit = (mInstance.rohT * mInstance.total_tris) + (mInstance.rohD * pred_meanD )+ mInstance.delta; // fit is predicted current throughput should be predicted distance (we have next 1 sec but ok since we don't move)  for current period
                    //double fit = mInstance.thSlope * totTris + mInstance.thIntercept;;
                    mape = Math.abs((meanThr - fit) / meanThr);// this is correct to calculate error coming from real throughput vs model


                   int conseq_error_counter= mInstance.conseq_error.get(0);// num of consequent noises/errors per AI model

                   if(mape> 0.1) {// assume we have the list of H during time for AI1: [13,14,15,13]
                       //mInstance.hAI_acc.set(aiIndx, false);
                       mInstance.conseq_error.set(0,conseq_error_counter+1);// this is to avoid multiple un-necessary
                   }
                   else if(conseq_error_counter>0)
                       mInstance.conseq_error.set(0,conseq_error_counter-1);// reset since the model is fine




                   ///  nil   replaced by mape>0.1
                    if ( mInstance.conseq_error.get(0) >2 && variousTris >=3) {// we ignore tris=0 them we need points with at least two diff tris in order to generate the line
// 8 april
                        ListMultimap<Double, List<Double>> copythParamList = ArrayListMultimap.create(mInstance.thParamList_Mir);// take a copy to then fill it for training up to capacity of 10
                        ListMultimap<Double, Double> copytrisMeanThr = ArrayListMultimap.create(mInstance.trisMeanThr);// take a copy to then fill it for training up to capacity of 10


                        for (double curT : mInstance.trisMeanThr.keySet()) {

                          //   this is to calculate the mean of values in the bins

                            if (mInstance.trisMeanDisk.get(curT).size() < binCap) {
                                int index = mInstance.trisMeanDisk.get(curT).size();
                                double mmeanTh = 0, mmeanDK = 0;

                                for (int i = 0; i < index; i++) {
                                    mmeanTh += mInstance.trisMeanThr.get(curT).get(i);
                                    mmeanDK += mInstance.trisMeanDisk.get(curT).get(i);

                                }

                                mmeanTh /= index;
                                mmeanDK /= index;
                                //   mmeanDKk/=index;

                                for (int j = index; j < binCap; j++) {

                                    copytrisMeanThr.put(curT, mmeanTh);
                                    copythParamList.put(curT, Arrays.asList(curT, mmeanDK, 1.0)); // this is to calculate the mean of values in the bins so it's correct

                                }
                            }// if <10
                        }


        //     @@@@ here we check if we have any record for the
        //  decimated objects, if yes, we need to check the ratio of decimated itration over added scenarios, we define a rate of 40% to make sure that we have
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
                                List<Double> thrList = new LinkedList<>(copytrisMeanThr.get(currentT));// since copy list has full data
                                for (double th : thrList)// elements of re list
                                    copytrisMeanThr.put(currentT, th);

                                List<List<Double>> thpList = new LinkedList<>(copythParamList.get(currentT));// since copy list has full data
                                for (List<Double> thpr : thpList)// throughput parameters
                                    copythParamList.put(currentT, thpr);
                                j += 1;

                            }

                        }
                        // to copy the decimated data into throughout modeling


                        double[] throughput = copytrisMeanThr.values().stream()
                                .mapToDouble(Double::doubleValue)
                                .toArray();

                        double[] y = Arrays.copyOfRange(throughput, 0, throughput.length); // should be real throughput
                        double[][] thRegParameters = copythParamList.values().stream()
                                .map(l -> l.stream().mapToDouble(Double::doubleValue).toArray())
                                .toArray(double[][]::new);

                        // should have predicted distance
                        mLinearRegression regression = new mLinearRegression(thRegParameters, y);
                        if (!Double.isNaN(regression.beta(0))) {

                            mInstance.rohT = regression.beta(0);
                            mInstance.rohD = regression.beta(1);
                            mInstance.delta = regression.beta(2);
                            mInstance.thRmse = regression.rmse;
                            trainedThr = true;


                        }
                        thRegParameters=null; // free the storage
                        y=null;
                        copytrisMeanThr.clear();
                        copythParamList.clear();



                        //         }// end of modeling throughput

                    }// end of i tris>2
                  //  nil   commented  sep 7   *


//                  get the right throughput after remodeling
                    predThr = (mInstance.rohT * mInstance.total_tris )+ (mInstance.rohD * pred_meanD) + mInstance.delta;// use predicted distance for almost current period (predicted distance for next 1 sec is the closest one we have)
                    predThr = (double) Math.round((double) predThr * 100) / 100;

                    mape = Math.abs((meanThr - predThr) / meanThr);
                    if (mape > 0.1) {
                        accmodel = false;// after training we check to see if the model is accurate to then cal next triangle
                        acc_throughput=false;

                        if (variousTris >= 3)
                            mInstance.thr_miss_counter1+= 1;

                        if (mInstance.thr_miss_counter1 > 3 &&  mInstance.decTris.size() != 0) { // this is to regulate the factor of considering decimated collected data in the bins
                            if (meanThr > predThr) {
                                mInstance.thr_factor -= 0.1;// reduce the effect of decimation to increase predicted throughput
                                if (mInstance.thr_factor < 0)
                                    mInstance.thr_factor = 0;
                            } else
                                mInstance.thr_factor += 0.1;// increase the effect of decimation to decrease the calculated pred_re

                        }

                    } else {

                            mInstance.conseq_error.set(0,0);

                        mInstance.thr_miss_counter1 = 0;// the model works fine so we don't need to re-adjust the throughput factor for decimation data collection
                    }



                    writeThr(meanThr, predThr, trainedThr,100*mape);// for the urrent period




                } //  throughput model




        //******************  RE modeling *************
                // double sum = 0;
                double avgq = calculateMeanQuality();
                  avgq = (double) (Math.round((double) (100 * avgq))) / 100;

//                writequality();

                posText_thr.setText("T: "+String.valueOf( meanThr));
                posText_q.setText("Q: "+String.valueOf( avgq));


                double PRoAR = (double) Math.round((avgq / mInstance.des_Q) * 100) / 100;
                double PRoAI = (double) Math.round((meanThr / mInstance.des_Thr) * 100) / 100;// should be real
                double reMsrd = PRoAR / PRoAI;
                reMsrd = (double) Math.round(reMsrd * 100) / 100;
                posText_re.setText("R: "+String.valueOf( reMsrd));

                double nextTris =  mInstance.total_tris;
                double algNxtTris =  mInstance.total_tris;

                if( acc_throughput && mInstance.activate_b) {//  mInstance.activate_b is a switch to have MIR activated, I use it for qualtric surveys to avoid running OTDA algorithm


                    if (mInstance.objectCount == 0) {
                    } else {

                        int reModSize = mInstance.trisReMir.size(); // has real mean-throughput

                        if(acc_throughput && reMsrd!= Double.NaN)     {
                        fixedT = mInstance.total_tris;
                        if (mInstance.trisReMir.containsKey(fixedT) && mInstance.trisReMir.get(fixedT).size() == binCap)
                            cleanOutArraysRE(fixedT, pred_meanD, mInstance);// check to remove extra value in the RE parameters list , substitue the newer one

                        mInstance.trisReMir.put(mInstance.total_tris, reMsrd);//correct:  has real re should have real throughout
                        // use predicted current dis and predicted current throughput in the modeling
                        mInstance.reParamList.put(mInstance.total_tris, Arrays.asList(mInstance.total_tris, pred_meanD, predThr, 1.0)); // the two pred are coming from prev period (that were predicted for current period)

                    }


                        double re_mape = 0.0;      //  sum of square error
                        double fit = (mInstance.alphaT * mInstance.total_tris) + (mInstance.alphaD * pred_meanD) + (mInstance.alphaH * predThr) + mInstance.zeta;// uses predicted modeling  for current period
                        re_mape = Math.abs((reMsrd - fit) / reMsrd);

                        if (re_mape > 0.10 && variousTris >= 3) {// we ignore tris=0 them we need points with at least two diff tris in order to generate the line


                            ListMultimap<Double, List<Double>> copyreParamList = ArrayListMultimap.create(mInstance.reParamList);// take a copy to then fill it for training up to capacity of 10
                            ListMultimap<Double, Double> copytrisRe = ArrayListMultimap.create(mInstance.trisReMir);// take a copy to then fill it for training up to capacity of 10


                            // This is to calculate mean of bins for distance, throughput ,...
                            for (double curT : mInstance.trisReMir.keySet()) {

                                if (curT != 0 && mInstance.trisReMir.get(curT).size() < binCap) {
                                    int index = mInstance.trisReMir.get(curT).size();
                                    double meanRE = 0, mmeanDK = 0, meanPrth = 0;

                                    for (int i = 0; i < index; i++) {
                                        meanRE += mInstance.trisReMir.get(curT).get(i);
                                        List<Double> reParL = mInstance.reParamList.get(curT).get(i);
                                        mmeanDK += reParL.get(1);
                                        meanPrth += reParL.get(2);
                                    }

                                    meanRE /= index;
                                    mmeanDK /= index;
                                    meanPrth /= index;

                                    for (int j = index; j < binCap; j++) {
                                        copytrisRe.put(curT, meanRE);
                                        copyreParamList.put(curT, Arrays.asList(curT, mmeanDK, meanPrth, 1.0)); // this is correct since it's mean of bins and they include predicted values

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


                            if (variousTris >= 3) {
                                mLinearRegression regression = new mLinearRegression(reRegParameters, RE);
                                if (!Double.isNaN(regression.beta(0))) {
                                    mInstance.alphaT = regression.beta(0);
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

//                if (reModSize < 4)// april 8{
//                {
//
//                     }
                        double predRE = mInstance.alphaT * mInstance.total_tris + mInstance.alphaD * pred_meanD + mInstance.alphaH * predThr + mInstance.zeta; // for almost current period

                        re_mape = Math.abs((reMsrd - predRE) / reMsrd);// log this
                        if (re_mape > 0.1) {
                            accmodel = false;// after training we check to see if the model is accurate to then cal next triangle
                            accRe = false;
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

                        if (
                                //variousTris >= 3 && no need to this since if lists are refreshed, we still need to activate balancer
                                (reMsrd >= 1.20 || (reMsrd <= 0.8 && avgq != 1)))// if re is not balances (or pAR is not close to PAI, we will change the next tris count
                            // the last cond (reMsrd <0.8 && avgq!=1) says that if the AI is working better than AR and AI has not in original quality so that we can increase tot tris
                            mInstance.lastConscCounter++;

                        else
                            mInstance.lastConscCounter = 0;
// now we calculate next period tris here :)


                        long time1 = 0;
                        long time2 = 0;

                        if (accRe && mInstance.lastConscCounter > 2) // the second condition is to skip change in nexttris for the first loop while we just had a change in tot tris
                        {
                            time1 = System.nanoTime() / 1000000; //starting first loop
                            time2 = time1;
                            double nomin = 1 - ((mInstance.alphaD + (mInstance.alphaH * mInstance.rohD)) * meanDkk)
                                    - (mInstance.zeta + (mInstance.alphaH * mInstance.delta));
                            // double nomin2= 1- (mInstance.alphaD* meanDkk) -(mInstance.alphaH *)
                            double denom = mInstance.alphaT + (mInstance.rohT * mInstance.alphaH); //α + ργ
                            double tmpnextTris = (nomin / denom);

                            if (tmpnextTris > 0 &&
                                    tmpnextTris <= mInstance.orgTrisAllobj) {

                                // temporarily inactive to not to run algo-> just wanna check nexttris values
                                if (tmpnextTris < mInstance.orgTrisAllobj && tmpnextTris >= minTrisThreshold)
                                    nextTris = tmpnextTris;

                                else if (tmpnextTris < minTrisThreshold)
                                    nextTris = minTrisThreshold + 1000;

//                        else if (tmpnextTris > mInstance.orgTrisAllobj)
//                            nextTris = mInstance.orgTrisAllobj;

                                // nextTris =  Math.round(nextTris * 100) / 100;
                                trainedTris = true;
//                                System.out.println("OTDA Activated");
                                //   if (mInstance.trainedTris == true) {
                                try {
                                    mInstance. mir_active_count+=1;

                                    posText_mir.setText("MIR: "+String.valueOf( mInstance. mir_active_count));
                                    posText_mir.setTextColor(Color.parseColor("#FFFA0000"));// make it red

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

                        }//if accRe and lastC>2

                    }//else inside


//                    if( mInstance.trisChanged==true)
//                    {  mInstance.cleanedbin=false;
//                        mInstance.trisChanged=false;
//
//                    }




                }// if acc trhoughput to start RE data gathering and training


                double predRE = (mInstance.alphaT * mInstance.total_tris) + (mInstance.alphaD * pred_meanD) + (mInstance.alphaH * predThr) + mInstance.zeta; // for almost current period
                double re_mape = Math.abs(reMsrd - predRE) / reMsrd;// log this
                if (re_mape > 0.1)
                    accRe = false;// after training we check to see if the model is accurate to then cal next triangle

                writeRE(reMsrd, predRE, trainedRE, mInstance.total_tris, nextTris, algNxtTris, trainedTris, PRoAR, PRoAI, (accRe), mInstance.orgTrisAllobj, avgq, mInstance.t_loop1);// writes to the file
                writequality();


                mInstance.pred_meanD_cur = meanDkk; // the predicted current_mean_distance for next period is saved here,
                // so we'll have the estimated current distance in next period

                     //  RE modeling and next tris




// heap clean-> memory efficiency
                if((variousTris % 12==0) && variousTris>2&& mInstance.cleanedbin==false )// every 5x times we check to clear the bins provided that the model is accurate
                {
                    mInstance.reParamList.clear();
                    mInstance.trisMeanDisk.clear();
                    mInstance.trisMeanThr.clear();
                    mInstance.thParamList_Mir.clear();
                    mInstance.trisReMir.clear();
                    mInstance.reParamList.clear();
                    mInstance.decTris.clear();
                    //mInstance.cleanedbin= true;
                    mInstance.basethr_tag=true;
                }






    }//run


    public void cleanOutArraysRE(double totTris, double predDis, MainActivity mInstance){

        int index;
        if ( mInstance.trisMeanDisk.get(totTris).size() == binCap)
        { // we keep inf of last 10 points
            double []disArray= mInstance.trisMeanDisk.get(totTris).stream()
                    .mapToDouble(Double::doubleValue)
                    .toArray();
            index= findClosest(disArray , predDis);// the index of value needed to be deleted
            // since we add if objcount != zero to avoid wrong inf from tris=0 -> we won't have re or distance at this situation
           if(index<mInstance.trisReMir.get(totTris).size() &&  index<mInstance.reParamList.get(totTris).size())
            {
                mInstance.trisReMir.get(totTris).remove(index);
            mInstance.reParamList.get(totTris).remove(index);
            }
        }

        // return index;
    }




    public int cleanOutArraysThr(double totTris, double predDis, MainActivity mInstance){

        int index=-1;
        while( mInstance.trisMeanDisk.get(totTris).size()>binCap) // to make sure if it doesn't have extra data
            mInstance.trisMeanDisk.get(totTris).remove(0);

        double[] disArray = mInstance.trisMeanDisk.get(totTris).stream()
                .mapToDouble(Double::doubleValue)
                .toArray(); // this has the array of predicted distance

            index = findClosest(disArray, predDis);// the index of value(closest to current mean dis) needed to be deleted
            mInstance.trisMeanThr.get(totTris).remove(index); // has the real throughput
            mInstance.thParamList_Mir.get(totTris).remove(index);
            mInstance.trisMeanDisk.get(totTris).remove(index); //removes from the head (older data) -> to then add to the tail
            // mInstance.trisMeanDiskk.get(totTris).remove(index);


        return  index;



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


               double r2 = ref_ratio * r1; // wanna compare obj level of sensitivity to see if we decimate object more -> to (ref *curr) ratio, would the current object hurt more than the other ones?
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
               //sensitivity[i] = (abs(tmper2 - tmper1) / (curtris - (ref_ratio * curtris)));
               tmper1 = (float) (Math.round((float) (tmper1 * 1000))) / 1000;

                StringBuilder sb = new StringBuilder();
                sb.append(dateFormat.format(new Date()));
                sb.append(',');
                sb.append(mInstance.renderArray.get(i).fileName+"_n"+(i+1)+"_d"+(d_k));
                sb.append(',');
                //sb.append(sensitivity[i]);
                sb.append(',');
                sb.append(r1);
                sb.append(',');
                sb.append(1-tmper1);
              //  sb.append(mInstance.tasks.toString());

                sb.append('\n');
                writer.write(sb.toString());
//                System.out.println("done!");
            }
        }catch (FileNotFoundException e) {
//            System.out.println(e.getMessage());
        }

    }



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
//            System.out.println("done!");
        } catch (FileNotFoundException e) {
//            System.out.println(e.getMessage());
        }
    }



//    public void writeThr(double realThr, double predThr, boolean trainedFlag){
//
//        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss:SSS");
//        String currentFolder = mInstance.getExternalFilesDir(null).getAbsolutePath();
//        String FILEPATH = currentFolder + File.separator + "Throughput"+mInstance. fileseries+".csv";
//
////        StringBuilder sbTaskSave = new StringBuilder();
////        for (AiItemsViewModel taskView : mInstance.mList) {
////            sbTaskSave
////                    //.append(taskView.getCurrentNumThreads())
////                    .append(",")
////                    .append(taskView.getModels().get(taskView.getCurrentModel()));
////                    //.append("-");
////            // .append(taskView.getDevices().get(taskView.getCurrentDevice()))
////                    //.append("\n");
////        }
//
//
//
//        try (PrintWriter writer = new PrintWriter(new FileOutputStream(FILEPATH, true))) {
//            StringBuilder sb = new StringBuilder();
//            sb.append(dateFormat.format(new Date())); sb.append(',');
//            sb.append(realThr);sb.append(',');
//            sb.append(predThr);
//            sb.append(',');  sb.append(trainedFlag);
//            sb.append(','); sb.append(mInstance.total_tris);
//            sb.append(mInstance.tasks.toString());
//            sb.append(','); sb.append(mInstance.des_Thr);
//            sb.append(','); sb.append(mInstance.des_Q);
//            sb.append('\n');
//            writer.write(sb.toString());
//            System.out.println("done!");
//        } catch (FileNotFoundException e) {
//            System.out.println(e.getMessage());
//        }
//    }
/*
    public void writeNextTris(double alphaD, double alphaH,double rohD, double meanDkk,double zeta,double delta,
                              double  alphaT,double rohT,double nomin,double denom, double totTris, double nextTris){

        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss:SSS");
        String currentFolder = mInstance.getExternalFilesDir(null).getAbsolutePath();
        String FILEPATH = currentFolder + File.separator + "NextTrisParameters.csv";

        try (PrintWriter writer = new PrintWriter(new FileOutputStream(FILEPATH, true))) {
            StringBuilder sb = new StringBuilder();
            sb.append(dateFormat.format(new Date())); sb.append(',');
            sb.append(alphaD);sb.append(',');
            sb.append(alphaH);
            sb.append(',');  sb.append(rohD);
            sb.append(',');  sb.append(meanDkk);
            sb.append(',');  sb.append(zeta);
            sb.append(',');  sb.append(delta);
            sb.append(',');  sb.append(alphaT);
            sb.append(',');  sb.append(rohT);
            sb.append(',');  sb.append(nomin);
            sb.append(',');  sb.append(denom);
            sb.append(',');  sb.append(totTris);
            sb.append(',');  sb.append(nextTris);
            sb.append('\n');
            writer.write(sb.toString());
            System.out.println("done!");
        } catch (FileNotFoundException e) {
            System.out.println(e.getMessage());
        }
    }
*/

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
            sb.append(',');  sb.append(mInstance.algName[mInstance.alg-1]);
            sb.append('\n');
            writer.write(sb.toString());
//            System.out.println("done!");
        } catch (FileNotFoundException e) {
//            System.out.println(e.getMessage());
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
            double curQ = mInstance.ratioArray.get(ind);

//            if (mInstance.renderArray.get(ind).fileName.contains("0.6")) // third scenario has ratio 0.6
//                curQ = 0.6f; // jsut for scenario3 objects are decimated
//            else if(mInstance.renderArray.get(ind).fileName.contains("0.3")) // sixth scenario has ratio 0.3
//                curQ=0.3f;

            float deg_error = (float) (Math.round((float) (Calculate_deg_er(a, b, c, d, gamma, curQ) * 1000))) / 1000;
            float max_nrmd = mInstance.excel_maxd.get(i);

            float cur_degerror = deg_error / max_nrmd;
            float quality= 1- cur_degerror;
            objquality[ind]=quality;
            sumQual+=quality;


        }


        return sumQual/mInstance.objectCount;



    }

    public float Calculate_deg_er(float a,float b,float creal,float d,float gamma, double r1) {

        float error;
        if(r1==1)
            return  0f;
        error = (float) (((a * Math.pow(r1,2)) + (b * r1) + creal) / (Math.pow(d , gamma)));
        return error;
    }



/*
    float odraAlg(double tUP) throws InterruptedException {





*/


    float odraAlg(float tUP) throws InterruptedException {


        objC=mInstance.objectCount;
        double []tMin = new double[objC];
        double [] sensitivity = new double[objC];
        double [] tris_share = new double[objC];
        fProfit= new double[objC][coarse_Ratios.length];
        tRemainder= new double[objC][coarse_Ratios.length];
        track_obj= new int[objC][coarse_Ratios.length];

        candidate_obj = new HashMap<>();
        Map<Integer, Double> sortedcandidate_obj = new HashMap<>();
        float sum_org_tris = 0; // sum of all tris of the objects o the screen



        for (int ind = 0; ind < mInstance.objectCount; ind++) {

            sum_org_tris += mInstance.renderArray.get(ind).orig_tris;// this will ne used to cal min of tris needed at each row (object) in bellow
            float r1 = mInstance.ratioArray.get(ind); // current object decimation ratio
            float curtris =(float) mInstance.renderArray.get(ind).orig_tris * r1;
            float r2 = ref_ratio * r1; // wanna compare obj level of sensitivity to see if we decimate object more -> to (ref *curr) ratio, would the current object hurt more than the other ones?
            int indq = mInstance.excelname.indexOf(mInstance.renderArray.get(ind).fileName);// search in excel file to find the name of current object and get access to the index of current object
            // excel file has all information for the degredation model
            float gamma = mInstance.excel_gamma.get(indq);
            float a = mInstance.excel_alpha.get(indq);
            float b = mInstance.excel_betta.get(indq);
            float c = mInstance.excel_c.get(indq);
            float d_k = mInstance.renderArray.get(ind).return_distance();// current distance
            float tmper1 = Calculate_deg_er(a, b, c, d_k, gamma, r1); // deg error for current sit
            float tmper2 = Calculate_deg_er(a, b, c, d_k, gamma, r2); // deg error for more decimated obj

            float max_nrmd = mInstance.excel_maxd.get(indq);
            tmper1 = tmper1 / max_nrmd; // normalized
            tmper2= tmper2 /max_nrmd;

            if (tmper2 < 0)
                tmper2 = 0;
            //Qi−Qi,r divided by Ti(1−Rr) = (1-er1) - (1-er2) / ....
            sensitivity[ind] = (abs(tmper2 - tmper1) / (curtris - (ref_ratio * curtris)));
            tris_share[ind] = (curtris / tUP);
            candidate_obj.put(ind,  (sensitivity[ind] / tris_share[ind]));

        }

//        for (int ind = 0; ind < mInstance.objectCount; ind++) {
//
//            sum_org_tris += mInstance.renderArray.get(ind).orig_tris;// this will ne used to cal min of tris needed at each row (object) in bellow
//            double curtris = mInstance.renderArray.get(ind).orig_tris * mInstance.ratioArray.get(ind);

//          /* I calculate this in quality function
//           float r1 = mInstance.ratioArray.get(ind); // current object decimation ratio
//            float r2 = ref_ratio * r1; // wanna compare obj level of sensitivity to see if we decimate object more -> to (ref *curr) ratio, would the current object hurt more than the other ones?
//
//            int indq = mInstance.excelname.indexOf(mInstance.renderArray.get(ind).fileName);// search in excel file to find the name of current object and get access to the index of current object
//            // excel file has all information for the degredation model
//            float gamma = mInstance.excel_gamma.get(indq);
//            float a = mInstance.excel_alpha.get(indq);
//            float b = mInstance.excel_betta.get(indq);
//            float c = mInstance.excel_c.get(indq);
//            float d_k = mInstance.renderArray.get(ind).return_distance();// current distance
//
//            float tmper1 = Calculate_deg_er(a, b, c, d_k, gamma, r1); // deg error for current sit
//            float tmper2 = Calculate_deg_er(a, b, c, d_k, gamma, r2); // deg error for more decimated obj
//
//            if (tmper2 < 0)
//                tmper2 = 0;
//
//            //Qi−Qi,r divided by Ti(1−Rr) = (1-er1) - (1-er2) / ....
//            sensitivity[ind] = (abs(tmper2 - tmper1) / (curtris - (ref_ratio * curtris)));*/
//            tris_share[ind] = (curtris / tUP);
//            candidate_obj.put(ind,  (sensitivity[ind] / tris_share[ind]));
//        }
//


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
                double quality = 1 -( Calculate_deg_er(a, b, c, d_k, gamma, coarse_Ratios[j]) / max_nrmd  ); // deg error for current sit

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



        //restart tot_tris:
        mInstance.total_tris =0;
        for (int i : sortedcandidate_obj.keySet())
        {// to avoid null pointer error
            // mInstance.total_tris = mInstance.total_tris - (mInstance.ratioArray.get(i) * (mInstance.o_tris.get(i)));// total =total -1*objtris

            mInstance.ratioArray.set(i, coarse_Ratios[j]);

            // $$$$$$$$$$$$$$$$$$$ temporary to show the OBJECT type to check the best Scenario
//
//             if(i%5==1)
//                posText2= mInstance.findViewById(R.id.rspT1);
//            else if(i%5==2)
//                posText2= mInstance.findViewById(R.id.rspT2);
//             else if(i%5==3)
//                posText2= mInstance.findViewById(R.id.rspT3);
//             else if(i%5==4)
//                posText2= mInstance.findViewById(R.id.rspT4);
////TextView posText2= mInstance.findViewById(R.id.app_thr);
//             posText2.setText(String.valueOf( mInstance.renderArray.get(i).fileName)+ mInstance.ratioArray.get(i));
            // $$$$$$$$$$$$$$$$$$$ temporary to show the OBJECT type to check the best Scenario


            mInstance.runOnUiThread(() -> mInstance.renderArray.get(i).decimatedModelRequest(mInstance.ratioArray.get(i), i, false));
            mInstance.total_tris = mInstance.total_tris + (mInstance.ratioArray.get(i) *  mInstance.renderArray.get(i).orig_tris);// total = total + 0.8*objtris
            // mInstance.trisDec.put(mInstance.total_tris,true);
            j = track_obj[i][j];
            Thread.sleep(sleepTime);// added to prevent the crash happens while redrawing all the objects at the same time
        }
        return (float)mInstance.total_tris; // this returns the total algorithm triangle count

    }




//    private static Map<Integer, Float> sortByValue(Map<Integer, Float> unsortMap, final boolean order)
//    {
//        List<Map.Entry<Integer, Float>> list = new LinkedList<>(unsortMap.entrySet());
//
//        // Sorting the list based on values
//        list.sort((o1, o2) -> order ? o1.getValue().compareTo(o2.getValue()) == 0
//                ? o1.getKey().compareTo(o2.getKey())
//                : o1.getValue().compareTo(o2.getValue()) : o2.getValue().compareTo(o1.getValue()) == 0
//                ? o2.getKey().compareTo(o1.getKey())
//                : o2.getValue().compareTo(o1.getValue()));
//        return list.stream().collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue, (a, b) -> b, LinkedHashMap::new));
//
//    }
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



