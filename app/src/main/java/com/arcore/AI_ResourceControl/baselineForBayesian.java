
package com.arcore.AI_ResourceControl;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.PrintWriter;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static java.lang.Math.abs;
import static java.lang.Thread.sleep;

import android.os.CountDownTimer;

public class baselineForBayesian implements Runnable {// baseline for MIR

    int max_cap=5;
    private final MainActivity mInstance;
    float ref_ratio=0.5f;
    int objC;
    Map <Integer, Double> candidate_obj;
    float []coarse_Ratios=new float[]{1f,0.8f, 0.7f , 0.5f,0.3f, 0.2f,0.1f};

    double [][]fProfit;
    double [][] tRemainder;
    int [][] track_obj;
    int sleepTime=10;

    double bys_Avgltcy= 62.3790;
    double avgLatency=0;// this is for the baseline average latency
    float selectedRatio=1;// gained from binary search
    boolean swichBaseline =false; //this to make sure the first baseline (mach quality is done to then revert the objects a=to their original ratio and use binary search for matching the average latencies
    double perc_error=1;

    public baselineForBayesian(MainActivity mInstance) {

        this.mInstance = mInstance;

        bys_Avgltcy= mInstance.bayesian1_bestLcty;


    }

    @Override
    public void run() {


            }//run


    public int findDevice(String deviceName){

        switch (deviceName) {
            case "CPU":
                return 0;
            case "GPU":
                return 1;
            case "NNAPI":
                return 2;

        }

        return 0;
    }

    // this is to  match the trinagles Ratio with the HBO (our solution)
    public void staticMatchTrisRatio(double tUPRatio) throws InterruptedException {
        selectedRatio=(float)tUPRatio;// first we run baseline 1
        ///// here is to just adjust the quality of objects:
        double nextTris = tUPRatio*mInstance.orgTrisAllobj; // the last input is the ratio of current nextTris to the max_total_tris of objects with highest quality
        //  Part 2:  start to apply the triangle count and OTDA
       // tmpDecimate(0.4f); for one baseline experiment

        try {
          otdaRevised(nextTris);// this is OTDA algorithm
                    }
        catch (InterruptedException e) {
            e.printStackTrace();
        }
        mInstance.avgq = calculateMeanQuality();
      //  swichBaseline =!swichBaseline;// when first baseline is finished it will be true, and for the second baseline it becomes false

            oneCycleWriteRT();// this is to have the data collected for the same triangle ratio of bayesian

           }

    public void staticDelegate(){// this is the baseline that always assigns each task to its best based on the static analysis

        // here is to assign delegates
        for (int i=0;i<mInstance.mList.size();i++) {
            AiItemsViewModel taskView = mInstance.mList.get(i);
            // first find the best offline AI response Time = EXPECTED RESPONSE TIme
            int indq = mInstance.excel_BestofflineAIname.indexOf(taskView.getModels().get(taskView.getCurrentModel()));// search in excel file to find the name of current object and get access to the index of current object
            // excel file has all information for the AI inference NAME, Delegate, and time
            String bestDelg = mInstance.excel_BestofflineAIdelg.get(indq);

            int model= (taskView.getCurrentModel());
            int device=taskView.getCurrentDevice();
            int new_device=findDevice(bestDelg);
            if(device!=new_device)// this means that the model should be updated
            {
                mInstance.adapter.setMList(mInstance.mList);
                mInstance.recyclerView_aiSettings.setAdapter(mInstance.adapter);
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
                sleep(40);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }// this is to make sure the device is updated
        }


    }




    public void allNNAPI(){// this is the baseline4 that always assigns taska to NNAPI

        for (int i=0;i<mInstance.mList.size();i++) {
            AiItemsViewModel taskView = mInstance.mList.get(i);
            int model= (taskView.getCurrentModel());
            int device=taskView.getCurrentDevice();
            int new_device=2;
            if(device!=new_device)// this means that the model should be updated
            {
                mInstance.adapter.setMList(mInstance.mList);
                mInstance.recyclerView_aiSettings.setAdapter(mInstance.adapter);
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
                sleep(40);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }// this is to make sure the device is updated
        }
        oneCycleWriteRT();

    }



    public void oneCycleWriteRT(){

        long time=12000;
        if(mInstance.bys_baseline1_2==false)
            time=20000;
        CountDownTimer sceneTimer = new CountDownTimer(time, 2000) {//is  better (50000, 5000) {

            @Override
            public void onTick(long millisUntilFinished) {
                perc_error = (double) (Math.round((double) (abs(avgLatency - bys_Avgltcy) * 100) / bys_Avgltcy)) / 100;
                writeRT();// this is to check wether we are close to average latency match?
             }
            @Override
            public void onFinish() {// at the end of 20 times * 2s data collection, we use binary search for the next ratio

                if(mInstance.bys_baseline1_2==true)// since we use the above function for all baselines, we dont' need to run matchAvgLatency for some baselines
                {
                    mInstance.total_tris = 0;
                    for (int i = 0; i < mInstance.objectCount; i++) {// to avoid null pointer error
                        mInstance.ratioArray.set(i, 1f);
                        int finalI = i;
                        mInstance.runOnUiThread(() -> mInstance.renderArray.get(finalI).decimatedModelRequest(mInstance.ratioArray.get(finalI), finalI, false));
                        mInstance.total_tris = mInstance.total_tris + (mInstance.renderArray.get(finalI).orig_tris);// total = total + 0.8*objtris
                        try {
                            Thread.sleep(sleepTime);// added to prevent the crash happens while redrawing all the objects at the same time
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                    }

                    selectedRatio = 1;

                    matchAvgLatency();
                }

            }

        }.start();

    }

/*
    public void matchAvgLatency() throws InterruptedException {// this is the cycle of writing latency data of static algorithm to the file

        mInstance.total_tris =0;
        for (int i =0;i<mInstance.objectCount;i++)
        {// to avoid null pointer error
            mInstance.ratioArray.set(i, 1f);
            int finalI = i;
            mInstance.runOnUiThread(() -> mInstance.renderArray.get(finalI).decimatedModelRequest(mInstance.ratioArray.get(finalI), finalI, false));
            mInstance.total_tris = mInstance.total_tris + (mInstance.renderArray.get(finalI).orig_tris);// total = total + 0.8*objtris
            Thread.sleep(sleepTime);// added to prevent the crash happens while redrawing all the objects at the same time
        }

        final double[] left = {0.05};
        final double[] right = {1};

        CountDownTimer sceneTimer = new CountDownTimer(27000, 3000) {//is  better (50000, 5000) {

            @Override
            public void onTick(long millisUntilFinished) {
                writeRT();// this is to check wether we are close to average latency match?

                perc_error = (double) (Math.round((double) (abs(avgLatency - bys_Avgltcy) * 100) / bys_Avgltcy)) / 100;

                if(perc_error <0.1 || selectedRatio<0.05){// cause we cannot decimate objects below 5%
                    this.cancel();
                    return;
                }


            }
            @Override
            public void onFinish() {// at the end of 20 times * 2s data collection, we use binary search for the next ratio

                if(selectedRatio!=1) {// this is because we don't want to change the initial indexes, but the index change is done after the first round trial
                    // Your onFinish logic for the current index here
                    if (avgLatency < bys_Avgltcy)
                        left[0] = selectedRatio;
                    else
                        right[0] = selectedRatio;

                }
                if(left[0]<=right[0]) {
                    mInstance.avg_AIperK.clear();// restart data collection when we change triangle count
                    avgLatency=0;
                    selectedRatio = left[0] + ((right[0] - left[0]) / 2);// selectedRatio is MID the selected triangle count ratio for binary search

                    double nextTris = selectedRatio * mInstance.orgTrisAllobj; // the last input is the ratio of current nextTris to the max_total_tris of objects with highest quality
                    //  Part 2:  start to apply the triangle count and OTDA
                    try {
                        //long time1 = System.nanoTime() / 1000000; //starting first loop
                        otdaAlg(nextTris);// this is OTDA algorithm

                        // long t2 = System.nanoTime() / 1000000;
                        // mInstance.t_loop1 = time2 - time1 - (sleepTime * (objC));

                    }

                    catch (InterruptedException e) {
                        e.printStackTrace();
                    }}
                try {
                    sleep(2000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                mInstance.avgq = calculateMeanQuality();
                this.start();
            }

        };

        sceneTimer.start();

    }*/



    public void matchAvgLatency()  {// this is the cycle of writing latency data of static algorithm to the file

        final double[] left = {0.05};
        final double[] right = {1};
        final int[] index = {0};

        CountDownTimer sceneTimer = new CountDownTimer(12000, 2000) {//is  better (50000, 5000) {

            @Override
            public void onTick(long millisUntilFinished) {
                writeRT();// this is to check wether we are close to average latency match?

                perc_error = (double) (Math.round((double) (abs(avgLatency - bys_Avgltcy) * 100) / bys_Avgltcy)) / 100;

            }
            @Override
            public void onFinish() {// at the end of 20 times * 2s data collection, we use binary search for the next ratio


                if(index[0]>=coarse_Ratios.length-1 ||( perc_error <0.05 || selectedRatio ==0.05)){// cause we cannot decimate objects below 5%
                    this.cancel();
                    return;
                }

                index[0] +=1;
                if(index[0]>=coarse_Ratios.length-1)
                {   this.cancel();
                    return;}


                selectedRatio=coarse_Ratios[index[0]];

                mInstance.smL_ratio=selectedRatio;

               // decimateall(selectedRatio);
                double nextTris = selectedRatio*mInstance.orgTrisAllobj;
                try {
                    otdaRevised(nextTris);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
/*
                if(selectedRatio!=1) {// this is because we don't want to change the initial indexes, but the index change is done after the first round trial
                    // Your onFinish logic for the current index here
                    if (avgLatency < bys_Avgltcy)
                        left[0] = selectedRatio;
                    else
                        right[0] = selectedRatio;

                }
                if(left[0]<=right[0]) {
                    mInstance.avg_AIperK.clear();// restart data collection when we change triangle count
                    avgLatency = 0;
                    selectedRatio = (float) (left[0] + ((right[0] - left[0]) / 2));// selectedRatio is MID the selected triangle count ratio for binary search

                    double nextTris = selectedRatio * mInstance.orgTrisAllobj; // the last input is the ratio of current nextTris to the max_total_tris of objects with highest quality
                    //  Part 2:  start to apply the triangle count and OTDA
                    try {
                        //long time1 = System.nanoTime() / 1000000; //starting first loop
                        otdaRevised(nextTris);// this is OTDA algorithm

                        // long t2 = System.nanoTime() / 1000000;
                        // mInstance.t_loop1 = time2 - time1 - (sleepTime * (objC));

                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }*/


                    mInstance.avgq = calculateMeanQuality();
                this.start();
            }

        };
        sceneTimer.start();
    }


    public void decimateall(float ratio){

        mInstance.total_tris =0;
        //  Part 2:  start to apply the triangle count and OTDA

        for (int i =0;i<mInstance.objectCount;i++)
        {// to avoid null pointer error

            mInstance.ratioArray.set(i, ratio);
            int finalI = i;
            mInstance.runOnUiThread(() -> mInstance.renderArray.get(finalI).decimatedModelRequest(mInstance.ratioArray.get(finalI), finalI, false));
            mInstance.total_tris = mInstance.total_tris + (mInstance.ratioArray.get(i) *  mInstance.renderArray.get(i).orig_tris);// total = total + 0.8*objtris
            try {
                Thread.sleep(sleepTime);// added to prevent the crash happens while redrawing all the objects at the same time
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }

    }

    public void staticAlg(double Tratio) throws InterruptedException // this is when we match the qualities and then latency
    {
        staticDelegate();
////        I want to expand staticDelegate here so that when it is done, we go for staticMatchTrisRatio
//        // here is to assign delegates
//        for (int i=0;i<mInstance.mList.size();i++) {
//            AiItemsViewModel taskView = mInstance.mList.get(i);
//            // first find the best offline AI response Time = EXPECTED RESPONSE TIme
//            int indq = mInstance.excel_BestofflineAIname.indexOf(taskView.getModels().get(taskView.getCurrentModel()));// search in excel file to find the name of current object and get access to the index of current object
//            // excel file has all information for the AI inference NAME, Delegate, and time
//            String bestDelg = mInstance.excel_BestofflineAIdelg.get(indq);
//            int model= (taskView.getCurrentModel());
//            int device=taskView.getCurrentDevice();
//            int new_device=findDevice(bestDelg);
//            if(device!=new_device)// this means that the model should be updated
//            {
//                mInstance.adapter.setMList(mInstance.mList);
//                mInstance.recyclerView_aiSettings.setAdapter(mInstance.adapter);
//                int finalI = i;
//                mInstance.runOnUiThread(() ->
//                        mInstance. adapter.updateActiveModel(
//                                model,
//                                new_device,
//                                1,
//                                taskView,
//                                finalI
//                        ));
//            }
//            try {
//                sleep(40);
//            } catch (InterruptedException e) {
//                e.printStackTrace();
//            }// this is to make sure the device is updated
//        }

        selectedRatio=(float)Tratio;// first we run baseline 1
        staticMatchTrisRatio(Tratio);
    }





// returns true if the percentage error is below 20% otherwise it's false
    public void writeRT( ){ // this is to collect the response time of all AIS after changing triangle count each time
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss:SSS");
        String currentFolder = mInstance.getExternalFilesDir(null).getAbsolutePath();
        String FILEPATH = currentFolder + File.separator + "Static_dataCollection"+mInstance.fileseries+".csv";

        StringBuilder sb = new StringBuilder();
        sb.append(dateFormat.format(new Date()));

        double avg_AIlatencyPeriod=0;// this is to calculate sum of each AI model response time per period

        try (PrintWriter writer = new PrintWriter(new FileOutputStream(FILEPATH, true))) {
            for (int i=0;i<mInstance.mList.size();i++)
            {
                AiItemsViewModel taskView=mInstance.mList.get(i);
                // first find the best offline AI response Time = EXPECTED RESPONSE TIme
                int indq = mInstance.excel_BestofflineAIname.indexOf(taskView.getModels().get(taskView.getCurrentModel()));// search in excel file to find the name of current object and get access to the index of current object
                // excel file has all information for the AI inference NAME, Delegate, and time
                double expected_time = mInstance.excel_BestofflineAIRT.get(indq);
                // find the actual response Time
                double meanRt= mInstance.mList.get(i).getTot_rps();
                //double[] t_h = mInstance.getResponseT(i);
                while (meanRt==0) // we wanna get a correct value
                    meanRt= mInstance.mList.get(i).getTot_rps();
                double actual_rpT=meanRt;
                // meanRt = mInstance.getResponseT(aiIndx);// after the objects are decimated
                //meanRt = t_h[0];
                // calculate the latency
                avg_AIlatencyPeriod+=(actual_rpT-expected_time)/actual_rpT;//normalized over curr time this is because we want to have this value minimized

                //avg_AIlatencyPeriod+=(actual_rpT-expected_time);// this is because we want to have this value minimized

////********** bellow line is for the function of finding the offline Response time which I already did,I changed it to calculate the latency
                ///   avg_AIlatencyPeriod=actual_rpT;

                sb.append(",").append(taskView.getModels().get(taskView.getCurrentModel()))
                        .append(",").append(taskView.getDevices().get(taskView.getCurrentDevice()))
                        .append(",").append(actual_rpT) .append(",").append(expected_time).append(",").append(actual_rpT-expected_time);
                //.append( mInstance.avg_reponseT.get(i));
            }

            double avgAIltcy= avg_AIlatencyPeriod/ mInstance.mList.size();
            boolean isempty=mInstance.avg_AIperK.isEmpty();
            if(isempty==false &&  mInstance.avg_AIperK.size()>2)// to check noisy data for more than two data points
            {
                if (avgAIltcy < 1.4 * avgLatency) // this is to remove possible noises
                    mInstance.avg_AIperK.add(avgAIltcy); //this is average of all AI response time at this period
            }
            else
                mInstance.avg_AIperK.add(avgAIltcy);


           // mInstance.avg_AIperK.add(avgAIltcy); //this is average of all AI response time at this period
          //  if( mInstance.avg_AIperK.size()>max_cap)/// we want to have the last updated values
         //       mInstance.avg_AIperK.remove(0);
            if( mInstance.avg_AIperK.size()>max_cap)/// we want to have the last updated values
                mInstance.avg_AIperK.remove(0);


            if(isempty ==false)
                avgLatency = mInstance.avg_AIperK.stream()
                    .mapToDouble(Double::doubleValue)
                    .average()
                    .orElseThrow(() -> new IllegalArgumentException("List is empty"));


            sb.append(",").append(mInstance.total_tris);
            sb.append(",").append(mInstance.avgq);
            sb.append(",").append( avgAIltcy).append(",").append(avgLatency).append(",").append( bys_Avgltcy).append(",").append( perc_error).append(",").append( selectedRatio);
            sb.append('\n');
            writer.write(sb.toString());
            System.out.println("done!");
        } catch (FileNotFoundException e) {
            System.out.println(e.getMessage());
        }



       // return percenT_error;// if large enough,this means still we are not closed to the right total triangle count ratio

    }

    public void tmpDecimate(float ratio){

        mInstance.total_tris =0;
        //  Part 2:  start to apply the triangle count and OTDA

        for (int i =0;i<=2;i+=2)// just for indexes 0 and 2
        {// to avoid null pointer error

            mInstance.ratioArray.set(i, ratio);
            int finalI = i;
            mInstance.runOnUiThread(() -> mInstance.renderArray.get(finalI).decimatedModelRequest(mInstance.ratioArray.get(finalI), finalI, false));
            mInstance.total_tris = mInstance.total_tris + (mInstance.ratioArray.get(i) *  mInstance.renderArray.get(i).orig_tris);// total = total + 0.8*objtris
            try {
                Thread.sleep(sleepTime);// added to prevent the crash happens while redrawing all the objects at the same time
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }

    }

    float otdaRevised(double tUP) throws InterruptedException {// this considers everytime all objects have the highest ratio cause bayesian choices are independant of each other
// don't want the last bayesian change affect the sensitivity of objects
        objC=mInstance.objectCount;
        double []tMin = new double[objC];
        double [] sensitivity = new double[objC];
        double [] tris_share = new double[objC];
        fProfit= new double[objC][coarse_Ratios.length];
        tRemainder= new double[objC][coarse_Ratios.length];
        track_obj= new int[objC][coarse_Ratios.length];
        tUP=tUP+0.1;// it's 1000 tris added to make sure for max goes all 1


        candidate_obj = new HashMap<>();
        Map<Integer, Double> sortedcandidate_obj = new HashMap<>();
        float sum_org_tris = 0; // sum of all tris of the objects o the screen

        for (int ind = 0; ind < mInstance.objectCount; ind++) {

            sum_org_tris += mInstance.renderArray.get(ind).orig_tris;// this will ne used to cal min of tris needed at each row (object) in bellow
            // float r1 = mInstance.ratioArray.get(ind); // current object decimation ratio
            float r1 =1;
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

            mInstance.total_tris = mInstance.total_tris + (coarse_Ratios[j] *  mInstance.renderArray.get(i).orig_tris);// total = total + 0.8*objtris

            if(mInstance.ratioArray.get(i)!=coarse_Ratios[j]) {
                mInstance.ratioArray.set(i, coarse_Ratios[j]);
                mInstance.runOnUiThread(() -> mInstance.renderArray.get(i).decimatedModelRequest(mInstance.ratioArray.get(i), i, false));
                Thread.sleep(sleepTime);// added to prevent the crash happens while redrawing all the objects at the same time
            }

            j = track_obj[i][j];

        }
        return (float)mInstance.total_tris; // this returns the total algorithm triangle count

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
/*
    public void writequality(){


        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        String currentFolder = mInstance.getExternalFilesDir(null).getAbsolutePath();
        String FILEPATH = currentFolder + File.separator + "Quality"+mInstance. fileseries+".csv";

        try (PrintWriter writer = new PrintWriter(new FileOutputStream(FILEPATH, true))) {
           for (int i=0; i<objC-1; i++) {
               double curtris = mInstance.renderArray.get(i).orig_tris * mInstance.ratioArray.get(i);
               double r1 = mInstance.ratioArray.get(i); // current object decimation ratio

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

*/




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
            //      objquality[ind]=quality;
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









}



