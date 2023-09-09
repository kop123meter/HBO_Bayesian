/**********This was for MIR data collection. noy used for XMIR*/

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

public class survey implements Runnable { // this is for survey2 to collec data of the low quality object and the reference

    int binCap=5;
    private final MainActivity mInstance;
    float ref_ratio=0.5f;
    int objC;
   // double sensitivity[] ;
    float objquality[];
    //double tris_share[];
    Map <Integer, Double> candidate_obj;
    float []coarse_Ratios=new float[]{1f,0.8f, 0.6f ,0.5f, 0.4f, 0.2f, 0.1f};
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






    public survey(MainActivity mInstance) {

        this.mInstance = mInstance;

        objC=mInstance.objectCount+1;
       // sensitivity = new double[objC];
      //  tris_share = new double[objC];
        objquality= new float[objC];// 1- degradation-error
        posText_re= mInstance.findViewById(R.id.app_re);
        posText_q= mInstance.findViewById(R.id.app_quality);
        posText_thr= mInstance.findViewById(R.id.app_thr);
        posText_mir= mInstance.findViewById(R.id.app_mir);

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



            int variousTris = mInstance.trisMeanThr.keySet().size();


         double predThr = 0;
// nill added 8 april
//            int ind = -1;
           // if (variousTris < 3) { // one object on the screen
        double fixedT=mInstance.total_tris;

        if ( mInstance.trisMeanThr.containsKey(fixedT) &&
                mInstance.trisMeanThr.get(fixedT).size() == binCap) { //we delete from array of initial tris not new, since we have data of initial tris by 100% we keep data up to binCap per triangleCount
            cleanOutArraysThr(fixedT, pred_meanD, mInstance);// cleans out the closest data to the curr one within bins
        }


        //******************  RE modeling *************
                // double sum = 0;
                double avgq = calculateMeanQuality();
                  avgq = (double) (Math.round((double) (100 * avgq))) / 100;

//                writequality();


                posText_q.setText("Q: "+String.valueOf( avgq));


               writequality();


                mInstance.pred_meanD_cur = meanDkk; // the predicted current_mean_distance for next period is saved here,


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
                sb.append(d_k);
                sb.append(',');
                sb.append(r1);
                sb.append(',');
                sb.append(1-tmper1);
              //  sb.append(mInstance.tasks.toString());

                sb.append('\n');
                writer.write(sb.toString());
                System.out.println("done!");
            }
        }catch (FileNotFoundException e) {
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
            sb.append(',');  sb.append(mInstance.algName[mInstance.alg-1]);
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



