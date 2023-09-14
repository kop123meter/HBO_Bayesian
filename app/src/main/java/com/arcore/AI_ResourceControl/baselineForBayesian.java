
package com.arcore.AI_ResourceControl;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static java.lang.Math.abs;
import static java.lang.Thread.sleep;

public class baselineForBayesian implements Runnable {// baseline for MIR


    private final MainActivity mInstance;
    float ref_ratio=0.5f;
    int objC;
    Map <Integer, Double> candidate_obj;
    float []coarse_Ratios=new float[]{1f,0.8f, 0.6f , 0.4f, 0.2f,0.1f,0.05f};
    //    0.05f};
    //ArrayList <ArrayList<Float>> F_profit= new ArrayList<>();
    double [][]fProfit;
    double [][] tRemainder;
    int [][] track_obj;
    int sleepTime=15;





    public baselineForBayesian(MainActivity mInstance) {

        this.mInstance = mInstance;




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
    public void staticMatchTris(double tUPRatio){
        ///// here is to just adjust the quality of objects:

        double nextTris = tUPRatio*mInstance.orgTrisAllobj; // the last input is the ratio of current nextTris to the max_total_tris of objects with highest quality
        //  Part 2:  start to apply the triangle count and OTDA
        try {
            long time1 = System.nanoTime() / 1000000; //starting first loop
            // nextTris=0.435;
            otdaAlg(nextTris);// this is OTDA algorithm
            //odraAlg(nextTris);// this is OTDA algorithm

            long time2 = System.nanoTime() / 1000000;
            // long t2 = System.nanoTime() / 1000000;
            mInstance.t_loop1 = time2 - time1 - (sleepTime * (objC));
            // mInstance.t_loop2 = t2 - t1;
            mInstance.lastConscCounter = 0;// we let the effect of change in triangle count stand for at least 4 times by reseting this counter. if you don't reset, by any chance new re might be <08 and then the orda happens again

            if (nextTris != mInstance.total_tris && !mInstance.decTris.contains(mInstance.total_tris)) // if next tris is lower than total tris we have decimation
                mInstance.decTris.add(mInstance.total_tris);// add new total triangle count in the decimated list

        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        mInstance.avgq = calculateMeanQuality();

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
                sleep(10);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }// this is to make sure the device is updated


        }


    }



    float otdaAlg(double tUP) throws InterruptedException {

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
            mInstance.runOnUiThread(() -> mInstance.renderArray.get(i).decimatedModelRequest(mInstance.ratioArray.get(i), i, false));
            mInstance.total_tris = mInstance.total_tris + (mInstance.ratioArray.get(i) *  mInstance.renderArray.get(i).orig_tris);// total = total + 0.8*objtris
            // mInstance.trisDec.put(mInstance.total_tris,true);
            j = track_obj[i][j];
            Thread.sleep(sleepTime);// added to prevent the crash happens while redrawing all the objects at the same time
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



