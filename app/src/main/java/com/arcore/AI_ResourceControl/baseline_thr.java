
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

public class baseline_thr implements Runnable {


    private final MainActivity mInstance;
    float ref_ratio=0.5f;
    int objC;
    float sensitivity[] ;
    float objquality[];
    float tris_share[];

    float []coarse_Ratios=new float[]{1f,0.8f, 0.6f , 0.4f, 0.2f, 0.05f};
    //ArrayList <ArrayList<Float>> F_profit= new ArrayList<>();

    int sleepTime=50;
    //float candidate_obj[] = new float[total_obj];
    float tMin[] ;

    public baseline_thr(MainActivity mInstance) {
        objC=mInstance.objectCount+1;
        this.mInstance = mInstance;
        sensitivity = new float[objC];
        tris_share = new float[objC];
        objquality= new float[objC];// 1- degradation-error





        //ArrayList <ArrayList<Float>> F_profit= new ArrayList<>(coarse_Ratios);

        //float candidate_obj[] = new float[total_obj];
        tMin = new float[objC];


    }

    @Override
    public void run() {




        double meanThr;
        long time1 = 0;
        long time2 = 0;
        time1 = System.nanoTime() / 1000000; //starting first loop
        time2 = time1;
        meanThr = mInstance.getThroughput();// after the objects are decimated

        if (meanThr < 100 && meanThr > 1) {

            meanThr = (double) Math.round(meanThr * 100) / 100;

            if (meanThr<mInstance.des_Thr) // now we want to check 5 times to make sure about it
                 mInstance.lastConscCounter += 1;

            else
                mInstance.lastConscCounter = 0; // the model works fine so we don't need to re-adjust the throughput factor for decimation data collection


            if (mInstance.lastConscCounter > 4 && (objC-1)>0) {// run decimate all for
                try {

                    decAll();
                    time2= System.nanoTime()/1000000;
                    mInstance.t_loop1 = time2 - time1 - (sleepTime * (objC-1) );
                    mInstance.lastConscCounter = 0;
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }

                writeThr(meanThr);// for the urrent period
            if((objC-1)>0){
               double totTris = mInstance.total_tris;
                double avgq = calculateMeanQuality();
                writequality();
                double PRoAR = (double) Math.round((avgq / mInstance.des_Q) * 100) / 100;
                double PRoAI = (double) Math.round((meanThr / mInstance.des_Thr) * 100) / 100;// should be real
                double reMsrd = PRoAR / PRoAI;
                reMsrd = (double) Math.round(reMsrd * 100) / 100;
                writeRE(reMsrd, 0, false, totTris, totTris,totTris, false, PRoAR, PRoAI, false, mInstance.orgTrisAllobj, avgq, mInstance.t_loop1);// writes to the file

            }

            }



    }//run



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




    public void writequality(){


        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        String currentFolder = mInstance.getExternalFilesDir(null).getAbsolutePath();
        String FILEPATH = currentFolder + File.separator + "Quality"+mInstance. fileseries+".csv";

        try (PrintWriter writer = new PrintWriter(new FileOutputStream(FILEPATH, true))) {
           for (int i=0; i<objC-1; i++) {
               float curtris = mInstance.renderArray.get(i).orig_tris * mInstance.ratioArray.get(i);
               float r1 = mInstance.ratioArray.get(i); // current object decimation ratio

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
               tmper1 = (float) (Math.round((float) (tmper1 * 1000))) / 1000;

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
              //  sb.append(mInstance.tasks.toString());

                sb.append('\n');
                writer.write(sb.toString());
                System.out.println("done!");
            }
        }catch (FileNotFoundException e) {
            System.out.println(e.getMessage());
        }

    }
    public void writeThr(double realThr){

        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss:SSS");
        String currentFolder = mInstance.getExternalFilesDir(null).getAbsolutePath();
        String FILEPATH = currentFolder + File.separator + "Throughput"+mInstance. fileseries+".csv";



        try (PrintWriter writer = new PrintWriter(new FileOutputStream(FILEPATH, true))) {
            StringBuilder sb = new StringBuilder();
            sb.append(dateFormat.format(new Date())); sb.append(',');
            sb.append(realThr);sb.append(',');
            sb.append("" );
            sb.append(',');
             sb.append("");
            sb.append(','); sb.append(mInstance.total_tris);
            sb.append(mInstance.tasks.toString());
            sb.append(','); sb.append(mInstance.des_Thr);
            sb.append(','); sb.append(mInstance.des_Q);
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


            float deg_error = (float) (Math.round((float) (Calculate_deg_er(a, b, c, d, gamma, curQ) * 1000))) / 1000;
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



    void decAll() throws InterruptedException {


// if last object's quality =1 -> we need to reset the baseline_index to 0 to start decimating all objects similarly.
        //Otherwise, objects are already decimated similarly, so we increase the index

        if(mInstance.ratioArray.get(mInstance.objectCount-1)==1)
            mInstance.baseline_index=1;// start from 0.8 decimation ratio for all objects
        else
           mInstance.baseline_index+=1;

        int index=mInstance.baseline_index;

        if(index<coarse_Ratios.length)// to decimate objects up to the available defined ratio
        {
            for (int i = 0; i <  mInstance.objectCount; i++) {

                //decimate all
                mInstance.total_tris = mInstance.total_tris - (mInstance.ratioArray.get(i) * mInstance.o_tris.get(i));// total =total -1*objtris
                mInstance.ratioArray.set(i, coarse_Ratios[index]);
                TextView posText = (TextView)mInstance. findViewById(R.id.dec_req);
                posText.setText("Request for " +mInstance. renderArray.get(i).fileName + " " + coarse_Ratios[index]);
                int finalI = i;
                mInstance.runOnUiThread(() -> mInstance.renderArray.get(finalI).decimatedModelRequest(coarse_Ratios[index], finalI, false));
                mInstance.total_tris = mInstance.total_tris + (coarse_Ratios[index] *  mInstance.renderArray.get(i).orig_tris);// total = total + 0.8*objtris
                Thread.sleep(sleepTime);// added to prevent the crash happens while redrawing all the objects at the same time


            }///for

        }



    }







}



