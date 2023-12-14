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
/*This code has the class for bayesian idea
* it has two function: run() that does the tests for all AI model delegate combination and selected triangle count
* The  apply_delegate_tris(selected_combinations); function on the other hand applies the selected combination gained from the java server
* which comes from python client
* */
 import java.util.Arrays;

 import java.io.File;
 import java.io.FileOutputStream;
 import java.io.PrintWriter;


 import java.io.FileNotFoundException;
 import java.text.SimpleDateFormat;
 import java.util.Comparator;
 import java.util.Date;
 import java.util.HashMap;
 import java.util.LinkedHashMap;
 import java.util.LinkedList;
 import java.util.List;
 import java.util.Map;
 import java.util.PriorityQueue;
 import java.util.stream.Collectors;

 import static java.lang.Math.abs;

 import android.annotation.SuppressLint;

 import java.util.ArrayList;

 import static java.lang.Math.abs;
import static java.lang.Math.max;
import static java.lang.Math.min;
 import static java.lang.Thread.sleep;

 import android.os.CountDownTimer;
 import android.widget.TextView;

 public class bayesian implements Runnable {
     double avgLatency=0;// this is for the baseline average latency
    private final MainActivity mInstance;
     double meanRt = 0;
     double meanThr = 0;
        boolean isnoisy=false;
    List<Integer> offline_dev=new ArrayList<>(Arrays.asList(0, 1, 2));// get the first device
    float ref_ratio=0.5f;
    int objC;
//    double sensitivity[] ;
////    float objquality[];
//    double tris_share[];
     int max_cap=5;
    Map <Integer, Double> candidate_obj;
    float []coarse_Ratios=new float[]{1f,0.8f, 0.7f , 0.5f,0.3f, 0.2f};
        //    0.05f};
    //ArrayList <ArrayList<Float>> F_profit= new ArrayList<>();
    double [][]fProfit;
    double [][] tRemainder;
    int [][] track_obj;
    int sleepTime=10;
     List<String> modelsToAssign= new ArrayList<>();
  //  double tMin[] ;
    int missCounter=3;//means at least 4 noises

     int curIteration=0;

    public List<double[]> listOfArrays = new ArrayList<>();
    public bayesian(MainActivity mInstance) {

        this.mInstance = mInstance;

        //float candidate_obj[] = new float[total_obj];
//        tMin = new double[objC];

        listOfArrays.add(new double[] { 0.0, 0.7, 0.3, 0.335 });
        listOfArrays.add(new double[] { 0.0, 0.0, 1.0, 0.435 });
        listOfArrays.add(new double[] { 0.0, 1.0, 0.0, 0.265 });


        //= Arrays.asList("AI1", "AI2", "AI3"); // List of model names to assign

        for (int i = 0; i < mInstance.mList.size(); i++) {// we have each list of digits such as 000 or 003 with size of tasks, so if 00 is te digit, the task count is 2

            AiItemsViewModel taskView = mInstance.mList.get(i);// the first to the last task
            modelsToAssign.add(taskView.getModels().get(taskView.getCurrentModel()));// adds the name of the model to modelsToAssign
        }




    }
    @SuppressLint("SuspiciousIndentation")
    @Override

    public void run() {// this function is for the bayesian test- dta collection to test all the combinations for aI delegates

     /*
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

//        mInstance.avgq= calculateMeanQuality();
*/

            }



    public void offlineAIdCol() {// this function is for the bayesian test- dta collection to test all the combinations for aI delegates

        if (offline_dev.size() > 0) {
            // first we assign the first delegate here and the rest in on Finish function of the countdown Timer
           int new_device = offline_dev.remove(0); // should be CPU
            mInstance.avg_AIperK.clear();// restart it for the next delegate
            AiItemsViewModel taskView = mInstance.mList.get(0);// the first to the last task
            int model = (taskView.getCurrentModel());
            int device = taskView.getCurrentDevice();

            String modelName= taskView.getModels().get(model);
            Boolean compatibility = checkCompatibility(modelName, new_device);

            if(compatibility) {
                if (device != new_device)// this means that the model should be updated
                {
                    mInstance.adapter.setMList(mInstance.mList);
                    mInstance.recyclerView_aiSettings.setAdapter(mInstance.adapter);
                    mInstance.runOnUiThread(() ->
                            mInstance.adapter.updateActiveModel(
                                    model,
                                    new_device,
                                    1,
                                    taskView,
                                    0
                            ));
                }
            }
            else // TRY NEXT DELEGATE OOOOOORRRR TERMINATE the function
            {
                if (offline_dev.size() > 0) {// go for the next delegate
                    offlineAIdCol();// call the function again to apply the next Delegate
                } else
                    return;
            }
            ///test I'm working on this as of now
            // this is to apply 3 diff delegates to an AI task and each time
            // collect 5 period of AI inference data and at the end calculate the average and write it to
            CountDownTimer sceneTimer = new CountDownTimer(30000, 3000){
//                    (10000, 5000){
                    //oroginal was this (60000, 3000) {//is  better (50000, 5000) {
                @Override
                public void onTick(long millisUntilFinished) {
                    writeRT(); // // this is to collect the response time of an AI after change in the AI delegate
                }

                @Override
                public void onFinish() {

                    // Your onFinish logic for the current index here
                    double average_AIRT = mInstance.avg_AIperK.stream()
                            .mapToDouble(Double::doubleValue)
                            .average()
                            .orElseThrow(() -> new IllegalArgumentException("List is empty"));


                    writeStaticAI(average_AIRT, taskView); // this is to write the avg result to the excel file

                    mInstance.avg_reponseT.replaceAll(s -> 0d);
                    if (offline_dev.size() > 0) {// go for the next delegate

                        offlineAIdCol();// call the function again to apply the next Delegate
                    }

                    // here we've done delegate assignment and triangle change now want to restart the responseT of all tasks to start data collection
                }// on finish
            };// tmer

            // Start the timer for the given index
            sceneTimer.start();

        }
    }


    public boolean checkCompatibility(String modelName, int new_device){


        if( (modelName.equals("segmentor") || modelName.equals("efficientdet-lite0")  ||modelName.equals("deeplabv3" ))  && new_device==2)// these models don't work for Google PIXEl on NNAPI
            return false;

        return true;
    }

   public List<Integer> old_delegate_capacity(double []selected_combinations){ // this function is to calculate the capacity of each delegate based on input % usage which is discrete (0.7,0.3,0) for example

        List<Integer> capacity=new ArrayList<>();
        int task_num=mInstance.mList.size();
        int cpu_cap=(int) Math.round(selected_combinations[0]*task_num);
       int gpu_cap=(int) Math.round(selected_combinations[1]*task_num);
       int nn_cap=(int) Math.round(selected_combinations[2]*task_num);
        capacity.addAll(List.of(cpu_cap,gpu_cap,nn_cap));

       return  capacity;
   }
// I don't this function to avoid over heating the phone with this computation, instead I add the functio nin the python client
     public List<Integer> delegate_capacity(double []selected_combinations) { // this function is to calculate the capacity of each delegate based on the continuos input % usage/ percentage usage
         int task_num = mInstance.mList.size();
         double[] percentageVector = {selected_combinations[0], selected_combinations[1], selected_combinations[2]};


         Integer[] sortedIndices = new Integer[percentageVector.length];
         for (int i = 0; i < percentageVector.length; i++) {
             sortedIndices[i] = i;
         }

         Arrays.sort(sortedIndices, Comparator.comparingDouble(i -> -percentageVector[i]));

         List<Integer> scaledValues = new ArrayList<>();

         for (int i = 0; i < percentageVector.length; i++) {
             scaledValues.add((int) (percentageVector[i] * task_num));
         }
         // Adjust the values to ensure they sum up to N
         int remainder = task_num - scaledValues.stream().mapToInt(Integer::intValue).sum();

         // Distribute the remainder evenly among the values
         for (int i = 0; i < task_num; i++) {
             int index = sortedIndices[i];
             scaledValues.set(index, scaledValues.get(index) + 1);
             remainder--;
             if (remainder <= 0) {
                 break;
             }
         }

    return scaledValues;

     }

    public void fifo_heuristic(List<Integer> capacity){

        for (int i = 0; i < mInstance.mList.size(); i++) {// we have each list of digits such as 000 or 003 with size of tasks, so if 00 is te digit, the task count is 2

            AiItemsViewModel taskView = mInstance.mList.get(i);// the first to the last task
            int model = (taskView.getCurrentModel());
            int device = taskView.getCurrentDevice();
            // int new_device=heauristic(i,capacity);
            int new_device = -1;
            for (int j = 0; j < capacity.size(); j++)//  This is my FIFO heuristic function: up to three delegates we have
            {
                if (capacity.get(j) != 0) {
                    new_device = j;// use the first available device
                    capacity.set(j, capacity.get(j) - 1);// update the capacity
                    break;// be out of the loop
                }

            }
            if (device != new_device)// this means that the model should be updated
            {
                mInstance.adapter.setMList(mInstance.mList);
                mInstance.recyclerView_aiSettings.setAdapter(mInstance.adapter);
                int finalI = i;
                int finalNew_device = new_device;
                mInstance.runOnUiThread(() ->
                        mInstance.adapter.updateActiveModel(
                                model,
                                finalNew_device,
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



    public void afinity_heuristic(List<Integer> capacity){


        List<AIModel> copuCurModels = new ArrayList<>();
        copuCurModels.addAll(mInstance.curModels);// this is copy of all models currently running
        List<AiItemsViewModel> copyAiItems = new ArrayList<>();
        copyAiItems.addAll(mInstance.mList);// this is copy of all models currently running
        int delegatedM=mInstance.mList.size();// num of current tasks
        PriorityQueue<AIModel> sortedCurModels = new PriorityQueue<>(copuCurModels);// to make sure current models are sorted based on their avg infTime
        while(delegatedM!=0){// do this till all tasks are assingned t their best delegate
            AiItemsViewModel taskView = null;
            AIModel assignedModel = sortedCurModels.poll();
            int bestDlg=assignedModel.delegate;
            for (AiItemsViewModel item : copyAiItems) {
                if (item.getID()==assignedModel.getID()){
           //     getModels().get(item.getCurrentModel()).equals( assignedModel.name)) {
                    taskView = item;
                    break;
                }
            }
            int tasksIndx = mInstance.mList.indexOf(taskView);
            if(capacity.get(bestDlg) !=0)// you can easily assing the task and update delgate
            {
                delegatedM-=1;
                capacity.set(bestDlg, capacity.get(bestDlg)-1);
                copyAiItems.remove(taskView);
                // assign the task
                if (bestDlg != taskView.getCurrentDevice())// this means that the model should be updated
                {
                    mInstance.adapter.setMList(mInstance.mList);
                    mInstance.recyclerView_aiSettings.setAdapter(mInstance.adapter);
                    int finalI = tasksIndx;
                    int finalNew_device = bestDlg;
                    AiItemsViewModel finalTaskView = taskView;
                    mInstance.runOnUiThread(() ->
                            mInstance.adapter.updateActiveModel(
                                    finalTaskView.getCurrentModel() ,
                                    finalNew_device,
                                    1,
                                    finalTaskView,
                                    finalI// this is the index of mlist
                            ));
                }
                try {
                    sleep(30);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }// this is to make sure the device is updated
                sortedCurModels.removeIf(modl -> modl.id==(assignedModel.id));
            }
            else{// capacity is zero , so remove all AIs from sorted list with the same capacity index
                sortedCurModels.removeIf(modl -> modl.delegate.equals(bestDlg));
            }
        }

        startSceneTimer();// this has the loop of i=0 to i=3 in it

        }// function




    public void apply_delegate_tris(double []selected_combinations1) {

           mInstance.avg_AIperK.clear();// restart data collection
           mInstance.last_latencyInstanceN =0;
        // FIRST APPLY YHE DELEGATE AND TRIANGLES
// here we apply the delegate at the end of 10 times of 10 data collection
            double[] selected_combinations = selected_combinations1;
            // I commented below to receive the correct discrete from python, ht ereason is here cimputation s make the phone hot

            //List<Integer> capacity = new ArrayList<>(delegate_capacity(Arrays.copyOfRange(selected_combinations, 0, selected_combinations.length - 1)));// exclude triangles

        double selectedTRatio= selected_combinations[selected_combinations.length - 1];
        double nextTris =selectedTRatio*mInstance.orgTrisAllobj; // the last input is the ratio of current nextTris to the max_total_tris of objects with highest quality
        curIteration=mInstance.curBysIters;
        mInstance.bysTratioLog.set(curIteration,selectedTRatio );

        // = Arrays.copyOfRange(selected_combinations, 0, selected_combinations.length - 1);
        List<Integer> capacity = new ArrayList();

        for (double value : selected_combinations) {
            capacity.add((int) value); // Cast each element of A to an integer
        }

        capacity.remove(3); // remove the triangle

           //fifo_heuristic(capacity); // this prioritizes AI model delegate to CPU. GPU, and NNAPI based on the AI index, eg, if capacity ary=[0.3,0,0.7], the first AI model is assigned to CPU
         //  afinity_heuristic(capacity); I expand the function afinity_heuristic here instead because we wanna have both aI and triangle count adjustment done sequentially

        List<AIModel> copuCurModels = new ArrayList<>();
        copuCurModels.addAll(mInstance.curModels);// this is copy of all models currently running
        List<AiItemsViewModel> copyAiItems = new ArrayList<>();
        copyAiItems.addAll(mInstance.mList);// this is copy of all models currently running
        int delegatedM=mInstance.mList.size();// num of current tasks
        PriorityQueue<AIModel> sortedCurModels = new PriorityQueue<>(copuCurModels);// to make sure current models are sorted based on their avg infTime
        while(delegatedM!=0){// do this till all tasks are assingned t their best delegate
            AiItemsViewModel taskView = null;
            AIModel assignedModel = sortedCurModels.poll();
            int bestDlg=assignedModel.delegate;
            for (AiItemsViewModel item : copyAiItems) {
                if (item.getID()==assignedModel.getID()){
                    //     getModels().get(item.getCurrentModel()).equals( assignedModel.name)) {
                    taskView = item;
                    break;
                }
            }
            int tasksIndx = mInstance.mList.indexOf(taskView);
            if(capacity.get(bestDlg) !=0)// you can easily assing the task and update delgate
            {
                delegatedM-=1;
                capacity.set(bestDlg, capacity.get(bestDlg)-1);
                copyAiItems.remove(taskView);
                // assign the task
                if (bestDlg != taskView.getCurrentDevice())// this means that the model should be updated
                {
                    mInstance.adapter.setMList(mInstance.mList);
                    mInstance.recyclerView_aiSettings.setAdapter(mInstance.adapter);
                    int finalI = tasksIndx;
                    int finalNew_device = bestDlg;
                    AiItemsViewModel finalTaskView = taskView;
                    mInstance.runOnUiThread(() ->
                            mInstance.adapter.updateActiveModel(
                                    finalTaskView.getCurrentModel() ,
                                    finalNew_device,
                                    1,
                                    finalTaskView,
                                    finalI// this is the index of mlist
                            ));
                }
                try {
                    sleep(30);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }// this is to make sure the device is updated
                sortedCurModels.removeIf(modl -> modl.id==(assignedModel.id));
            }
            else{// capacity is zero , so remove all AIs from sorted list with the same capacity index
                sortedCurModels.removeIf(modl -> modl.delegate.equals(bestDlg));
            }
        }


        //  Part 2:  start to apply the triangle count and OTDA
        try {
            long time1 = System.nanoTime() / 1000000; //starting first loop
            // nextTris=0.435;
            otdaRevised(nextTris);// this is OTDA algorithm
            //odraAlg(nextTris);// this is OTDA algorithm
            long time2 = System.nanoTime() / 1000000;
            // long t2 = System.nanoTime() / 1000000;
            mInstance.t_loop1 = time2 - time1 - (sleepTime * (objC));
            // mInstance.t_loop2 = t2 - t1;
            mInstance.lastConscCounter = 0;// we let the effect of change in triangle count stand for at least 4 times by reseting this counter. if you don't reset, by any chance new re might be <08 and then the orda happens again

        } catch (InterruptedException e) {
            e.printStackTrace();
        }
//        mInstance.avgq = calculateMeanQuality();
        // end


        //  I expand the function afinity_heuristic here instead because we wanna have both aI and triangle count adjustment done sequentially

///*temp deactive to make sure of heuristic func works


        // Start the timer for COLLECTING DATA OF ai INFERENCE
        startSceneTimer();// this has the loop of i=0 to i=3 in it



    }

    void startSceneTimer() { // THIS IS TO APPLY JUST ONE INSTANCE OF DELEGATE AND CALCULATE REWARD AT THE END
       // original CountDownTimer sceneTimer = new CountDownTimer(21000, 3000) {// 25 TIMES (50000/2000) DATA COLLECTION EVERY 5S ,
      // original of Dec 2023 tests CountDownTimer sceneTimer = new CountDownTimer(18000, 2000) {// 25 TIMES (50000/2000) DATA COLLECTION EVERY 5S ,
        CountDownTimer sceneTimer = new CountDownTimer(6000, 1500) {
            @Override
            public void onTick(long millisUntilFinished) {
                writeRT();
            }

            @Override
            public void onFinish() {
                avgLatency= (double) (Math.round((double) (avgLatency * 1000))) / 1000;
                // this is to include fixed possible noise over the first 7 iterations
                double reward=0;


                if(mInstance.bys_baseline1_2)// this is for HBO compared to the frist two baselines
                 reward =mInstance. avgq - (mInstance.reward_weight*avgLatency);

                else // this is for baseline #3 without triangle count change
                    reward=- (avgLatency);// I don't use weight here becuase there is no use

               // reward=(double) (Math.round((double) (reward * 10000))) / 10000;



                if(mInstance.curBysIters ==0 ||mInstance.curBysIters ==1 )
                    reward-=0.18;
                else
                    if(mInstance.curBysIters <4 )
                    reward-=0.1;
                    //reward-=0.15;

                reward=(double) (Math.round((double) (reward * 1000))) / 1000;
                mInstance.avg_reward=   reward;
//                TextView posText_app_hbo = (TextView)mInstance. findViewById(R.id.app_bt);
//                posText_app_hbo.setText("B_t: "+ Double.toString(reward));

                mInstance.bysRewardsLog.set(curIteration,reward);
                mInstance.bysAvgLcyLog.set(curIteration,avgLatency);

              //  send_thread connectionThread = new send_thread(reward);
              //   connectionThread.start();
                System.out.println("reward is "+ reward);
            }
        };

        // Start the timer for the given index
        sceneTimer.start();
    }

    public void apply_delegate_trismANUAL(double []selected_combinations1, int ind) { // THIS TRIGGERS startSceneTimermAUNUAL


        // Start the timer for the first index
        startSceneTimer();// this has the loop of i=0 to i=3 in it


    }

    /*
    void startSceneTimermAUNUAL(int in) {// THIS IS FOR MANUALL DELEGATE IF YOU HAVE AN ARRAY OF 53 INSTANCES TO APPLY
        CountDownTimer sceneTimer = new CountDownTimer(8000, 4000) {//is  better (50000, 5000) {

            int ind = in;

            @Override
            public void onTick(long millisUntilFinished) {
                writeRT();

            }

            @Override
            public void onFinish() {

                // Your onFinish logic for the current index here
                double average_AIRT = mInstance.avg_AIperK.stream()
                        .mapToDouble(Float::doubleValue)
                        .average()
                        .orElseThrow(() -> new IllegalArgumentException("List is empty"));

                double reward =mInstance. avgq - average_AIRT;
                mInstance.avg_reward=reward;
                //  send_thread connectionThread = new send_thread(reward);
                //   connectionThread.start();
                System.out.println("reward is "+ reward);

                if(ind<=2) {

// here we apply the delegate at the end of 10 times of 10 data collection
                    double[] selected_combinations = listOfArrays.get(ind);
                    List<Integer> capacity = new ArrayList<>(delegate_capacity(Arrays.copyOfRange(selected_combinations, 0, selected_combinations.length - 1)));// exclude triangles
                    // this loop is to change the AI task delegate for one combination: one by one
                    // Part 1: this is to apply delegate

                    //     temp commented to just check tris delegate part after this
                    for (int i = 0; i < mInstance.mList.size(); i++) {// we have each list of digits such as 000 or 003 with size of tasks, so if 00 is te digit, the task count is 2

                        AiItemsViewModel taskView = mInstance.mList.get(i);// the first to the last task
                        int model = (taskView.getCurrentModel());
                        int device = taskView.getCurrentDevice();
                        // int new_device=heauristic(i,capacity);
                        int new_device = -1;
                        for (int j = 0; j < capacity.size(); j++)//  This is my FIFO heuristic function: up to three delegates we have
                        {
                            if (capacity.get(j) != 0) {
                                new_device = j;// use the first available device
                                capacity.set(j, capacity.get(j) - 1);// update the capacity
                                break;// be out of the loop
                            }

                        }

                        if (device != new_device)// this means that the model should be updated
                        {
                            mInstance.adapter.setMList(mInstance.mList);
                            mInstance.recyclerView_aiSettings.setAdapter(mInstance.adapter);

//                            mInstance.runOnUiThread(() -> mInstance.renderArray.get(i).decimatedModelRequest(mInstance.ratioArray.get(i), i, false));
                            int finalI = i;
                            int finalNew_device = new_device;
                            mInstance.runOnUiThread(() ->
                                    mInstance.adapter.updateActiveModel(
                                            model,
                                            finalNew_device,
                                            1,
                                            taskView,
                                            finalI
                                    ));
                        }
                        try {
                            sleep(20);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }// this is to make sure the device is updated

                    }// for all AI models to apply its delegate
                    //

                    double nextTris = selected_combinations[selected_combinations.length - 1];
                    //  Part 2:  start to apply the triangle count and OTDA
                    try {
                        long time1 = System.nanoTime() / 1000000; //starting first loop
                        // nextTris=0.435;
                        odraAlg(nextTris);// this is OTDA algorithm
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


                    double algNxtTris = mInstance.total_tris;
                    mInstance.avgq = calculateMeanQuality();
                    // end
                }
                ind++;
                if (ind <=3) {
                    // Start the timer for the next index
                    startSceneTimer(ind);
                }
            }
        };

        // Start the timer for the given index
        sceneTimer.start();
    }
*/
    float otdaRevised(double tUP) throws InterruptedException {// this considers everytime all objects have the highest ratio cause bayesian choices are independant of each other
// don't want the last bayesian change affect the sensitivity of objects
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

    public void writeStaticAI(double average, AiItemsViewModel taskView){ // this is to write AI average
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss:SSS");
        String currentFolder = mInstance.getExternalFilesDir(null).getAbsolutePath();
        String FILEPATH = currentFolder + File.separator + "StaticAIinference"+".csv";

        StringBuilder sb = new StringBuilder();
        sb.append(dateFormat.format(new Date())+","+taskView.getModels().get(taskView.getCurrentModel())+","+taskView.getDevices().get(taskView.getCurrentDevice()) + ","+average+"\n");

        try (PrintWriter writer = new PrintWriter(new FileOutputStream(FILEPATH, true))) {

            writer.write(sb.toString());
            System.out.println("done!");
        } catch (FileNotFoundException e) {
            System.out.println(e.getMessage());
        }
    }




    public void writeRT( ){ // this is to collect the response time of all AIS after change in the AI device

        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss:SSS");
        String currentFolder = mInstance.getExternalFilesDir(null).getAbsolutePath();
        String FILEPATH = currentFolder + File.separator + "Bayesian_dataCollection"+mInstance.fileseries+".csv";

        StringBuilder sb = new StringBuilder();
        sb.append(dateFormat.format(new Date()));
        
        double avg_AIlatencyPeriod=0;// this is to calculate sum of each AI model response time per period
        mInstance.avgq = calculateMeanQuality();

        try (PrintWriter writer = new PrintWriter(new FileOutputStream(FILEPATH, true))) {
            for (int i=0;i<mInstance.mList.size();i++)
            {


                AiItemsViewModel taskView=mInstance.mList.get(i);

                // first find the best offline AI response Time = EXPECTED RESPONSE TIme
                int indq = mInstance.excel_BestofflineAIname.indexOf(taskView.getModels().get(taskView.getCurrentModel()));// search in excel file to find the name of current object and get access to the index of current object
                // excel file has all information for the AI inference NAME, Delegate, and time
                double expected_time = mInstance.excel_BestofflineAIRT.get(indq);
                // find the actual response Time

                meanRt = mInstance.mList.get(i).getTot_rps();
                //double[] t_h = mInstance.getResponseT(i);

                while (meanRt==0) // we wanna get a correct value
                    meanRt= mInstance.mList.get(i).getTot_rps();

                double actual_rpT=meanRt;

                // meanRt = mInstance.getResponseT(aiIndx);// after the objects are decimated
                //meanRt = t_h[0];
                // calculate the latency
                avg_AIlatencyPeriod+=(actual_rpT-expected_time)/actual_rpT;//normalized over curr time this is because we want to have this value minimized

                double cur_latency=actual_rpT-expected_time;
                double tmp_lastLatency=mInstance.last_latencyInstanceN;
///* tmp deactivate maybe not necessary
               // if(i==mInstance.mList.size()-1 && tmp_lastLatency!=0)
               if(i==mInstance.mList.size()-1 && tmp_lastLatency!=0)// we check atleast one instance to make sure our latency is not noisy
                {
                    if(cur_latency/tmp_lastLatency >1.4 &&  mInstance.avg_AIperK.size()>2)
                        isnoisy=true;
                    else
                        isnoisy=false;
                }

                if(cur_latency<0)
                    isnoisy=true;

                //if(i==mInstance.mList.size()-1 && isnoisy==false)// update last latency
                if(i==mInstance.mList.size()-1 && isnoisy==false)// update last latency
                   mInstance.last_latencyInstanceN =cur_latency;

////********** bellow line is for the function of finding the offline Response time which I already did,I changed it to calculate the latency
             ///   avg_AIlatencyPeriod=actual_rpT;
                
                sb.append(",").append(taskView.getModels().get(taskView.getCurrentModel()))
                        .append(",").append(taskView.getDevices().get(taskView.getCurrentDevice()))
                        .append(",").append(actual_rpT) .append(",").append(expected_time).append(",").append(cur_latency);
                        //.append( mInstance.avg_reponseT.get(i));
            }

// avg_AIperK is to calculate average of all AI model  response time  per period
            double avgAIltcy= avg_AIlatencyPeriod/ mInstance.mList.size();
            boolean isempty=mInstance.avg_AIperK.isEmpty();
            if(isempty==false &&  mInstance.avg_AIperK.size()>2)// to check noisy data for more than two data points
            {
                if (isnoisy==false )
                        //&& avgAIltcy < 1.4 * avgLatency) // this is to remove possible noises
                    mInstance.avg_AIperK.add(avgAIltcy); //this is average of all AI response time at this period
            }
            else
                 mInstance.avg_AIperK.add(avgAIltcy);

            if( mInstance.avg_AIperK.size()>max_cap)/// we want to have the last updated values
                mInstance.avg_AIperK.remove(0);

            if(isempty==false)
                 avgLatency = mInstance.avg_AIperK.stream()
                    .mapToDouble(Double::doubleValue)
                    .average()
                    .orElseThrow(() -> new IllegalArgumentException("List is empty"));

            sb.append(",").append(mInstance.total_tris);
            sb.append(",").append(mInstance.avgq);
            sb.append(",").append( avgAIltcy).append(",").append(avgLatency).append(",").append( mInstance.curBysIters+1).append(",").append( mInstance.reward_weight);

            sb.append('\n');
            writer.write(sb.toString());
            System.out.println("done!");
        } catch (FileNotFoundException e) {
            System.out.println(e.getMessage());
        }
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












//}



