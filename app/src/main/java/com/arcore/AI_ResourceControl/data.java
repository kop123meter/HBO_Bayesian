
package com.arcore.AI_ResourceControl;

import android.util.Log;
import android.widget.TextView;
import android.os.SystemClock;
import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.ListMultimap;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.PrintWriter;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static java.lang.Math.abs;

public class data implements Runnable {

    int binCap=10;
    private final MainActivity mInstance;
    float ref_ratio=0.5f;
    int objC;
    float sensitivity[] ;
    float objquality[];
    float tris_share[];
    Map <Integer, Float> candidate_obj;
    float []coarse_Ratios=new float[]{1f,0.8f, 0.6f , 0.4f, 0.2f, 0.05f};
    //ArrayList <ArrayList<Float>> F_profit= new ArrayList<>();
    float [][]fProfit;
    float [][] tRemainder;
    int [][] track_obj;
    int sleepTime=60;
    //float candidate_obj[] = new float[total_obj];
    float tMin[] ;



    public data(MainActivity mInstance) {

        this.mInstance = mInstance;

        objC=mInstance.objectCount+1;
        sensitivity = new float[objC];
        tris_share = new float[objC];
        objquality= new float[objC];// 1- degradation-error


        //ArrayList <ArrayList<Float>> F_profit= new ArrayList<>();
        fProfit= new float[objC][coarse_Ratios.length];
        tRemainder= new float[objC][coarse_Ratios.length];
        track_obj= new int[objC][coarse_Ratios.length];
        //float candidate_obj[] = new float[total_obj];
        tMin = new float[objC];


    }

    @Override
    public void run() {

        boolean accmodel = true;// this is to check if the trained model for thr is accurate
        //boolean accRe=true;// this is to check if the trained model for re is accurate
        boolean trainedTris = false;
        boolean trainedThr = false;
        boolean trainedRE = false;
        double maxTrisThreshold = mInstance.orgTrisAllobj;
        double minTrisThreshold = maxTrisThreshold * mInstance.coarse_Ratios[mInstance.coarse_Ratios.length - 1];
        double meanThr, totTris;
        double meanDk = 0; // mean current dis
        double meanDkk = 0; // mean of d in the next period-> you need to cal the average of predicted d for all the objects
        double pred_meanD = mInstance.pred_meanD_cur; // for next 1 sc

        ///1000;

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

        totTris = mInstance.total_tris;




        meanThr = mInstance.getThroughput();// after the objects are decimated

     //   if (meanThr < 100 && meanThr > 1) {

            meanThr = (double) Math.round(meanThr * 100) / 100;



// this is thr calculated using the modeling
          //  double predThr = (mInstance.rohT *  totTris) + (mInstance.rohD * pred_meanD) + mInstance.delta;// use predicted distance for almost current period (predicted distance for next 1 sec is the closest one we have)
           // predThr = (double) Math.round((double) predThr * 100) / 100;

                // temp nilo   writeThr(meanThr,pred_meanD);// for the urrent period
        writeThr2(meanThr,0,trainedThr);

        mInstance.pred_meanD_cur = meanDkk;




    }//run






    public void writeThr2(double realThr, double predThr, boolean trainedFlag){

        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss:SSS");
        String currentFolder = mInstance.getExternalFilesDir(null).getAbsolutePath();
        String FILEPATH = currentFolder + File.separator + "Throughput"+mInstance. fileseries+".csv";



        StringBuilder sb = new StringBuilder();
        sb.append(dateFormat.format(new Date())); sb.append(',');
        sb.append(realThr);sb.append(',');

        sb.append(mInstance.total_tris);

        int i=0;
        try (PrintWriter writer = new PrintWriter(new FileOutputStream(FILEPATH, true))) {
            for (AiItemsViewModel taskView :mInstance.mList) {

                sb.append(",").append(taskView.getModels().get(taskView.getCurrentModel()))
                        .append(",").append(taskView.getDevices().get(taskView.getCurrentDevice()))
                        .append(",").append(taskView.getCurrentNumThreads()).append(",").append(taskView.getThroughput())
                        .append(",").append(taskView.getInferenceT()).append(",").append(taskView.getOverheadT());

            }


            sb.append('\n');
            writer.write(sb.toString());
            System.out.println("done!");
        } catch (FileNotFoundException e) {
            System.out.println(e.getMessage());
        }
    }


    public void writeThr(double realThr, double pred_meanD){

        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss:SSS");
        String currentFolder = mInstance.getExternalFilesDir(null).getAbsolutePath();
        String FILEPATH = currentFolder + File.separator + "Throughput"+mInstance. fileseries+".csv";

        try (PrintWriter writer = new PrintWriter(new FileOutputStream(FILEPATH, true))) {
            StringBuilder sb = new StringBuilder();
            sb.append(dateFormat.format(new Date())); sb.append(',');
            sb.append(realThr);sb.append(',');
          //  sb.append(',');
            sb.append(mInstance.total_tris);

            int i=0;
            for (AiItemsViewModel taskView :mInstance.mList) {

                sb.append(",").append(taskView.getModels().get(taskView.getCurrentModel()))
                        .append(",").append(taskView.getDevices().get(taskView.getCurrentDevice()))
                        .append(",").append(taskView.getCurrentNumThreads()).append(",").append(taskView.getThroughput())
                        .append(",").append(taskView.getInferenceT()).append(",").append(taskView.getOverheadT());

                if(taskView.getThroughput()!=0   ) {

                    if(mInstance.coeff_total_triangle.get(i).size()>0) {


                        if(mInstance.modelMeanRsp.size()>i) {
                            int n = mInstance.modelMeanRsp.get(i).size();
                            if (n == binCap) {// this is to collect the updated data of response time for each model by removing the older data
                                mInstance.modelMeanRsp.get(i).remove(0);// remove first element
                                mInstance.total_triangle.get(i).remove(0);
                              //  mInstance.modelMeanDisk.get(i).remove(0);

                            }
                        }

                        if (mInstance.last_tris == mInstance.total_tris) // means that we collect data of same tris-> so, average and update last/current value of coeff hashmap
//
                        {

                            mInstance.modelMeanRsp.put(i, 1000 / taskView.getThroughput());
                            mInstance.total_triangle.put(i, (double) mInstance.total_tris);
                           // mInstance.modelMeanDisk.put(i, pred_meanD);


                            double rspT_avg = mInstance.modelMeanRsp.get(i).stream()
                                    .mapToDouble(d -> d)
                                    .average()
                                    .orElse(0.0);

//                            double dis_avg = mInstance.modelMeanDisk.get(i).stream()
//                                    .mapToDouble(d -> d)
//                                    .average()
//                                    .orElse(0.0);


                            int current_index = mInstance.coeff_total_triangle.get(i).size() - 1;
                            double last_rpT= mInstance.coeff_modelMeanRsp.get(i).get(current_index);

// calculate average of response time for the same triangle count

                            mInstance.coeff_total_triangle.get(i).set(current_index,(double) mInstance.total_tris);
                            mInstance.coeff_modelMeanRsp.get(i).set(current_index,(rspT_avg+last_rpT)/2);
                           // mInstance.rspParamList.get(i).set(current_index, Arrays.asList((double)mInstance.total_tris, dis_avg, 1.0));
                                   // add();

                        }

                        else {// means that the triangle count is changed, so clear the hashmap of tris and response time
                            mInstance.modelMeanRsp.get(i).clear();
                            mInstance.total_triangle.get(i).clear();
                      Log.d("tris changed","");

                        }
//
//                        }

                        int size = mInstance.coeff_total_triangle.get(i).size();
                        if (size >= 2) {// we have at least two points to calculate r
                            // update coefficient maps with latest data
                            // calculate correlation r
                            float r = correlationCoefficient(mInstance.coeff_total_triangle.get(i), mInstance.coeff_modelMeanRsp.get(i), size);
                            sb.append(",").append(r);
                        }
                    }// size of coeeff is positive

                else{// initially add first value to coeff matrices

                        mInstance.coeff_total_triangle.put(i, (double) mInstance.total_tris);
                        mInstance.coeff_modelMeanRsp.put(i, 1000/taskView.getThroughput());

                    }



                }
                i++;

            }
            //date, thr, tris, model, device, thread


            mInstance.last_tris=mInstance.total_tris;

            sb.append('\n');
            writer.write(sb.toString());
            System.out.println("done!");
        } catch (FileNotFoundException e) {
            System.out.println(e.getMessage());
        }
    }



    // function that returns correlation coefficient.
    static float correlationCoefficient(List<Double> X,
                                        List<Double> Y, int n)
    {

        double sum_X = 0, sum_Y = 0, sum_XY = 0;
        double squareSum_X = 0, squareSum_Y = 0;

        for (int i = 0; i < n; i++)
        {
            // sum of elements of array X.
            sum_X = sum_X + X.get(i);

            // sum of elements of array Y.
            sum_Y = sum_Y + Y.get(i);

            // sum of X[i] * Y[i].
            sum_XY = sum_XY + X.get(i) * Y.get(i);

            // sum of square of array elements.
            squareSum_X = squareSum_X + X.get(i) * X.get(i);
            squareSum_Y = squareSum_Y + Y.get(i) * Y.get(i);
        }

        // use formula for calculating correlation
        // coefficient.
        float corr = (float)(n * sum_XY - sum_X * sum_Y)/
                (float)(Math.sqrt((n * squareSum_X -
                        sum_X * sum_X) * (n * squareSum_Y -
                        sum_Y * sum_Y)));

        return  (Math.round((double) (100 * corr))) / 100;
    }


    static float correlationCoefficient(Double X,
                                       Double Y)
    {

        double sum_X = 0, sum_Y = 0, sum_XY = 0;
        double squareSum_X = 0, squareSum_Y = 0;
//
//        for (int i = 0; i < n; i++)
//        {
            // sum of elements of array X.
            sum_X = X;

            // sum of elements of array Y.
            sum_Y = Y;

            // sum of X[i] * Y[i].
            sum_XY = X * Y;

            // sum of square of array elements.
            squareSum_X =  X* X;
            squareSum_Y = Y * Y;
//        }

        // use formula for calculating correlation
        // coefficient.
       int n=1;
        float corr = (float)(n * sum_XY - sum_X * sum_Y)/
                (float)(Math.sqrt((n * squareSum_X -
                        sum_X * sum_X) * (n * squareSum_Y -
                        sum_Y * sum_Y)));

        return corr;
    }


    // Driver function
//    public static void main(String args[])
//    {
//
//        int X[] = {15, 18, 21, 24, 27};
//        int Y[] = {25, 25, 27, 31, 32};
//
//        // Find the size of array.
//        int n = X.length;
//
//        // Function call to correlationCoefficient.
//        System.out.printf("%6f",
//                correlationCoefficient(X, Y, n));
//
//
//    }
//}













    private static Map<Integer, Float> sortByValue(Map<Integer, Float> unsortMap, final boolean order)
    {
        List<Map.Entry<Integer, Float>> list = new LinkedList<>(unsortMap.entrySet());

        // Sorting the list based on values
        list.sort((o1, o2) -> order ? o1.getValue().compareTo(o2.getValue()) == 0
                ? o1.getKey().compareTo(o2.getKey())
                : o1.getValue().compareTo(o2.getValue()) : o2.getValue().compareTo(o1.getValue()) == 0
                ? o2.getKey().compareTo(o1.getKey())
                : o2.getValue().compareTo(o1.getValue()));
        return list.stream().collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue, (a, b) -> b, LinkedHashMap::new));

    }




}



