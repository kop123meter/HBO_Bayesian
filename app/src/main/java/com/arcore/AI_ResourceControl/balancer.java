// temp deactivate
//package com.arcore.AI_ResourceControl;
///*This code has relation ship between AI throughput and distance and triangle count*/
//import java.io.File;
//import java.io.FileNotFoundException;
//import java.io.FileOutputStream;
//import java.io.PrintWriter;
//import java.text.SimpleDateFormat;
//import java.util.Arrays;
//import java.util.Date;
//import java.util.HashMap;
//import java.util.LinkedHashMap;
//import java.util.LinkedList;
//import java.util.List;
//import java.util.Map;
//import java.util.stream.Collectors;
//
//import static java.lang.Math.abs;
//
//import com.google.common.collect.ArrayListMultimap;
//import com.google.common.collect.ListMultimap;
//
//public class balancer implements Runnable {
//
//    int binCap=5;
//    private final MainActivity mInstance;
//    float ref_ratio=0.5f;
//    int objC;
//    float sensitivity[] ;
//    float objquality[];
//    float tris_share[];
//    Map <Integer, Float> candidate_obj;
//    float []coarse_Ratios=new float[]{1f,0.8f, 0.6f , 0.4f, 0.2f, 0.05f};
//    //ArrayList <ArrayList<Float>> F_profit= new ArrayList<>();
//    float [][]fProfit;
//    float [][] tRemainder;
//    int [][] track_obj;
//    int sleepTime=60;
//    //float candidate_obj[] = new float[total_obj];
//    float tMin[] ;
//    int aiIndx;
//
//    public balancer(MainActivity mInstance,int ai_index) {
//
//        this.mInstance = mInstance;
//
//        objC=mInstance.objectCount+1;
//        sensitivity = new float[objC];
//        tris_share = new float[objC];
//        objquality= new float[objC];// 1- degradation-error
//        aiIndx=ai_index;
//                //model_index;
//
//        //ArrayList <ArrayList<Float>> F_profit= new ArrayList<>();
//        fProfit= new float[objC][coarse_Ratios.length];
//        tRemainder= new float[objC][coarse_Ratios.length];
//        track_obj= new int[objC][coarse_Ratios.length];
//        //float candidate_obj[] = new float[total_obj];
//        tMin = new float[objC];
//
//
//    }
//
//    @Override
//    public void run() {
//
//        boolean accmodel = true;// this is to check if the trained model for thr is accurate
//        //boolean accRe=true;// this is to check if the trained model for re is accurate
//        boolean trainedTris = false;
//        boolean trainedThr = false;
//        boolean trainedRE = false;
//        double maxTrisThreshold = mInstance.orgTrisAllobj;
//        double minTrisThreshold = maxTrisThreshold * mInstance.coarse_Ratios[mInstance.coarse_Ratios.length - 1];
//        double meanThr, totTris;
//        double meanDk = 0; // mean current dis
//        double meanDkk = 0; // mean of d in the next period-> you need to cal the average of predicted d for all the objects
//        double pred_meanD = mInstance.pred_meanD_cur; // for next 1 sc
//
//
//
//
//
//
//      for (int i = 0; i < mInstance.objectCount; i++) {
//            double cur_dis=(double) mInstance.renderArray.get(i).return_distance();
//               meanDk += cur_dis;
//                meanDkk += mInstance.predicted_distances.get(i).get(1); // 0: next 1 sec, 1: next 2s :  // gets the first time, next 2s-- 3s of every object, ie. d1 of every obj
//
//        }
//
//        int objc= mInstance.objectCount;
//        if(objc>0) {
//            meanDk /= objc;
//            meanDkk /= objc;
//
//            meanDk = (double) (Math.round((double) (100 * meanDk))) / 100;
//            meanDkk = (double) (Math.round((double) (100 * meanDkk))) / 100;
//
//
//            if (meanDkk == 0)
//                meanDkk = meanDk;
//            if (pred_meanD == 0)
//                pred_meanD = meanDk; // for the first objects
//
//        }
//
//
//
//        //I got an error for regression since decimation occurs in UI thread and Modeling runs at the same time
//        // solution is to start data collection after one period passes from algorithm
// // else{ // just collect data when algorithm was applied in the last period
//
//        totTris = mInstance.total_tris;
//
//
//        meanThr =mInstance.getThroughput(aiIndx);// after the objects are decimated
//
//
//
//
//        if (meanThr < 100 && meanThr > 1) {
//
//            meanThr = (double) Math.round(meanThr * 100) / 100;
//
//            int variousTris = mInstance.thr_models.get(aiIndx).keySet().size();
//
//// this is thr calculated using the modeling
//            double predThr = (mInstance.rohTL.get(aiIndx) *  totTris) + (mInstance.rohDL.get(aiIndx) * pred_meanD) + mInstance.deltaL.get(aiIndx);// use predicted distance for almost current period (predicted distance for next 1 sec is the closest one we have)
//            predThr = (double) Math.round((double) predThr * 100) / 100;
//
////            writeThr(meanThr, predThr, trainedThr);// for the urrent period
//
//            int ind = -1;
//            if (variousTris < 3) { // one object on the screen
//
//                if (mInstance.thr_models.get(aiIndx).get(totTris).size() == binCap) { // we keep inf of last 10 points
//                     cleanOutArraysThr(totTris, pred_meanD, mInstance);// cleans out the closest data to the curr one
//
//                }
//
//                mInstance.thr_models.get(aiIndx).put(totTris, meanThr); //correct:  should have real throughput for the regression
//                // uses predicted cur distance for thr modeling
//                mInstance.thParamList.get(aiIndx).put(totTris, Arrays.asList(totTris, pred_meanD, 1.0));
//
//                mInstance.trisMeanDisk.get(aiIndx).put(totTris, pred_meanD); //correct:  should have predicted value removes from the head (older data) -> to then add to the tail
//
//
//            }
//
//
//            if (mInstance.objectCount != 0) {//to avoid wrong inf from tris=0 -> we won't have re or distance at this situation
//
//                 // starting throughput model
//                if (variousTris >= 2) {// at least two points to start modeling
//
//// checks error of the model after new added model
//                    double mape = 0.0; // mean of absolute error
//                    double fit = (mInstance.rohTL.get(aiIndx) * totTris) + (mInstance.rohDL.get(aiIndx) * pred_meanD )+ mInstance.deltaL.get(aiIndx); // fit is predicted current throughput should be predicted distance (we have next 1 sec but ok since we don't move)  for current period
//                    //double fit = mInstance.thSlope * totTris + mInstance.thIntercept;;
//                    mape = abs((meanThr - fit) / meanThr);// this is correct to calculate error coming from real throughput vs model
//
//                    if (mInstance.thr_models.get(aiIndx).get(totTris).size() == binCap) { // we keep inf of last 10 points
//                         cleanOutArraysThr(totTris, pred_meanD, mInstance);// cleans out the closest data to the curr one
//                    }
//
//                 ///  nil   commented  sep 7
//                    if (mape > 0.1 && variousTris >= 2) {// we ignore tris=0 them we need points with at least two diff tris in order to generate the line
//// 8 april
//                        mInstance.thr_models.get(aiIndx).put(totTris, meanThr); // corrrect: should have real throughput for regression and thr param should have predicted distance
//                        mInstance.thParamList.get(aiIndx).put(totTris, Arrays.asList(totTris, pred_meanD, 1.0));
//                        mInstance.trisMeanDisk.get(aiIndx).put(totTris, pred_meanD); //correct: should have predicted value dist => removes from the head (older data) -> to then add to the tail
//
//                        ListMultimap<Double, List<Double>> copythParamList = ArrayListMultimap.create(mInstance.thParamList.get(aiIndx));// take a copy to then fill it for training up to capacity of 10
//                        ListMultimap<Double, Double> copytrisMeanThr = ArrayListMultimap.create(mInstance.thr_models.get(aiIndx));// take a copy to then fill it for training up to capacity of 10
//
//
//                        for (double curT : mInstance.thr_models.get(aiIndx).keySet()) {
//
//                          //   this is to calculate the mean of values in the bins
//                            if (mInstance.trisMeanDisk.get(aiIndx).get(curT).size() < binCap) {
//                                int index = mInstance.trisMeanDisk.get(aiIndx).get(curT).size();
//                                double mmeanTh = 0, mmeanDK = 0;
//
//                                for (int i = 0; i < index; i++) {
//                                    mmeanTh += mInstance.thr_models.get(aiIndx).get(curT).get(i);
//                                    mmeanDK += mInstance.trisMeanDisk.get(aiIndx).get(curT).get(i);
//
//                                }
//
//                                mmeanTh /= index;
//                                mmeanDK /= index;
//                                //   mmeanDKk/=index;
//
//                                for (int j = index; j < binCap; j++) {
//
//                                    copytrisMeanThr.put(curT, mmeanTh);
//                                    copythParamList.put(curT, Arrays.asList(curT, mmeanDK, 1.0)); // this is to calculate the mean of values in the bins so it's correct
//
//                                }
//                            }// if <10
//                        }
//
//
//        //     @@@@ here we check if we have any record for the
//        //  decimated objects, if yes, we need to check the ratio of decimated iteration over added scenarios, we define a rate of 40% to make sure that we have
//           //at least the record for decimated object equal to 40% of the added scenarios
//
//                        int decCount = mInstance.decTris.size();// #iterations of decimated objects
//                        if (decCount != 0) // means that we have at least one record of decimated objects
//                        {
//                            int j = 0;
//                            int upCount = (int) Math.ceil(mInstance.thr_factor * mInstance.objectCount);// we had addition up to the object count
//                            for (int i = decCount; i < upCount; i++) {
//                                if (j > mInstance.decTris.size() - 1)
//                                    j = 0;
//                                double currentT = mInstance.decTris.get(j);// one of the triangles from the list of decimated-exp
//                                List<Double> thrList = new LinkedList<>(copytrisMeanThr.get(currentT));// since copy list has full data
//                                for (double th : thrList)// elements of re list
//                                    copytrisMeanThr.put(currentT, th);
//
//                                List<List<Double>> thpList = new LinkedList<>(copythParamList.get(currentT));// since copy list has full data
//                                for (List<Double> thpr : thpList)// throughput parameters
//                                    copythParamList.put(currentT, thpr);
//                                j += 1;
//
//                            }
//
//                        }
//                        // to copy the decimated data into throughout modeling
//
//
//                        double[] throughput = copytrisMeanThr.values().stream()
//                                .mapToDouble(Double::doubleValue)
//                                .toArray();
//
//                        double[] y = Arrays.copyOfRange(throughput, 0, throughput.length); // should be real throughput
//                        double[][] thRegParameters = copythParamList.values().stream()
//                                .map(l -> l.stream().mapToDouble(Double::doubleValue).toArray())
//                                .toArray(double[][]::new);
//
//                        // should have predicted distance
//
//                        mLinearRegression regression = new mLinearRegression(thRegParameters, y);
//                        if (!Double.isNaN(regression.beta(0))) {
//
//                            mInstance.rohTL.set(aiIndx,regression.beta(0))  ;
//                            mInstance.rohDL.set(aiIndx,regression.beta(1));
//                            mInstance.deltaL .set(aiIndx, regression.beta(2));
//                            //mInstance.thRmse = regression.rmse;
//                            trainedThr = true;
//
//                        }
//                        thRegParameters=null; // free the storage
//                        y=null;
//                        copytrisMeanThr.clear();
//                        copythParamList.clear();
//
//                    // end of modeling throughput
//
//                    }// end of i tris>2
//
//
////                  get the right throughput after remodeling
//                    predThr = (mInstance.rohTL.get(aiIndx) * totTris )+ (mInstance.rohDL.get(aiIndx) * pred_meanD) + mInstance.deltaL.get(aiIndx);// use predicted distance for almost current period (predicted distance for next 1 sec is the closest one we have)
//                    predThr = (double) Math.round((double) predThr * 100) / 100;
//
//
//
//
//
//
//                    mape = abs((meanThr - predThr) / meanThr);
//                    if (mape > 0.1) {
//                        accmodel = false;// after training we check to see if the model is accurate to then cal next triangle
//
//                        if (variousTris >= 3){
//
//                            int cur_count= mInstance.thr_miss_counter.get(aiIndx);
//                            mInstance.thr_miss_counter.set(aiIndx,cur_count+1 ) ;
//                        }
//
//
//                        if (mInstance.thr_miss_counter.get(aiIndx) > 5 && mInstance.decTris.size() != 0) { // this is to regulate the factor of considering decimated collected data in the bins
//                            if (meanThr > predThr) {
//                                mInstance.thr_factor -= 0.1;// reduce the effect of decimation to increase predicted throughput
//                                if (mInstance.thr_factor < 0)
//                                    mInstance.thr_factor = 0;
//                            } else
//                                mInstance.thr_factor += 0.1;// increase the effect of decimation to decrease the calculated pred_re
//
//                        }
//
//                    } else
//                        mInstance.thr_miss_counter.set(aiIndx,  0);// the model works fine so we don't need to re-adjust the throughput factor for decimation data collection
//
//
//
//
//                } //  throughput model
//
//
//                //******************  RE modeling *************
//
//
//              //  writequality();
//
//                writeThr(meanThr, predThr, trainedThr);// for the urrent period
///*  double avgq = calculateMeanQuality();
//                double PRoAR = (double) Math.round((avgq / mInstance.des_Q) * 100) / 100;
//                double PRoAI = (double) Math.round((meanThr / mInstance.des_Thr) * 100) / 100;// should be real
//                double reMsrd = PRoAR / PRoAI;
//                reMsrd = (double) Math.round(reMsrd * 100) / 100;
//
//                int reModSize = mInstance.trisRe.size(); // has real mean-throughput
//
//                if (reModSize < 4)// april 8{
//                {
//
//                    cleanOutArraysRE(totTris, pred_meanD, mInstance);// check to remove extra value in the RE parameters list , substitue the newer one
//                    mInstance.trisRe.put(totTris, reMsrd);//correct:  has real re should have real throughout
//                    // use predicted current dis and predicted current throughput in the modeling
//                    mInstance.reParamList.put(totTris, Arrays.asList(totTris, pred_meanD, predThr, 1.0)); // the two pred are coming from prev period (that were predicted for current period)
//                }
//
//
//                if (reModSize >= 4) { // ignore first 10 point we need to have four known variables to solve an equation with three unknown var
////@@ niloo please add test the trained data and check rmse, if it is above 20% , then retrain
//
//                    double mape = 0.0;      //  sum of square error
//                    double fit =( mInstance.alphaT *totTris) + (mInstance.alphaD * pred_meanD) + (mInstance.alphaH * predThr )+ mInstance.zeta;// uses predicted modeling  for current period
//                    mape = Math.abs((reMsrd - fit) / reMsrd);
//
//
//                  //  /*nill commented  sep 7
//
//                    if (mape > 0.10 && variousTris >= 2) {// we ignore tris=0 them we need points with at least two diff tris in order to generate the line
//
//                        cleanOutArraysRE(totTris, pred_meanD, mInstance);
//                        mInstance.trisRe.put(totTris, reMsrd); // april 8
//                        mInstance.reParamList.put(totTris, Arrays.asList(totTris, pred_meanD, predThr, 1.0));
//
//                        ListMultimap<Double, List<Double>> copyreParamList = ArrayListMultimap.create(mInstance.reParamList);// take a copy to then fill it for training up to capacity of 10
//                        ListMultimap<Double, Double> copytrisRe = ArrayListMultimap.create(mInstance.trisRe);// take a copy to then fill it for training up to capacity of 10
//
//
//                       // This is to calculate mean of bins for distance, throughput ,...
//                        for (double curT : mInstance.trisRe.keySet()) {
//
//                            if (curT != 0 && mInstance.trisRe.get(curT).size() < binCap) {
//                                int index = mInstance.trisRe.get(curT).size();
//                                double meanRE = 0, mmeanDK = 0, meanPrth = 0;
//
//                                for (int i = 0; i < index; i++) {
//                                    meanRE += mInstance.trisRe.get(curT).get(i);
//                                    List<Double> reParL = mInstance.reParamList.get(curT).get(i);
//                                    mmeanDK += reParL.get(1);
//                                    meanPrth += reParL.get(2);
//                                }
//
//                                meanRE /= index;
//                                mmeanDK /= index;
//                                meanPrth /= index;
//
//                                for (int j = index; j < binCap; j++) {
//                                    copytrisRe.put(curT, meanRE);
//                                    copyreParamList.put(curT, Arrays.asList(curT, mmeanDK, meanPrth, 1.0)); // this is correct since it's mean of bins and they include predicted values
//
//                                }
//                            }// if <10
//                        }// for all the current data
//
//
//
//
//                        // for REEE
//
//
//             //             @@@@ here we check if we have any record for the
//        //  decimated objects, if yes, we need to check the ratio of decimated iteration over added scenarios, we define a rate of 40% to make sure that we have
//        //   at least the record for decimated object equal to 40% of the added scenarios
//                        // int added=mInstance.trisDec.size();
//
//                        //  add the decimated data for throughput modeling too
//
//                        int decCount = mInstance.decTris.size();// #iterations of decimated objects
//                        if (decCount != 0) // means that we have at least one record of decimated objects
//                        {
//                            int j = 0;
//                            //int upCount= (int) Math.ceil(0.5 * mInstance.objectCount);
//
//
//                            int upCount = (int) Math.ceil(mInstance.re_factor * mInstance.objectCount);// we had addition up to the object count
//
//                            for (int i = decCount; i <= upCount; i++) {
//                                if (j > mInstance.decTris.size() - 1)
//                                    j = 0;
//                                double currentT = mInstance.decTris.get(j);// one of the triangles from the list of decimated-exp
//                                List<Double> reList = new LinkedList<>(copytrisRe.get(currentT));// since copy list has full data
//                                for (double re : reList)// elements of re list
//                                    copytrisRe.put(currentT, re);
//
//                                List<List<Double>> repList = new LinkedList<>(copyreParamList.get(currentT));// since copy list has full data
//                                for (List<Double> repr : repList)
//                                    copyreParamList.put(currentT, repr);
//                                j += 1;
//
//                            }
//
//                        }
//                        // to copy the decimated data into re modeling
//
//
//                        double[] RE = copytrisRe.values().stream()
//                                .mapToDouble(Double::doubleValue)
//                                .toArray();
//
//
//
//                        double[][] reRegParameters = copyreParamList.values().stream()
//                                .map(l -> l.stream().mapToDouble(Double::doubleValue).toArray())
//                                .toArray(double[][]::new);
//
//
//
//
//                        if (variousTris >= 3) {
//                            mLinearRegression regression = new mLinearRegression(reRegParameters, RE);
//                            if (!Double.isNaN(regression.beta(0))) {
//                                mInstance.alphaT = regression.beta(0);
//                                mInstance.alphaD = regression.beta(1);
//                                mInstance.alphaH = regression.beta(2);
//                                mInstance.zeta = regression.beta(3);
//                                trainedRE = true;
//
//                            }
//
//
//                        }
//
//                        reRegParameters=null;
//                        RE=null;
//
//                        copytrisRe.clear();
//                        copyreParamList.clear();
//
//                    }
//
//           //   nil   commented  sep 7
//
//                    // current period
//                    double predRE = mInstance.alphaT *totTris + mInstance.alphaD * pred_meanD + mInstance.alphaH * predThr + mInstance.zeta; // for almost current period
//
//
//// nill added temp
//
//
//                    mape = Math.abs((reMsrd - predRE) / reMsrd);// log this
//                    if (mape > 0.1) {
//                        accmodel = false;// after training we check to see if the model is accurate to then cal next triangle
//                        if (variousTris >= 3) // this is to regulate throughput factor for decimated values
//                            mInstance.re_miss_counter += 1;
//
//                        if (mInstance.re_miss_counter > 5 && mInstance.decTris.size() != 0) {
//                            if (reMsrd > predRE) {
//                                mInstance.re_factor -= 0.1;// reduce the effect of decimation to increase re
//                                if (mInstance.re_factor < 0)
//                                    mInstance.re_factor = 0;
//
//                            } else
//                                mInstance.re_factor += 0.1;// increase the effect of decimation to decrease the calculated pred_re
//
//                        }
//
//
//                    } else
//                        mInstance.re_miss_counter = 0; // the model works fine so we don't need to re-adjust the throughput factor for decimation data collection
//
//
//                    predRE = (double) Math.round((double) predRE * 100) / 100;
//
//                    //  if (variousTris>=3 && Math.abs(deltaRe) >= 0.2 && (PRoAR < 0.7 || PRoAI < 0.7))// test and see what is the re range
//                    double nextTris = totTris;
//                    double algNxtTris = totTris;
//
//                    if (variousTris >= 3 && (reMsrd >= 1.20 || (reMsrd <= 0.8 && avgq != 1)))// if re is not balances (or pAR is not close to PAI, we will change the next tris count
//                        // the last cond (reMsrd <0.8 && avgq!=1) says that if the AI is working better than AR and AI has not in original quality so that we can increase tot tris
//                        mInstance.lastConscCounter++;
//
//                    else
//                        mInstance.lastConscCounter = 0;
//
//
//                    long time1 = 0;
//                    long time2 = 0;
//                    if (accmodel && mInstance.lastConscCounter > 4) // the second condition is to skip change in nexttris for the first loop while we just had a change in tot tris
//                    {
//
//                        time1 = System.nanoTime() / 1000000; //starting first loop
//                        time2 = time1;
//                        double nomin = 1 - ((mInstance.alphaD + (mInstance.alphaH * mInstance.rohD)) * meanDkk)
//                                - (mInstance.zeta + (mInstance.alphaH * mInstance.delta));
//                        // double nomin2= 1- (mInstance.alphaD* meanDkk) -(mInstance.alphaH *)
//                        double denom = mInstance.alphaT + (mInstance.rohT * mInstance.alphaH); //α + ργ
//                        double tmpnextTris = (nomin / denom);
//
//                        if (tmpnextTris > 0) {
//
//                            // temporarily inactive to not to run algo-> just wanna check nexttris values
//                            if (tmpnextTris < mInstance.orgTrisAllobj && tmpnextTris >= minTrisThreshold)
//                                nextTris = tmpnextTris;
//
//                            else if (tmpnextTris < minTrisThreshold)
//                                nextTris = minTrisThreshold + 1000;
//
//                            else if (tmpnextTris > mInstance.orgTrisAllobj)
//                                nextTris = mInstance.orgTrisAllobj;
//
//
//
//                            nextTris =  Math.round(nextTris * 100) / 100;
//                            trainedTris = true;
//
//                                try {
//
//
//
//                                    odraAlg((float) nextTris);
//                                    time2= System.nanoTime()/1000000;
//                                   // long t2 = System.nanoTime() / 1000000;
//                                    mInstance.t_loop1 = time2 - time1 - (sleepTime * (objC-1) );
//                                   // mInstance.t_loop2 = t2 - t1;
//                                    mInstance.lastConscCounter = 0;// we let the effect of change in triangle count stand for at least 4 times by reseting this counter. if you don't reset, by any chance new re might be <08 and then the orda happens again
//
//                                    if (nextTris != totTris && !mInstance.decTris.contains(mInstance.total_tris) ) // if next tris is lower than total tris we have decimation
//                                        mInstance.decTris.add(mInstance.total_tris);// add new total triangle count in the decimated list
//
//
//                                } catch (InterruptedException e) {
//                                    e.printStackTrace();
//                                }
//
//
//                                algNxtTris = mInstance.total_tris;
//
//                        }
//
//
//                    }//if
//                    writeRE(reMsrd, predRE, trainedRE, totTris, nextTris,algNxtTris, trainedTris, PRoAR, PRoAI, accmodel, mInstance.orgTrisAllobj, avgq, mInstance.t_loop1);// writes to the file
//
//
//
//
//
//                }   */         //  RE modeling and next tris
//
//
//                if( mInstance.trisChanged==true)
//                {  mInstance.cleanedbin=false;
//                    mInstance.trisChanged=false;
//
//                }
//
//
//// heap clean-> memory efficiency
//                if((variousTris % 8==0) && variousTris>2&& mInstance.cleanedbin==false )// every 5x times we check to clear the bins provided that the model is accurate
//                {
//                    mInstance.reParamList.clear();
//                    mInstance.trisMeanDisk.clear();
//                    mInstance.thr_models.get(aiIndx).clear();
//                    mInstance.thParamList.clear();
//                    mInstance.trisRe.clear();
//                    mInstance.reParamList.clear();
//                    // to start over data collection
//
//                    mInstance.decTris.clear();
//                    mInstance.cleanedbin= true;
//                }
//
//
//            }// if we have objs on the screen, we start RE model & training
//
//
//
//        }   // if Nan
//
//       // else
//           // Log.d("Mean Throughput is", "not accepted");
//
//
//
//
//
//        mInstance.pred_meanD_cur = meanDkk; // the predicted current_mean_distance for next period is saved here
//        mInstance.prevtotTris = totTris;
//       // if(mInstance.trainedTris==false) // we do this if we don't want to run the odra algoritm. this will be done while alg is running otherwise, we won't able to access mainactivity instance in the algorithm
//           // mInstance=null;  // to force it for garbage collection and avoid heap storage limitation
//       // else // we have our algorithm running
//      //  mInstance.trainedTris = false;
//
//
//
//
//
//
//
//
//
//    }//run
//
//
//    public void cleanOutArraysRE(double totTris, double predDis, MainActivity mInstance){
//
//        int index;
//        if ( mInstance.trisMeanDisk.get(aiIndx).get(totTris).size() == binCap)
//        { // we keep inf of last 10 points
//            double []disArray= mInstance.trisMeanDisk.get(aiIndx).get(totTris).stream()
//                    .mapToDouble(Double::doubleValue)
//                    .toArray();
//            index= findClosest(disArray , predDis);// the index of value needed to be deleted
//            // since we add if objcount != zero to avoid wrong inf from tris=0 -> we won't have re or distance at this situation
//           if(index<mInstance.trisRe.get(totTris).size() &&  index<mInstance.reParamList.get(totTris).size())
//            {
//                mInstance.trisRe.get(totTris).remove(index);
//            mInstance.reParamList.get(totTris).remove(index);
//            }
//        }
//
//        // return index;
//    }
//
//
//
//
//    public int cleanOutArraysThr(double totTris, double predDis, MainActivity mInstance){
//
//        int index=-1;
//        if ( mInstance.trisMeanDisk.get(aiIndx).get(totTris).size() == binCap) {
//            double[] disArray = mInstance.trisMeanDisk.get(aiIndx).get(totTris).stream()
//                    .mapToDouble(Double::doubleValue)
//                    .toArray(); // this has the array of predicted distance
//
//            index = findClosest(disArray, predDis);// the index of value(closest to current mean dis) needed to be deleted
//            mInstance.thr_models.get(aiIndx).get(totTris).remove(index); // has the real throughput
//            mInstance.thParamList.get(aiIndx).get(totTris).remove(index);
//
//            mInstance.trisMeanDisk.get(aiIndx).get(totTris).remove(index); //removes from the head (older data) -> to then add to the tail
//            // mInstance.trisMeanDiskk.get(totTris).remove(index);
//        }
//
//        return  index;
//    }
//
//
//    public static int findClosest(double[] arr, double target) { // find the closest index of arr to value distance= target to then substitue that with the newer one
//        int idx = 0;
//        double dist = Math.abs(arr[0] - target);
//
//        for (int i = 1; i< arr.length; i++) {
//            double cdist = Math.abs(arr[i] - target);
//
//            if (cdist < dist) {
//                idx = i;
//                dist = cdist;
//            }
//        }
//
//        return idx;
//    }
//
//    public void writequality(){
//
//
//        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
//        String currentFolder = mInstance.getExternalFilesDir(null).getAbsolutePath();
//        String FILEPATH = currentFolder + File.separator + "Quality"+mInstance. fileseries+".csv";
//
//        try (PrintWriter writer = new PrintWriter(new FileOutputStream(FILEPATH, true))) {
//           for (int i=0; i<objC-1; i++) {
//               float curtris = mInstance.renderArray.get(i).orig_tris * mInstance.ratioArray.get(i);
//               float r1 = mInstance.ratioArray.get(i); // current object decimation ratio
////               if (mInstance.renderArray.get(i).fileName.contains("0.6")) // third scenario has ratio 0.6
////                   r1 = 0.6f; // jsut for scenario3 objects are decimated
////               else if(mInstance.renderArray.get(i).fileName.contains("0.3")) // sixth scenario has ratio 0.3
////                   r1=0.3f;
//
//
//               float r2 = ref_ratio * r1; // wanna compare obj level of sensitivity to see if we decimate object more -> to (ref *curr) ratio, would the current object hurt more than the other ones?
//               int indq = mInstance.excelname.indexOf(mInstance.renderArray.get(i).fileName);// search in excel file to find the name of current object and get access to the index of current object
//               // excel file has all information for the degredation model
//               float gamma = mInstance.excel_gamma.get(indq);
//               float a = mInstance.excel_alpha.get(indq);
//               float b = mInstance.excel_betta.get(indq);
//               float c = mInstance.excel_c.get(indq);
//               float d_k = mInstance.renderArray.get(i).return_distance();// current distance
//
//               float tmper1 = Calculate_deg_er(a, b, c, d_k, gamma, r1); // deg error for current sit
//               float tmper2 = Calculate_deg_er(a, b, c, d_k, gamma, r2); // deg error for more decimated obj
//
//
//
//
//
//               float max_nrmd = mInstance.excel_maxd.get(indq);
//               tmper1 = tmper1 / max_nrmd; // normalized
//               tmper2= tmper2 /max_nrmd;
//
//               if (tmper2 < 0)
//                   tmper2 = 0;
//
//               //Qi−Qi,r divided by Ti(1−Rr) = (1-er1) - (1-er2) / ....
//               sensitivity[i] = (abs(tmper2 - tmper1) / (curtris - (ref_ratio * curtris)));
//               tmper1 = (float) (Math.round((float) (tmper1 * 1000))) / 1000;
//
//                StringBuilder sb = new StringBuilder();
//                sb.append(dateFormat.format(new Date()));
//                sb.append(',');
//                sb.append(mInstance.renderArray.get(i).fileName+"_n"+(i+1)+"_d"+(d_k));
//                sb.append(',');
//                sb.append(sensitivity[i]);
//                sb.append(',');
//                sb.append(r1);
//                sb.append(',');
//                sb.append(1-tmper1);
//              //  sb.append(mInstance.tasks.toString());
//
//                sb.append('\n');
//                writer.write(sb.toString());
//                System.out.println("done!");
//            }
//        }catch (FileNotFoundException e) {
//            System.out.println(e.getMessage());
//        }
//
//    }
//    public void writeThr(double realThr, double predThr, boolean trainedFlag){
//
//        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss:SSS");
//        String currentFolder = mInstance.getExternalFilesDir(null).getAbsolutePath();
//        String FILEPATH = currentFolder + File.separator + "Throughput"+mInstance. fileseries+".csv";
//
//
//
//        StringBuilder sb = new StringBuilder();
//        sb.append(dateFormat.format(new Date())); sb.append(',');
//        sb.append(realThr);sb.append(',').append(predThr);sb.append(',').append(trainedFlag);sb.append(',');
//        sb.append(mInstance.rohTL.get(aiIndx));sb.append(',').append(mInstance.rohDL.get(aiIndx));sb.append(',').append(mInstance.deltaL.get(aiIndx));sb.append(',');
//
//        sb.append(mInstance.total_tris);
//
//        int i=0;
//        try (PrintWriter writer = new PrintWriter(new FileOutputStream(FILEPATH, true))) {
//        for (AiItemsViewModel taskView :mInstance.mList) {
//
//            sb.append(",").append(taskView.getModels().get(taskView.getCurrentModel()))
//                    .append(",").append(taskView.getDevices().get(taskView.getCurrentDevice()))
//                    .append(",").append(taskView.getCurrentNumThreads()).append(",").append(taskView.getThroughput())
//                    .append(",").append(taskView.getInferenceT()).append(",").append(taskView.getOverheadT());
//
//        }
//
//
//
//
//
//
////                sb.append(dateFormat.format(new Date())); sb.append(',');
////            sb.append(realThr);sb.append(',');
////            sb.append(predThr);
////            sb.append(',');
////            sb.append(trainedFlag);
////            sb.append(','); sb.append(mInstance.total_tris);
////            sb.append(',').append(mInstance.mList.get(mIndx).getCurrentModel());
////            sb.append(',').append(mInstance.mList.get(mIndx).getDevices());
////            sb.append(',').append(mInstance.mList.get(mIndx).getCurrentNumThreads()).append(",").
////                    append(mInstance.mList.get(mIndx).getInferenceT()).append(",").append(mInstance.mList.get(mIndx).getOverheadT());
////            sb.append(',').append(mInstance.rohTL.get(mIndx))
////             .append(',').append(mInstance.rohDL.get(mIndx))
////                    .append(',').append(mInstance.deltaL.get(mIndx));
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
//
//
//    // public void writeRE(double realRe, double predRe, boolean trainedFlag,double totT, double nextT, boolean trainedT, double pAR, double pAI){
//    public void writeRE(double realRe, double predRe, boolean trainedFlag, double totT, double nextT, double algTris, boolean trainedT,
//                        double pAR, double pAI, boolean accM, double totTris, double avgq, long duration){
//
//        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss:SSS");
//        String currentFolder = mInstance.getExternalFilesDir(null).getAbsolutePath();
//        String FILEPATH = currentFolder + File.separator + "RE"+mInstance. fileseries+".csv";
//
//        try (PrintWriter writer = new PrintWriter(new FileOutputStream(FILEPATH, true))) {
//            StringBuilder sb = new StringBuilder();
//            sb.append(dateFormat.format(new Date())); sb.append(',');
//            sb.append(realRe);sb.append(',');
//            sb.append(predRe);
//            sb.append(',');  sb.append(trainedFlag);
//            sb.append(',');  sb.append(totT);
//            sb.append(',');  sb.append(nextT);
//            sb.append(',');  sb.append(algTris);
//            sb.append(',');  sb.append(trainedT);
//            sb.append(',');  sb.append(pAR);
//            sb.append(',');  sb.append(pAI);
//            sb.append(',');  sb.append(accM);// if both models are accurate
//            sb.append(',');  sb.append(totTris);// if both models are accurate
//            sb.append(',');  sb.append(avgq);
//            sb.append(',');  sb.append(duration);
//            sb.append('\n');
//            writer.write(sb.toString());
//            System.out.println("done!");
//        } catch (FileNotFoundException e) {
//            System.out.println(e.getMessage());
//        }
//    }
//
//
//
//    public float calculateMeanQuality( ) {
//
//        float sumQual=0;
//        for (int ind = 0; ind < mInstance.objectCount; ind++)
//        {
//            int i =  mInstance.excelname.indexOf( mInstance.renderArray.get(ind).fileName);
//            float gamma = mInstance.excel_gamma.get(i);
//            float a = mInstance.excel_alpha.get(i);
//            float b = mInstance.excel_betta.get(i);
//            float c = mInstance.excel_c.get(i);
//            float d = mInstance.renderArray.get(ind).return_distance();
//            float curQ = mInstance.ratioArray.get(ind);
//
////            if (mInstance.renderArray.get(ind).fileName.contains("0.6")) // third scenario has ratio 0.6
////                curQ = 0.6f; // jsut for scenario3 objects are decimated
////            else if(mInstance.renderArray.get(ind).fileName.contains("0.3")) // sixth scenario has ratio 0.3
////                curQ=0.3f;
//
//            float deg_error = (float) (Math.round((float) (Calculate_deg_er(a, b, c, d, gamma, curQ) * 1000))) / 1000;
//            float max_nrmd = mInstance.excel_maxd.get(i);
//
//            float cur_degerror = deg_error / max_nrmd;
//            float quality= 1- cur_degerror;
//            objquality[ind]=quality;
//            sumQual+=quality;
//
//
//        }
//
//
//        return sumQual/mInstance.objectCount;
//
//
//
//    }
//
//    public float Calculate_deg_er(float a,float b,float creal,float d,float gamma, float r1) {
//
//        float error;
//        if(r1==1)
//            return  0f;
//        error = (float) (((a * Math.pow(r1,2)) + (b * r1) + creal) / (Math.pow(d , gamma)));
//        return error;
//    }
//
//
//
//    float odraAlg(float tUP) throws InterruptedException {
//
//
//        candidate_obj = new HashMap<>();
//        Map<Integer, Float> sortedcandidate_obj = new HashMap<>();
//        float sum_org_tris = 0; // sum of all tris of the objects o the screen
//
//        for (int ind = 0; ind < mInstance.objectCount; ind++) {
//
//            sum_org_tris += mInstance.renderArray.get(ind).orig_tris;// this will ne used to cal min of tris needed at each row (object) in bellow
//
//
//            float curtris = mInstance.renderArray.get(ind).orig_tris * mInstance.ratioArray.get(ind);
//
//
//            tris_share[ind] = (curtris / tUP);
//            candidate_obj.put(ind, sensitivity[ind] / tris_share[ind]);
//
//
//        }
//        sortedcandidate_obj = sortByValue(candidate_obj, false); // second arg is for order-> ascending or not? NO
//        // Up to here, the candidate objects are known
//
//
//        float updated_sum_org_tris = sum_org_tris; // keeps the last value which is sum_org_tris - tris1-tris2-....
//        for (int i : sortedcandidate_obj.keySet()) { // check this gets the candidate object index to calculate min weight
//            float sum_org_tris_minus = updated_sum_org_tris - mInstance.renderArray.get(i).orig_tris; // this is summ of tris for all the objects except the current one
//            updated_sum_org_tris = sum_org_tris_minus;
//            tMin[i] = coarse_Ratios[coarse_Ratios.length - 1] * sum_org_tris_minus;// minimum tris needs for object i+1 to object n
//            ///@@@@ if this line works lonely, delete the extra line for the last object to zero in the alg
//        }
//
//        Map.Entry<Integer, Float> entry = sortedcandidate_obj.entrySet().iterator().next();
//        int key = entry.getKey(); // get access to the first key -> to see if it is the first object for bellow code
//
//        int prevInd = 0;
//        for (int i : sortedcandidate_obj.keySet()){  // line 10 i here is equal to alphai -> the obj with largest candidacy
//            // check this gets the candidate object index to maintain its quality
//            for (int j = 0; j < coarse_Ratios.length; j++) {
//
//                int indq = mInstance.excelname.indexOf(mInstance.renderArray.get(i).fileName);// search in excel file to find the name of current object and get access to the index of current object
//                float gamma =mInstance. excel_gamma.get(indq);
//                float a =mInstance. excel_alpha.get(indq);
//                float b = mInstance.excel_betta.get(indq);
//                float c =mInstance. excel_c.get(indq);
//                float d_k = mInstance.renderArray.get(i).return_distance();// current distance
//                float max_nrmd = mInstance.excel_maxd.get(indq);
//                float quality = 1 -( Calculate_deg_er(a, b, c, d_k, gamma, coarse_Ratios[j]) / max_nrmd  ); // deg error for current sit
//
//                if (i == key && tUP >= mInstance.renderArray.get(i).getOrg_tris() * coarse_Ratios[j]) { // the first object in the candidate list
//                    fProfit[i][j] = quality;// Fα(i),j ←Qα(i),j -> i is alpha i
//                    tRemainder[i][j] = tUP - (mInstance.renderArray.get(i).getOrg_tris() * coarse_Ratios[j]);
//                } else //  here is the dynamic programming section
//                    for (int s = 0; s < coarse_Ratios.length; s++) {
//
//                        float f = fProfit[prevInd][s] + quality;
//                        float t = tRemainder[prevInd][s] - (mInstance.renderArray.get(i).getOrg_tris() * coarse_Ratios[j]);
//                        if (t >= tMin[i] && fProfit[i][j] < f) {
//
//                            fProfit[i][j] = f;
//                            tRemainder[i][j] = t;
//                            track_obj[i][j] = s;
//                        }
//
//                    }//
//
//            }//for j  up to here we reach line 25
//            prevInd=i;
//        }// for i
///// start with object with least priority
//
//        sortedcandidate_obj = sortByValue(candidate_obj, true); // to iterate through the list from lowest to highest values
//
//        int lowPobjIndx = sortedcandidate_obj.entrySet().iterator().next().getKey(); // line 26
//        float tmp=fProfit[lowPobjIndx][0];
//        int j=0;
//        for  (int maxindex=1;maxindex<coarse_Ratios.length;maxindex++) // line 27
//            if(fProfit[lowPobjIndx][maxindex]>tmp)// finds the index of coarse-grain ratio with maximum profit
//            {
//                tmp = fProfit[lowPobjIndx][maxindex];
//                j=maxindex;
//            }
//
//
//        for (int i : sortedcandidate_obj.keySet())
//            //if ( mInstance.renderArray.size()>i &&  mInstance.renderArray.get(i)!=null)
//
//            {// to avoid null pointer error
//
//            mInstance.total_tris = mInstance.total_tris - (mInstance.ratioArray.get(i) * (mInstance.o_tris.get(i)/1000));// total =total -1*objtris
//            mInstance.ratioArray.set(i, coarse_Ratios[j]);
//
//
//            mInstance.runOnUiThread(() -> mInstance.renderArray.get(i).decimatedModelRequest(mInstance.ratioArray.get(i), i, false));
//
//            mInstance.total_tris = mInstance.total_tris + (mInstance.ratioArray.get(i) *  mInstance.renderArray.get(i).orig_tris);// total = total + 0.8*objtris
//           // mInstance.trisDec.put(mInstance.total_tris,true);
//
//            j = track_obj[i][j];
//            Thread.sleep(sleepTime);// added to prevent the crash happens while redrawing all the objects at the same time
//
//
//        }
//
//
//        return (float) mInstance.total_tris; // this returns the total algorithm triangle count
//
//
//
//    }
//
//
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
//
//
//
//
//}
//
//
//
